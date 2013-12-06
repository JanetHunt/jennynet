package org.jhunt.jennynet.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.jhunt.jennynet.intfa.ISerialization;


/** Class for global setting for the JennyNet networking service.
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
   public static final int DEFAULT_TRANSMISSION_PARCEL_SIZE = 1024*8;
   public static final int DEFAULT_ALIVE_TIMEOUT = 20000;
   private static final int DEFAULT_CONFIRM_TIMEOUT = 10000; 
   
   private static int baseThreadPriority = Thread.NORM_PRIORITY;
   private static int transmitThreadPriority = Thread.MAX_PRIORITY;
   private static int objectQueueCapacity =  DEFAULT_QUEUE_CAPACITY;
   private static int parcelQueueCapacity = DEFAULT_QUEUE_CAPACITY;
   private static int transmissionParcelSize = DEFAULT_TRANSMISSION_PARCEL_SIZE;
   
   private static File defaultTempDirectory = new File(System.getProperty("java.io.tmpdir"));
   private static File defaultFileRootDirectory = null;  // defaults to null
   private static ISerialization globalSerialisation = new KryoSerialisation();
   private static Charset codingCharset;

   private static int alive_timeout = DEFAULT_ALIVE_TIMEOUT;
   private static int alive_period;  // set by system
   private static int confirm_timeout = DEFAULT_CONFIRM_TIMEOUT;
   

   
   static {
      // create the coding charset (does not affect object serialisation methods)
      try { codingCharset = Charset.forName("utf-8"); 
      } catch (UnsupportedCharsetException e) {
         codingCharset = Charset.defaultCharset();
      }
      // default initialise values
      set_Alive_Period();
      
      // default initialise some serialisable classes
      defaultInitSerialisation();
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
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY) {
         baseThreadPriority = p;
         transmitThreadPriority = Math.max(baseThreadPriority, transmitThreadPriority);
      } else {
         throw new IllegalArgumentException("illegal priority value: ".
               concat(String.valueOf(p)));
      }
   }
   
   private static void defaultInitSerialisation() {
      globalSerialisation.registerClassForTransmission(String.class);      
      globalSerialisation.registerClassForTransmission(JennyNetByteBuffer.class);      
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
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY
          & p >= baseThreadPriority) {
         transmitThreadPriority = p;
      } else {
         throw new IllegalArgumentException("illegal priority value: ".
               concat(String.valueOf(p)));
      }
   }

   
   /** Returns the thread priority of the service threads involved in
    * data processing of this layer. This includes threads which
    * deliver received objects to the application or handle outgoing
    * objects handed to the layer. Defaults to Thread.NORM_PRIORITY.
    *  
    *  @return int thread priority
    */
   public static int getBaseThreadPriority() {
      return baseThreadPriority;
   }

   /** Returns the thread priority of the core data transmission threads
    *  involved in dealing with socket communication. Defaults to
    *  Thread.MAX_PRIORITY.
    *  
    *  @return int thread priority
    */
   public static int getTransmitThreadPriority() {
      return transmitThreadPriority;
   }

   /** Returns the global Serialisation object.
    * This object is per default valid for all newly created connections.
    * With this instance Object registrations can be performed on
    * a global level.   
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
   public static void setObjectQueueCapacity (int cap) {
      if (cap < 1)
         throw new IllegalArgumentException("illegal capacity; minimum = 1");
      objectQueueCapacity = Math.max(cap, 1);
   }
   
   /** Returns the queue capacity for sending objects over
    * the net. Attempts to send an object at a connection
    * with a full send queue results in an exception thrown.
    * Defaults to 200.
    * 
    * @return int max. number of objects in queue
    */
   public static int getObjectQueueCapacity() {
      return objectQueueCapacity;
   }

   
   /**
    * @param args
   public static void main(String[] args) {
      // TODO Auto-generated method stub

   }
    */

   /** Returns the global setting for TRANSMISSION_PARCEL_SIZE.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum. Defaults to 8192.
    * 
    * @return int maximum data size of a transmission parcel 
    */
   public static int getTransmissionParcelSize() {
      return transmissionParcelSize;
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
      transmissionParcelSize = Math.min( Math.max(size, 1024), MAX_TRANSMISSION_PARCEL_SIZE );
   }

   /** The charset used for layer internal use.
    * (Does not reflect object serialisation methods!)
    * 
    * @return Charset string coding charset
    */
   public static Charset getCodingCharset() {
      return codingCharset;
   }

   /** Returns the code number of the default serialisation method 
    * applied by this layer.
    * 
    * @return int method code
    */
   public static int getDefaultSerialisationMethod() {
      return DEFAULT_SERIALISATION;
   }

   /** Returns the directory definition for the layer's default TEMP directory.
    * 
    * @return File TEMP directory
    */
   public static File getDefaultTempDirectory() {
      return defaultTempDirectory;
   }

   /** Sets the directory definition for the layer's default TEMP directory.
    * 
    * @param dir File TEMP directory
    * @throws IllegalArgumentException if parameter is null or not a directory
    */
   public static void setDefaultTempDirectory (File dir) {
      if (dir == null)
         throw new IllegalArgumentException("dir == null");
      if (!dir.isDirectory())
         throw new IllegalArgumentException("parameter is not a directory");

      defaultTempDirectory = dir;
   }

   /** Returns the layer's default root directory for incoming file transmissions.
    * Defaults to null. Null implies that received file transmissions will be 
    * stored and reflected as temporary files only.
    * 
    * @return File directory
    */
   public static File getDefaultTransmissionRoot() {
      return defaultFileRootDirectory;
   }
   
   /** Sets the layer's default root directory for incoming file transmissions.
    * Null implies that received file transmissions will be stored and reflected 
    * as temporary files only. Otherwise an attempt is made to store incoming files
    * under their given paths in the root directory.
    * 
    * @param dir File directory (must exist)
    * @throws IOException 
    * @throws IllegalArgumentException if parameter is null or not a directory
    * @throws IOException if the path cannot get verified (canonical name)
    */
   public static void setDefaultTransmissionRoot (File dir) throws IOException {
      if (dir == null)
         throw new IllegalArgumentException("dir == null");
      if (!dir.isDirectory())
         throw new IllegalArgumentException("parameter is not a directory");
      
      defaultFileRootDirectory = dir.getCanonicalFile();
   }

   /** Returns the global setting for the capacity of queues handling with data parcels.
    * Defaults to 200.
    * 
    * @return int max. number of parcels in a queue
    */
   public static int getParcelQueueCapacity () {
      return parcelQueueCapacity;
   }
   
   /** Sets the global value for the capacity of queues handling with data parcels.
    * 
    * @param c int maximum number of parcels (10 .. 10000)
    * @throws IllegalArgumentException if parameter is out of range 
    */
   public static void setParcelQueueCapacity (int c) {
      if (c < 10 | c > 10000) 
         throw new IllegalArgumentException("queue capacity out of range (10..10,000)");
      parcelQueueCapacity = c;
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
      return alive_period;
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
      return alive_timeout;
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
      if (timeout < 1000) 
         throw new IllegalArgumentException("timeout minimum = 1000");
      if (timeout - confirm_timeout/2 < 500) 
         throw new IllegalArgumentException("illegal ALIVE value: " + timeout + "; results in ALIVE_PERIOD below 500");
      
      JennyNet.alive_timeout = timeout;
      set_Alive_Period();
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
      return confirm_timeout;
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
      if (timeout < 1000) 
         throw new IllegalArgumentException("timeout minimum = 1000");
      if (alive_timeout - timeout/2 < 500) 
         throw new IllegalArgumentException("illegal CONFIRM value: " + timeout + "; results in ALIVE_PERIOD below 500");
      
      JennyNet.confirm_timeout = timeout;
      set_Alive_Period();
   }

   /** Sets value of ALIVE_PERIOD in dependence of ALIVE_TIMEOUT and
    * CONFIRM_TIMEOUT.
    */
   private static void set_Alive_Period () {
      alive_period = alive_timeout - confirm_timeout/2;
   }
}
