package org.janeth.jennynet.intfa;



/** Interface for a listener to events issued by a <code>Server</code>
 *  instance.
 *  
 *  @see IServer
 */

public interface ServerListener {

   /** A new connection has reached the server and requests operation.
    * The connection given with the method parameter can be started or 
    * rejected by the application. If it is rejected, its resources are
    * cancelled. If it is accepted, the connection is started and added
    * to the connection registry of the server.
    * If application does not decide on the case, the connection is
    * automatically rejected after a timeout defined by server parameter 
    * CONFIRM_TIMEOUT. 
    * 
    * @param server <code>IServer</code> server source of the event
    * @param connection <code>ServerConnection</code> incoming connection
    */
   public void connectionAvailable (IServer server, ServerConnection connection);
   
   /** The given <code>Connection</code> instance was added to the connection
    * registry of the issuing server.
    * 
    * @param server <code>IServer</code> server source of the event
    * @param connection <code>Connection</code> connection added to registry
    */
   public void connectionAdded (IServer server, Connection connection);
   
   /** The given <code>Connection</code> instance was removed from the 
    * connection registry of the issuing server.
    * 
    * @param server <code>IServer</code> server source of the event
    * @param connection <code>Connection</code> connection removed from registry
    */
   public void connectionRemoved (IServer server, Connection connection);
   
   /** The server has been closed. It will not issue any further events.
    *  
    * @param server <code>IServer</code> server source of the event
    */
   public void serverClosed (IServer server);
   
   /** An error has occurred in one of the multiplexing transactions of the
    * server.
    * <p><small>As errors are reported synchronously, the given transaction-Id
    * has not yet returned from the multiplex action command while it is already
    * stated in the error reports!</small>
    * 
    * @param server <code>IServer</code> server source of the event
    * @param con <code>Connection</code> throwing the error condition
    * @param transAction int id number as returned by the transaction call
    * @param e Throwable error condition
    */
   public void errorOccurred (IServer server, Connection con, int transAction, Throwable e);
}
