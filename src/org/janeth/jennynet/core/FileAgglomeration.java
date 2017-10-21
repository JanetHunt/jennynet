package org.janeth.jennynet.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.janeth.jennynet.exception.InsufficientFileSpaceException;
import org.janeth.jennynet.exception.ParcelOutOfSyncException;
import org.janeth.jennynet.exception.ParcelProtocolErrorException;
import org.janeth.jennynet.intfa.TransmissionEvent.TransmissionEventType;
import org.janeth.jennynet.util.Util;

/** Class to collect data parcels of the FILE type in order to build up
 * a data file which was transmitted over the net. An instance of this class
 * takes reference to a <code>Connection</code> and an object header (<code>
 * IObjectHeader</code>, which gives the required technical parameters for the
 * transmission. These parameters are:
 * <p><b>Object-ID (=File-ID)</b> - a long integer identifying the transmission
 * <br><b>expected file length</b> - an integer for the file length (Integer.MAX_VALUE)
 * <br><b>expected number of parcels</b> - integer
 * <br><b>target filepath (=PATH)</b> - String to identify an output path and name for the 
 * transmitted file. See the special convention for this variable below.
 * 
 * <p><b>FILE PATH and File Storage Convention</b>
 * <p>An ongoing file transmission is always stored in a TEMPORARY file which is allocated 
 * in the JennyNet global TEMP directory. When transmission completes, the file is copied 
 * to its final DESTINATION and the incoming event issued to the user with reference to
 * this last copy. The DESTINATION is defined using the PATH variable in transmission header.
 * <p><u>The following convention holds</u>: If the PATH variable is void, or the DESTINATION path
 * cannot be allocated, or the DESTINATION partition cannot hold the file size, the TEMP file 
 * is reported to the user. Otherwise the DESTINATION file is reported and the TEMP file 
 * deleted. If the PATH variable 
 * holds a file path (which may be noted absolute or relative), this path is made relative
 * to the connection's FILE ROOT DIRECTORY. If that directory is undefined or does not exist
 * or the resulting DESTINATION path is invalid, file allocation fails. A DESTINATION path
 * is valid if its canonical path starts with the FILE ROOT DIRECTORY and does not name a
 * directory. A DESTINATION path may contain path elements which don't exist and which are
 * realised when the file is created.
 */

class FileAgglomeration extends ParcelAgglomeration {
   // init data
   private long fileID;
   private String path;
   private long expectedFileLength;
   private long receivedFileLength;
   private int expectedNrOfParcels;

   // operational
   private ConnectionImpl connection;
   private int nextParcelNr;
   private long startTime, duration;
   private File file;  // output file during data collection
   
   private File destination;  // remote indicated output file after transmission (may be null)
   private OutputStream fileOutput;
   
   /**
    * Creates a new parcel agglomeration device for an incoming file transmission.
    * 
    * @param connection Connection
    * @param fileID long object number to reference
    * @throws IOException if local resources cannot be allocated
    */
   
   public FileAgglomeration (ConnectionImpl connection, long fileID) {
      super(connection);
      if (connection == null) 
         throw new NullPointerException("connection == null");
      
      this.connection = connection;
      this.fileID = fileID;
   }

