package org.janeth.jennynet.exception;

public class IllegalFileLengthException extends JennyNetException {

   public IllegalFileLengthException() {
   }

   public IllegalFileLengthException(String message, Throwable cause) {
      super(message, cause);
   }

   public IllegalFileLengthException(String message) {
      super(message);
   }

   public IllegalFileLengthException(Throwable cause) {
      super(cause);
   }

}
