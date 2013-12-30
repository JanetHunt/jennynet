package org.jhunt.jennynet.intfa;

/** Interface for a listener to events issued by a <code>Server</code> instance.
 */

public interface ServerListener {

   /** A new connection has reached the server and requests to get started.
    * The parameter connection can be started or rejected by the application.
    * If this doesn't happen, a timeout (defined by connection parameter CONFIRM_TIMEOUT)
    * will close the connection.
    * 
    * @param connection <code>ServerConnection</code> incoming connection
    */
   public void connectionAvailable (ServerConnection connection);
   
   /** A <code>Connection</code> instance was added to the connection
    * registry of the issuing server.
    * 
    * @param connection <code>Connection</code> connection added to registry
    */
   public void connectionAdded (Connection connection);
   
   /** A <code>Connection</code> instance was removed to the connection
    * registry of the issuing server.
    * 
    * @param connection <code>Connection</code> connection added to registry
    */
   public void connectionRemoved (Connection connection);
   
   /** The server has been closed. It will not issue any further events. 
    */
   public void serverClosed ();
}