   private void init (ObjectHeader header) throws IOException {
      if (header == null) 
         throw new NullPointerException("header == null");
      
      // technical
      fileID = header.getObjectID();
      path = header.getPath();
      expectedNrOfParcels = header.getNumberOfParcels();
      expectedFileLength = header.getTransmissionSize();
      startTime = System.currentTimeMillis();
      
      // create operational output file (temporary)
      file = Util.getTempFile(connection.getParameters().getTempDirectory());
      
      // set up a thread name 
      String name = "FILE: " + file.getAbsolutePath();
      setName(name);

      // verify ultimate storage path for transmitted file
      // only valid if connection has a file root path defined
      File conRootDir = connection.getParameters().getFileRootDir();
      try {
         if (path != null & conRootDir != null && conRootDir.isDirectory()) {
            // create file path for the destination file
            File test = new File(conRootDir, path).getCanonicalFile();

            // accept remote indicated destination path only if it 
            // falls under connection's file root directory
            // (must do this because ".." elements in PATH can lead to irritating results!)
            if (test.getPath().startsWith(conRootDir.getPath())) {
               destination = test;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      // verify storage space
      if (file.getFreeSpace() < expectedFileLength + 32000) {
         throw new InsufficientFileSpaceException("demanded bytes: " + expectedFileLength + 
               " on " + file.getParent());
      }
      
      // create output stream
      fileOutput = new FileOutputStream(file);

      // inform user about NEW FILE INCOMING
      TransmissionEventImpl event = new TransmissionEventImpl(connection,
            TransmissionEventType.FILE_INCOMING, fileID);
      event.setExpectedLength(expectedFileLength);
      event.setPath(path);
      event.setFile(file);
      connection.fireTransmissionEvent(event);
   }
   
   /**
    * This method adds a FILE data parcel to the data field of the reception file.
    * The parcel serial number must be in sequence of its predecessor, so that only
    * a  closed series of processed parcels will be permitted in order to build up 
    * the file. 
    * 
    * @param parcel TransmissionParcel
    */
   @Override
   protected void processReceivedParcel (TransmissionParcel parcel)
         throws Exception {
      // check the parcel CHANNEL
      if (parcel.getChannel() != TransmissionChannel.FILE)  
         throw new IllegalArgumentException("false channel");
    
      // paranoically check the object number
      if (parcel.getObjectID() != fileID)  
         throw new IllegalArgumentException("false object (gush!)");
    
      // test for expected parcel sequence number 
      if (parcel.getParcelSequencelNr() != nextParcelNr) {
         throw new ParcelOutOfSyncException("FILE TRANSFER: object-ID = " + fileID + 
               ", parcel-nr = " + parcel.getParcelSequencelNr() + ", expected = " + nextParcelNr +
               "\n" + path);
      }
      
      // initialise Agglomeration on parcel == 0
      if (parcel.getParcelSequencelNr() == 0) {
         ObjectHeader header = parcel.getObjectHeader();
         if (header == null) {
            // PROTOCOL ERROR
            throw new ParcelProtocolErrorException("NO OBJECT HEADER on new FILE transfer, obj=" +
                  fileID + ", parcel=0");
         }
         init(header);
      }
      
      // write parcel data to file
      if (parcel.getLength() > 0 && fileOutput != null) {
    	 synchronized(fileOutput) {
    		 byte[] data = parcel.getData();
    		 fileOutput.write(data);
    		 receivedFileLength += data.length;
    	 }
      }
      
      // promote expected parcel number
      nextParcelNr++;
      if (nextParcelNr == expectedNrOfParcels) {
         finishFileOutput();
      }
   }

   private void finishFileOutput() throws IOException {
      if (fileOutput == null) return;
      cancelTransfer();
      boolean haveDestination = false;
      boolean success = true;
      
  	  synchronized(fileOutput) {
	      fileOutput.close();
	      fileOutput = null;
	      
	      // if we have a destination path defined for the transmission
	      // attempt copy from temp-file to the destination
	      if (path != null) {
	         
	         // verify destination file and destination drive space
	         if (destination == null || destination.isDirectory() || 
	             connection.getParameters().getFileRootDir().getFreeSpace() < file.length() + 32000
	             ) {
	            // cannot realise destination file (environment reasons)
	            success = false;
	            
	         } else {
	            // create destination file
	            destination.getParentFile().mkdirs();
	            OutputStream out = new FileOutputStream(destination);
	            InputStream in = null;
	            try {
	               in = new FileInputStream(file);
	               Util.transferData(in, out, JennyNet.STREAM_BUFFER_SIZE);
	               haveDestination = true;
	            } finally {
	               if (in != null) {
	                  in.close();
	               }
	               out.close();
	            }
	         }
	      }
	      
	      // remove the TEMP file if destination copy is successful
	      // and re-define file as destination
	      if (haveDestination) {
	         file.delete();
	         file = destination;
	      }
  	  }

      // signal transfer success or failure to remote
      // (failure prevails if a file destination could not be realised)
      Signal signal = success ? Signal.newConfirmSignal(fileID) : 
                      Signal.newFailSignal(fileID, 1, null);    
      connection.sendSignal(signal);
      
      // inform the user about file-received (event)
      TransmissionEventImpl event = new TransmissionEventImpl(connection, 
            TransmissionEventType.FILE_RECEIVED, fileID, file, path, 
            haveDestination);
      event.setTransmissionLength(receivedFileLength);
      event.setExpectedLength(expectedFileLength);
      event.setDuration(getDuration());
      connection.fireTransmissionEvent(event);
   }

   @Override
   protected void exceptionThrown(Throwable e) {
      e.printStackTrace();
      dropTransfer(1, 1, e);
   }

   /** Terminates this file agglomeration by shutting down the thread
    * and removing it from the file-receptor map. Silent operation, no signals
    * sent or events reported! Reception file remains untouched.
    */
   private void cancelTransfer () {
      // terminate thread specific resources (and the collection thread itself)
      connection.removeFileReceptor(fileID);
      duration = System.currentTimeMillis() - startTime;
      super.terminate();
   }

   /** Drops this file agglomeration including removing its 
    * reception file (TEMP) and optionally issuing a signal 
    * to remote and a layer event to the user.
    * 
    * @param eventInfo int if != 0 a user event will be thrown 
    * @param signalInfo int if != 0 a BREAK signal will be sent to remote 
    * @param e Throwable, may be null
    */
   public void dropTransfer (int eventInfo, int signalInfo, Throwable e) {
      cancelTransfer();
      
      // erase reception file
      if (fileOutput != null) {
	  	  synchronized(fileOutput) {
		      try {
		         fileOutput.close();
		         fileOutput = null;
		      } catch (IOException e1) {
		         e1.printStackTrace();
		      }
		      file.delete();
	  	  }
      }
         
      // if opted, signal remote about transmission break
      if (signalInfo != 0 && connection.isConnected()) {
         String text = e == null ? null : e.toString();
         connection.sendSignal(Signal.newBreakSignal(fileID, signalInfo, text));
      }

      if (eventInfo != 0) {
         // inform the user (abortion event)
         TransmissionEventImpl event = new TransmissionEventImpl(connection,
              TransmissionEventType.FILE_ABORTED,
              fileID, eventInfo, e );
         event.setDuration(getDuration());
         event.setPath(path);
         event.setTransmissionLength(receivedFileLength);
         event.setExpectedLength(expectedFileLength);
         connection.fireTransmissionEvent(event);
      }
   }

   @Override
   public void terminate() {
      cancelTransfer();
   }
   
   public String getPath() {
      return path;
   }

   public long getDuration() {
      return duration;
   }

   public File getFile() {
      return file;
   }

}
