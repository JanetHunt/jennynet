package org.jhunt.jennynet.intfa;

import java.io.File;

public interface TransmissionEvent {

   public enum TransmissionEventType {
      FILETRANSFER_INCOMING,
      FILETRANSFER_ABORTED,
      FILETRANSFER_RECEIVED,
      FILETRANSFER_CONFIRMED,
      FILETRANSFER_FAILED
   }
   
   public Connection getConnection();

   public TransmissionEventType getType();

   /** Time the transmission was active in milliseconds.
    *  
    * @return long milliseconds
    */
   public long getDuration();

   /** The amount of data which has actually been exchanged
    * with remote station.
    * 
    * @return long transmitted data length (bytes)
    */
   public long getTransmissionLength();

   /** The total size of the file to be transfered.
    * 
    * @return long transmission file length
    */
   public long getExpectedLength();

   /** The "remote path" information for the transmission.
    * 
    * @return String file path or null
    */
   public String getPath();

   /** Whether a received file is at the intended path destination.
    * 
    * @return boolean true == file is at destination, false == file is TEMP file
    */
   public boolean isDestination();

   /** Returns the file received. (Valid for event types FILETRANSFER_RECEIVED and
    * FILETRANSFER_INCOMING)
    * 
    * @return File received file or null
    */
   public File getFile();

   public int getInfo();

   public long getObjectID();

   public Throwable getException();

}