package org.janeth.jennynet.core;

import org.janeth.jennynet.intfa.Serialization;

class ObjectAgglomeration {

   private ConnectionImpl connection;
   private long objectID;
   private int serialSize, bufferPos;
   private int numberOfParcels;
   private SendPriority priority;
   private Object object;
   private byte[] byteStore;
   
   private int nextParcelNr;
   
   public ObjectAgglomeration (ConnectionImpl connection, long objectID, SendPriority priority) {
      this.connection = connection;
      this.objectID = objectID;
      this.priority = priority;
   }

   public boolean objectReady () {
      return object != null;
   }
   
   /** Returns the de-serialised transmission object.
    * 
    * @return Object object or null if unavailable
    */
   public Object getObject () {
      return object;
   }
   
   /** Returns the priority class by which the contained object was
    * or is being sent.
    * 
    * @return SendPriority
    */
   public SendPriority getPriority () {
	   return priority;
   }
   
   /** Digest a single data parcel into the agglomeration.
    * 
    * @param parcel <code>TransmissionParcel</code>
    * @throws IllegalStateException if parcel is malformed, out of sequence, 
    *         or object serialisation size overflows maximum
    */
   public void digestParcel (TransmissionParcel parcel) {
      // verify fitting
      if (parcel.getChannel() != TransmissionChannel.OBJECT) 
         throw new IllegalArgumentException("illegal parcel channel; must be OBJECT");
         
      if (parcel.getObjectID() != objectID)
         throw new IllegalStateException("mismatching object-ID in agglomeration parcel");

      if (parcel.getParcelSequencelNr() != nextParcelNr) {
         String hstr = objectReady() ? " (object completed)" : "";
         throw new IllegalStateException("PARCEL SERIAL NUMBER out of sequence (object agglomeration); " +
         		" expected: " + nextParcelNr + hstr + ", received: " + parcel.getParcelSequencelNr());
      }

      // initialise on parcel number 0 (HEADER PARCEL)
      if (parcel.getParcelSequencelNr() == 0) {
         ObjectHeader header = parcel.getObjectHeader();
         numberOfParcels = header.getNumberOfParcels();
         serialSize = header.getTransmissionSize();

         // check correctness of indicated object data size 
         if (numberOfParcels < 0 | serialSize < 0) {
        	 throw new IllegalStateException("negative parcel amount or data length detected");
         }
         
         // check serialisation method consistency
         if (header.getSerialisationMethod() != connection.getReceiveSerialization().getMethodID()) {
            throw new IllegalStateException("mismatching serialisation method on RECEIVE OBJECT parcel: "
                  + header.getSerialisationMethod());
         }
         
         // check feasibility of serialisation buffer length 
         if (serialSize > connection.getParameters().getMaxSerialisationSize()) {
            throw new IllegalStateException("received oversized object serialisation: ID=" + objectID +
                  ", serial-size=" + serialSize);
         }
         byteStore = new byte[serialSize];
      }
      
      // add parcel data to byte stream
      try {
         System.arraycopy(parcel.getData(), 0, byteStore, bufferPos, parcel.getLength());
         bufferPos += parcel.getLength();
      } catch (Throwable e) {
         e.printStackTrace();
         throw new IllegalStateException("unable to store parcel BYTE BUFFER in object agglomeration; current size == " 
                     + bufferPos);
      }

      // if last parcel arrived, perform object de-serialisation
      if (nextParcelNr+1 == numberOfParcels) {
         Serialization ser = connection.getReceiveSerialization(); 
         object = ser.deserialiseObject(byteStore);
      } else {
         nextParcelNr++;
      }
      
   }
}
