
package org.janeth.jennynet.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.janeth.jennynet.exception.ClosedConnectionException;
import org.janeth.jennynet.exception.ConnectionTimeoutException;
import org.janeth.jennynet.exception.DoubleConnectionException;
import org.janeth.jennynet.exception.JennyNetHandshakeException;
import org.janeth.jennynet.intfa.IClient;

/** 
 * Client is an extension of the generic <code>Connection</code> implementation with the
 * ability to bind to a local port address and to establish connection to 
 * a remote server endpoint running a JennyNet service layer. 
 * 
 * @see org.janeth.jennynet.intfa.Connection
 */
public class Client extends ConnectionImpl implements IClient {

	private Socket socket = new Socket();


   /** Creates an unbound client. When connecting an unbound client, an ephemeral
    * local port number is automatically assigned to the connection.
    */
   public Client () {
   }
   
   /** Creates a client which is bound to the specified port. A port number of
    * zero selects any free port.
    * 
    * @param port int local port number
    * @throws IOException if binding to the specified port is not possible
    */
   public Client (int port) throws IOException {
	   this();
       bind(port);
   }
   
   /** Creates a client with the given socket address. An address of null 
    * creates a socket with the wildcard IP-address on any free port.
    * 
    * @param address SocketAddress client address or null for
    *                any free port
    * @throws IOException
    */
   public Client (SocketAddress address) throws IOException {
      this();
      if (address == null) {
         address = new InetSocketAddress(0);
      }
      bind(address);
   }
   
   @Override
   protected void close (Throwable ex, int info) {
	   super.close(ex, info);
	   JennyNet.removeClientFromGlobalSet(this);
	   System.out.println("-- removed CLIENT from global client set (" + 
			   JennyNet.getNrOfClients() + ") : " + getLocalAddress());
   }

   @Override
   public void bind (int port) throws IOException {
      bind( new InetSocketAddress(port) );
   }


   @Override
   public void bind (SocketAddress address) throws IOException {
      socket.setReuseAddress(true);
      socket.bind(address);
   }

   @Override
   public boolean isBound () {
      return socket.isBound();
   }

   private void startSocket (InetSocketAddress target, int timeout)
         throws IOException {
      // control and correct parameters
      checkConnectionTarget(target);
      if (timeout < 1) {
         timeout = getParameters().getConfirmTimeout() / 2;
      }

      // attempt connection on socket level
      long startTime = System.currentTimeMillis();
      try {
         socket.connect(target, timeout);
      } catch (SocketTimeoutException e) {
         throw new ConnectionTimeoutException("socket timeout: " + timeout + " ms");
      }
      
      // verify JennyNet layer handshake
      if (!JennyNet.verifyNetworkLayer(1, socket, timer, timeout)) {
         throw new JennyNetHandshakeException("no remote JennyNet layer");
      }

      // verify connection was accepted
      int time = timeout - (int)(System.currentTimeMillis() - startTime); 
      JennyNet.waitForConnection(socket, timer, time);
      
      // only then start Connection resources (running status)
      start(socket);
      setAlivePeriod(getParameters().getAlivePeriod());
      
      // increase global active client counter
	  JennyNet.addClientToGlobalSet(this);
	  System.out.println("-- created NEW CLIENT, added to global client set (" + 
			   JennyNet.getNrOfClients() + ") : " + getLocalAddress());
	   
      // dispatch CONNECTED event
      connected();
      fireConnectionEvent(ConnectionEventType.connect, 0, null);
   }
   
   /**
    * Controls whether this client can be connected and that
    * the assigned port number is valid.
    * 
    * @param addr InetSocketAddress
    * @throws DoubleConnectionException
    * @throws ClosedConnectionException
    * @throws IllegalArgumentException
    */
   private void checkConnectionTarget (InetSocketAddress addr) {
      if (isConnected()) 
         throw new DoubleConnectionException();
      if (isClosed())
         throw new ClosedConnectionException("this connection is closed!");
      int port = addr.getPort();
      if (port < 0 | port > 65535) {
         throw new IllegalArgumentException("port number out of range");
      }
//    if (addr.isUnresolved()) {
//    throw new UnresolvedAddressException();
// }
   }

   /** Method called internally when this client enters into 'connected' state.
    * The method of this class does nothing.
    */
   protected void connected () {
   }
   
   @Override
   public void connect (int timeout, String host, int port) throws IOException {
      InetSocketAddress target = new InetSocketAddress(host, port);
      startSocket(target, timeout);
   }

   @Override
   public void connect (int timeout, InetAddress host, int port)
         throws IOException {
      InetSocketAddress target = new InetSocketAddress(host, port);
      startSocket(target, timeout);
   }

   @Override
   public void connect (int timeout, InetSocketAddress host) throws IOException {
      startSocket(host, timeout);
   }
/*
   @Override
   public void reconnect () throws IOException {
      if (isConnected()) 
         throw new DoubleConnectionException();
      
      if (remoteAddress != null) {
         socket = new Socket();
         if (localPort != 0) {
            socket.setReuseAddress(true);
            socket.bind( new InetSocketAddress(localPort) );
         }
         startSocket(remoteAddress, connectTimeout);
      }
   }

   @Override
   public void reconnect (int timeout) throws IOException {
      connectTimeout = timeout;
      reconnect();
   }
*/
   @Override
   public void setPerformancePreferences (int connectionTime, int latency, int bandwidth) {
      if (socket.isConnected()) 
         throw new IllegalStateException("socket must be unconnected");
      
      socket.setPerformancePreferences(connectionTime, latency, bandwidth);
   }

   @Override
   public InetSocketAddress getLocalAddress () {
      return (InetSocketAddress)socket.getLocalSocketAddress();
   }

   @Override
   public InetSocketAddress getRemoteAddress () {
      return (InetSocketAddress)socket.getRemoteSocketAddress();
   }

   @Override
   protected Socket getSocket () {
      return socket;
   }
   
}
