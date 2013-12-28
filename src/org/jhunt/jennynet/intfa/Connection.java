package org.jhunt.jennynet.intfa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.jhunt.jennynet.core.ConnectionParameters;

public interface Connection {

   /** Returns a record containing the set of parameters currently operative 
    * for this connection. The returned instance of <code>ConnectionParameters</code> 
    * is operational for parameter modifications on this connection.
    *  
    * @return IConnectionParameters
    */
   public ConnectionParameters getParameters();

   /** Copies a sets of parameters instructional for this connection.
    * It may not be assumed that the given <code>ConnectionParameters</code> 
    * instance becomes operational for this connection. 
    * Method is only valid if this connection is not  
    * currently connected, otherwise an exception is thrown.
    *  
    * @param parameters IConnectionParameters
    * @throws NullPointerException if parameter is null
    * @throws IllegalStateException if connection is connected
    * @throws IOException if some directory setting doesn't work
    */
   public void setParameters(ConnectionParameters parameters) throws IOException;

   /** Returns the serialisation manager for the outgoing stream 
    * of this connection.
    * Modifications performed on the returned manager stay local
    * to this connection.
    *  
    * @return Serialization
    */
   public Serialization getSendSerialization();

   /** Returns the serialisation manager for the incoming stream 
    * of this connection.
    * Modifications performed on the returned manager stay local
    * to this connection.
    *  
    * @return Serialization
    */
   Serialization getReceiveSerialization();

   /** Returns the unique ID of this connection.
    *  
    * @return UUID technical connection identifier
    */
   public UUID getUUID ();

   /** Returns 4 bytes of a short identifier value for this connection
    * based on its UUID value.
    * 
    * @return byte[] (4 bytes)
    */
   public byte[] getShortId ();
   
   /** Whether this Connection is connected to the remote end. 
    * Note that a connection can become disconnected at any time.
    * A disconnection is indicated at the event listeners.
    * 
    *  @return boolean connected status
    */
   public boolean isConnected();

   /** Whether this Connection is closed. A closed connection cannot be reused. 
    * 
    *  @return boolean closed status
    */
   boolean isClosed();

   /** Whether this connection is currently engaged in data transmission
    * (reading or writing) over the net. The fall down of this flag has a
    * latency of 2 seconds.
    * 
    *  @return boolean transmission status
    */
   public boolean isTransmitting ();

   /** Returns the time point (epoch time) of the last send event
    * occurring on the socket.
    * 
    * @return time in milliseconds
    */
   public long getLastSendTime ();
   
   /** Returns the time point (epoch time) of the last receive event
    * occurring on the socket.
    * 
    * @return time in milliseconds
    */
   public long getLastReceiveTime ();
   
   /** Sends the given serialisable Object over the network in the normal
    * transmission priority class.
    * The object's class has to be registered for transmission otherwise
    * an exception is thrown.
    * 
    * @param object Object serialisable object
    * @returns long object ID number
    * @throws UnregisteredObjectException if parameter object is not registered
    *         for transmission
    * @throws IllegalStateException if the send queue was full (rejected)
    * @throws NullPointerException if parameter is null
    */
   public long sendObject (Object object);

   /** Sends the given serialisable Object over the network with an
    * option for the priority of transmission.
    * The object's class has to be registered for transmission otherwise
    * an exception is thrown.
    * 
    * @param object Object serialisable object
    * @param priority boolean true == high priority, false == normal priority
    * @returns long object ID number
    * @throws UnregisteredObjectException if parameter object is not registered
    *         for transmission
    * @throws IllegalStateException if the send queue was full (rejected)
    * @throws NullPointerException if parameter is null
    */
   public long sendObject (Object object, boolean priority);

   /** Transfers a file to the remote station. Files can be scheduled
    * with a length up to 2.1 GB (Integer.MAX_VALUE). The returned file
    * identifier number is referenced with transfer events concerning
    * the posted file. File transfers always run in the lowest transmission 
    * priority class. (For details on events and handling see the manual page
    * for file transfers.)
    * 
    * @param file File the file to be sent
    * @returns long file ID number
    * @param remotePath String path information for the remote station
    * @throws FileNotFoundException if the file cannot be found or read
    * @throws FileInTransmissionException if the file is in ongoing transmission
    * @throws IllegalFileLengthException if maximum file length is exceeded
    * @throws NullPointerException if file parameter is null
    * @throws IOException 
    */
   public long sendFile (File file, String remotePath) throws IOException;

