package org.janeth.jennynet.intfa;

import java.io.IOException;

import org.janeth.jennynet.exception.ClosedConnectionException;

/** Interface for a <code>Connection</code> which was rendered by a 
 * <code>Server</code> instance. This extension to interface <code>Connection
 * </code> consists in the requirement to <i>start()</i> or <i>reject()</i> a 
 * connection before it commences operations. 
 * 
 * <p>Typically a <code>ServerConnection</code> is indicated by the <code>Server
 * </code> in a non-started state in order to allow the application to 
 * decide whether to accept or reject this connection. Only when the
 * connection is started it will allocate required resources and become
 * operational. Correspondingly, the remote end <code>Client</code> has to
 * wait in its <code>connect()</code> method until a decision is taken on
 * the server or the request has timed out.
 * 
 * <p>The server-connection can be set to own primacy over transmission speed
 * settings.
 * 
 * @see Connection
 * @see IServer
 * 
 */
public interface ServerConnection extends Connection {

   /** Starts operations of this server connection. 
    * If the connection request is found to be timed out
    * a <code>ClosedConnectionException</code> exception thrown.
    * 
    * @throws ClosedConnectionException if connection is closed
    * @throws IllegalStateException if the socket is unconnected
    * @throws IOException
    */
   public void start() throws IOException;

   /** Rejects a connection which was not yet started. After rejection,
    * a connection is closed and cannot be started. 
    * 
    * @throws IOException
    */
   public void reject() throws IOException;

   /** Sets this connection's transmission speed as the supreme setting.
    * This effects ignoring and resetting of remote (client) transmission
    * speed signals. If this value is <b>false</b> (the default) then any
    * of both sides can set the transmission speed for both via
    * the TEMPO signal.
    *   
    * @param isFixed boolean
    */
   public void setTempoFixed (boolean isFixed);
   
   /** Whether this connection's transmission speed as the supreme setting.
    * This effects ignoring and resetting of remote (client) transmission
    * speed signals. If this value is <b>false</b> (the default) then any
    * of both sides can set the transmission speed for both via
    * the TEMPO signal.
    *   
    * @param isFixed boolean
    */
   public boolean getTempoFixed ();
   
   /** Returns the server instance which was the source of this connection. 
    * 
    * @return Server
    */
   public IServer getServer ();

}