package org.janeth.jennynet.core;

import java.io.File;
import java.util.EventObject;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.TransmissionEvent;

class TransmissionEventImpl extends EventObject implements TransmissionEvent {
   private TransmissionEventType type;
   private long objectID;
   private int info;
   private Throwable exception;
   private long duration;
   private long transmittedLength, expectedLength;
   private String path;
   private File file;
   private boolean destinationRealised;
   
   public TransmissionEventImpl (
         Connection connection, 
         TransmissionEventType type, 
         long objectID,
         int info, 
         Throwable e 
         ) {
      
      super(connection);

      this.type = type;
      this.setObjectID(objectID);
      this.info = info;
      this.setException(e);
   }

   public TransmissionEventImpl (
         Connection connection, 
         TransmissionEventType type, 
         long objectID,
         int info 
         ) {
      this(connection, type, objectID, info, null);
   }
   
   public TransmissionEventImpl (
         Connection connection, 
         TransmissionEventType type, 
         long objectID
         ) {
      this(connection, type, objectID, 0, null);
   }

   /** Creates a transmission event for the types FILETRANSFER_INCOMING and
    * FILETRANSFER_RECEIVED.
    * 
    * @param connection Connection
    * @param type TransmissionEventType
    * @param fileID long file number (object number)
    * @param file File reception file
    * @param pathInfo String (may be null)
    * @param haveDestination boolean whether destination (pathInfo) has been realised
    */
   public TransmissionEventImpl(
         ConnectionImpl connection,
         TransmissionEventType type, 
         long fileID,
         File file, 
         String pathInfo,
         boolean haveDestination 
         ) {
      this(connection, type, fileID);
      if (type != TransmissionEventType.FILE_INCOMING &
          type != TransmissionEventType.FILE_RECEIVED ) 
         throw new IllegalArgumentException("illegal event type: " + type);
      if (file == null)
         throw new NullPointerException("file == null");
      
      this.file = file;
      this.path = pathInfo;
      this.destinationRealised = haveDestination;
   }

   protected void setObjectID (long objectID) {
      if (objectID < 1)
         throw new IllegalArgumentException("illegal object number: " + objectID);
      
      this.objectID = objectID; 
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getConnection()
    */
   @Override
   public Connection getConnection () {
      return (Connection)source;
   }
   
   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getType()
    */
   @Override
   public TransmissionEventType getType () {
      return type;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getDuration()
    */
   @Override
   public long getDuration() {
      return duration;
   }

   /** Sets the duration of the transmission.
    * 
    * @param duration long milliseconds
    */
   protected void setDuration(long duration) {
      this.duration = duration;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getTransmissionLength()
    */
   @Override
   public long getTransmissionLength() {
      return transmittedLength;
   }

   /** Sets the actually transmitted data length.
    * 
    * @param transmittedLength long data length (bytes)
    */
   protected void setTransmissionLength(long transmittedLength) {
      this.transmittedLength = transmittedLength;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getExpectedLength()
    */
   @Override
   public long getExpectedLength() {
      return expectedLength;
   }

   /** Sets the total size of the file to be transfered.
    * 
    * @param length long transmission file length
    */
   protected void setExpectedLength(long length) {
      this.expectedLength = length;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getPath()
    */
   @Override
   public String getPath() {
      return path;
   }

   /** Sets the "remote path" information for the transmission.
    * 
    * @param path String file path (may be null)
    */
   protected void setPath(String path) {
      this.path = path;
   }
   
   /** Sets the File information on this event.
    * 
    * @param f File current reception file
    */
   protected void setFile (File f) {
      this.file = f;
   }
   
   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#isDestination()
    */
   @Override
   public boolean haveDestination () {
      return destinationRealised;
   }
   
   /** Sets whether the transferred file has been realised at its intended
    * destination.
    * 
    * @param realised boolean true == file is at destination, false == file is TEMP file
   protected void setDestinationRealised (boolean realised) {
      destinationRealised = realised;
   }
    */

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getFile()
    */
   @Override
   public File getFile () {
      return file;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getInfo()
    */
   @Override
   public int getInfo() {
      return info;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getObjectID()
    */
   @Override
   public long getObjectID() {
      return objectID;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.TransmissionEvent#getException()
    */
   @Override
   public Throwable getException() {
      return exception;
   }

   protected void setException(Throwable exception) {
      this.exception = exception;
   }

}
