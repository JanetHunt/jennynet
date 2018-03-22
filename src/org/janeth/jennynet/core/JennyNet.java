package org.janeth.jennynet.core;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.janeth.jennynet.exception.ConnectionRejectedException;
import org.janeth.jennynet.exception.ConnectionTimeoutException;
import org.janeth.jennynet.exception.JennyNetHandshakeException;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionParameters;
import org.janeth.jennynet.intfa.IClient;
import org.janeth.jennynet.intfa.Serialization;
import org.janeth.jennynet.util.Util;

/** Class for global settings of the JennyNet networking service.
 * 
 */

public class JennyNet {

   protected static boolean debug = false;

   // markers for version 0.3.0
   public static final int PARCEL_MARKER = 0xe40dd5a8;
   static final byte[] LAYER_HANDSHAKE_SERVER = Util.hexToBytes("83BFAA19D69E9976D845D09684D2CAED");
   static final byte[] LAYER_HANDSHAKE_CLIENT = Util.hexToBytes("83BFAA19D69E9976D845D09684D280D6");
   static final byte[] CONNECTION_CONFIRM = Util.hexToBytes("D6BC4AA0EF3CE5A01515BAC1B80EA38F");
   
   /** Buffer size for file IO streams. */
   public static final int STREAM_BUFFER_SIZE = 64000;
   public static final int DEFAULT_QUEUE_CAPACITY = 200;
   public static final int DEFAULT_PARCEL_QUEUE_CAPACITY = 600;
//   public static final int SOCKET_TIMEOUT = 1000;

   /** Maximum buffer size for object serialisation. */
   public static final int DEFAUL_MAX_SERIALISE_SIZE = 100000000; // 100 Mega
   public static final int MIN_SERIALISE_SIZE = 10000; 
   public static final int DEFAULT_SERIALISATION_METHOD = 0; // code number KRYO_
   public static final int MAX_TRANSMISSION_PARCEL_SIZE = 1024*256; 
   public static final int MIN_TRANSMISSION_PARCEL_SIZE = 1024; 
   public static final int DEFAULT_TRANSMISSION_PARCEL_SIZE = 1024*64;
   public static final int DEFAULT_ALIVE_PERIOD = 30000;
   public static final int DEFAULT_CONFIRM_TIMEOUT = 15000; 
   public static final int DEFAULT_IDLE_CHECK_PERIOD = 60000; 
   public static final int DEFAULT_TRANSMISSION_TEMPO = -1; 
   
   // global structures
   private static Vector<IClient> globalClientList = new Vector<>(16, 32);
   private static Serialization globalSerialisation = new KryoSerialisation();
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
      parameters = new ConnectionParametersImpl();
      
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
   
