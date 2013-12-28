package org.jhunt.jennynet.intfa;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public interface IServer {

   enum SignalMethod {Listener, Accept}
   
   /** Binds the server to a port.
    * 
    * @throws IOException if the server could not be bound 
    */
   public void bind (int port) throws IOException;

   /** Binds the server to a port and an Internet address.
    * 
    * @throws IOException if the server could not be bound 
    */
   public void bind (SocketAddress address) throws IOException;

   /** Returns IP address and port of this servers 
    *  socket address.
    *  
    *  @return InetSocketAddress server socket address
    */
   public InetSocketAddress getSocketAddress();

   /** Sets the method by which incoming connections are signalled to
    * user application. By default new connections are indicated via
    * event dispatcher to listeners. They may alternatively be set to 
    * be available at the <code>accept()</code> polling method
    * instead (in which case events are not issued). 
    *  
    * @param method SignalMethod
    */
   public void setSignalMethod (SignalMethod method);

   /** Returns this server's signalling method for incoming new
    * connections. Defaults to "Listener".
    * 
    * @return <code>IServer.SignalMethod</code>
    */
   public SignalMethod getSignalMethod ();

   /** Sets the queue capacity for incoming server connections.
    * The queue capacity is only relevant if this server's signalling
    * method is set to "Accept".
    * This setting can only be performed while the server is not yet
    * started.
    * 
    * @param capacity int queue capacity
    * @throws IllegalStateException if server has been started
    */
   public void setAcceptQueueCapacity (int capacity);
   
   /** Returns the queue capacity for incoming server connections.
    * Default value is <code>JennyNet.getObjectQueueCapacity()</code>.
    * 
    * @return int queue capacity
    */
   public int getAcceptQueueCapacity ();
   
   /** Accepts and returns new connections that reach at the port to 
    * which this server is bound. This method blocks until a connection is 
    * available, the thread has been interrupted or a given wait-time has elapsed.
    * This method only works if this server's <code>SignalMethod</code> is set 
    * to "accept".
    * <p><u>Note:</u> The server holds a buffer queue for incoming connections
    * with limited capacity. If that capacity is exceeded, new connections are 
    * rejected at the socket. 
    * 
    * @param timeout int time in milliseconds to wait before this method returns 
    *        from blocking if no connection arrives; a value 0 is interpreted as
    *        limitless.
    * @return IConnection or null if no connection is available in the given time span
    * @throws InterruptedException
    */        
   public Connection accept (int timeout) throws InterruptedException;

   /** Starts operating this server. */
   public void start();

   /** Whether this server instance is capable of receiving new connections.
    * 
    * @return boolean true == server is alive
    */
   public boolean isAlive ();
   
   /** Sends an object to all connected clients.
    * 
    * @param object Object sendable object (type must be registered)
    * @param priority boolean true == high priority, false == normal priority
    */
   public void sendObjectToAll (Object object, boolean priority);

   /** Sends an object to all connected clients except the one
    * given as parameter.
    *
    * @param id UUID connection exempted as target
    * @param object Object sendable object (type must be registered)
    * @param priority boolean true == high priority, false == normal priority
    */
   public void sendObjectToAllExcept (UUID id, Object object, boolean priority);
   
   /** Sends a file to all connected clients. The sending may fail
    * for a client if this feature is prohibited at the remote station
    * or if it is not ready to accept the transmission. The failure for some
    * client does not influence the sending to other clients.
    * 
    * @param file File file to transmit
    * @param pathInfo String intended file path at the remote station
    *                 (see documentation for operation contract)
    */
   public void sendFileToAll (File file, String pathInfo);

   /** Sends a file to all connected clients except the one
    * given as parameter. The sending may fail
    * for a client if this feature is prohibited at the remote station
    * or if it is not ready to accept the transmission. The failure for some
    * client does not influence the sending to other clients.
    * 
    * @param id UUID connection exempted as target
    * @param file File file to transmit
    * @param pathInfo String intended file path at the remote station
    *                 (see documentation for operation contract)
    */
   public void sendFileToAllExcept (UUID id, File file, String pathInfo);

   /** Sends a PING signal to all connected clients. (ECHOs will be reported
    * as events at the listeners on the connections.) */
   public void sendPingToAll ();

   /** Sends a maximum speed request to all connected clients.
    * 
    * @param baud int requested BAUD measured transmission speed
    */
   public void sendTempoToAll (int baud);

   /** Adds a server listener to this server.
    * 
    * @param listener IServerListener
    */
   public void addListener (ServerListener listener);

   /** Removes a server listener from this server.
    * 
    * @param listener IServerListener
    */
   public void removeListener (ServerListener listener);

   /** Closes this server's port activity. A closed server
    * cannot be re-opened, instead a new server instance has to be created. 
    * This server's connections remain alive but are removed from the
    * connection registry. Hence, after closure of a server, its <code>
    * getConnections()</code> returns the empty array.
    */
   public void close();

   /** Closes all registered connections. 
    */
   public void closeAllConnections ();
   
   /** Returns the connection with the given UUID identifier or null
    * if this connection is not listed in the registry. The returned 
    * connection could be unconnected but not closed. 
    * 
    * @param uuid UUID connection name 
    * @return IConnection or null
    */
   public Connection getConnection (UUID uuid);
   
   /** Returns an array with all listed open connections.
    * The array returned is a copy and may be modified by the application.
    */
   public Connection[] getConnections ();

   /** Removes a connection from the registry of this server.
    * <p><small>This method tolerates parameter <b>null</b> for no-operation.</small>
    * 
    * @param connection <code>ServerConnection</code> (may be null)
    */
   public void removeConnection (Connection connection);

   /** Adds a connection to the registry of this server.
    * Adding may fail if the given connection is closed or null.
    * <p><small>This method tolerates parameter <b>null</b> for no-operation.</small>
    * 
    * @param connection <code>ServerConnection</code> (may be null)
    * @return boolean true == connection is in registry, false otherwise
    */
   public boolean addConnection(Connection connection);
   
}