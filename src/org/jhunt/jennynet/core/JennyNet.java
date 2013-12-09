package org.jhunt.jennynet.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.jhunt.jennynet.intfa.ISerialization;

/** Class for global settings for the JennyNet networking service.
 * 
 * @author janet hunt
 *
 */

public class JennyNet {

   public static final int PARCEL_MARKER = 0xe40dd5a8;
   /** Buffer size for file IO streams. */
   public static final int STREAM_BUFFER_SIZE = 64000;
   public static final int DEFAULT_QUEUE_CAPACITY = 200;

   /** Maximum buffer size for object serialisation. */
   public static final int MAX_SERIALBUFFER_SIZE = 10000000; // 10 MB
   public static final int DEFAULT_SERIALISATION = 0; // code number KRYO
   public static final int MAX_TRANSMISSION_PARCEL_SIZE = 1024*128; 
   public static final int MIN_TRANSMISSION_PARCEL_SIZE = 1024; 
   public static final int DEFAULT_TRANSMISSION_PARCEL_SIZE = 1024*8;
   public static final int DEFAULT_ALIVE_TIMEOUT = 20000;
   public static final int DEFAULT_CONFIRM_TIMEOUT = 10000; 
   
   private static ISerialization globalSerialisation = new KryoSerialisation();
   private static Charset defaultCodingCharset;

   /** The layer parameters in a shell. Carries default values if not modified by
    * the application. */
   private static ConnectionParameters parameters;
   
   static {
      // create the coding charset (does not affect object serialisation methods)
      try { defaultCodingCharset = Charset.forName("utf-8"); 
      } catch (UnsupportedCharsetException e) {
         defaultCodingCharset = Charset.defaultCharset();
      }
      // default initialise values
      parameters = new ConnectionParameters();
      
      // default initialise some serialisable classes
      defaultInitSerialisation(globalSerialisation);
   }
   
   
   /** Sets thread priority (BASE_THREAD_PRIORITY) of the service threads
    * involved in data processing of the top layer of this network package. 
    * This concerns threads dealing with the user interface of the layer like
    * object serialisation or delivering received objects to the application via 
    * registered connection listeners.
    * <p>Note: TRANSMIT_THREAD_PRIORITY is guaranteed to not fall below
    * the value of BASE_THREAD_PRIORITY. By this rule it may be 
    * implicitly raised in the course of this setting. This setting becomes active for
    *  connections which are created <u>after</u> it is given.
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
   public static void setBaseThreadPriority (int p) {
      parameters.setBaseThreadPriority(p);
   }
   
   private static void defaultInitSerialisation(ISerialization ser) {
      ser.registerClassForTransmission(String.class);      
      ser.registerClassForTransmission(JennyNetByteBuffer.class);      
   }

   /** Sets thread priority (TRANSMIT_THREAD_PRIORITY) of the core data
    *  transmission threads involved in dealing with socket communication. This
    *  value can not be less than BASE_THREAD_PRIORITY. This setting becomes 
    *  active for connections which are created <u>after</u> it is given.
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
   public static void setTransmitThreadPriority (int p) {
      parameters.setTransmitThreadPriority(p);
   }

   
   /** Returns the thread priority of the service threads involved in
    * data processing of this layer. This includes threads which
    * deliver received objects to the application or handle outgoing
    * objects handed to the layer. Defaults to Thread.NORM_PRIORITY.
    *  
    *  @return int thread priority
    */
   public static int getBaseThreadPriority() {
      return parameters == null ? Thread.NORM_PRIORITY 
            : parameters.getBaseThreadPriority();
   }

   /** Returns the thread priority of the core data transmission threads
    *  involved in dealing with socket communication. Defaults to
    *  Thread.MAX_PRIORITY.
    *  
    *  @return int thread priority
    */
   public static int getTransmitThreadPriority() {
      return parameters == null ? Thread.MAX_PRIORITY 
            : parameters.getTransmitThreadPriority();
   }

   /** Returns the global Serialisation object.
    * This object is per default valid for all newly created connections.
    * On the returned instance transmittable class registration for object
    * serialisation can be performed on a global level, while still
    * additions and subtractions can be performed on particular
    * connections after their creation.   
    * 
    * @return <code>Serialization</code>
    */
   public static ISerialization getGlobalSerialisation () {
      return globalSerialisation;
   }

   /** Sets the queue capacity for sending objects over
    * the net. Has a minimum of 1.
    * 
    * @param cap int maximum number of objects in queue
    * @throws IllegalArgumentException if value is below 1
    */
   public static void setObjectQueueCapacity (int capacity) {
      parameters.setObjectQueueCapacity(capacity);
   }
   
   /** Returns the queue capacity for sending objects over
    * the net. Attempts to send an object at a connection
    * with a full send queue results in an exception thrown.
    * Defaults to 200.
    * 
    * @return int max. number of objects in queue
    */
   public static int getObjectQueueCapacity() {
      return parameters == null ? DEFAULT_QUEUE_CAPACITY 
            : parameters.getObjectQueueCapacity();
   }

   
   /**
    * @param args
   public static void main(String[] args) {

   }
    */

   /** Returns the global setting for TRANSMISSION_PARCEL_SIZE.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum. Defaults to 8192.
    * 
    * @return int maximum data size of a transmission parcel 
    */
   public static int getTransmissionParcelSize() {
      return parameters == null ? DEFAULT_TRANSMISSION_PARCEL_SIZE 
            : parameters.getTransmissionParcelSize();
   }

