package org.jhunt.jennynet.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class serves as a means to set, retrieve and administer a set of parameters
 * for a <code>Connection</code>. By creating a new instance, parameter values equal 
 * their counterparts of the global layer level (specified via <code>JennyNet</code>
 * class). No action of the user is required; a connection always owns a valid set
 * of parameters. 
 *   
 */
public class ConnectionParameters implements Cloneable {
   /** The minimum data transport capacity of a transmission parcel. */
   protected static final int PARCEL_SIZE_MIN = 128;

   private int baseThreadPriority = JennyNet.getBaseThreadPriority();
   private int transmitThreadPriority = JennyNet.getTransmitThreadPriority();
   private int transmissionParcelSize = JennyNet.getTransmissionParcelSize();
   private int parcelQueueCapacity = JennyNet.getParcelQueueCapacity();
   private int objectQueueCapacity = JennyNet.getObjectQueueCapacity();
   private int aliveTimeout = JennyNet.getAlive_timeout();
   private int alivePeriod;
   private int confirmTimeout = JennyNet.getConfirm_timeout();
   private int serialMethod = JennyNet.getDefaultSerialisationMethod();
   private File fileRootDir = JennyNet.getDefaultTransmissionRoot();
   private File fileTempDir = JennyNet.getDefaultTempDirectory();
   private Charset codingCharset = JennyNet.getCodingCharset();

   public ConnectionParameters() {
      set_Alive_Period();
   }
   
   public Object clone () {
      try {
         return super.clone();
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
   
   /** Returns the thread priority of the service threads involved in
    * data processing. This includes threads which
    * deliver received objects to the application or handle outgoing
    * objects handed to the connection. Defaults to Thread.NORM_PRIORITY.
    *  
    *  @return int thread priority
    */
   public int getBaseThreadPriority() {
      return baseThreadPriority;
   }

   /** Sets thread priority (BASE_THREAD_PRIORITY) of the service threads
    * involved in data processing. 
    * This concerns threads dealing with the user interface like
    * object serialisation or delivering received objects to the application via 
    * registered connection listeners.
    * <p>Note: TRANSMIT_THREAD_PRIORITY is guaranteed to not fall below
    * the value of BASE_THREAD_PRIORITY. By this rule it may be 
    * implicitly raised in the course of this setting. 
    * <p><small>To ensure maximum efficiency, throughput and reactivity of the 
    * network layer, it can be considered to set this value above an application's
    * standard thread priority. This is particularly useful on multi-core
    * computers. Where networking falls second rank and is meant to
    * never disturb the speed of the rest of application, it can be set below the
    * standard thread priority.</small>
    *  
    * @param p int thread priority
    * @throws IllegalArgumentException if value is out of range
    * @see #setTransmitThreadPriority(int)
    */
   public void setBaseThreadPriority(int p) {
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY) {
         baseThreadPriority = p;
         transmitThreadPriority = Math.max(baseThreadPriority, transmitThreadPriority);
      } else {
         throw new IllegalArgumentException("illegal priority value: ".
               concat(String.valueOf(p)));
      }
   }

   /** Returns the thread priority of the core data transmission threads
    *  involved in dealing with socket communication. Defaults to
    *  Thread.MAX_PRIORITY.
    *  
    *  @return int thread priority
    */
   public int getTransmitThreadPriority() {
      return transmitThreadPriority;
   }

   /** Sets thread priority (TRANSMIT_THREAD_PRIORITY) of the core data
    *  transmission threads involved in dealing with socket communication. This
    *  value can not be less than BASE_THREAD_PRIORITY.
    *  
    *  <p><small>Note: For minimised latency of net transmission it is recommended 
    *  to set this value above BASE_THREAD_PRIORITY. Leaving it at MAX_PRIORITY
    *  is not expected to disturb the thread logic of any application or create extra load
    *  but only ensures that network socket activity is immediately performed when data 
    *  is ready to send or available to receive. </small> 
    *  
    * @param p int thread priority 
    * @throws IllegalArgumentException if value is out of range or below BASE_THREAD_PRIORITY
    * @see #setBaseThreadPriority(int)
    */
   public void setTransmitThreadPriority(int p) {
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY
            & p >= baseThreadPriority) {
           transmitThreadPriority = p;
      } else {
           throw new IllegalArgumentException("illegal priority value: ".
                 concat(String.valueOf(p)));
      }
   }

