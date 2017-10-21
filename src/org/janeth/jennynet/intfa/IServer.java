package org.janeth.jennynet.intfa;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.SendPriority;

/**
 * Describes the behaviour and features of a server object.
 * A server can be created with instances of class <code>Server</code>.
 * A server must be bound before it can be started. It has 
 * to be explicitly started with the <code>start()</code> method to become 
 * operational. Closing a server means to discontinue the service and release
 * socket resources. Closing the server, however, does not close rendered 
 * connections.
 * 
 * <p>A server object has a set of parameters associated. These
 * parameters are basically connection parameters which become the default
 * parameters of new connections rendered by the server. Some of the parameters
 * may also become instrumental for server operations, like e.g. the 
 * CONFIRM_TIMEOUT parameter for the time the server waits before it closes
 * down connection requests which are not dealt with by the application.
 * 
 * <p>A server object holds memory of the connections it has rendered. This 
 * is called the "connection registry" and consists of an array of open 
 * connections which the application can obtain at the interface. It is kept
 * solely for the convenience of the user, connections and server being 
 * completely separate functional entities. Note in particular that the closing
 * of a server object does not invalidate or close listed connections. 
 * If connections have to be closed along with the server, method <code>
 * closeAllConnections</code> can be called. The connection registry holds only
 * open connections while closing a connection removes them from the registry.
 * 
 * <p>This class is event dispatching. In the usual manner listeners can be 
 * added to Server instances. The listener has to comply with a set of methods
 * and is informed most importantly about new connections being made available
 * through the server (in signal method LISTENER). The listener interface is 
 * {@link ServerListener}; Class {@link DefaultServerListener} can aid 
 * programming of instant listener subclasses.   
 * 
 * <p>A server can be run in one of two connection signalling modes: LISTENER
 * and ACCEPT. The default method is LISTENER. LISTENER means that new 
 * connections are made available to the application through event dispatching
 * (in a thread owned by the server).
 * ACCEPT follows a different policy by letting applications poll new 
 * connections from a queue in a blocking method. Both methods are exclusive.
 * 
 * <p>Last but not least a server can function as a multiplexer for sending
 * objects, files or pings to all connections contained in the connection 
 * registry. Furthermore, all these connections can be closed with a single 
 * method.
 * 
 */

public interface IServer {

   enum SignalMethod {Listener, Accept}
   
   /** Binds the server to a port. The IP-address is
    * the <i>wildcard</i> (0.0.0.0). A port number of zero leads
    * to a system created ephemeral port number.
    * 
    * @param port int port-number (0..65535) to define 
    *             the address of this server
    * @throws IOException if the server could not be bound or is 
    *         already bound
    * @throws IllegalArgumentException if port is out of range        
    */
   public void bind (int port) throws IOException;

   /** Binds the server to a given local socket address (IP-address and 
    * port number). An address of null leads to a system created ephemeral
    * port number and the <i>wildcard</i> IP-address (0.0.0.0) to bind the 
    * socket.
    * 
    * @param address <code>SocketAddress</code>
    * @throws IOException if the server could not be bound or is 
    *         already bound
    */
   public void bind (SocketAddress address) throws IOException;

   /** Whether this server is successfully bound to an address.
    * 
    * @return boolean true == bound to server-address
    */
   public boolean isBound ();
   
   /** Returns IP address and port of this server's 
    *  socket or null if this socket is unbound.
    *  
    *  @return InetSocketAddress server socket address
    */
   public InetSocketAddress getSocketAddress();

   /** Returns the set of connection parameters which function as default
    * for this server. Incoming connections released by this server receive
    * this set of parameters (which then can be altered on individual base).
    * 
    * @return <code>ConnectionParameters</code>
    */
   public ConnectionParameters getParameters ();

   /** Sets the set of connection parameters which function as default
    * for new (incoming) connections on this server.
    * 
    * @param parameters <code>ConnectionParameters</code>
    * @throws NullPointerException if parameter is null
    */ 
   public void setParameters (ConnectionParameters pararameters)
         throws IOException;
   