   private static void defaultInitSerialisation(Serialization ser) {
      ser.registerClass(String.class);      
      ser.registerClass(JennyNetByteBuffer.class);      
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
      return parameters == null ? Thread.MAX_PRIORITY -2
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
   public static Serialization getGlobalSerialisation () {
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

   /** Returns the global setting for the capacity of queues handling with data parcels.
    * Defaults to 200.
    * 
    * @return int max. number of parcels in a queue
    */
   public static int getParcelQueueCapacity () {
      return parameters == null ? DEFAULT_PARCEL_QUEUE_CAPACITY 
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

   
   /**
    * @param args
   public static void main(String[] args) {

   }
    */

   /** Returns the global setting for TRANSMISSION_PARCEL_SIZE.
    * This is the data transport capacity that a single transmission parcel may 
    * assume in maximum. Defaults to 32,768 (0x8000).
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

   /** Sets the charset used for layer internal use.
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
      return parameters == null ? DEFAULT_SERIALISATION_METHOD
            : parameters.getSerialisationMethod();
   }

//   /** Sets the code number of the default serialisation method of this layer.
//    * (Currently ignored.)
//    * 
//    * @param method int method code
//    */
//   public static void setDefaultSerialisationMethod (int method) {
//      parameters.setSerialisationMethod(method);
//   }
   
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

   
   /** The current value of global parameter REMOTE_ALIVE_PERIOD.
    * This value determines the period for a connection to dispatch
    * ALIVE signals to remote to indicate that it is still alive.
    * Defaults to 20,000.
    * 
    * @param timeout int milliseconds (minimum 500)
    */
   public static int getAlivePeriod () {
      
      return parameters == null ? DEFAULT_ALIVE_PERIOD 
             : parameters.getAlivePeriod();
   }
   
   /** Sets the value of REMOTE_ALIVE_PERIOD.
    * This value defines the period in which ALIVE signals will 
    * be sent to the remote station of a connection if no other 
    * outgoing network activity occurs.
    *  
    * @param timeout int milliseconds (minimum 10000)
    */
   public static void setAlivePeriod (int period) {
      parameters.setAlivePeriod(period);
   }

   /** The value of global parameter CONFIRM_TIMEOUT.
    * This value determines the maximum time for a connection to wait for
    * an expected CONFIRM signal from remote. In consequence of failure
    * the affected connection will react depending on the case. 
    * Defaults to 10,000.
    * 
    * @return int timeout in milliseconds
    */  
   public static int getConfirmTimeout () {
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

   /** Verifies the JennyNet network layer on the remote end of the connection.
    * Blocks for a maximum of ? milliseconds to read data from remote.
    * The socket must be connected. If false is returned or an IO exception is
    * thrown, the socket gets closed.
    *
    * @param agent int controlling agent: 0 = server, 1 = client
    * @param socket Socket connected socket
    * @param timer Timer the timer thread to use for the timer task
    * @param time int milliseconds to wait for a remote signal

    * @return boolean true == JennyNet confirmed, false == invalid endpoint or timeout
    * @throws IllegalArgumentException if socket is unconnected
    * @throws IOException 
    */
   static boolean verifyNetworkLayer (int agent, final Socket socket, Timer timer, int time) 
         throws IOException {
      // check for conditions
      if (!socket.isConnected())
         throw new IllegalArgumentException("socket is unconnected!");
      
      // write our handshake to remote
      byte[] sendHandshake = agent == 0 ? JennyNet.LAYER_HANDSHAKE_SERVER : 
                                          JennyNet.LAYER_HANDSHAKE_CLIENT;
      byte[] receiveHandshake = agent == 0 ? JennyNet.LAYER_HANDSHAKE_CLIENT : 
                                          JennyNet.LAYER_HANDSHAKE_SERVER;
      socket.getOutputStream().write(sendHandshake);
      
      try {
         // file in for the socket shutdown timer
         // which covers the case that remote doesn't send enough bytes
         TimerTask task = new TimerTask() {

            @Override
            public void run() {
               try {
                  socket.close();
                  if (debug) {
                	  System.out.println("----- TIMEOUT on socket listening (SERVER : VERIFY NETWORK LAYER)");
                	  System.out.println("      REMOTE = " + socket.getRemoteSocketAddress());
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         };
         timer.schedule(task, time);
         
         // try read remote handshake
         byte[] handshake = new byte[16];
         new DataInputStream(socket.getInputStream()).readFully(handshake);
         task.cancel();

         // test and verify remote handshake
         return Util.equalArrays(handshake, receiveHandshake);
         
      } catch (SocketException e) {
         // this is a typical timeout response
         e.printStackTrace();
         socket.close();
         return false;
      } catch (EOFException e) {
         // this is a remote closure response
         e.printStackTrace();
         socket.close();
         return false;
      } catch (IOException e) {
         socket.close();
         throw e;
      }
   }

   /** Returns the IDLE THRESHOLD for a connection. The IDLE THRESHOLD states an
    * amount of bytes exchanged with the remote end per minute (incoming or outgoing).
    * This threshold determines whether a connection qualifies for BUSY or IDLE status,
    * which is indicated via connection events once a threshold is defined.
    * Defaults to 0 (undefined).
    *  
    *  @return int threshold: bytes per minute
    */
   public static int getIdleThreshold() {
      return parameters == null ? 0 : parameters.getIdleThreshold();
   }
   
   /** Sets the IDLE THRESHOLD for a connection. The IDLE THRESHOLD states an
    * amount of bytes exchanged with the remote end per minute (incoming or outgoing).
    * This threshold determines whether a connection qualifies for BUSY or IDLE status,
    * which is indicated via connection events once a threshold is defined.
    * Defaults to 0 (undefined).
    *  
    *  @param idleThreshold int bytes per minute
    */
   public static void setIdleThreshold(int idleThreshold) {
      parameters.setIdleThreshold(idleThreshold);
   }

   /** Returns a clone of the global set of connection parameters.
    * 
    * @return <code>ConnectionParameters</code>
    */
   public static ConnectionParameters getParameters () {
      return (ConnectionParameters)parameters.clone();
   }

   /** Returns an unmodifiable list of currently active clients
    * in the JennyNet layer.
    * 
    * @return <code>List&lt;IClient&gt;</code>
    */
   public static List<IClient> getGlobalClientSet () {
	   return Collections.unmodifiableList(globalClientList);
   }
   
   /** Returns the number of currently active clients in the JennyNet layer.
    * 
    * @return int number of active clients
    */
   public static int getNrOfClients () {
	   return globalClientList.size();
   }
   
   /** Adds a client to the layer's global client set. Double entry of 
    * the same object is silently prevented.
    *  
    * @param client <code>IClient</code>
    */
   protected static void addClientToGlobalSet (IClient client) {
	   if (!globalClientList.contains(client)) {
		   globalClientList.add(client);
	   }
   }

   /** Removes a client from the global client set.
    * 
    * @param client <code>IClient</code>
    */
   protected static void removeClientFromGlobalSet (IClient client) {
	   globalClientList.remove(client);
   }
   
   /** Waits the given time for a CONNECTION_VERIFIED signal received from the
    * remote endpoint. This should only take place after <i>verifyNetworkLayer()</i> has been
    * passed positively. Method throws exceptions to indicate various failure conditions.
    * The socket must be connected. If a timeout, rejection or IO exception is thrown, the socket 
    * gets closed.
    * 
    * @param socket Socket connected socket
    * @param timer Timer the timer thread to use for the timer task
    * @param time int milliseconds to wait for a remote signal
    * @return int ALIVE period as requested by remote
    * 
    * @throws JennyNetHandshakeException if remote sent a false signal (out of protocol)
    * @throws ConnectionRejectedException if remote JennyNet layer refused the connection
    * @throws ConnectionTimeoutException if waiting for connection signal expired or time 
    *              parameter is below 1
    * @throws IOException
    * @throws IllegalArgumentException if socket is unconnected
    *  
    */
   static int waitForConnection (final Socket socket, Timer timer, int time) 
         throws IOException {
      // check for conditions
      if (!socket.isConnected())
         throw new IllegalArgumentException("socket is unconnected!");
      if (time < 1) 
         throw new ConnectionTimeoutException("illegal time value");
      
      // file in for the socket shutdown timer
      // which covers the case that remote doesn't send enough bytes
      class WaitTimerTask extends TimerTask {
         boolean expired = false;

         @Override
         public void run() {
            try {
               expired = true;
               socket.close();
               if (debug) {
            	   System.out.println("----- TIMEOUT on socket listening (SERVER : VERIFY CONNECTION STATUS)");
            	   System.out.println("      REMOTE = " + socket.getRemoteSocketAddress());
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         
         public boolean hasExpired () {
            return expired;
         }
      };
      WaitTimerTask task = new WaitTimerTask();
      timer.schedule(task, time);
         
      try {
         // try read remote connection confirm signal
         byte[] remoteSignal = new byte[20];
         new DataInputStream(socket.getInputStream()).readFully(remoteSignal);
         task.cancel();
   
         // test and verify remote handshake
         byte[] verifySignal = Arrays.copyOf(remoteSignal, 16);
         if ( !Util.equalArrays(verifySignal, JennyNet.CONNECTION_CONFIRM) )
            throw new JennyNetHandshakeException("false signal on WAIT_FOR_CONNECTION_CONFIRM");
         
         // extract ALIVE-PERIOD request from remote
         int alivePeriod = Util.readInt(remoteSignal, 16);
         return alivePeriod;
         
      } catch (SocketException e) {
         // this is the timeout response (triggered by the timer task)
         socket.close();
         if (task.hasExpired()) {
            throw new ConnectionTimeoutException("waiting expired for " + time + " milliseconds") ;
         // this is some other SocketException   
         } else {
            throw e;
         }
      } catch (EOFException e) {
         // this is a remote closure response
         socket.close();
         throw new ConnectionRejectedException();
      } catch (IOException e) {
         // this is a remote closure response
         socket.close();
         throw e;
      }
   }

   /** Sends a CONNECTION_CONFIRM message to remote station including a
    * request for ALIVE signals, if any. This is part of the initial handshake
    * protocol during establishing a connection between client and server.
    *  
    * @param connection <code>Connection</code> sending connection
    * @throws IOException 
    */
   static void sendConnectionConfirm (Connection connection) 
                 throws IOException {
      byte[] signal = Arrays.copyOf(JennyNet.CONNECTION_CONFIRM, 20);
      Util.writeInt(signal, 16, connection.getParameters().getAlivePeriod());
      ((ConnectionImpl)connection).getSocket().getOutputStream().write(signal);
   }

   public static int getDefaultIdleCheckPeriod () {
      return parameters == null ? DEFAULT_IDLE_CHECK_PERIOD : parameters.getIdleCheckPeriod();
   }
   
   public static void setDefaultIdleCheckPeriod (int period) {
      parameters.setIdleCheckPeriod(period);
   }

	public static int getDefaultTransmissionTempo () {
       return parameters == null ? DEFAULT_TRANSMISSION_TEMPO : parameters.getTransmissionSpeed();
	}

	public static void setDefaultTransmissionTempo (int tempo) {
		parameters.setTransmissionSpeed(tempo);
	}

	public static int getDefaultMaxSerialiseSize() {
        return parameters == null ? DEFAUL_MAX_SERIALISE_SIZE : 
        	parameters.getMaxSerialisationSize();
	}

	public static void setDefaultMaxSerialiseSize(int size) {
		parameters.setMaxSerialisationSize(size);
	}


}