   /** Returns the value for capacity of queues handling with data parcels.
    * Defaults to 200.
    * 
    * @return int maximum number of parcels in a queue
    */
   public int getParcelQueueCapacity() {
      return parcelQueueCapacity;
   }

   /** Sets the value for capacity of queues handling with data parcels.
    * 
    * @param c int maximum number of parcels (10 .. 10000)
    * @throws IllegalArgumentException if parameter is out of range 
    */
   public void setParcelQueueCapacity(int parcelQueueCapacity) {
      if (parcelQueueCapacity < 10 | parcelQueueCapacity > 10000) 
         throw new IllegalArgumentException("queue capacity out of range (10..10000)");
      this.parcelQueueCapacity = parcelQueueCapacity;
   }

   /** Returns the queue capacity for sending objects over
    * the net. Attempts to send an object 
    * with a full send queue results in an exception thrown.
    * Defaults to 200.
    * 
    * @return int maximum number of objects in queue
    */
   public int getObjectQueueCapacity() {
      return objectQueueCapacity;
   }

   /** Sets the queue capacity for sending objects over the net. 
    * Has a minimum of 1.
    * 
    * @param cap int maximum number of objects in queue
    * @throws IllegalArgumentException if value is below 1
    */
   public void setObjectQueueCapacity(int objectQueueCapacity) {
      if (objectQueueCapacity < 1)
         throw new IllegalArgumentException("illegal capacity; minimum = 1");
      this.objectQueueCapacity = objectQueueCapacity;
   }

   /** The current value of ALIVE_PERIOD.
    * This value determines the period for a connection to dispatch
    * ALIVE signals to remote to indicate that it is still alive.
    * ALIVE signals are only dispatched when no other network activity
    * is ongoing.
    * 
    * @param timeout int milliseconds (minimum 500)
    */
   public int getAlivePeriod () {
      return alivePeriod;
   }
   
   /** Sets value of ALIVE_PERIOD in dependence of ALIVE_TIMEOUT and
    * CONFIRM_TIMEOUT.
    */
   private void set_Alive_Period () {
      alivePeriod = aliveTimeout - confirmTimeout/2;
   }
   
   /** The value of parameter ALIVE_TIMEOUT.
    * Value determines the maximum time since the last network activity
    * of a socket until the remote station is categorised as DEAD and
    * consequently the connection closed. Defaults to 20,000.
    *  
    * @return int timeout in milliseconds
    */
   public int getAliveTimeout() {
      return aliveTimeout;
   }

   /** Sets the value of ALIVE_TIMEOUT.
    * Value determines the maximum time since the last network activity
    * of a socket until the remote station is categorised as DEAD and
    * consequently the connection closed.
    *  
    * @param timeout int milliseconds (minimum 1000)
    * @throws IllegalArgumentException if parameter is below 1000 or mismatching
    */
   public void setAliveTimeout(int timeout) {
      if (timeout < 1000) 
         throw new IllegalArgumentException("timeout minimum = 1000");
      if (timeout - confirmTimeout/2 < 500) 
         throw new IllegalArgumentException("illegal ALIVE value: " + timeout 
               + "; results in ALIVE_PERIOD below 500");
      
      aliveTimeout = timeout;
      set_Alive_Period();
   }

   /** The value of parameter CONFIRM_TIMEOUT.
    * This value determines the maximum time for the connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the connection will react depending on the case. 
    * Defaults to 10,000.
    * 
    * @return int timeout in milliseconds
    */  
   public int getConfirmTimeout() {
      return confirmTimeout;
   }

