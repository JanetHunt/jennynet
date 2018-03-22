package org.janeth.jennynet.core;

public enum SignalType {
   
   ALIVE,
   ALIVE_ECHO,
   TEMPO,
   BREAK,
   CONFIRM,
   FAIL,
   PING,
   ECHO,
   CLOSE
;

   public static SignalType valueOf (int ordinal) {
      SignalType sp;
      switch (ordinal) {
      case 0 : sp = SignalType.ALIVE; break;
      case 1 : sp = SignalType.ALIVE_ECHO; break;
      case 2 : sp = SignalType.TEMPO; break;
      case 3 : sp = SignalType.BREAK; break;
      case 4 : sp = SignalType.CONFIRM; break;
      case 5 : sp = SignalType.FAIL; break;
      case 6 : sp = SignalType.PING; break;
      case 7 : sp = SignalType.ECHO; break;
      case 8 : sp = SignalType.CLOSE; break;
      default: throw new IllegalArgumentException("undefined ordinal value: " + ordinal);
      }
      return sp;
   }   

}
