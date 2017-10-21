package org.janeth.jennynet.exception;

import java.io.IOException;

public class InsufficientFileSpaceException extends IOException {

   public InsufficientFileSpaceException() {
   }

   public InsufficientFileSpaceException(String message, Throwable cause) {
      super(message, cause);
   }

   public InsufficientFileSpaceException(String message) {
      super(message);
   }

   public InsufficientFileSpaceException(Throwable cause) {
      super(cause);
   }

}
