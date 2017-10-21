package org.janeth.jennynet.exception;

import java.net.SocketException;

/** Exception to indicate that that the remote endpoint does not comply
 * with the JENNYNET handshake protocol. Reason could be that the remote listener 
 * is not a JennyNet software layer.  This is a subclass of java.net.SocketException.
 * 
 *  
 */

public class JennyNetHandshakeException extends SocketException {

   private static final long serialVersionUID = -6670055890266924934L;

   public JennyNetHandshakeException() {
   }

   public JennyNetHandshakeException(String msg) {
      super(msg);
   }

}
