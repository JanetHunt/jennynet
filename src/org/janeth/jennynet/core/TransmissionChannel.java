package org.janeth.jennynet.core;


enum TransmissionChannel {
   SIGNAL,
   OBJECT,
   FILE
;

public static TransmissionChannel valueOf (int ordinal) {
   TransmissionChannel sp;
   switch (ordinal) {
   case 0 : sp = TransmissionChannel.SIGNAL; break;
   case 1 : sp = TransmissionChannel.OBJECT; break;
   case 2 : sp = TransmissionChannel.FILE; break;
   default: throw new IllegalArgumentException("undefined ordinal value: " + ordinal);
   }
   return sp;
}
}

