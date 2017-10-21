package org.janeth.jennynet.core;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.util.Util;

/** A type implementing <code>Connection</code> which originates from a server socket.
 * A <code>ServerConnection</code> must be explicitly started by the receiving application
 * in order to commence operations.
 *   
 */
class ServerConnectionImpl extends ConnectionImpl implements ServerConnection {

   private Server server;
   private Socket startSocket;
   private boolean started;
   
   public ServerConnectionImpl(Server server, Socket socket) throws IOException {
      super();
      this.server = server;
      startSocket = socket;

      if (server == null)
         throw new NullPointerException("server == null");
      if (!socket.isConnected()) {
         throw new IllegalArgumentException("socket must be connected!");
      }
   }

   @Override
   public void start () throws IOException {
      if (!started) {
         // write connection confirm to remote
         JennyNet.sendConnectionConfirm(this);

         // start connection's operational resources
         super.start(startSocket);
         started = true;

         // start ALIVE signals to client 
         setAlivePeriod(getParameters().getAlivePeriod());
         
         // dispatch CONNECTED event
         fireConnectionEvent(ConnectionEventType.connect, 0, null);
      }
   }
   
   @Override
   public void reject () throws IOException {
      if (!started & startSocket.isConnected()) {
         startSocket.close();
         super.close();  // this sets the "closed" marker for the connection
      }
   }

   @Override
   public void close () {
      // we have to secure the start-socket close operation
      // because "super.close" would not do it in unstarted state
      if (!started) {
         try {
            reject();
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         super.close();
      }
   }

   @Override
   public void setTempoFixed(boolean isFixed) {
	   fixedTransmissionSpeed = isFixed;
   }
      
	@Override
	public boolean getTempoFixed() {
		return fixedTransmissionSpeed;
	}

   private void test1 () throws IOException {
      Server server = new Server();
      final File rootDirectory = new File("/tmp");
      
      server.addListener( new DefaultServerListener() {
         
         @Override
         public void connectionAvailable(IServer server, ServerConnection connection) {
            // we think about whether we can handle this connection
            boolean isAcceptable = true;
            // we accept or refute it
            try {
               if (isAcceptable) {
                  // we can set some parameter, like e.g. the file-root-path
                  connection.getParameters().setFileRootDir(rootDirectory);
                  connection.start();
               } else {
                  connection.reject();
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      });
   }
   
   @Override
   public InetSocketAddress getRemoteAddress() {
      return (InetSocketAddress)startSocket.getRemoteSocketAddress();
   }

   @Override
   public InetSocketAddress getLocalAddress() {
      return (InetSocketAddress)startSocket.getLocalSocketAddress();
   }

   @Override
   protected Socket getSocket () {
      return startSocket;
   }

   @Override
   public Server getServer () {
      return server;
   }

  
}
