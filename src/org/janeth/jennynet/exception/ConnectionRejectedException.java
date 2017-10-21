package org.janeth.jennynet.exception;

import java.net.SocketException;

/** Exception to indicate that that the remote endpoint is a verified JennyNet
 * layer but rejects the connection attempt, possibly giving further detail about
 * the cause in a text argument. This is a subclass of java.net.SocketException.
 *  
 */

public class ConnectionRejectedException extends SocketException {

   public ConnectionRejectedException() {
   }

   public ConnectionRejectedException(String msg) {
      super(msg);
   }

}
