package org.jhunt.jennynet.intfa;

import java.io.File;

/** Interface for a transmission event issued by a <code>Connection</code>.
 * Transmission events are part of the <code>ConnectionListener</code> event
 * dispatching description.
 * 
 * <p>Transmission events are only concerned with FILE-TRANSFERS. They own a
 * type property defined by <b>enum</b> <code>TransmissionEventType</code>.
 * The types are described below. File transfers can be identified by their ID
 * numbers (long integer). There are separate name spaces for outgoing and incoming
 * file transfers.
 * 
 * <p>     FILETRANSFER_INCOMING
 * <br>A new incoming file transfer is indicated. With getPath() the intended
 * location and semantics of the file can be traced. With getFile() the temporary
 * file buffering stream data is named.
 *   
 * <p>     FILETRANSFER_ABORTED
 * <br>A file transfer has been aborted. With luck, a message for the cause of this
 * is available. 
 * 
 * <p>     FILETRANSFER_RECEIVED
 * <br>A file transfer has been completed and is available for application use 
 * (rendered ready). With getFile() the final location of the received file is named.
 *  Notably this may be different to the intended location (given
 *  by the sender) if a problem occurred with the storage of the intended location.
 *  In this case the received file will be a TEMPORARY file only and should be moved
 *  or consumed by the application.
 * 
 * <p>     FILETRANSFER_CONFIRMED
 * <br>This is the answer for the sender of a file transfer indicating that 
 * the transfer has completed and reached its intended destination. 
 *  
 * <p>     FILETRANSFER_FAILED
 * This indicates that a file transfer has completed with FAILED status due to
 * storage problems on the remote side.
 */

public interface TransmissionEvent {

   public enum TransmissionEventType {
      FILETRANSFER_INCOMING,
      FILETRANSFER_ABORTED,
      FILETRANSFER_RECEIVED,
      FILETRANSFER_CONFIRMED,
      FILETRANSFER_FAILED
   }
   
   /** The connection which issued this event. (Source of event.)
    * 
    * @return  <code>Connection</code> source of this event
    */
   public Connection getConnection();

   /** The type of this event. See class description! 
    * 
    * @return TransmissionEventType
    */
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
    * (This is known with FILETRANSFER_INCOMING.)
    * 
    * @return long transmission file length
    */
   public long getExpectedLength();

   /** The "remote path" information for the transmission.
    * This names the location where the sender intends to store
    * the transfered file. <p>This is by definition a relative path
    * and made absolute against connection parameter FILE_ROOT_PATH.
    * If FILE_ROOT_PATH is undefined the rendered file will be a 
    * TEMPORARY file, but the "remote path" information is still present
    * to give a clue about the file's semantics.
    * 
    * @return String file path or null
    */
   public String getPath();

   /** Whether a received file is at the sender intended path destination.
    * 
    * @return boolean true == file is at destination, false == file is TEMP file
    */
   public boolean isDestination();

   /** Returns the file received or the file which buffers stream data. 
    * Valid for event types FILETRANSFER_RECEIVED and FILETRANSFER_INCOMING.
    * 
    * @return File received/receiving file or null
    */
   public File getFile();

   /** Code to detail the protocol state where this event took place.
    * (Only interesting for specialists. Ignore!) 
    */
   public int getInfo();

   /** Returns the file transfer identifier. Incoming and outgoing transmission
    * objects share different name spaces, but objects and files share the same
    * space (file is a kind of object in the layer's understanding).
    * 
    * @return long ID for the file transfer concerned by this event 
    */
   public long getObjectID();

   /** If an error exception is known as to the cause of a transfer abort,
    * it is shown here. Otherwise this method returns null.
    * 
    * @return Throwable cause of abort error or null
    */
   public Throwable getException();

}