   /** Sets the value for CONFIRM_TIMEOUT.
    * This value determines the maximum time for the connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the connection will react depending on the case. 
    *  
    * @param timeout int milliseconds (minimum 1000)
    * @throws IllegalArgumentException if parameter is below 1000 or mismatching
    */
   public void setConfirmTimeout(int timeout) {
      if (timeout < 1000) 
         throw new IllegalArgumentException("timeout minimum = 1000");
      if (aliveTimeout - timeout/2 < 500) 
         throw new IllegalArgumentException("illegal CONFIRM value: " + timeout 
               + "; results in ALIVE_PERIOD below 500");
      
      confirmTimeout = timeout;
      set_Alive_Period();
   }

   /** Returns the code number of the object serialisation method 
    * applied by the connection.
    * 
    * @return int method code
    */
   public int getSerialisationMethod() {
      return serialMethod;
   }

   /** Sets the method for object serialisation.
    * (Currently there is only one serialisation method
    * available and this setting ignored!)
    * 
    * @param methodID int method
    */
   public void setSerialisationMethod(int method) {
      // we only use method 0 = kryo this time
//      this.serialMethod = serialMethod;
   }

   /** Returns the root directory (TRANSFER_ROOT_PATH) for 
    * incoming file transmissions. Defaults to null. 
    * <p><small>Null implies that received file transmissions will be 
    * stored and reflected as temporary files only. For mechanics of file
    * transfers see the user manual.</small>
    * 
    * @return File directory
    */
   public File getFileRootDir() {
      return fileRootDir;
   }

   /** Sets the root directory (TRANSFER_ROOT_PATH) for incoming 
    * file transmissions. If not null, the parameter must be 
    * an existing directory.
    * <p><small>Null implies that received file transmissions will be stored and reflected 
    * as temporary files only. For mechanics of file transfers see the user manual.</small>
    * 
    * @param dir File directory
    * @throws IllegalArgumentException if parameter is not a directory
    * @throws IOException if the path cannot be verified (canonical name)
    */
   public void setFileRootDir(File dir) throws IOException {
      if (dir == null) {
         fileRootDir = null;
      } else {
         if (!dir.isDirectory())
            throw new IllegalArgumentException("parameter is not a directory");

         fileRootDir = dir.getCanonicalFile();
      }
   }

   /** Returns the TEMP directory for the connection.
    * 
    * @return File directory
    */
   public File getTempDirectory() {
      return fileTempDir;
   }

   /** Sets the directory for TEMPORARY files. The parameter must be 
    * an existing directory.
    * 
    * @param dir File TEMP directory
    * @throws IOException 
    * @throws IllegalArgumentException if parameter is null or not a directory
    * @throws IOException if the path cannot get verified (canonical name)
    */
  public void setTempDirectory (File dir) throws IOException {
      if (dir == null)
         throw new IllegalArgumentException("dir == null");
      if (!dir.isDirectory())
         throw new IllegalArgumentException("parameter is not a directory");

      fileTempDir = dir.getCanonicalFile();
   }
   
   /** Returns the setting for TRANSMISSION_PARCEL_SIZE in the connection.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum. Defaults to 8192.
    * 
    * @return int maximum data size of a transmission parcel 
    */
   public int getTransmissionParcelSize() {
      return transmissionParcelSize;
   }

   /** Sets the value for TRANSMISSION_PARCEL_SIZE for the connection.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum.  The value has a range of 1k to 128k (0x400 .. 0x20000).

    * 
    * @param size int maximum capacity of transmission parcels (bytes)
    */
   public void setTransmissionParcelSize (int size) {
      transmissionParcelSize = Math.min( Math.max(size, 
            JennyNet.MIN_TRANSMISSION_PARCEL_SIZE), JennyNet.MAX_TRANSMISSION_PARCEL_SIZE );
   }
   
   /** The charset used for layer internal use.
    * (Does not reflect object serialisation methods!)
    * 
    * @return Charset text coding charset
    */
   public Charset getCodingCharset() {
      return codingCharset;
   }

   /** Sets the charset used for layer internal text encoding.
    * (Does not affect object serialisation methods!)
    * 
    * @param charset Charset
    */
   public void setCodingCharset(Charset charset) {
      if (charset == null)
         throw new NullPointerException();
      this.codingCharset = charset;
   }

}
