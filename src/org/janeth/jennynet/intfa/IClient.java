package org.janeth.jennynet.intfa;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.exception.DoubleConnectionException;
import org.janeth.jennynet.exception.JennyNetHandshakeException;

import com.esotericsoftware.kryo.Kryo;

/**
 * A client is an extension of a <code>Connection
 * </code> with the additional abilities to bind a specific local port address
 * and perform connection to a remote JennyNet <code>Server</code> instance or 
 * compatible implementation. Successful connecting automatically starts
 * operations of the <code>Connection</code>. 
 * 
 * <p>A client needs to assume a port address in order to operate. It is,
 * however, not required that the application explicitly binds the client.
 * An unbound client receives a random port number assigned during connection.  
 * 
 * @see org.janeth.jennynet.intfa.Connection
 */

public interface IClient extends Connection {

   /** Binds the TCP client to a local port address. The binding
    *  has to occur before a connection is established. If the port number
    *  is zero, an ephemeral local port number is automatically assigned.
    * <p><small>It is not required to set up a binding for a client.
    * If no binding is specified, a free port of the network system
    * is automatically selected. </small>
    *  
    * @param port int client port number to bind this client to; 0..65535
    * @throws IOException if the binding is not possible
    */
   public void bind (int port) throws IOException;

   /** Binds the TCP client to a local port address. The binding
    *  has to occur before a connection is established.
    * <p><small>It is not required to set up a binding for a client.
    * If no binding is specified, a free port of the network system
    * is automatically selected. </small>
    *  
    * @param address <code>SocketAddress</code> client address; if null
    *        a valid ephemeral address is automatically selected.
    * @throws IOException if the binding is not possible
    */
   public void bind (SocketAddress address) throws IOException;
   
   /** Whether this client is successfully bound to a local address.
    * 
    * @return boolean true == client bound to local address
    */
   public boolean isBound ();
   
   /** Attempts to connect this TCP client to the given server address.
     * An attempt is made to resolve the given host name into an 
     * <code>InetAddress</code>. This method blocks until the connection is 
     * fully established on the JennyNet transport layer, an error/rejection 
     * occurred or the timeout to wait for confirmation has expired.
     * 
     * <p><small>If the host is known but the full address unavailable, 
     * typically a java.net.NoRouteToHostException is thrown. If the address
     * is available but there is no listener catching the call, typically a 
     * java.net.ConnectException is thrown. If the remote listener does not 
     * understand the JENNYNET protocol, a JennyNetHandshakeException is 
     * thrown. If the remote end is a verified JennyNet layer but does reject
     * the connection, a ConnectionRejectedException is thrown. All system 
     * relevant exceptions thrown by this method can be caught with 
     * IllegalArgumentException, IOException and JennyNetException clauses.
     * </small>
    * 
    * @param timeout int milliseconds to timeout; 0 selects a default value
    * @param host String host name, the host address in an IP literal 
    *                    (without protocol)
    * @param port int server port address; 0..65535
    * 
    * @throws DoubleConnectionException if already connected
    * @throws ClosedConnectionException if this client has been closed
    * @throws JennyNetHandshakeException if remote endpoint is not a JennyNet
    *         layer
    * @throws ConnectionRejectedException if remote JennyNet layer rejects the
    *         connection
    * @throws ConnectionTimeoutException if timeout expires before connection
    *         succeeds
    * @throws java.net.UnknownHostException if the host's IP address cannot be
    *         resolved
    * @throws java.net.NoRouteToHostException if the host's IP address cannot
    *         be reached
    * @throws java.net.ConnectException if the host does not offer a service
    *         at this address
    * @throws IllegalArgumentException if port is out of range or host is null
    * @throws IOException
    */
   public void connect (int timeout, String host, int port)
         throws IOException;

