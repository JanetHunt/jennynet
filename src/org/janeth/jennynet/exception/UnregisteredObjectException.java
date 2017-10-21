package org.janeth.jennynet.exception;

/** Thrown when an object is targeted for sending which type is not
 * registered for transmission at the connection.
 *  
 */
public class UnregisteredObjectException extends JennyNetException {

   public UnregisteredObjectException() {
   }

   public UnregisteredObjectException(String message, Throwable cause) {
      super(message, cause);
   }

   public UnregisteredObjectException(String message) {
      super(message);
   }

   public UnregisteredObjectException(Throwable cause) {
      super(cause);
   }

}
