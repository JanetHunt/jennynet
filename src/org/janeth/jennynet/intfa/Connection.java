package org.janeth.jennynet.intfa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.UUID;

import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.exception.FileInTransmissionException;
import org.janeth.jennynet.exception.IllegalFileLengthException;
import org.janeth.jennynet.exception.UnregisteredObjectException;

/**
 * Set of methods that characterise, build and use the features of a JennyNet
 * network connection.
 */
public interface Connection {

   /** Returns a record containing the set of parameters currently operative 
    * for this connection. The returned instance of <code>
    * ConnectionParameters</code> is operational for parameter modifications
    * on this connection.
    *  
    * @return <code>ConnectionParameters</code>
    */
   public ConnectionParameters getParameters();

   /** Copies a sets of parameters to become instructive for this connection.
    * It may not be assumed that the given <code>ConnectionParameters</code> 
    * instance becomes operational for this connection! 
    * Method is only valid if this connection is not currently connected, 
    * otherwise an exception is thrown.
    *  
    * @param parameters <code>ConnectionParameters</code>
    * @throws NullPointerException if parameter is null
    * @throws IllegalStateException if connection is connected
    * @throws IOException if some directory setting doesn't work
    */
   public void setParameters(ConnectionParameters parameters) 
         throws IOException;

   /** Returns the serialisation device for the outgoing stream 
    * of this connection. Modifications performed on the returned
    * device remain local to this connection.
    *  
    * @return <code>Serialization</code>
    */
   public Serialization getSendSerialization();

   /** Returns the serialisation device for the incoming stream 
    * of this connection. Modifications performed on the returned
    * device remain local to this connection.
    *  
    * @return <code>Serialization</code>
    */
   Serialization getReceiveSerialization();

   /** Makes a copy of the parameter serialisation device operational for
    * sending objects in this connection. 
    * <p><small>Specifying any other value than
    * null with this method establishes a priority setting for the send
    * serialisation; consequently any setting of a serialisation method in
    * connection parameters is ignored until the priority setting is
    * cancelled.</small>
    *  
    * @param s <code>Serialization</code>
    */
   public void setSendSerialization (Serialization s);
   
   /** Makes a copy of the parameter serialisation device operational for
    * receiving objects in this connection.
    * <p><small>Specifying any other value than
    * null with this method establishes a priority setting for the receive
    * serialisation; consequently any setting of a serialisation method in
    * connection parameters is ignored until the priority setting is
    * cancelled.</small>
    *  
    * @param s <code>Serialization</code>
    */
   public void setReceiveSerialization (Serialization s);
   
   /** Returns the unique ID of this connection. The value is automatically
    * created together with the instance of <code>Connection</code>.
    *  
    * @return <code>UUID</code> technical connection identifier
    */
   public UUID getUUID ();

   /** Sets the UUID identifier for this connection. Care has to be taken
    * about the circumstance when this method is called as this instance
    * may lose its membership in hash-tables and environments. As for 
    * JennyNet handling, UUID of a <code>Connection</code> owned/rendered
    * by a <code>Server</code> should not be altered or otherwise be removed
    * and re-added into the server's connection list explicitly.
    *  
    * @param uuid <code>UUID</code>
    */
   void setUUID (UUID uuid);

   /** Returns 4 bytes of a short identifier value for this connection
    * based on its UUID value.
    * 
    * @return byte[] (4 bytes)
    */
   public byte[] getShortId ();
   
   /** Returns the IP-address and port of the remote endpoint of this
    *  TCP connection, or null if this is undefined.
    *  
    *  @return InetSocketAddress remote address or null
    */
   public InetSocketAddress getRemoteAddress();

   /** Returns the IP-address and port of the local endpoint 
    *  of this TCP connection, or null if this is undefined.
    *  
    *  @return InetSocketAddress local address or null
    */
   public InetSocketAddress getLocalAddress();

   /** Whether this Connection is connected to the remote end. 
    * Note that a connection can become disconnected at any time.
    * A disconnection is indicated to event listeners.
    * 
    * @return boolean "connected" status
    */
   public boolean isConnected();

   /** Whether this Connection is closed. A closed connection cannot be 
    * reused. Closing is indicated to event listeners.
    * 
    * @return boolean "closed" status
    */
   boolean isClosed();

