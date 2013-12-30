package org.jhunt.jennynet.intfa;

import java.io.IOException;

/** Interface for a <code>Connection</code> which was rendered by a 
 * <code>Server</code> instance. The extension to <code>Connection</code>
 * consists in the requirement to <u>start</u> or <u>reject</u> a connection
 * before it commences operations. 
 * 
 */
public interface ServerConnection extends Connection {

   /** Starts operations of this server connection. 
    * 
    * @throws IOException
    * @throws IllegalStateException if the socket has lost connection
    */
   public void start() throws IOException;

   /** Rejects a connection which was not yet started. After rejection,
    * a connection cannot be started. 
    * 
    * @throws IOException
    */
   public void reject() throws IOException;

}