   /** Sends the given block of data over the network.
    * <p><small>A standard internal class (<i>JennyNetByteBuffer</i>) is used 
    * to represent the given block at the remote endpoint, signalling it as 
    * incoming object in the same manner as serialised objects are received.</small>    
    * 
    * @param buffer byte[] data buffer
    * @param start int buffer offset of data to be sent
    * @param length int length of data to be sent
    * @param priority boolean true == high priority, false == normal priority
    * @returns long object ID number
    * @throws IllegalArgumentException if data addressing is wrong
    * @throws IllegalStateException if the send queue was full (rejected)
    * @throws NullPointerException if parameter is null
    */ 
   public long sendData (byte[] buffer, int start, int length, boolean priority);

   /** Sends a PING to the remote station. The corresponding PING-ECHO 
    * is indicated as a reception event to listeners. (PINGs have their
    * own name space.)
    * 
    * @return long PING identifier number
    */
   public long sendPing ();

   /** Attempts to set the sending TEMPO on both ends of the connection
    * to match the given baud rate, where possible. This is useful to 
    * slow down the transmission rate of a particular connection.
    * 
    * @param baud int
    */
   public void setTempo (int baud);
   
   /** Fatally terminates an incomplete transmission of an object or
    * a file. The layer discriminates incoming and outgoing name spaces
    * for objects (including files). The name space is represented here 
    * as an integer direction parameter. Does nothing if there is no 
    * transmission found for the given id.
    * <p><small>The practical value of this lies in rejecting or
    * aborting an incoming file transmission which was indicated as connection
    * event. Outgoing transmissions are identified by a long integer value which
    * is returned by the <code>sendObject()</code> or <code>sendFile()</code>
    * methods.</small>
    * 
    * @param objectID long identifier number for transmitted object (or file)
    * @param direction int 0 = incoming, 1 = outgoing
    */
   public void breakTransfer (long objectID, int direction);
   
   /** Closes this connection (including an event for listeners).
    */
   public void close();

   /** Adds a listener to communication events of the network interface. 
    * If the listener already exists, it is not added again.
    * 
    *  @param ConnectionListener event listener
    */
   public void addListener (ConnectionListener listener);

   /** Removes a listener to user relevant communication events. 
    * 
    *  @param ConnectionListener event listener
    */
   public void removeListener (ConnectionListener listener);

   /** Returns the IP address and port of the remote endpoint of this
    *  TCP connection, or null if this is undefined.
    *  
    *  @return InetSocketAddress remote address
    */
   public InetSocketAddress getRemoteAddress();

   /** Returns the IP address and port of the local endpoint 
    *  of this TCP connection, or null if this is undefined.
    *  
    *  @return InetSocketAddress local address
    */
   public InetSocketAddress getLocalAddress();

   /** Sets the human friendly name for this connection.
    *  
    * @param name String readable connection name
    */
   public void setName(String name);

   /** Returns the human friendly name of this connection or the empty string
    * if undefined.
    * 
    * @return String name of connection (may be empty)
    */
   public String getName ();
   
   /** Returns the human readable name of this connection followed by a
    * printout of its Internet IP address and port number.
    * 
    * @return String 
    */
   public String toString();

   /** If the percent of the TCP write buffer that is filled is less than the specified threshold,
    * {@link Listener#idle(Connection)} will be called for each network thread update. Default is 0.1. */
//   public void setIdleThreshold(float idleThreshold);

   /** @see #setIdleThreshold(float) */
// public boolean isIdle();

   /** Requests the connection to communicate with the remote computer to determine a new value for the
    * {@link #getReturnTripTime() return trip time}. When the connection receives a {@link FrameworkMessage.Ping} object with
    * {@link Ping#isReply isReply} set to true, the new return trip time is available. 
   public void updateReturnTripTime();
    */

   /** Returns the last calculated TCP return trip time, or -1 if {@link #updateReturnTripTime()} has never been called or the
    * {@link FrameworkMessage.Ping} response has not yet been received. 
   public int getReturnTripTime();
   */
}