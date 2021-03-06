package org.janeth.jennynet.core;

import org.janeth.jennynet.util.Util;

public class Signal extends TransmissionParcel {
   private SignalType sigType;
   private int info;
   private String text;
   
   public Signal (SignalType s, long objectID, int info, String text) {
      super(s, objectID, info, text);
      this.sigType = s;
      this.info = info;
      this.text = text;
   }

   public Signal (SignalType s, long objectID) {
      this(s, objectID, 0, null);
   }
   
   /** Creates a Signal from a signal parcel.
    * 
    * @param parcel TransmissionParcel
    * @throws 
    */
   public Signal (TransmissionParcel parcel) {
      super(parcel);
      
      if (parcel.getChannel() != TransmissionChannel.SIGNAL)
         throw new IllegalArgumentException("parcel not in SIGNAL channel");
    
      int serialNr = getParcelSequencelNr();
      this.sigType = SignalType.valueOf(serialNr & 0xFFFF);
//      this.info = serialNr >>> 16;
      byte[] data = getData();
      this.text = data == null ? null : 
    	          data.length == 4 ? null : new String(data, 4, data.length-4, JennyNet.getCodingCharset());
      this.info = data == null ? 0 : Util.readInt(data, 0); 
   }

   public SignalType getSigType() {
      return sigType;
   }

   public int getInfo() {
      return info;
   }

   public String getText() {
      return text;
   }
/*
   public long getObjectID () {
      return objectID;
   }
*/   
   /** Creates a new BREAK transmission signal for a transmission object
    * and a reason.
    *   
    * @param objectID long ID of object which is broken
    * @param text String cause for the break (may be null)
    * @return
    */
   public static Signal newBreakSignal (long objectID, int info, String text) {
	  Signal s = new Signal(SignalType.BREAK, objectID, info, text);
	  s.setPriority(SendPriority.High);
      return s;
   }
   
   public static Signal newPingSignal (long pingID) {
	  Signal s = new Signal(SignalType.PING, pingID);
	  s.setPriority(SendPriority.Top);
      return s;
   }
   
   public static Signal newEchoSignal (long pingID) {
	  Signal s = new Signal(SignalType.ECHO, pingID);
	  s.setPriority(SendPriority.Top);
      return s;
   }

   public static Signal newAliveSignal () {
	  Signal s = new Signal(SignalType.ALIVE, 0);
	  s.setPriority(SendPriority.Bottom);
      return s;
   }
   
   public static Signal newAliveEchoSignal () {
	  Signal s = new Signal(SignalType.ALIVE_ECHO, 0);
	  s.setPriority(SendPriority.Bottom);
      return s;
   }
   
   public static Signal newTempoSignal (int baud) {
	  Signal s = new Signal(SignalType.TEMPO, 0, baud, null);
	  s.setPriority(SendPriority.Low);
      return s;
   }
   
   public static Signal newConfirmSignal (long objectID) {
      return new Signal(SignalType.CONFIRM, objectID);
   }
   
   public static Signal newFailSignal (long objectID, int info, String text) {
      return new Signal(SignalType.FAIL, objectID, info, text);
   }
   

   
}