   /** Sets the priority value for this server's daemon thread dealing with
    * accepting connections and dispatching server events. Defaults 
    * to Thread.MAX_PRIORITY.
    * 
    * @param threadPriority
    * @throws IllegalArgumentException if threadPriority is out of range
    */
   public void setThreadPriority (int threadPriority);
   
   /** Returns the priority value for this server's daemon thread dealing 
    * with accepting connections and dispatching server events. Defaults 
    * to Thread.MAX_PRIORITY.
    * 
    * @param threadPriority
    */
   public int getThreadPriority ();

   /** Sets any name for this server. This is for convenience of 
    * application use.
    * 
    * @param name String server name (may be null)
    */
   public void setName (String name);

   /** Returns the text name given to this server by method <code>
    * setName(String)</code>.
    * 
    * @return String or null if undefined
    */
   public String getName ();
   
   /** Sets the method by which incoming connections are signalled to
    * user application. By default new connections are indicated via
    * event dispatcher to listeners. They may alternatively be set to 
    * be available at the <code>accept()</code> polling method
    * instead (in which case events are not issued). 
    * This setting must be performed while the server is not yet
    * started, otherwise an exception is thrown.
    *  
    * @param method <code>IServer.SignalMethod</code>
    * @throws IllegalArgumentException if method == null
    * @throws IllegalStateException if server has been started
    */
   public void setSignalMethod (SignalMethod method);

   /** Returns this server's signalling method for incoming new
    * connections. Defaults to "Listener".
    * 
    * @return <code>IServer.SignalMethod</code>
    */
   public SignalMethod getSignalMethod ();

   /** Sets the queue capacity for incoming server connections.
    * The queue capacity is only relevant for this server's signalling
    * method "Accept".
    * This setting must be performed while the server is not yet
    * started, otherwise it is ignored.
    * 
    * @param capacity int accept queue capacity
    * @throws IllegalStateException if server has been started
    */
   public void setAcceptQueueCapacity (int capacity);
   
   /** Sets whether this server owns the primacy to set TEMPO
    * (transmission speed) for connections. If primacy is switched on,
    * clients cannot set TEMPO on the connection.
    * 
    * @param prime boolean true == server primacy, false == no primacy 
    *              (default)
    */
   public void setTempoPrimacy (boolean prime);
   
   /** Whether this server owns the primacy to set TEMPO
    * (transmission speed) for connections. If primacy is switched on,
    * clients cannot set TEMPO on the connection.
    * 
    * @return boolean true == server primacy, false == no primacy (default)
    */
   public boolean getTempoPrimacy ();
   
   /** Returns the queue capacity for incoming server connections.
    * Default value is <code>JennyNet.getObjectQueueCapacity()</code>.
    * 
    * @return int accept queue capacity
    */
   public int getAcceptQueueCapacity ();
   
   /** Returns a new connection that has reached at the port to 
    * which this server is bound. This method blocks until a connection is 
    * available, the calling thread has been interrupted or a given timeout 
    * expired. This method only works if this server's <code>SignalMethod
    * </code> is set to "Accept", otherwise an exception is thrown.
    * 
    * <p><small><u>Note:</u> The server holds a buffer queue for incoming
    * connections with limited capacity. If that capacity is exceeded, new
    * connections are rejected at the socket without informing the application.
    * </small> 
    * 
    * @param timeout int time in milliseconds to wait before this method
    *        returns from blocking if no connection arrives; a value 0 is
    *        interpreted as limitless.
    * @return <code>Connection</code> or null if no connection becomes 
    *         available within the given time
    * @throws InterruptedException if the calling thread has been interrupted
    * @throws IllegalStateException if this server is not ALIVE or signalling
    *         method is not set to ACCEPT
    * @see setAcceptQueueCapacity        
    */        
   public Connection accept (int timeout) throws InterruptedException;

   /** Starts operating this server.
    * 
    * @throws IllegalStateException if this server is not bound or closed  
    */
   public void start();

   /** Whether this server instance is capable of receiving new connections.
    * 
    * @return boolean true == server is alive
    */
   public boolean isAlive ();
   
   /** Closes this server's port activity. A closed server
    * cannot be re-opened, instead a new server instance has to be created. 
    * This server's connections remain alive and the connection registry
    * untouched. The close method can be called at any time, including before
    * the server is started.
    */
   public void close();