   /** Whether this connection is currently engaged in data transmission
    * (reading or writing) over the net. The fall down of this flag has a
    * latency of 2 seconds.
    * 
    * @return boolean transmission status
    */
   public boolean isTransmitting ();

   /** Whether this connection operates below the defined IDLE threshold.
    * If no IDLE threshold has been defined, always <i>false</i> is returned.
    * <p><small>The IDLE threshold can be set up at the connection parameters.
    * Per package default it is switched off.</small>     
    *  
    * @return boolean false == above or equal IDLE threshold; 
    *                 true == below IDLE threshold
    */
   public boolean isIdle();
   
   /** Returns the time point (epoch time) of the last send event
    * occurring on the socket, or 0 if unavailable.
    * 
    * @return long epoch time in milliseconds or 0
    */
   public long getLastSendTime ();
   
   /** Returns the time point (epoch time) of the last receive event
    * occurring on the socket, or 0 if unavailable.
    * 
    * @return long epoch time in milliseconds or 0
    */
   public long getLastReceiveTime ();
   
   /** Sends the given serialisable Object over the network in the normal
    * transmission priority class. 
    * <p><small>In order to be serialisable, the object's class has to be 
    * registered for transmission at this connection's send-serialisation 
    * device otherwise an exception is thrown.</small>
    * 
    * @param object Object serialisable object
    * @returns long object ID number
    * @throws NullPointerException if parameter is null
    * @throws UnregisteredObjectException if parameter object is not 
    *         registered for transmission
    * @throws IllegalStateException if the send queue was full (order 
    *         rejected)
    */
   public long sendObject (Object object);

   /** Sends the given serialisable Object over the network with an
    * option for the priority of transmission.
    * <p><small>An object's class has to be registered for transmission at 
    * this connection's send-serialisation instance 
    * otherwise an exception is thrown.</small>
    * 
    * @param object Object serialisable object
    * @param priority <code>SendPriority</code> transmission priority
    * 			in the class of send objects (ordering of send objects)
    * @returns long object ID number
    * @throws NullPointerException if parameter is null
    * @throws UnregisteredObjectException if parameter object is not 
    *         registered for transmission
    * @throws IllegalStateException if the send queue was full (order 
    *         rejected)
    */
   public long sendObject (Object object, SendPriority priority);

   /** Transfers a file to the remote station. Files can be scheduled
    * with a length up to 2.1 GB (Integer.MAX_VALUE). The returned file
    * identifier number is referenced at subsequent transfer events 
    * concerning the posted file. The
    * file related send-priority of this method is <i>Normal</i>. 
    * <p>(For details on events and handling transmission see the manual page 
    * for file transfers.)
    * 
    * @param file <code>File</code> the file to be sent
    * @param remotePath String path information for the remote station
    *                 (relative path)
    * @returns long file ID number
    * @throws FileNotFoundException if the file cannot be found or read
    * @throws FileInTransmissionException if the file is already in 
    *         transmission for this connection
    * @throws IllegalFileLengthException if maximum file length is exceeded
    * @throws IllegalStateException if the sender list was full (rejected order)
    * @throws NullPointerException if file parameter is null
    * @throws IOException 
    */
   public long sendFile (File file, String remotePath) throws IOException;

   /** Transfers a file to the remote station. Files can be scheduled
    * with a length up to 2.1 GB (Integer.MAX_VALUE). The returned file
    * identifier number is referenced at subsequent transfer events 
    * concerning the posted file. 
    * <p>(For details on events and handling transmission see the manual page 
    * for file transfers.)
    * 
    * @param file <code>File</code> the file to be sent
    * @param remotePath String path information for the remote station
    *                 (relative path)
    * @param priority <code>SendPriority</code> priority in the class of
    *  				send files (ordering of send files)
    * @returns long file ID number
    * @throws FileNotFoundException if the file cannot be found or read
    * @throws FileInTransmissionException if the file is already in 
    *         transmission for this connection
    * @throws IllegalFileLengthException if maximum file length is exceeded
    * @throws IllegalStateException if the sender list was full (rejected order)
    * @throws NullPointerException if file parameter is null
    * @throws IOException 
    */
   public long sendFile (File file, String remotePath, SendPriority priority) throws IOException;