   /** Attempts to connect this TCP client to the given server address.
    * This method blocks until the connection is fully established on the 
    * JennyNet transport layer, an error/rejection occurred or the timeout
    * to wait for confirmation has expired.
    * 
    * <p><small>If the host is known but the full address unavailable, 
    * typically a java.net.NoRouteToHostException is thrown. If the address
    * is available but there is no listener catching the call, typically a 
    * java.net.ConnectException is thrown. If the remote listener does not 
    * understand the JENNYNET protocol, a JennyNetHandshakeException is 
    * thrown. If the remote end is a verified JennyNet layer but does reject
    * the connection, a ConnectionRejectedException is thrown. All system 
    * relevant exceptions thrown by this method can be caught with 
    * IllegalArgumentException, IOException and JennyNetException clauses.
    * </small>
    * 
    * @param timeout int milliseconds to timeout; 0 selects a default value
    * @param host InetAddress host address
    * @param port int server port address; 0..65535
    * 
    * @throws DoubleConnectionException if already connected
    * @throws ClosedConnectionException if this client has been closed
    * @throws JennyNetHandshakeException if remote endpoint is not a JennyNet
    *         layer
    * @throws ConnectionRejectedException if remote JennyNet layer rejects the
    *         connection
    * @throws ConnectionTimeoutException if timeout expires before connection
    *         succeeds
    * @throws java.net.UnknownHostException if the host's IP address cannot be
    *         resolved
    * @throws java.net.NoRouteToHostException if the host's IP address cannot
    *         be reached
    * @throws java.net.ConnectException if the host does not offer a service
    *         at this address
    * @throws IllegalArgumentException if port is out of range or host is null
    * @throws IOException
    */
   public void connect (int timeout, InetAddress host, int port)
         throws IOException;

   /** Attempts to connect this TCP client to the given server address.
    * This method blocks until the connection is fully established on the 
    * JennyNet transport layer, an error/rejection occurred or the timeout
    * to wait for confirmation has expired.
    * 
    * <p><small>If the host is known but the full address unavailable, 
    * typically a java.net.NoRouteToHostException is thrown. If the address
    * is available but there is no listener catching the call, typically a 
    * java.net.ConnectException is thrown. If the remote listener does not 
    * understand the JENNYNET protocol, a JennyNetHandshakeException is 
    * thrown. If the remote end is a verified JennyNet layer but does reject
    * the connection, a ConnectionRejectedException is thrown. All system 
    * relevant exceptions thrown by this method can be caught with 
    * IllegalArgumentException, IOException and JennyNetException clauses.
    * </small>
    * 
    * @param timeout int milliseconds to timeout; 0 selects a default value
    * @param host InetAddress host address
    * @param port int server port address; 0..65535
    * 
    * @throws DoubleConnectionException if already connected
    * @throws ClosedConnectionException if this client has been closed
    * @throws JennyNetHandshakeException if remote endpoint is not a JennyNet
    *         layer
    * @throws ConnectionRejectedException if remote JennyNet layer rejects the
    *         connection
    * @throws ConnectionTimeoutException if timeout expires before connection
    *         succeeds
    * @throws java.net.UnknownHostException if the host's IP address cannot be
    *         resolved
    * @throws java.net.NoRouteToHostException if the host's IP address cannot
    *         be reached
    * @throws java.net.ConnectException if the host does not offer a service
    *         at this address
    * @throws IllegalArgumentException if port is out of range or host is null
    * @throws IOException
    */
   public void connect (int timeout, InetSocketAddress host)
         throws IOException;

   /** Sets performance preferences for this connection, where possible.
    *  <p>Performance preferences are described by three integers whose values
    *  indicate the relative importance of short connection time, low 
    *  latency, and high bandwidth. The absolute values of the integers are 
    *  irrelevant; in order to choose a protocol the values are simply 
    *  compared, with larger values indicating stronger preferences. 
    *  <p>This setting must be done prior to connecting to the remote
    *  endpoint.
    * 
    * @param connectionTime int
    * @param latency int
    * @param bandwidth int
    */
   public void setPerformancePreferences (
         int connectionTime,
         int latency,
         int bandwidth);
   
}