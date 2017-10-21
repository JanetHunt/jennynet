package org.janeth.jennynet.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Object Header Data is available on the first parcel
 * received for a transmittable object. Likewise, it has
 * to be given to outgoing when the object is split into
 * parcels for sending. Package internal only.
 *
 * The serialisation size of this class is minimum 12 bytes,
 * plus what may be necessary for optional PATH information.
 */

class ObjectHeader {
   
   private long objectID;
   private int method = JennyNet.getDefaultSerialisationMethod(); 
   private int bufferSize;
   private int nrParcels;
   private String path; // * setting required (optional)
   private byte[] serialisedPath; // * setting required (optional)
   
   public ObjectHeader (long objectID) {
      this.objectID = objectID;
   }

   public long getObjectID() {
      return objectID;
   }

   public String getPath() {
      return path;
   }

   public int getSerialisationMethod() {
      return method;
   }

   public int getTransmissionSize() {
      return bufferSize;
   }

   public int getNumberOfParcels() {
      return nrParcels;
   }

   public void writeObject (DataOutputStream output) throws IOException {
      DataOutputStream out = output;
      
      out.write(method);
      out.writeInt(bufferSize);
      out.writeInt(nrParcels);
      
      // write path string if available
      if ( path != null) {
         out.writeShort(serialisedPath.length);
         out.write(serialisedPath);
      } else {
         out.writeShort(0);
      }
   }
   
   /** Returns the length required to write this header to serialisation.
    * 
    * @return int length in bytes
    */
   public int getSerialisedLength () {
      return 5 + (path != null ? serialisedPath.length+2 : 0);
   }
   
   public void readObject (DataInputStream input) throws IOException {
      DataInputStream in = input;
      
      method = in.read();
      bufferSize = in.readInt();
      nrParcels = in.readInt();
      
      // read path string if available
      int len = in.readShort();
      if ( len > 0) {
         serialisedPath = new byte[len];
         in.readFully(serialisedPath);
         path = new String(serialisedPath, JennyNet.getCodingCharset());
      } else {
         path = null;
      }
   }

   public boolean verify() {
      return objectID > 0 & bufferSize > -1 & method > -1 & nrParcels > 0; 
   }

   public void setTransmissionSize (int length) {
      bufferSize = length;
   }

   public void setNrOfParcels (int nrOfParcels) {
      nrParcels = nrOfParcels;
   }

   public void setMethod(int method) {
      this.method = method;
   }

   public void setPath(String path) {
      if (path != null && path.length() > 0xFFFF) 
         throw new IllegalArgumentException("PATH too long!");
      
      this.path = path;
      if ( path != null) {
         serialisedPath = path.getBytes(JennyNet.getCodingCharset());
      } else {
         serialisedPath = null;
      }
   }
   
   
}
