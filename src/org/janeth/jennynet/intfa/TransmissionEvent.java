package org.janeth.jennynet.intfa;

import java.io.File;

/** Interface for a file transmission event issued by a <code>Connection</code>.
 * Transmission events are part of the <code>ConnectionListener</code> event
 * dispatching description.
 * 
 * <p>Transmission events are only concerned with FILE-TRANSFERS. They own a
 * type property defined by <b>enum</b> <code>TransmissionEventType</code>.
 * The types are described below. File transfers can be identified by their ID
 * numbers (long integer) which are within the name-space of OBJECT-IDs. 
 * There are separate name spaces for outgoing and incoming file transfers.
 * 
 * <p>     FILE_INCOMING
 * <br>A new incoming file transfer is indicated for the receiver. With 
 * getPath() the intended relative filepath can be obtained. With getFile() the 
 * temporary file is named which is buffering streamed data.
 *   
 * <p>     FILE_ABORTED
 * <br>A file transfer has been aborted. This is indicated at both sides of
 * the transfer. With some luck, a message about the cause
 * of the event is available. Temporary resources are erased. 
 * 
 * <p>     FILE_RECEIVED
 * <br>Indicated to the receiver, a file transfer has been completed and is 
 * available for use. With getFile() the final location of the file can be 
 * obtained. Notably this may be different to the sender intended location
 * if a problem occurred with storing.
 * In this case the received file will be a TEMPORARY file only and should be 
 * moved by the application as required. Both cases can be discriminated with
 * method haveDestination().
 * 
 * <p>     FILE_CONFIRMED
 * <br>This is the answer available for the sender of a file indicating that 
 * the transfer has been completed and reached its intended destination.
 *  
 * <p>     FILE_FAILED
 * This indicates that a file transfer has terminated with FAILED status due to
 * storage problems at the receiving station. This may be the case if the
 * receiver's system runs out of space or the intended destination as given by
 * the sender cannot be realised.
 */

public interface TransmissionEvent {

   public enum TransmissionEventType {
	  /**  A new incoming file transfer is indicated for the receiver. With 
	   * getPath() the intended relative filepath can be obtained. With getFile() the 
	   * temporary file is named which is buffering streamed data.
	   */
	  FILE_INCOMING,

	  /** A file transfer has been aborted. This event is indicated at both 
	   * sides of the transfer. Details are indicated with the getInfo() value. 
	   * With getPath() the intended relative filepath can be obtained. 
	   * With getFile() the file is named which has been buffering streamed data
	   * (incoming) or which held the data source (outgoing).
	  */
	  FILE_ABORTED,

	  /** Indicates to the receiver the success of a file transfer. 
      * With getFile() the final location of the file can be obtained. 
      */
      FILE_RECEIVED,
      
	  /** Indicates to the sender the success of a file transfer.
       * With getFile() the data source file can be obtained. 
       */
      FILE_CONFIRMED,
      
      /** This indicates that a file transfer has terminated with FAILED status due to
      * realisation problems at the receiving station. This may be the case if the
      * receiver's system runs out of space or the intended destination as given by
      * the sender cannot be realised.
      * With getPath() the intended relative filepath can be obtained. 
	  * With getFile() the file is named which has been buffering streamed data
	  * (incoming) or which held the data source (outgoing).
      */
      FILE_FAILED
   }
   
   /** The connection which issued this event.
    * 
    * @return  <code>Connection</code> source of this event
    */
   public Connection getConnection();

   /** The type of this event. Semantics see class description! 
    * 
    * @return TransmissionEventType
    */
   public TransmissionEventType getType();

   /** Time the transmission was active in milliseconds.
    *  
    * @return long milliseconds
    */
   public long getDuration();

   /** The amount of file data which has actually been exchanged
    * with remote station. This may be smaller than the "expected length"
    * for events issued before transmission was completed.
    * 
    * @return long transmitted data length (bytes)
    */
   public long getTransmissionLength();

   /** The total size of the file to be transfered.
    * (This is known together with FILETRANSFER_INCOMING.)
    * 
    * @return long transmission file length in bytes
    */
   public long getExpectedLength();

   /** The "remote path" information for the transmission.
    * This names the location where the sender intends to store
    * the transfered file a the receiver. <p>This is by definition a relative 
    * path and made absolute at the receiver system against connection parameter
    * FILE_ROOT_PATH. If FILE_ROOT_PATH is undefined the rendered file 
    * will be a TEMPORARY file, but the "remote path" information is still
    * available to give the receiver a clue about the file's semantics.
    * If "remote path" is unavailable (return value null) the rendered file
    * will also be a TEMPORARY file.
    * 
    * @return String file path or null
    */
   public String getPath();

   /** Returns the file received or the file which buffers streaming data 
    * (TEMP-file), depending on the state-of-transmission.
    * Valid for event types FILETRANSFER_RECEIVED and FILETRANSFER_INCOMING,
    * otherwise null.
    * 
    * @return File received/receiving file or null
    */
   public File getFile();

   /** Code to detail the protocol state where this event took place.
    * (Only interesting for debugging and test cases. Ignore!) 
    */
   public int getInfo();

   /** Returns the file transfer identifier. Incoming and outgoing
    * transmission objects share different name spaces, but objects and files
    * share the same space (file is a kind of object in the layer's 
    * understanding).
    * 
    * @return long ID for the file transfer concerned by this event 
    */
   public long getObjectID();

   /** If an error exception is known for the cause of a transfer abort,
    * it is shown here. Otherwise this method returns null.
    * 
    * @return Throwable cause of abort error or null
    */
   public Throwable getException();

}