   /** Sends the given block of byte data over the network.
    * <p><small>A standard internal class (<i>JennyNetByteBuffer</i>) is used 
    * to represent the given block at the remote endpoint, signalling it as 
    * incoming object in the same manner as other serialised objects are 
    * received.</small>    
    * 
    * @param buffer byte[] data buffer
    * @param start int buffer offset of data to be sent
    * @param length int length of data to be sent
    * @param priority <code>SendPriority</code> transmission priority
    * @returns long object ID number
    * @throws IllegalArgumentException if data addressing is wrong
    * @throws IllegalStateException if the send queue was full (operation 
    *         rejected)
    * @throws NullPointerException if parameter is null
    */ 
   public long sendData (byte[] buffer, int start, int length, 
                         SendPriority priority);

   /** Sends a PING to the remote station. The corresponding PING-ECHO 
    * will be indicated as PING-ECHO event to connection listeners. 
    * 
    * @return long PING identifier number (PINGs have their own name space)
    */
   public long sendPing ();

   /** Attempts to set the sending TEMPO on both ends of this connection
    * to match the given baud rate (bytes per second), where possible. 
    * This is useful to slow down the transmission rate of a connection.
    * 
    * @param baud int bytes per second or -1 for no limit
    */
   public void setTempo (int baud);
   
   /** Fatally terminates an incomplete file transmission, incoming or 
    * outgoing. The layer discriminates incoming and 
    * outgoing name spaces for objects, including files. The name space 
    * is represented here as the integer <i>direction</i> parameter. Does 
    * nothing if there is no transmission found for the given identifier.
    * <p><small>The practical value of this method lies in rejecting or
    * aborting an incoming file transmission which was indicated as connection
    * event, or an outgoing transmission if it should become obsolete for a 
    * reason. Outgoing transmissions are identified by a long integer value 
    * which is returned by the <code>sendFile()</code> methods. 
    * The breaking of transmissions which have completed is meaningless.
    * </small>
    * 
    * @param objectID long identifier number for transmitted file
    * @param direction int 0 = incoming, 1 = outgoing
    */
   public void breakTransfer (long objectID, int direction);
   
   /** Closes this connection. This causes an event to connection listeners
    * and the closing of this connection on the remote station.
    */
   public void close();

   /** Adds a listener to communication events of this connection. 
    * If the listener already exists, it is not added again.
    * <p><small>Hint: JennyNet offers the no-operation <code>
    * DefaultConnectionsListener</code> class to ease programming of 
    * listeners.</small>. 
    * 
    * @param listener <code>ConnectionListener</code> event listener 
    *                 (may be null)
    */
   public void addListener (ConnectionListener listener);

   /** Removes a listener to communication events of this connection. 
    * 
    * @param listener <code>ConnectionListener</code> event listener 
    *                 (may be null)
    */
   public void removeListener (ConnectionListener listener);

   /** Sets the human friendly name for this connection.
    *  
    * @param name String connection name
    */
   public void setName (String name);

   /** Returns the human friendly name of this connection or null
    * if it is undefined.
    * 
    * @return String name of connection or null
    */
   public String getName ();
   
   /** Returns a <code>Properties</code> instance owned by this connection.
    *  
    * @return <code>Properties</code>
    */
   public Properties getProperties ();
   
   /** Returns the total amount of bytes exchanged with the remote station.
    * This includes both sending and receiving.
    *  
    * @return long data volume
    */
   public long getTransmissionVolume ();

   
   /** Returns the currently active transmission speed of this connection
    * expressed in bytes per second. A value zero indicates that transmission
    * is blocked. This value reflects a setting, not a measurement.
    * 
    * @return int bytes per second
    */
   public int getTransmissionSpeed ();
   
   /** Two Connection instances are equal if they share identical values
    * on both their local and remote socket addresses. 
    * 
    * @param obj <code>Object</code>
    * @return boolean true if instances share same endpoint addresses
    */
   @Override
   public boolean equals (Object obj);
   
   /** An equals-compliant hashcode for this connection.
    * 
    * @return int instance hashcode
    */
   @Override
   public int hashCode ();
   
   /** Returns the human readable name of this connection, if any, followed 
    * by a printout of 2 Internet socket addresses (IP-address and port number
    * ), leading with the local and trailing with the remote address, 
    * separated by an arrow.
    * 
    * @return String textual representation of this connection
    */
   @Override
   public String toString();

}