package org.janeth.jennynet.intfa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public interface ConnectionParameters extends Cloneable {

   public Object clone ();

   /** Returns the thread priority of the service threads involved in
    * data processing. This includes threads which
    * deliver received objects to the application or handle outgoing
    * objects handed to the connection. Defaults to Thread.NORM_PRIORITY.
    *  
    *  @return int thread priority
    */
   public int getBaseThreadPriority ();

   /** Sets thread priority (BASE_THREAD_PRIORITY) of the service threads
    * involved in data processing. 
    * This concerns layer threads dealing with the user interface like
    * object serialisation or delivery of received objects to the application
    * via registered connection listeners. Defaults to Thread.NORM_PRIORITY.
    * <p><small>In general it is most efficient to set this value to the 
    * application's standard thread priority. In cases where a particular
    * high reactivity of the network layer is requested, this value should
    * be set higher.</small>
    *  
    * @param p int thread priority
    * @throws IllegalArgumentException if value is out of range of legal
    *         Thread priorities
    * @see #setTransmitThreadPriority(int)
    */
   public void setBaseThreadPriority (int p);

   /** Returns the thread priority of the core data transmission threads
    *  involved in dealing with socket communication. Defaults to
    *  Thread.MAX_PRIORITY - 2.
    *  
    *  @return int thread priority
    */
   public int getTransmitThreadPriority ();

   /** Sets thread priority (TRANSMIT_THREAD_PRIORITY) of the core data
    *  transmission threads involved in dealing with socket communication. 
    *  Defaults to Thread.MAX_PRIORITY - 2.
    *  
    *  <p><small>Note: For minimised latency of net transmission it is 
    *  recommended to set this value above or equal to BASE_THREAD_PRIORITY. 
    *  Setting it to MAX_PRIORITY is not expected to disturb the thread logic
    *  of any application or create extra load but only ensures that network
    *  socket activity is immediately performed when data is ready to send or 
    *  available to receive.</small> 
    *  
    * @param p int thread priority 
    * @throws IllegalArgumentException if value is out of range of legal
    *         Thread priorities
    * @see #setBaseThreadPriority(int)
    */
   public void setTransmitThreadPriority (int p);

   /** Returns the value for capacity of queues handling with data parcels.
    * Defaults to 600.
    * 
    * @return int maximum number of parcels in a queue
    */
   public int getParcelQueueCapacity ();

   /** Sets the value for capacity of queues handling with data parcels.
    * This value can only be set before a connection starts. The value
    * ranges within a minimum of 10 and a maximum of 10,000 and defaults to
    * 600. 
    * 
    * @param c int maximum number of parcels (10 .. 10,000)
    * @throws IllegalArgumentException if parameter is out of range 
    * @throws IllegalStateException if the related Connection is connected
    */
   public void setParcelQueueCapacity (int parcelQueueCapacity);

   /** Returns the maximum size of any serialisation of an object which is
    * to be sent over the network. The default value is 100 Mega.
    * <p><small>Encountering a size overflow
    * during send or receive operations causes the connection closed.
    * </small>
    * 
    * @return int maximum object serialisation size
    */
   public int getMaxSerialisationSize ();
   
   /** Sets the maximum size of any serialisation of an object which is
    * to be sent over the network. The default value is 100 Mega.
    * <p><small>Encountering a size overflow
    * during send or receive operations causes the connection closed.
    * </small>
    * 
    * @param size int maximum object serialisation size
    */
   public void setMaxSerialisationSize (int size);
   
   
   /** Returns the queue capacity for sending objects over
    * the net. Attempts to send an object 
    * with a full send queue results in an exception thrown.
    * Defaults to 200.
    * 
    * @return int maximum number of objects in queue
    */
   public int getObjectQueueCapacity ();

   /** Sets the queue capacity for sending objects over the net. 
    * This value can only be set before a connection starts. 
    * Has a minimum of 1, defaults to 200.
    * 
    * @param cap int maximum number of objects in queue
    * @throws IllegalArgumentException if value is below 1
    * @throws IllegalStateException if the related Connection is connected
    */
   public void setObjectQueueCapacity (int objectQueueCapacity);

   /** The current value of ALIVE_PERIOD.
    * This value determines the time interval by which ALIVE signals are sent 
    * to the remote station. A value of zero indicates that no ALIVE signals 
    * are sent. Defaults to 20,000.
    * 
    * @param timeout int milliseconds
    */
   public int getAlivePeriod ();

   /** Sets the value of ALIVE_PERIOD.
    * This value defines the interval by which ALIVE_SIGNALs are sent to the 
    * remote station. A value of zero indicates that no ALIVE signals are sent 
    * and no ALIVE_ECHO timeout control performed on the connection. This value 
    * can only be set before a connection starts and defaults to 20,000.
 
    * <p><small>The layer ensures a minimum of 1,000 milliseconds and a
    * maximum of 300,000, automatically correcting arguments except zero. 
    * The internal value ALIVE_TIMEOUT determines the period by which an
    * ALIVE_ECHO has to occur after ALIVE_SIGNAL is sent, otherwise the
    * connection is marked as dead. ALIVE_TIMEOUT is double the value of 
    * CONFIRM_TIMEOUT.
    * </small>
    *       
    * @param period int milliseconds
    * @throws IllegalStateException if the related Connection is connected
    */
   public void setAlivePeriod (int period);

   /** The value of parameter CONFIRM_TIMEOUT.
    * This value determines the maximum time for the connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the connection will react depending on the case. Defaults to 10,000.
    * 
    * @return int timeout in milliseconds
    */
   public int getConfirmTimeout ();

   /** Sets the value for CONFIRM_TIMEOUT.
    * This value determines the maximum time for the connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the connection will react depending on the case. 
    * <p><small>The layer implements a minimum of 1000 milliseconds
    * automatically correcting lower settings.</small>
    *  
    * @param timeout int milliseconds
    */
   public void setConfirmTimeout (int timeout);

   /** Returns the code number of the object serialisation method 
    * applied by the connection.
    * 
    * @return int method code
    */
   public int getSerialisationMethod ();

//   /** Sets the method for object serialisation.
//    * (Currently there is only one serialisation method
//    * available and this setting ignored!)
//    * 
//    * @param methodID int method
//    */
//   public void setSerialisationMethod (int method);

   /** Returns the root directory (TRANSFER_ROOT_PATH) for 
    * incoming file transmissions. Defaults to null. 
    * <p><small>Null implies that only file transmissions without path target
    * can be received. In this case file transmissions with target are aborted
    * and cause a transmission event of type FILE_FAILED in the 
    * <code>ConnectionListener</code>.</small>
    * 
    * @return File directory or null
    */
   public File getFileRootDir ();

   /** Sets the root directory (TRANSFER_ROOT_PATH) for incoming 
    * file transmissions. If not null, the parameter must be 
    * an existing directory. The default value is null.
    * <p><small>Null implies that only file transmissions without path target
    * can be received. In this case file transmissions with target are aborted
    * and cause a transmission event of type FILE_FAILED in the 
    * <code>ConnectionListener</code>.</small>
    * 
    * @param dir File directory or null
    * @throws IllegalArgumentException if parameter is not a directory
    * @throws IOException if the path cannot be verified (canonical name)
    */
   public void setFileRootDir (File dir) throws IOException;

   /** Returns the TEMP directory for the connection.
    * 
    * @return File directory
    */
   public File getTempDirectory ();

   /** Sets the directory for TEMPORARY files. Parameter must be an existing 
    * directory or null. Null implies the layer's default TEMP directory (
    * <code>JennyNet.getDefaultTempDirectory()</code>).  
    * 
    * @param dir File TEMP directory or null
    * @throws IllegalArgumentException if parameter does not refer to an
    *         existing directory
    * @throws IOException if the path cannot get verified (canonical name)
    */
   public void setTempDirectory (File dir) throws IOException;

   /** Returns the setting for TRANSMISSION_PARCEL_SIZE in the connection.
    * This is the data transport capacity that a single transmission parcel
    * may assume in maximum. Defaults to  32,768 (0x8000).
    * 
    * @return int maximum data size of a transmission parcel 
    */
   public int getTransmissionParcelSize ();

   /** Sets the value for TRANSMISSION_PARCEL_SIZE for the connection.
    * This is the data transport capacity that a single transmission parcel
    * may assume in maximum.  The value has a range of 1k to 262k (0x400 .. 
    * 0x40000) and defaults to 32,768 (0x8000).
   
    * 
    * @param size int maximum capacity of transmission parcels (bytes)
    */
   public void setTransmissionParcelSize (int size);

   /** Returns the transmission speed setting for the connection in bytes 
    * per second. Value -1 indicates no speed restrictions are imposed.
    * Defaults to -1.
    * 
    * @return int tempo in bytes per second
    */
   public int getTransmissionSpeed ();
   
   /** Sets the transmission speed for the connection in bytes 
    * per second. Value -1 indicates no speed restrictions are imposed.
    * Value 0 stops data transmission without closing the connection.
    * Defaults to -1.
    * 
    * @return int tempo in bytes per second
    */
   public void setTransmissionSpeed (int tempo);
   
   /** The charset used for layer internal use. Defaults to UTF-8.
    * 
    * @return Charset text coding charset
    */
   public Charset getCodingCharset ();

   /** Sets the charset used for layer internal text encoding.
    * (Does not affect object serialisation!)
    * This value can only be set before a connection starts. 
    * Defaults to UTF-8.
    * 
    * @param charset Charset
    * @throws IllegalStateException if the related Connection is connected
    */
   public void setCodingCharset (Charset charset);

   /** Sets the IDLE_THRESHOLD for a connection. The IDLE_THRESHOLD states an
    * amount of bytes exchanged with the remote end per minute (incoming or
    * outgoing). This threshold determines whether a connection qualifies for
    * BUSY or IDLE status, which is indicated via connection events once a 
    * threshold is defined. Defaults to 0 (undefined).
    *  
    *  @param idleThreshold int bytes per minute
    */
   public void setIdleThreshold (int idleThreshold);

   /** Returns the IDLE_THRESHOLD for a connection. The IDLE_THRESHOLD states
    * an amount of bytes exchanged with the remote end per minute (incoming or
    * outgoing). This threshold determines whether a connection qualifies for
    * BUSY or IDLE status, which is indicated via connection events once a 
    * threshold is defined. Defaults to 0 (undefined). 
    *  
    *  @return int threshold: bytes per minute
    */
   public int getIdleThreshold ();

   
   /** Returns the period with which IDLE state of the connection is checked
    * to be true or false. If IDLE is found to be true, an event is issued
    * to listeners on the connection. Note that IDLE TRUE event is repeatedly 
    * reported with each periodical check. Defaults to 60,000.
    * 
    * @return int check period in milliseconds
    */
   public int getIdleCheckPeriod ();
   
   /** Sets the period by which IDLE state of the connection is checked
    * to be true or false. If IDLE is found to be true, an event is issued
    * to listeners on the connection. Note that IDLE TRUE event is repeatedly 
    * reported with each periodical check. Defaults to 60,000. A minimum of 
    * 1,000 is ensured.
    * 
    * @param period int checking period in milliseconds
    */
   public void setIdleCheckPeriod (int period);
   
}