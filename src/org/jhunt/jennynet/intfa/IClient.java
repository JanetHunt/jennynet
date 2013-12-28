package org.jhunt.jennynet.intfa;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.esotericsoftware.kryo.Kryo;

public interface IClient extends Connection {

//   public Kryo getKryo();

   /** Binds the TCP client to a local port address. The binding
    *  has to occur before a connection is established.
    * <p><small>It is not required to set up a binding for a client.
    * If no binding is specified, a free port of the network system
    * is automatically selected. </small>
    *  
    * @param port int client port address
    * @throws IOException if the binding is not possible
    */
   public void bind (int port) throws IOException;
   
   /** Opens a TCP client and connects it to the given server address.
    * 
    * @param timeout int milliseconds to timeout
    * @param host String host name
    * @param port int server port address
    * @throws IOException
    */
   public void connect (int timeout, String host, int port)
         throws IOException;

   /** Opens a TCP and UDP client.
    * 
    * @param timeout
    * @param host
    * @param tcpPort
    * @param udpPort
    * @throws IOException
   public void connect (int timeout, String host, int tcpPort, int udpPort)
         throws IOException;
    */

   /** Opens a TCP client and connects it to the given server address.
    * 
    * @param timeout int milliseconds to timeout
    * @param host InetAddress host address object 
    * @param port int server port address
    * @throws IOException
    */
   public void connect (int timeout, InetAddress host, int port)
         throws IOException;

   /** Opens a TCP client and connects it to the given server address.
    * 
    * @param timeout int milliseconds to timeout
    * @param host InetSocketAddress server endpoint address object 
    * @throws IOException
    */
   public void connect (int timeout, InetSocketAddress host)
         throws IOException;

   /** Opens a TCP and UDP client. Blocks until the connection is complete or
    *  the timeout is reached.
    * <p>
    * Because the framework must perform some minimal communication before the
    * connection is considered successful, {@link #update(int)} must be called 
    * on a separate thread during the connection process.
    * 
    * @param timeout
    * @param host
    * @param tcpPort
    * @throws IllegalStateException if called from the connection's update thread.
    * @throws IOException if the client could not be opened or connecting times out. 
   public void connect (int timeout, InetAddress host, int tcpPort, int udpPort)
         throws IOException;
    */

   /** Calls {@link #connect(int, InetAddress, int) connect} with the values last
    *  passed to connect.
    *  
    *  @throws IllegalStateException if connect has never been called. 
    */
   public void reconnect () throws IOException;

   /** Calls {@link #connect(int, InetAddress, int) connect} with the specified timeout
    *  and the other values last passed to connect.
    *  
    *  @param timeout int 
    *  @throws IllegalStateException if connect has never been called.
    */
   public void reconnect (int timeout) throws IOException;

   public void close();

   /** Sets performance preferences for this connection, where possible.
    *  <p>Performance preferences are described by three integers whose values
    *  indicate the relative importance of short connection time, low 
    *  latency, and high bandwidth. The absolute values of the integers are 
    *  irrelevant; in order to choose a protocol the values are simply compared,
    *  with larger values indicating stronger preferences. 
    *  <p>This setting must be done prior to connecting to the remote endpoint.
    * 
    * @param connectionTime int
    * @param latency int
    * @param bandwidth int
    */
   public void setPerformancePreferences (
         int connectionTime,
         int latency,
         int bandwidth);
   
   /** Broadcasts a UDP message on the LAN to discover any running servers. 
    * The address of the first server to respond is returned.
    * 
    * @param udpPort The UDP port of the server
    * @param timeoutMillis The number of milliseconds to wait for a response
    * @return InetAddress of the first server found, or null if no server responded 
   public InetAddress discoverHost (int udpPort, int timeoutMillis);
    */

   /** Broadcasts a UDP message on the LAN to discover any running servers.
    * 
    * @param udpPort UDP port of the server
    * @param timeoutMillis number of milliseconds to wait for a response
    * @return List of InetAddress 
   public List<InetAddress> discoverHosts (int udpPort, int timeoutMillis);
    */

}