   /** Whether this server has been closed.
    * 
    * @return boolean true == server is closed
    */
   public boolean isClosed ();
   
   /** Closes all registered connections. (This also removes them from the 
    * connection registry.)
    */
   public void closeAllConnections ();
   
   /** Sends an object to all connected clients with "Normal" (medium) 
    * send priority.
    * 
    * @param object Object serialisable object (type must be registered
    *        for serialisation)
    * @return int transaction id number
    */
   public int sendObjectToAll (Object object);

   /** Sends an object to all connected clients with a given send priority.
    * 
    * @param object Object serialisable object (type must be registered
    *        for serialisation)
    * @param priority <code>SendPriority</code> transmission priority
    * @return int transaction id number
    */
   public int sendObjectToAll (Object object, SendPriority priority);

   /** Sends an object to all connected clients except the one
    * given as argument.
    *
    * @param id UUID of the connection exempted as target
    * @param object Object serialisable object (type must be registered)
    * @param priority <code>SendPriority</code> transmission priority
    * @return int transaction id number
    */
   public int sendObjectToAllExcept (UUID id, Object object, SendPriority priority);
   
   /** Sends a file to all connected clients. The sending may fail
    * for a client if it is not ready to accept the transmission. 
    * The failure for one does not influence the sending to other
    * clients. Failures are indicated by issuing an error event to 
    * connection listeners.
    * 
    * @param file <code>File</code> file to transmit
    * @param pathInfo String intended file path at the remote station
    *                 (see documentation for operation contract)
    * @return int transaction id number
    */
   public int sendFileToAll (File file, String pathInfo, SendPriority priority);

   /** Sends a file to all connected clients except the one
    * given as argument. The sending may fail for a client if it is 
    * not ready to accept the transmission. The failure for one
    * does not influence the sending to other clients. Failures are
    * indicated by issuing an error event to connection listeners.
    * 
    * @param id UUID of the connection exempted as target
    * @param file File file to transmit
    * @param pathInfo String intended file path at the remote station
    *                 (see documentation for operation contract)
    * @return int transaction id number
    */
   public int sendFileToAllExcept (UUID id, File file, String pathInfo, SendPriority priority);

   /** Sends a PING signal to all connected clients. Corresponding 
    * PING-ECHOs will be reported as events to connection listeners.
    *  
    * @return int transaction id number
    */
   public int sendPingToAll ();

   /** Sends a maximum speed setting to all active connections.
    * 
    * @param baud int BAUD transmission speed in bytes per second
    * @return int transaction id number
    */
   public int sendTempoToAll (int baud);

   /** Adds a server listener to this server.
    * 
    * @param listener <code>IServerListener</code>
    * @throws NullPointerException if parameter is null
    */
   public void addListener (ServerListener listener);

   /** Removes a server listener from this server.
    * 
    * @param listener <code>IServerListener</code> (may be null)
    */
   public void removeListener (ServerListener listener);

   /** Returns the connection with the given UUID identifier or null
    * if this connection is not listed in the registry. The returned 
    * connection could be unconnected but not closed. 
    * 
    * @param uuid UUID connection name 
    * @return <code>IConnection</code> or null
    */
   public ServerConnection getConnection (UUID uuid);
   
   /** Returns an array with all listed open connections.
    * The array returned is a copy and may be modified by the application.
    */
   public ServerConnection[] getConnections ();

   /** Removes the given connection from the registry of this server.
    * <p><small>This method tolerates parameter <b>null</b> 
    * for no-operation!</small>
    * 
    * @param connection <code>ServerConnection</code> (may be null)
    */
   public void removeConnection (ServerConnection connection);

   /** Adds a connection to the registry of this server.
    * Adding may fail silently if the given connection is closed or null. 
    * A connection contained in the registry prior to this call with the same
    * UUID value will be replaced with the parameter connection.
    * <p><small>This method tolerates parameter <b>null</b> 
    * for no-operation!</small>
    * 
    * @param connection <code>Connection</code> (may be null)
    * @return boolean true == connection is in registry, false otherwise
    */
   public boolean addConnection(ServerConnection connection);

}