   /** Sets the global value for TRANSMISSION_PARCEL_SIZE.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum.  The value has a range of 1k to 128k (0x400 .. 0x20000).
    * <p>This setting becomes active for connections which are created <u>after</u> 
    * it is given.
    * 
    * @param size int maximum capacity of transmission parcels (bytes)
    */
   public static void setTransmissionParcelSize (int size) {
      parameters.setTransmissionParcelSize(size);
   }

   /** The charset used for layer internal use.
    * (Does not reflect object serialisation methods!)
    * 
    * @return Charset text coding charset
    */
   public static Charset getCodingCharset() {
      return parameters == null ? defaultCodingCharset
            : parameters.getCodingCharset();
   }

   /** Sets the charset used for layer internal text encoding.
    * (Does not affect object serialisation methods!)
    * 
    * @param charset Charset
    */
   public static void setCodingCharset (Charset charset) {
      parameters.setCodingCharset(charset);
   }
   
   /** Returns the code number of the default serialisation method 
    * applied by this layer.
    * 
    * @return int method code
    */
   public static int getDefaultSerialisationMethod() {
      return parameters == null ? DEFAULT_SERIALISATION
            : parameters.getSerialisationMethod();
   }

   /** Sets the code number of the default serialisation method of this layer.
    * (Currently ignored.)
    * 
    * @param method int method code
    */
   public static void setDefaultSerialisationMethod (int method) {
      parameters.setSerialisationMethod(method);
   }
   
   /** Returns the layer's default TEMP directory.
    * 
    * @return File directory
    */
   public static File getDefaultTempDirectory() {
      return parameters == null ? new File(System.getProperty("java.io.tmpdir"))
            : parameters.getTempDirectory();
   }

   /** Sets the directory for the layer's default TEMP directory.
    * 
    * @param dir File TEMP directory
    * @throws IOException 
    * @throws IllegalArgumentException if parameter is null or not a directory
    * @throws IOException if the path cannot get verified (canonical name)
    */
   public static void setDefaultTempDirectory (File dir) throws IOException {
      parameters.setTempDirectory(dir);
   }

   /** Returns the layer's default root directory (TRANSFER_ROOT_PATH) for 
    * incoming file transmissions.
    * Defaults to null. Null implies that received file transmissions will be 
    * stored and reflected as temporary files only. For mechanics of file
    * transfers see user manual.
    * 
    * @return File directory
    */
   public static File getDefaultTransmissionRoot () {
      return parameters == null ? null : parameters.getFileRootDir();
   }
   
   /** Sets the layer's default root directory (TRANSFER_ROOT_PATH) for incoming 
    * file transmissions.
    * Null implies that received file transmissions will be stored and reflected 
    * as temporary files only. For mechanics of file transfers see user manual.
    * 
    * @param dir File directory (if not null it must exist)
    * @throws IllegalArgumentException if parameter is not a directory
    * @throws IOException if the path cannot be verified (canonical name)
    */
   public static void setDefaultTransmissionRoot (File dir) throws IOException {
      parameters.setFileRootDir(dir);
   }

   /** Returns the global setting for the capacity of queues handling with data parcels.
    * Defaults to 200.
    * 
    * @return int max. number of parcels in a queue
    */
   public static int getParcelQueueCapacity () {
      return parameters == null ? DEFAULT_QUEUE_CAPACITY 
            : parameters.getParcelQueueCapacity();
   }
   
   /** Sets the global value for the capacity of queues handling with data parcels.
    * 
    * @param capacity int maximum number of parcels (10 .. 10000)
    * @throws IllegalArgumentException if parameter is out of range 
    */
   public static void setParcelQueueCapacity (int capacity) {
      parameters.setParcelQueueCapacity(capacity);
   }

   
   /** The current value of global parameter ALIVE_PERIOD.
    * This value determines the period for a connection to dispatch
    * ALIVE signals to remote to indicate that it is still alive.
    * ALIVE signals are only dispatched when no other network activity
    * is ongoing for that connection.
    * 
    * @param timeout int milliseconds (minimum 500)
    */
   public static int getAlive_Period () {
      return parameters.getAlivePeriod();
   }
   
   /** The current value of global parameter ALIVE_TIMEOUT.
    * Value determines the maximum time since the last network activity
    * of a socket until the remote station is categorised as DEAD and
    * consequently the affected connection closed. Default value is
    * 20,000.
    *  
    * @return int timeout in milliseconds
    */
   public static int getAlive_timeout () {
      return parameters == null ? DEFAULT_ALIVE_TIMEOUT 
            : parameters.getAliveTimeout();
   }

   /** Sets the value of global parameter ALIVE_TIMEOUT.
    * Value determines the maximum time since the last network activity
    * of a socket until the remote station is categorised as DEAD and
    * consequently the affected connection closed. 
    *  
    * @param timeout int milliseconds (minimum 1000)
    * @throws IllegalArgumentException if parameter is below 1000 
    */
   public static void setAlive_timeout (int timeout) {
      parameters.setAliveTimeout(timeout);
   }

   /** The value of global parameter CONFIRM_TIMEOUT.
    * This value determines the maximum time for a connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the affected connection will react depending on the case. 
    * Defaults to 10,000.
    * 
    * @return int timeout in milliseconds
    */  
   public static int getConfirm_timeout () {
      return parameters == null ? DEFAULT_CONFIRM_TIMEOUT 
            : parameters.getConfirmTimeout();
   }

   /** Sets the value of global parameter CONFIRM_TIMEOUT.
    * This value determines the maximum time for a connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the affected connection will react depending on the case. 
    *  
    * @param timeout int milliseconds (minimum 1000)
    * @throws IllegalArgumentException if parameter is below 1000 
    */
   public static void setConfirm_timeout (int timeout) {
      parameters.setConfirmTimeout(timeout);
   }

}
