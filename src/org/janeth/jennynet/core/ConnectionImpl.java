
package org.janeth.jennynet.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.janeth.jennynet.exception.ClosedConnectionException;
import org.janeth.jennynet.exception.ConnectionTimeoutException;
import org.janeth.jennynet.exception.FileInTransmissionException;
import org.janeth.jennynet.exception.IllegalFileLengthException;
import org.janeth.jennynet.exception.RemoteTransferBreakException;
import org.janeth.jennynet.exception.UnconnectedException;
import org.janeth.jennynet.exception.UnregisteredObjectException;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.ConnectionParameters;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.Serialization;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.intfa.TransmissionEvent.TransmissionEventType;
import org.janeth.jennynet.util.ArraySet;
import org.janeth.jennynet.util.SchedulableTimerTask;
import org.janeth.jennynet.util.Util;

/** Implementation of JennyNet <code>Connection</code> interface, building 
 * the common part of both <code>Client</code> and <code>ServerConnection</code>.
 */
class ConnectionImpl implements Connection {
   
   protected static boolean debug = false;
   
   /** Internal static Timer for time-control tasks. */
   protected static Timer timer = new Timer();
   /** Internal type marker for events dispatched to IConnectionListeners */
   protected enum ConnectionEventType {
      object, connect, idle, disconnect 
   }
   
   // parametric
   private UUID uuid = UUID.randomUUID();
   private byte[] shortId = Util.makeShortId(uuid);
   private String connectionName;
   private OurParameters parameters = new OurParameters();

   // objects
   private Set<ConnectionListener> listeners = new ArraySet<ConnectionListener>();
   private Serialization inputSerialisation = JennyNet.getGlobalSerialisation().copyFor(this);
   private Serialization outputSerialisation = JennyNet.getGlobalSerialisation().copyFor(this);
   private Properties properties;

   private Socket socket;
   private OutputStream socketOutput;
   private InputStream socketInput;
   private Map<Long, Long> pingSentMap; // maps ping-id -> time sent
   private Map<Object, SendFileProcessor> fileSenderMap; 
   private Map<Long, FileAgglomeration> fileReceptorMap; 
   private Map<Long, ObjectAgglomeration> objectReceptorMap; 

   // processors
   private InputProcessor inputProcessor;
   private OutputProcessor outputProcessor;
   private ReceiveProcessor receiveProcessor;
   
   // data queues sending
   private CoreSend coreSend;  // also a processor!
   private PriorityBlockingQueue<UserObject> inputQueue;
   
   // data queues receiving
   private CoreReceive coreReceive;
   private PriorityBlockingQueue<UserObject> objectReceiveQueue;
   
   // operational
   private AliveSignalTimerTask aliveSignalTask;
   private AliveEchoControlTask aliveTimeoutTask;
   private CheckIdleTimerTask checkIdleTask;
   private Object listenerDispatchLock = new Object();
   private long objectSerialCounter;
   private long pingSerialCounter;
   private long exchangedDataVolume;
   private int transmitSpeed = -1;
   private boolean isCheckIdleState;
   protected boolean fixedTransmissionSpeed;
   private boolean closed;
   private boolean connected;
   private boolean isIdle;

   
   public ConnectionImpl () {
   }

   public ConnectionImpl (Socket socket) throws IOException {
      start(socket);
   }

   @Override
   public ConnectionParameters getParameters() {
      return parameters;
   }

   @Override
   public void setParameters(ConnectionParameters parameters) throws IOException {
      if (parameters == null)
         throw new NullPointerException();
      if (isConnected()) 
         throw new IllegalStateException("cannot set parameters on operating connection");
      ((OurParameters)this.parameters).takeOver(parameters);
   }

   @Override
   public Serialization getSendSerialization() {
      return inputSerialisation;
   }

   @Override
   public Serialization getReceiveSerialization() {
      return outputSerialisation;
   }

   @Override
   public void setSendSerialization (Serialization s) {
      if (s != null) {
         inputSerialisation = s.copyFor(this);
      }
   }

   @Override
   public void setReceiveSerialization (Serialization s) {
      if (s != null) {
         outputSerialisation = s.copyFor(this);
      }
   }

   @Override
   public UUID getUUID() {
      return uuid;
   }

   @Override
   public void setUUID (UUID uuid) {
	   if (uuid == null)
		   throw new NullPointerException("uuid is null"); 
	   this.uuid = uuid;
	   this.shortId = Util.makeShortId(uuid);
   }
	   
   @Override
   public int hashCode () {
      InetSocketAddress s1 = getLocalAddress(); 
      InetSocketAddress s2 = getRemoteAddress(); 
      int h = s1 == null ? 2 : s1.hashCode();
      int j = s2 == null ? 1 : s2.hashCode();
      return h ^ j;
   }

   /** Whether both parameters share the same value, including possibly null. 
    * 
    * @param o1 Object
    * @param o2 Object
    * @return boolean true if (o1 == null & o2 == null) | o1.equals(o2)
    */
   private boolean sameValues (Object o1, Object o2) {
      return o1 == o2 || (o1 == null & o2 == null) || 
         ((o1 != null & o2 != null) && o1.equals(o2));   
   }
   
   @Override
   public boolean equals (Object obj) {
      if (obj == null || !(obj instanceof Connection))
         return false;
      
      InetSocketAddress thisLocal = getLocalAddress(); 
      InetSocketAddress thisRemote = getRemoteAddress(); 
      Connection con = (Connection)obj;
      InetSocketAddress conLocal = con.getLocalAddress(); 
      InetSocketAddress conRemote = con.getRemoteAddress();
      
      return sameValues(thisLocal, conLocal) && sameValues(thisRemote, conRemote);
   }

   @Override
   public String toString () {
      InetSocketAddress local = getLocalAddress(); 
      InetSocketAddress remote = getRemoteAddress(); 
      
      String localAddr = local == null ? "null" : local.toString();
      String remoteAddr = remote == null ? "null" : remote.toString();
      String name = getName() == null ? "" : getName().concat(" = ");
      return name + localAddr + " --> " + remoteAddr;
   }

   @Override
   protected void finalize () throws Throwable {
      close(null, 99);
   }

   @Override
   public boolean isConnected() {
      return connected;
   }

   @Override
   public boolean isClosed() {
      return closed;
   }

   @Override
   public boolean isTransmitting() {
	  long transmitTime = getLastTransmitTime();
      int delta = (int)(System.currentTimeMillis() - transmitTime);
      boolean result = isConnected() && transmitTime > 0 ? delta < 2000 : false; 
      return result;
   }
   
   protected long getLastTransmitTime() {
      return Math.max(getLastSendTime(), getLastReceiveTime());
   }

   @Override
   public long getLastSendTime() {
      return coreSend == null ? 0 : coreSend.getLlastTransmitTime();
   }

   @Override
   public long getLastReceiveTime() {
      return coreReceive == null ? 0 : coreReceive.getLlastTransmitTime();
   }

   @Override
   public long sendObject(Object object, SendPriority priority) {
      checkConnected();
      checkObjectRegisteredForSending(object);
      
      long objNr = -1; 
      if (inputProcessor != null && !inputProcessor.isTerminated()) {
          // throw exception if input queue is at maximum
    	  int topSize = getParameters().getObjectQueueCapacity();
    	  if (inputQueue.size() >= topSize) {
    		  throw new IllegalStateException("input queue is full, caps = " + topSize);
    	  }

    	  // assign object number and add to input queue
    	  objNr = getNextObjectNr();
    	  inputQueue.add(new UserObject(object, objNr, priority));
      }
      return objNr;
   }

   @Override
   public long sendObject(Object object) {
      return sendObject(object, SendPriority.Normal);
   }

   /** Checks whether this connection is closed or disconnected and throws
    * an exception if true. 
    * 
    * @throws ClosedConnectionException
    * @throws UnconnectedException
    */
   private void checkConnected() {
      if (closed) {
         throw new ClosedConnectionException();
      }
      if (!isConnected()) {
          throw new UnconnectedException("socket unconnected");
      }
   }

   @Override
   public long sendFile (File file, String remotePath, SendPriority priority) throws IOException {
      checkConnected();
      return new SendFileProcessor(file, remotePath, priority).getFileID();
   }
   
   @Override
   public long sendFile (File file, String remotePath) throws IOException {
      return sendFile(file, remotePath, SendPriority.Normal);
   }
   
   /** Sends a signal to remote. This queues the signal object for sending
    * but does not check for connection readiness.
    * 
    * @param signal <code>Signal</code>
    */
   protected void sendSignal (Signal signal) {
      if (debug) {
    	  System.out.println("-- SIGNAL SND (ob " + signal.getObjectID() + "): " + signal.getSigType() +
    			  " (i " + signal.getInfo() + ") to " + getRemoteAddress() + ", [" + getLocalAddress() + "]");
      }
      String text = signal.getText();
      if (debug && text != null) {
         System.out.println("   Text = ".concat(text));
      }

      if (coreSend != null) {
    	  coreSend.add(signal);
      }
   }

   /** Inserts the specified user object (wrapper) into the outgoing queue. 
   * This method may block until space is made available in the queue. The
   * queue may not become larger than <code>
   * getParameters().getObjectQueueCapacity()</code>.
   * 
   * @param object <code>UserObject</code>
   */
   private void putObjectToReceiveQueue (UserObject object) {
		// naive blocking behaviour depending on queue size
	   int topSize = getParameters().getObjectQueueCapacity();
		do {
			if (objectReceiveQueue.size() < topSize) {
				break;
			}
			Util.sleep(25);
		} while (true);

		// put parcel into sorting queue
		objectReceiveQueue.put(object);
   }
   
   @Override
   public long sendPing() {
	  checkConnected();
	  if (pingSentMap == null) return -1;
	  
      long pingId = ++pingSerialCounter;
      pingSentMap.put(pingId, System.currentTimeMillis());
      sendSignal( Signal.newPingSignal(pingId) );
      return pingId;
   }

   
   @Override
   public void setTempo (int baud) {
	  checkConnected();
      if (debug) {
    	  System.out.println(baud > -1 ? ("-- setting TEMPO to " + baud + " bytes/sec")
    	  : "-- setting TEMPO to NO LIMIT");
      }

	  // adjust connection settings
	  transmitSpeed = baud;
      if (coreSend != null) {
    	  coreSend.setSending(baud != 0);
      }
	  
	  // signal remote about current baud setting
      Signal tempo = Signal.newTempoSignal(baud);
      sendSignal(tempo);
   }

   /** Throws an exception if the given object is not of a class that
    *  is registered for transmission.
    * 
    * @param object Object testable object
    * @throws UnregisteredObjectException if parameter object is not eligible for transmission
    * @throws NullPointerException if parameter is null
    */
   private void checkObjectRegisteredForSending (Object object) {
      if (object == null) 
         throw new NullPointerException();
      
       if ( !getSendSerialization().isRegisteredClass(object.getClass()) )
          throw new UnregisteredObjectException(object.getClass().toString());
   }

   @Override
   public long sendData(byte[] buffer, int start, int length, SendPriority priority) {
      JennyNetByteBuffer buf = new JennyNetByteBuffer(buffer, start, length);
      return sendObject(buf, priority);
   }

   @Override
   public void breakTransfer(long objectID, int direction) {
      if (direction == 0) {
         breakIncomingTransfer(objectID);
      } else {
         breakOutgoingTransfer(objectID);
      }
   }

   protected void breakIncomingTransfer (long objectID) {
      checkConnected();
      
      // find an incoming file transmission
      FileAgglomeration fileRec = fileReceptorMap.get(objectID);
      if (fileRec != null) {
         fileRec.dropTransfer(108, 3, null);
      }
   }
   
   protected void breakOutgoingTransfer (long objectID) {
      checkConnected();
      
      // find an outgoing file transmission
      SendFileProcessor fileSender = fileSenderMap.get(objectID);
      if (fileSender != null) {
         fileSender.breakTransfer(105, 4, null);
      }
   }
   
   /**
    * Schedules a new task to realise sending of ALIVE signals to remote
    * with the given period. Also a control task is created to check
    * ALIVE-ECHO signals from remote and close the connection on timeout.
    * Any previously scheduled tasks are cancelled. 
    * A period value of 0 cancels the ALIVE mechanism. 
    * 
    * @param alivePeriod int signalling period in milliseconds
    * @throws IlllegalArgumentException if alivePeriod is negative
    */
   protected void setAlivePeriod (int alivePeriod) {
//     System.out.println("-- (ConnectionImpl.start) setting ALIVE period " + par.getAlivePeriod() 
//		+ ",  " + toString());
      AliveSignalTimerTask.createNew(this, alivePeriod);
      int controlPeriod = alivePeriod == 0 ? 0 : 
    	  				  alivePeriod + getParameters().getConfirmTimeout();
      AliveEchoControlTask.createNew(this, controlPeriod);
   }
   
   /** Sets whether this connection performs a period based checking of
    * data volume exchanged with remote station (IDLE/BUSY state).
    * The PERIOD of checking is derived from connection parameters.
    * Switch TRUE is suppressed (no-op) if connection is not connected.
    *  
    * @param v boolean true == check idle state, false == don't check idle state
    */
   protected void setCheckIdleState (boolean v) {
      // switch ON
      if (v) {
         if (!isConnected()) return;

         isCheckIdleState = true;
         int period = getParameters().getIdleCheckPeriod();

         // cancel an existing task if PERIOD differs with request
         if (checkIdleTask != null && checkIdleTask.getPeriod() != period) {
            checkIdleTask.cancel();
            checkIdleTask = null;
         }
         
         // create new check-idle-task if not running
         if (checkIdleTask == null) {
            checkIdleTask = new CheckIdleTimerTask(period);
         }
         
      // switch OFF   
      } else {
         isCheckIdleState = false;
         if (checkIdleTask != null) {
            // cancel running check-idle-task
            checkIdleTask.cancel();
            checkIdleTask = null;
         }
      }
   }
   
   /** Sets the IDLE state checking complete from connection parameters. 
    */ 
   protected void setupCheckIdleState () {
      setCheckIdleState(getParameters().getIdleThreshold() > 0);
   }
   
   void removeFileReceptor(long fileID) {
	   if (fileReceptorMap != null) {
		   fileReceptorMap.remove(fileID);
	   }
   }

   @Override
   public boolean isIdle() {
      return isIdle;
   }

   @Override
   public Properties getProperties () {
      if (properties == null) {
         properties = new Properties();
      }
      return properties;
   }

   /** Initialises all operations of this connection with new subsystem instances.
    * Any still lingering data elements are discarded and name counters start from zero.
    * 
    * @param socket Socket (requires to be bound and connected)
    * @throws IOException
    * @throws IllegalStateException if socket is not bound or not connected
    * @throws ClosedConnectionException if connection is closed      
    */
   protected void start (Socket socket) throws IOException {
      if (!socket.isConnected()) 
         throw new IllegalStateException("socket not connected!"); 
      if (!socket.isBound()) 
         throw new IllegalStateException("socket not bound to local address!"); 
      if (isClosed())
         throw new ClosedConnectionException(toString());
      
      // socket initialisation
      this.socket = socket;
      connected = true;
      int bufferSize = getParameters().getTransmissionParcelSize() + 100;
      socketOutput = new BufferedOutputStream(socket.getOutputStream(), bufferSize);
      socketInput = socket.getInputStream();
      ConnectionParameters par = getParameters();

      // data inits
      objectSerialCounter = 0;
      pingSerialCounter = 0;
      exchangedDataVolume = 0;
      transmitSpeed = par.getTransmissionSpeed();
      
      // create hashtables and services
      fileSenderMap = new Hashtable<Object, SendFileProcessor>(); 
      fileReceptorMap = new Hashtable<Long, FileAgglomeration>(); 
      objectReceptorMap = new Hashtable<Long, ObjectAgglomeration>(); 
      pingSentMap = new Hashtable<Long, Long>();

      // create data queues
      inputQueue = new PriorityBlockingQueue<UserObject>();
      objectReceiveQueue = new PriorityBlockingQueue<UserObject>();
      coreReceive = new CoreReceive();
      coreSend = new CoreSend();
      
      // create ALIVE signal sending if defined
      // also starts ALIVE-ECHO timeout control task 
      setAlivePeriod(par.getAlivePeriod());
      
      // create IDLE state checking task (if requested)
      setupCheckIdleState();
      
      // create and start data processors
      inputProcessor = new InputProcessor();
      outputProcessor = new OutputProcessor();
      receiveProcessor = new ReceiveProcessor();
      receiveProcessor.start();
      outputProcessor.start();
      inputProcessor.start();
   }
   
   @Override
   public void close() {
      close(null, 0);
   }
      
   protected void close (Throwable ex, int info) {
      if (closed) return;
      closed = true;

      // terminate structures
      if (aliveSignalTask != null) {
         aliveSignalTask.cancel();
      }
      if (aliveTimeoutTask != null) {
         aliveTimeoutTask.cancel();
      }
      setCheckIdleState(false);
      
      // shutdown send file threads (including signalling)
      if (fileSenderMap != null) {
    	  for (SendFileProcessor pro : getSendFileAgents()) {
    		  pro.breakTransfer(113, 6, null);
    	  }
      }
      
      // shutdown file receptor threads (including signalling)
      if (fileReceptorMap != null) {
    	  for (FileAgglomeration agg : getFileReceptors()) {
    		  agg.dropTransfer(114, 5, null);
    	  }
      }
      
      // terminate our processing threads
      if (inputProcessor != null) {
          inputProcessor.terminate();
       }
       
      if (outputProcessor != null) {
          outputProcessor.terminate();
       }
       
      if (receiveProcessor != null) {
          receiveProcessor.terminate();
       }
      
      if (coreReceive != null) {
          coreReceive.terminate();
       }

      if (coreSend != null) {
    	  String msg = ex == null ? null : ex.toString(); 
          coreSend.terminate(new ErrorObject(info, msg));
      }
      
      // clear incoming data queues
      if (objectReceiveQueue != null) {
    	  objectReceiveQueue.clear();
      }
      if (objectReceptorMap != null) {
    	  objectReceptorMap.clear();
      }
   }

   private void closeSocket (ErrorObject error) {
	  if (!connected) return;
      try {
          if (socket != null) {
             connected = false;
             socket.close();
             
             String message = error == null ? null : error.message;
             int info = error == null ? 0 : error.info;
             fireConnectionEvent(ConnectionEventType.disconnect, info, message);
             if (debug) {
	             System.out.println("----- Connection Closed ----- " + getLocalAddress() + "  --->  " 
	                   + getRemoteAddress() );
	             if (message != null || info != 0) {
	            	 System.out.println("-----    on error: code " + info + ", msg: " + message);
	             }
             }
          }
          
       } catch (IOException e) {
          e.printStackTrace();
       }
   }
   
   /** Returns the set of SendFileProcessor instances currently active
    * for sending file transmissions.
    * 
    * @return Set&lt;SendFileProcessor&gt;
    */
   protected Set<SendFileProcessor> getSendFileAgents () {
	   if (fileSenderMap == null) return null;
	   Collection<SendFileProcessor> values = fileSenderMap.values();
	   return new CopyOnWriteArraySet<SendFileProcessor>(values);
   }
   
   /** Returns the set of FileAgglomeration instances currently active
    * for receiving file transmissions.
    * 
    * @return Set&lt;SendFileProcessor&gt;
    */
   protected Set<FileAgglomeration> getFileReceptors () {
	   if (fileReceptorMap == null) return null;
	   return new CopyOnWriteArraySet<FileAgglomeration>(fileReceptorMap.values());
   }
   
   @Override
   public void addListener (ConnectionListener listener) {
      if (listener != null) {
         synchronized (listeners) {
            listeners.add(listener);
         }
      }
   }

   @Override
   public void removeListener (ConnectionListener listener) {
      if (listener != null) {
         synchronized (listeners) {
            listeners.remove(listener);
         }
      }
   }
   
   /** Returns a copy of the current list of listeners on this connection.
    * Modifications on the returned list do not strike through to
    * the connection's operative list!
    *  
    * @return array of <code>ConnectionListener</code>
    */
   protected ConnectionListener[] getListeners () {
      synchronized (listeners) {
         ConnectionListener[] array = new ConnectionListener[listeners.size()];
         listeners.toArray(array);
         return array;
      }
   }
   
   protected void fireTransmissionEvent (TransmissionEvent event) {
      if (event == null)
         throw new NullPointerException();
      
      synchronized (listenerDispatchLock) {
         ConnectionListener[] array = getListeners();
         for (ConnectionListener i : array) {
            i.transmissionEventOccurred(event);
         }
      }
   }

   protected void fireObjectEvent (UserObject object) {
      if (object == null)
         throw new NullPointerException();
      
      synchronized (listenerDispatchLock) {
         // dispatch object receive event to listeners
         ConnectionListener[] array = getListeners();
         for (ConnectionListener i : array) {
            i.objectReceived(this, object.getObjectNr(), object.getObject());
         }
      }
   }

   protected void firePingEchoEvent (PingEcho pingEcho) {
      if (pingEcho == null)
         throw new NullPointerException();
      
      synchronized (listenerDispatchLock ) {
         // dispatch PING-ECHO event to listeners
         ConnectionListener[] array = getListeners();
         for (ConnectionListener i : array) {
            i.pingEchoReceived(pingEcho);
         }
      }
   }

   protected void fireConnectionEvent (ConnectionEventType type, 
         int info, String message) {
      if (type == null)
         throw new NullPointerException();
      
      synchronized (listenerDispatchLock ) {
         // dispatch event to registered listeners
         ConnectionListener[] array = getListeners();
         
         switch (type) {
         case connect:
            for (ConnectionListener i : array) {
               i.connected(this);
            }
            break;
         case disconnect:
            for (ConnectionListener i : array) {
               i.disconnected(this, info, message);
            }
            break;
         case idle:
            for (ConnectionListener i : array) {
               i.idle(this, isIdle);
            }
            break;
         default:
            throw new UnsupportedOperationException("not ready for type: ".concat(type.toString()));
         }
      }
   }

   @Override
   public InetSocketAddress getRemoteAddress() {
      return socket == null ? null :
         (InetSocketAddress)socket.getRemoteSocketAddress();
   }

   @Override
   public InetSocketAddress getLocalAddress() {
      return socket == null ? null :
         (InetSocketAddress)socket.getLocalSocketAddress();
   }

   @Override
   public void setName(String name) {
      connectionName = name;
   }

   @Override
   public String getName() {
      return connectionName;
   }

   @Override
   public byte[] getShortId() {
      return shortId;
   }

	@Override
	public long getTransmissionVolume() {
		return exchangedDataVolume;
	}

	@Override
	public int getTransmissionSpeed() {
		return transmitSpeed;
	}
     
   protected void writeToSocket (TransmissionParcel parcel) throws IOException {
      if (debug) {
//         System.out.println("--- SEND: writing parcel to socket: " + socket.getPort());
         parcel.report(1, System.out);
      }
      
      long markTime = getLastSendTime();
      if (markTime == 0) {
    	  markTime = System.currentTimeMillis();
      }
      
      parcel.writeObject(socketOutput);
      socketOutput.flush();
      delay_baud(parcel, markTime, "writeToSocket");
   }
   
   protected TransmissionParcel readParcelFromSocket () throws IOException {
      long markTime = getLastReceiveTime();
      if (markTime == 0) {
    	  markTime = System.currentTimeMillis();
      }
      
       TransmissionParcel parcel = TransmissionParcel.readParcel(socketInput);
       delay_baud(parcel, markTime, "readParcelFromSocket");
       
       if (debug) {
          parcel.report(0, System.out);
       }
	   return parcel;
   }

   /** Performs BAUD related sleep time if opted. This is called after some 
    * socket activity has taken place.
    * 
    * @param parcel <code>TransmissionParcel</code>
    * @param markTime long 
    * @param function String algorithmic location
    */
   private void delay_baud (TransmissionParcel parcel, long markTime, String function) {
	  long speed = getTransmissionSpeed();
      if (speed > 0) {
    	  long shallLast = (long)parcel.getSerialisedLength() * 1000 / speed;
    	  long hasTaken = System.currentTimeMillis() - markTime;
    	  int delay = (int)(shallLast - hasTaken);
    	  if (delay > 0) {
              if (debug) {
            	  System.out.println("--- (" + function + ") : BAUD delay performing " 
            		  + delay + " ms sleep, " + "consumed " + hasTaken 
            		  + ", obj " + parcel.getObjectID());
              }
    		  Util.sleep(delay);
    	  }
      }
   }
   
   /** Returns the next object serial number for sending.
    * The number may not re-occur for this connection.
    * Numbers are starting from 1;
    * 
    * @return long serial number
    */
   protected long getNextObjectNr() {
      return ++objectSerialCounter;
   }

   protected Socket getSocket () {
      return socket;
   }
   
// --------------- inner classes ----------------   
   
   /** This thread performs serialisation of user input objects and puts them
    * into the core-send parcel queue.
    */
   private class InputProcessor extends Thread {
      boolean operating, terminated;
      
      InputProcessor () {
         super("Input Processor ".concat(String.valueOf(getLocalAddress())));
         setDaemon(true);
      }
      
      @Override
      public void run() {
         operating = true;
         setPriority(parameters.getBaseThreadPriority());
         
         while (operating) {
            try {
            	// check operative status
            	operating = !terminated || !inputQueue.isEmpty();
            	if (!operating) continue;
            	
               // get the next input object (blocking)
               UserObject object = inputQueue.take();
               long objectNr = object.objectID; 
               SendPriority priority = object.priority;
               
               // serialise the input object
               byte[] serObj;
               try {
            	   serObj = getSendSerialization().serialiseObject(object.getObject());
               } catch (Exception e) {
            	   throw new IllegalStateException("send serialisation error (" +
            			   getLocalAddress() + ") object-id " + objectNr, e);
               }
            		   
               if (serObj.length > parameters.getMaxSerialisationSize()) {
            	   throw new IllegalStateException("send serialisation size overflow for object " + 
            			   objectNr + ", size " + serObj.length);
               }
               
               // split serialisation into send parcels
               TransmissionParcel[] parcelBundle = 
                  TransmissionParcel.createParcelArray(
                        TransmissionChannel.OBJECT, serObj, objectNr, priority,
                        parameters.getTransmissionParcelSize());

               // store parcels in send-queue
               for (TransmissionParcel parcel : parcelBundle) {
                  coreSend.put(parcel);
               }
            } catch (InterruptedException e) {
            } catch (Throwable e) {
            	e.printStackTrace();
            	close(e, 2);
            }
         }
      }
      
      /** Terminates this thread. Thread may continue
       * operations in termination state until its input queue is empty. 
       */
      public void terminate () {
         terminated = true;
         interrupt();
      }

      /** Whether this thread has been terminated. Thread may continue
       * operations in termination state until its inpu queue is empty. 
       * 
       * @return boolean
       */
      public boolean isTerminated () {
    	  return terminated;
      }
   }
   
   /** Handles delivery of output objects (de-serialised net-received objects)
    * to the application.
    */
   private class OutputProcessor extends Thread {
      boolean operating;

      public OutputProcessor () {
         super("Output Processor ".concat(String.valueOf(getLocalAddress())));
         setDaemon(true);
      }
      
      @Override
      public synchronized void run() {
         operating = true;
         setPriority(parameters.getBaseThreadPriority());
         
         while (operating) {
            UserObject object;
            try {

               // read from object-receive-queue and deliver to application
               while ((object = objectReceiveQueue.take()) != null) {
                  // dispatch event to connection listeners
            	  if (object.getObject() instanceof PingEcho) {
                      firePingEchoEvent((PingEcho)object);
            	  } else {
            		  fireObjectEvent(object);
            	  }
               }
               
            } catch (InterruptedException e) {
            	
            } catch (Throwable e) {
               System.out.println("********  UNCAUGHT APPLICATION EXCEPTION  ******** : \n" + e);
               e.printStackTrace();
            }
         }
      }
      
      public void terminate() {
         operating = false;
         interrupt();
      }
   }
   
   /** Thread to send a single file over the net.
    * 
    */
   private class SendFileProcessor extends Thread 
   {
      private boolean terminate;
      private File file;
      private String remotePath;
      private InputStream fileIn;
      private SendPriority priority;
      private long fileID;  // transmission object number
      private long fileLength;
      private long transmittedLength;
      private long startTime, duration;
      private int nrOfParcels;
      private int parcelBufferSize;
      private boolean ongoing;

      /** Creates a new file send processor (Thread) for a given file
       * and remote destination parameter
       * 
       * @param file File file to transmit
       * @param remotePath String destination parameter for remote system (may be null)
       * @param priority <code>SendPriority</code>
       * @throws FileInTransmissionException if there is already a transmission for that file
       * @throws IllegalFileLengthException if file length exceeds limit of 2.1 GB
       * @throws FileNotFoundException if opening the file was impossible
       * @throws IllegalStateException if sender limitation is exceeded (rejected order)
       * @throws IOException
       */
      public SendFileProcessor (File file, String remotePath, SendPriority priority) 
            throws IOException {
         super("Send File Processor ".concat(String.valueOf(getLocalAddress())));
         if (file == null)
            throw new NullPointerException("file == null");
         
         this.remotePath = remotePath;
         this.file = file.getCanonicalFile();
         this.priority = priority;
         init();
         start();
      }
      
      private void init () throws FileNotFoundException {
          if (coreSend == null | fileSenderMap == null) {
              throw new IllegalStateException("initialisation error");
          }
          if (fileSenderMap.size()+1 > getParameters().getObjectQueueCapacity()) {
              throw new IllegalStateException("send-file limitation overflow");
          }

         // check if file is not already in transmission
         if (fileSenderMap.containsKey(file)) {
            throw new FileInTransmissionException();
         }
         
         // check if file input stream can be obtained
         fileIn = new BufferedInputStream(new FileInputStream(file), JennyNet.STREAM_BUFFER_SIZE);
         fileLength = file.length();
         
         // check if filelength is legal
         if (fileLength > Integer.MAX_VALUE) {
            throw new IllegalFileLengthException("maximum file length is 2.1 GB");
         }

         // calculate operation values
         parcelBufferSize = parameters.getTransmissionParcelSize();
         nrOfParcels = (int)(fileLength / parcelBufferSize);
         if (fileLength % parcelBufferSize > 0) {
            nrOfParcels++;
         }
         nrOfParcels = Math.max(1, nrOfParcels);
         fileID = getNextObjectNr();
         ongoing = true;
         if (debug) {
        	 System.out.println("--- created FileSendProcessor ID " + fileID + ", length " + fileLength 
        			 + ", parcels " + nrOfParcels + ", remote [" + remotePath + "]");
         }
         
         // register transmission
         fileSenderMap.put(file, this);
         fileSenderMap.put(fileID, this);
      }
      
      @Override
      public void run() {
         setPriority(Math.max(parameters.getBaseThreadPriority()-2, Thread.MIN_PRIORITY));
         startTime = System.currentTimeMillis();
         byte[] buffer = new byte[parcelBufferSize];
         int parcelNr = 0;
          
         while (!terminate) {
            try {
               // read from file (blocking) 
               int readLen = fileIn.read(buffer);
               if (parcelNr > 0 & readLen == -1) {
                  parcelsSent();
                  break;
               }
               
               // construct next parcel
               TransmissionParcel parcel = new TransmissionParcel(
                     fileID, parcelNr, buffer, 0, Math.max(readLen, 0));
               parcel.setChannel(TransmissionChannel.FILE);
               parcel.setPriority(priority);
               if (debug) {
            	   System.out.println("--- created FILE PARCEL: file-ID " + fileID + ", ser " + parcelNr);
               }
               
               // construct an object header in parcel number 0
               if (parcelNr == 0) {
                  ObjectHeader header = parcel.getObjectHeader();
                  header.setTransmissionSize((int)fileLength);
                  header.setPath(remotePath);
                  header.setNrOfParcels(nrOfParcels);
               }

               // add a timer task for TRANSFER CONFIRM on last parcel
               if (parcelNr+1 == nrOfParcels) {
                  AbortFileTimeoutTask timeoutTask = new AbortFileTimeoutTask(
                      ConnectionImpl.this, fileID, parameters.getConfirmTimeout());
                  parcel.setTimerTask(timeoutTask);
               }

               // queue file parcel for sending (blocking)
               coreSend.put(parcel);
               transmittedLength += parcel.getLength();
               parcelNr++;
               
            } catch (Exception e) {
            	e.printStackTrace();
                breakTransfer(111, 2, e);
                break;
            } 
         }
      }

      /** Upon finishing queueing file parcels for sending. 
       * @throws IOException */
      private void parcelsSent () throws IOException {
         fileIn.close();
         if (debug) {
        	 System.out.println("--- parcels queued for sending, source = " + file);
         }
      }
      
      private void cancelTransfer () {
         // de-register this transmission thread
         duration = getTransmitTime();
         ongoing = false;
         fileSenderMap.remove(fileID);
         fileSenderMap.remove(file);

         // purge outgoing parcels
//         purgeSendFileQueue(fileID);
// was not successful because of collisions of iterator remove with peek+poll (send-processor)   
         
         try { fileIn.close(); } 
         catch (IOException e) {
            e.printStackTrace();
         }
      }
      
      /** Upon reception of a CONFIRM or a FAIL signal from remote. 
       * 
       * @param success boolean true == file transfer confirmed 
       */
      public void finishTransfer (boolean success) {
         cancelTransfer();
         if (debug) {
        	 System.out.println("--- finishing SEND file transfer ID " + fileID  
        			 + ", success=" + success + ", target: " + remotePath
        			 + ", " + ConnectionImpl.this);
         }
         
         // EVENT dispatch: inform user (file transfer event)
         TransmissionEventType type = success ? TransmissionEventType.FILE_CONFIRMED 
               : TransmissionEventType.FILE_FAILED;
         TransmissionEventImpl event = new TransmissionEventImpl(
               ConnectionImpl.this, type, fileID);
         event.setInfo(success ? 0 : 101);
         event.setDuration(duration);
         event.setTransmissionLength(transmittedLength);
         event.setPath(remotePath);
         event.setFile(file);
         fireTransmissionEvent(event);
      }

      /** Terminates this file transmission by stating a causing exception
       * and sending a BREAK signal to remote.
       *
       * @param eventInfo int
       * @param signalInfo int signal subtype, if != 0 sends a BREAK signal 
       *        of this type to remote
       * @param e Exception error message
       */
      public void breakTransfer (int eventInfo, int signalInfo, Exception e) {
         if ( !ongoing ) {
            return;
         }
         cancelTransfer();

         if (debug) { 
       	     System.out.println("-- dropping outgoing file transfer ID " + fileID + ", con: " + ConnectionImpl.this);
    	     System.out.println("   signal to remote: BREAK " + signalInfo);
             System.out.println("   FILE_ABORTED event " + eventInfo);
          }

         // send a TRANSFER BREAK signal to remote
         if (signalInfo != 0) {
            String text = e == null ? null : e.toString();
            sendSignal(Signal.newBreakSignal(fileID, signalInfo, text));
         }

         // issue ABORTED event to user
         if (eventInfo != 0) {
            // inform the user (remote abortion event)
            TransmissionEventImpl event = new TransmissionEventImpl(ConnectionImpl.this,
                 TransmissionEventType.FILE_ABORTED,
                 fileID, eventInfo, e);
            event.setDuration(duration);
            event.setPath(remotePath);
            event.setFile(file);
            event.setTransmissionLength(transmittedLength);
            event.setExpectedLength(fileLength);
            fireTransmissionEvent(event);
         }
         
         terminate = true;
         interrupt();
      }

      /** Terminates this file transmission for a timeout event
       * sends a FAIL signal to remote and issues a FILE_FAILED event to local.
       * 
       * @param sec int timeout time in seconds
       */
      public void timeoutTransfer (int sec) {
         if (!ongoing) return;
         
         // stop transfer
         cancelTransfer();

         // signal FAILURE to remote
         sendSignal(Signal.newFailSignal(fileID, 2, "timeout = " + sec + " sec"));

         // EVENT dispatch: inform user (timeout abortion event)
         TransmissionEventImpl event = new TransmissionEventImpl(ConnectionImpl.this,
              TransmissionEventType.FILE_FAILED, fileID, 103);
         event.setDuration(duration);
         event.setPath(remotePath);
         event.setFile(file);
         event.setTransmissionLength(transmittedLength);
         event.setExpectedLength(fileLength);
         fireTransmissionEvent(event);
         
         // trigger thread termination
         terminate = true;
         interrupt();
      }

      public long getFileID() {
         return fileID;
      }

      @SuppressWarnings("unused")
	public long getTransmittedLength() {
         return transmittedLength;
      }

      public long getTransmitTime() {
         return ongoing ? System.currentTimeMillis() - startTime : duration;
      }

//      /** Terminates this file transmission without stating a cause.
//       */
//      public void terminate () {
//         breakTransfer(0, 0, null);
//      }

   }
   
   
   /** This is a BlockingQueue for TransmissionParcel whose digesting end
    * is automatically sending parcels over the net socket. It owns a 
    * daemon thread which takes and sends the parcels. The thread runs
    * as long as parcels are available in the queue OR it has not been
    * terminated.
    *
    * Currently this terminates if an IO error occurs on the socket.
    */
   private class CoreSend extends PriorityBlockingQueue<TransmissionParcel> 
   {
      boolean terminate, working;
      long lastTransmitTime;
      long currentLoad;
      long loadLimit;
      boolean sendingOff;
      ErrorObject error;
      Thread send;
      
      public CoreSend () {
         super(64);
         loadLimit = Math.max(parameters.getParcelQueueCapacity() *
        		 	parameters.getTransmissionParcelSize(), 16*1024);
         sendingOff = getTransmissionSpeed() == 0;
         
         send = new Thread("CoreSend ".concat(String.valueOf(getLocalAddress())))
         {
            @Override
            public synchronized void run() {
               setPriority(Thread.MAX_PRIORITY);
               
               while (working) {
                  try {
                	 // determine whether thread has to stop 
                	 working = !terminate || (!isEmpty() & !sendingOff);
                	 if (!working) continue;
            		 interrupted();
                	  
                	 // enter waiting if sending switched off
            		 // we send only SIGNAL parcels in sending-off state
                	 if (sendingOff) {
                		 if (debug) {
                			 System.out.println("-- CORE-SEND: sending is OFF");
                		 }
                		 wait_for_send_signal();
                	 }
                	  
                     // take next parcel from send-queue
                     TransmissionParcel parcel = take();
                     
                     // avoid sending invalid parcels of cancelled file transfers
                     if (parcel.getChannel() == TransmissionChannel.FILE &&
                    	 fileSenderMap.get(parcel.getObjectID()) == null) {
                    	 if (debug) {
                    		 System.out.println("-- dropped a FILE SENDER parcel, id " 
                    				 + parcel.getObjectID() + ", nr " + parcel.getParcelSequencelNr());
                    	 }
                    	 continue;
                     }
                     
                     // send parcel over network socket
                     currentLoad -= parcel.getSerialisedLength();
                     writeToSocket(parcel);
                     lastTransmitTime = System.currentTimeMillis();
                     
                     // sum up exchanged data (e.g. for IDLE state control)
                     if (!parcel.isSignal()) {
                        exchangedDataVolume += parcel.getSerialisedLength();
                     }
                     
                     // schedule a timer-task that may be defined on the parcel
                     SchedulableTimerTask task = parcel.getTimerTask();
                     if (task != null) {
                        task.schedule(timer);
                     }
                     
                  } catch (InterruptedException e) {

                  } catch (Throwable e) {
                     e.printStackTrace();
                     working = false;
                     close(e, 1);
                  } 
               }  // while loop

               // close the network socket when core-send terminates
               closeSocket(error);
               clear();
            }
         };
         working = true;
         send.setDaemon(true);
         send.start();
      }
      
   /** Waits until a send-signal appears in the send-queue or an interrupt
    * occurs. Granularity 1000 ms.
    * 
    * @throws InterruptedException
    */
   private void wait_for_send_signal() throws InterruptedException {
	  do {
		 TransmissionParcel p = peek();
		 if (p != null && p.isSignal()) {
			 if (debug) {
				 System.out.println("-- SIGNAL detected (on wait): " + p.getObjectID() 
				 			+ " " + p.getParcelSequencelNr());
			 }
			 break;
		 }
		 Thread.sleep(1000);
	  } while (true);
   }
      
    /** Inserts the specified data parcel into this priority queue. 
    * This method may block until space is made available in the queue.
    * 
    * @param parcel <code>TransmissionParcel</code>
    */
    @Override
	public void put (TransmissionParcel parcel) {
		// naive blocking behaviour depending on data load
		do {
			if (currentLoad < loadLimit) {
				break;
			}
			Util.sleep(25);
		} while (true);

		// put parcel into sorting queue
		super.put(parcel);
		currentLoad += parcel.getSerialisedLength();
		if (debug) {
			System.out.println("-- (coreSend) putting PARCEL w/ priority " + parcel.getPriority().ordinal() 
					+ ", " + parcel.getPriority());
		}
	}

    @Override
	public boolean add (TransmissionParcel parcel) {
		// unconditional parcel add
		boolean b = super.add(parcel);
		currentLoad += parcel.getSerialisedLength();
		return b;
	}

      /** Sets the cardinal send control (on/off state). If sending is off
       * the send-parcel queue is not addressed for reduction.
       *    
       * @param send boolean true == send data, false == wait state
       */
      public void setSending (boolean doSend) {
    	  if (debug & sendingOff == doSend) {
    		  System.out.println("-- set SENDING to '" + doSend +"' : " + ConnectionImpl.this.toString());
    	  }
    	  sendingOff = !doSend;
    	  if (doSend) {
    		  send.interrupt();
    	  }
      }
    
      public long getLlastTransmitTime () {
          return lastTransmitTime;
       }
       
      public void terminate (ErrorObject error) {
    	 this.error = error;
         terminate = true;
         if (sendingOff) {
        	 clear();
         }
         send.interrupt();
      }

      @Override
      protected void finalize() throws Throwable {
         terminate(new ErrorObject(99, "instance finalized"));
         super.finalize();
      }

      public void setThreadPriority(int p) {
         send.setPriority(p);
      }

//    public long getCurrentLoad () {
//  	return currentLoad;
//  }
//    
   }
   
   /** A BlockingQueue that contains transmission parcels received from
    * the network socket for the OBJECT CHANNEL and also works as the core
    * RECEIVE PROCESSOR. Received SIGNALs are immediately digested, 
    * FILE CHANNEL parcels are distributed to the corresponding file 
    * agglomeration objects, which are processors themselves.
    */
   private class CoreReceive extends LinkedBlockingQueue<TransmissionParcel> {
      boolean operating;
      long lastTransmitTime;
      
      public CoreReceive () {
    	  super(getParameters().getParcelQueueCapacity());
    	  operating = true;
    	  receive.setDaemon(true);
    	  receive.start();
      }
      
      Thread receive = new Thread("CoreReceive ".concat(String.valueOf(getLocalAddress())))
      {
	      @Override
	      public void run() {
	         operating = true;
	         setPriority(parameters.getTransmitThreadPriority());
	         
	         while (operating) {
	            try {
	               // read next incoming parcel from remote (blocking)
	               TransmissionParcel parcel = readParcelFromSocket();
	               lastTransmitTime = System.currentTimeMillis();
	
	               // branch parcel path into SIGNAL, FILE and OBJECT digestion
	               switch (parcel.getChannel()) {
	               case SIGNAL: 
	                  signalReceiveDigestion(parcel);
	               break;
	               case OBJECT: 
		              // sum up exchanged data for IDLE state control (if opted)
		              exchangedDataVolume += parcel.getSerialisedLength();
		              CoreReceive.this.put(parcel);
	               break;
	               case FILE: 
	                  // sum up exchanged data for IDLE state control (if opted)
	                  exchangedDataVolume += parcel.getSerialisedLength();
	                  fileReceiveDigestion(parcel);
	               break;
	               default: throw new IllegalStateException("SOCKET-RECEIVE: unknown parcel channel");
	               }
	            
	            } catch (SocketException e) {
	               if (!closed) {
	                  e.printStackTrace();
	                  close(e, 3);
	               }
	               
	            } catch (Throwable e) {
	               if (!(e instanceof EOFException)) {
	                  e.printStackTrace();
	               }
	               close(e, 3);
	            }
	         }
	         
	         // empty queue when finished operations
	         clear();
	      }
      };
      
      private void fileReceiveDigestion(TransmissionParcel parcel) 
            throws InterruptedException {
         
         // try find the specific receptor queue to take the parcel
         long fileID = parcel.getObjectID();
         FileAgglomeration fileQueue = fileReceptorMap.get(fileID);

         // create and memorise a new file receptor if none exists
         if (fileQueue == null) {
            
            // drop parcels of aborted  objects 
        	// we expect serial 0 for initial parcel
            if (parcel.getParcelSequencelNr() > 0) {
            	if (debug) {
            		System.out.println("-- FILE RECEIVE dropping out-of-sync parcel (ID=" + 
            				parcel.getObjectID() + ", serial=" + 
            				parcel.getParcelSequencelNr() + ")");
            	}
                return;
            }
            
            // insert new receptor into receptor map 
            // happens only with header != null
            try { 
               fileQueue = new FileAgglomeration(ConnectionImpl.this, fileID); 
               fileReceptorMap.put(fileID, fileQueue);
            } catch (Exception e) {
               // RETURN SIGNAL: incoming file can not be received (some error)!
               sendSignal(Signal.newBreakSignal(fileID, 1, e.toString()));
            }
         }
         
         // just put the parcel in queue, they do the rest!
         fileQueue.put(parcel);
      }

      private void signalReceiveDigestion (TransmissionParcel parcel) {
         
         // identify signal (analyse parcel)
         Signal signal = new Signal(parcel);
         SignalType type = signal.getSigType();
         int info = signal.getInfo();
         long objectID = parcel.getObjectID();
     	 if (debug) {
     		 System.out.println("-- SIGNAL REC (ob " + objectID + "): " + type +
     				 " (i " + signal.getInfo() + ") from " + getRemoteAddress() + ", [" + getLocalAddress() + "]");
     	 }
         
         switch (type) {
         case ALIVE:
            sendSignal(Signal.newAliveEchoSignal());
         break;
         case ALIVE_ECHO:
        	 if (aliveTimeoutTask != null) {
        		 aliveTimeoutTask.pushConfirmedTime();
        	 }
          break;
         case PING:
            sendSignal(Signal.newEchoSignal(objectID));
         break;
         case ECHO:
            try {
               // create and store a PING-ECHO instance 
               // by removing the stored ping-sent time information
               long timeSent = pingSentMap.remove(objectID);
               PingEcho pingEcho = PingEchoImpl.create(ConnectionImpl.this, objectID, 
                     timeSent, (int)(System.currentTimeMillis() - timeSent));
               putObjectToReceiveQueue(new UserObject(pingEcho));
               
            } catch (Exception e) {
               e.printStackTrace();
            }
         break;
         case BREAK:
            // if a file receptor is concerned ..
        	boolean isIncomingFile = info == 6 || info == 4 || info == 2;
        	
        	// if incoming file is concerned ..
        	if (isIncomingFile) {
        		// drop transfer on the file-agglomeration
                if (debug) {
             	   System.out.println("-- (signal digestion) dropping INCOMING FILE TRANSFER (BREAK) " + objectID);
                }
	            FileAgglomeration fileQueue = fileReceptorMap.get(objectID);
	            if (fileQueue != null) {
	               int eventInfo = info == 2 ? 112 : info == 4 ? 106 : 114;
	               fileQueue.dropTransfer(eventInfo, 0, 
	                     new RemoteTransferBreakException(signal.getText()));
	            }
        	}
        	
            // if outgoing file is concerned ..
            else {
        	   // drop transfer on the send-file-processor
               if (debug) {
            	   System.out.println("-- (signal digestion) dropping OUTGOING FILE TRANSFER (BREAK) " + objectID);
               }
               SendFileProcessor fileSender = fileSenderMap.get(objectID);
               if (fileSender != null) {
                  int eventInfo = info == 1 ? 109 : info == 3 ? 107 : 113;
                  fileSender.breakTransfer(eventInfo, 0, 
                      new RemoteTransferBreakException(signal.getText()));
               } else {
            	   System.out.println("   ERROR: send-file-processor not found!");
               }
            }
         break;
         case CONFIRM:
            // finish a file sender (OK)
        	SendFileProcessor fileSender = fileSenderMap.get(objectID);
            if (fileSender != null) {
               fileSender.finishTransfer(true);
            }
         break;
         case FAIL:
             // finish a file sender (Failure)
            if (info == 1) {
               fileSender = fileSenderMap.get(objectID);
               if (fileSender != null) {
                  fileSender.finishTransfer(false);
               }
               
               // finish a file receptor (Failure)
            } else if (info == 2) {
               FileAgglomeration fileQueue = fileReceptorMap.get(objectID);
               if (fileQueue != null) {
            	   fileQueue.dropTransfer(104, 0, null);
               }
            }
         break;
         case TEMPO:
        	if (getTransmissionSpeed() != info) {
	        	if (!fixedTransmissionSpeed) {
	        		// set the new local transmission speed after remote
	        		transmitSpeed = info;
	        		if (coreSend != null) {
	      	  			coreSend.setSending(info != 0);
	        		}
	            	if (debug) {
	      	  			System.out.println("-- TEMPO SIGNAL received, new TRANSMIT BAUD is " + info);
	            	}
	        	} else {
	        		// ignore remote TEMPO setting due to local priority setting
	        		// reset remote speed
	            	if (debug) {
	      	  			System.out.println("-- TEMPO SIGNAL received with Baud " + info + " --> ignored!");
	            	}
	      	  		setTempo(getTransmissionSpeed());
	        	}
        	}
         break;
         }
      }

      public long getLlastTransmitTime () {
         return lastTransmitTime;
      }
      
      public void setThreadPriority(int p) {
         receive.setPriority(p);
      }
      
      public void terminate () {
//         System.out.println("~~~ CoreRecevie Terminate! ~~~");
         operating = false;
         receive.interrupt();
      }
   }

   private static class AbortFileTimeoutTask extends SchedulableTimerTask {
      private ConnectionImpl connection;
      private long fileId;
      private int out_time;
      
      /** A new file transfer cancel task.
       * 
       * @param c IConnection
       * @param fileId long file transfer
       * @param time int delay in milliseconds
       */
      public AbortFileTimeoutTask (ConnectionImpl c, long fileId, int time) {
         super(time, "AbortFileTimeoutTask, file=" + fileId);
         connection = c;
         this.fileId = fileId;
//         timer.schedule(this, time);
      }
      
      @Override
      public void run() {
         // find outgoing file transmission and timeout
         SendFileProcessor fileSender = connection.fileSenderMap.get(fileId);
         if (fileSender != null) {
         	if (debug) {
               System.out.println("CON-TIMER: Cancelling file transfer on missing CONFIRM: " 
                       + connection.getRemoteAddress() + " FILE = " + fileId);
         	}

            fileSender.timeoutTransfer(out_time);
         }
      }
   }
   
   private class CheckIdleTimerTask extends TimerTask {
      private long volumeMarker;
      private int period;
      
      public CheckIdleTimerTask (int period) {
         this.period = period;
         this.volumeMarker = exchangedDataVolume;
         
         // log output
     	 if (debug) {
     		 System.out.println("-- created new CHECK-IDLE-TIMERTASK, period = "
     			 + period + ", marker = " + System.currentTimeMillis() + ", " 
                 + ConnectionImpl.this);
     	 }

         // schedule the new task
         timer.schedule(this, period, period);
      }
      
      public int getPeriod () {
         return period;
      }
      
      @Override
      public boolean cancel () {
         boolean ok = super.cancel();
         
     	 if (debug) {
            System.out.println("--- canceled CHECK-IDLE-TIMERTASK, marker = " 
                  + System.currentTimeMillis() + ", " + ConnectionImpl.this);
     	 }
         return ok;
      }

      @Override
      public void run () {
         // determine data exchanged since last investigation
         long delta = exchangedDataVolume - volumeMarker;
         volumeMarker = exchangedDataVolume;
         
         if (isCheckIdleState) {
            // calculate the current threshold in a complicated manner
            int period = this.period / 1000;
            long threshold = ((long)getParameters().getIdleThreshold())*1000/60 
                             * period / 1000;
   
            // fire connection event if IDLE checking results positive
            if (delta < threshold) {
               fireConnectionEvent(ConnectionEventType.idle, (int)delta, null);
            }
         }
      }
   }
   
   /** Timer Task to send periodic ALIVE signals to remote station.
    * 
    */
   private static class AliveSignalTimerTask extends TimerTask {
      private ConnectionImpl connection;
      private long sendTime;
      
      /** Creates a new ALIVE SIGNAL timer task for periodic signalling
       * to the remote station of the given connection. A previously 
       * created task is cancelled. 
       * 
       * <p><small>If <code>period</code> is zero, a previous
       * task is cancelled but no new one is created.</small>
       * 
       * @param con Connection
       * @param period int ALIVE sending interval in milliseconds
       * @throws IllegalArgumentException if period is negative
       */
      public static void createNew (ConnectionImpl con, int period) {
    	 if (con.aliveSignalTask != null) {
    		 con.aliveSignalTask.cancel();
    	 }

    	 if (period > 0) {
             // create a new timer task
             con.aliveSignalTask = new AliveSignalTimerTask(con, period);
         } else {
        	 con.aliveSignalTask = null;
         }
      }
      
      /** A new timer task for periodic ALIVE signalling to remote
       * or controlling ALIVE timeout from remote (depending on execution
       * modus). Cancels a previously scheduled task of the same type.
       *  
       * @param c Connection
       * @param period int period in milliseconds (must be > 0)
       * @throws IllegalArgumentException if period is 0 or negative          
       */
      private AliveSignalTimerTask (ConnectionImpl c, int period) {
         if (period <= 0) 
            throw new IllegalArgumentException("period <= 0");
         
         connection = c;

         // log output
     	 if (debug) {
     		 System.out.println("CREATE ALIVE-TIMER on : " + c + ", period=" + period);
     		 System.out.println("             time marker = " + System.currentTimeMillis());
     	 }

         // schedule this task
         timer.schedule(this, period, period);
      }
      
      public long getSendTime () {
    	  return sendTime;
      }
      
      @Override
      public void run() {
    	 // cancel task if connection stopped
         if (!connection.isConnected()) {
        	cancel();
            return;
         }
         
    	 connection.sendSignal(Signal.newAliveSignal());
    	 int delta = sendTime == 0 ? 0 : (int)(System.currentTimeMillis() - sendTime)/1000;
    	 sendTime = System.currentTimeMillis();
     	 if (debug) {
     		 System.out.println("-- ALIVE sent to : " + connection.getRemoteAddress() + 
     				 ", delta=" + delta + " sec");
     	 }
      }
   }

   /** Class to contain the local connection parameters and administer
    * local services on parameter value change.
    */
   private class OurParameters extends ConnectionParametersImpl {
      private String rejectMsg = "parameter may not change in operational connection";

      /** Creates a new OurParameters from the global set of JennyNet parameters.
       */
      OurParameters () {
      }
      
      /** Creates a new OurParameters from the given set of parameters.
       * All values from the given parameter set are copied.
       * 
       * @param p ConnectionParameters
       * @throws IOException 
      OurParameters (ConnectionParameters p) throws IOException {
      }
       */
      
      /** Copies all values from the given parameter set to this
       * parameter set. This includes side effects to the enclosing 
       * connection.
       * 
       * @param p ConnectionParameters
       * @throws IOException
       */
      protected void takeOver (ConnectionParameters p) throws IOException {
         if (isConnected()) 
            throw new IllegalStateException(rejectMsg);

         setAlivePeriod(p.getAlivePeriod());
         setBaseThreadPriority(p.getBaseThreadPriority());
         setCodingCharset(p.getCodingCharset());
         setConfirmTimeout(p.getConfirmTimeout());
         setFileRootDir(p.getFileRootDir());
         setObjectQueueCapacity(p.getObjectQueueCapacity());
         setParcelQueueCapacity(p.getParcelQueueCapacity());
//         setSerialisationMethod(p.getSerialisationMethod());
         setTempDirectory(p.getTempDirectory());
         setTransmissionParcelSize(p.getTransmissionParcelSize());
         setTransmitThreadPriority(p.getTransmitThreadPriority());
         setIdleThreshold(p.getIdleThreshold());
         setTransmissionSpeed(p.getTransmissionSpeed());
      }
      
      @Override
      public void setBaseThreadPriority(int p) {
         super.setBaseThreadPriority(p);
         if (inputProcessor != null)  
            inputProcessor.setPriority(p);
         if (outputProcessor != null)  
            outputProcessor.setPriority(p);
         if (receiveProcessor != null)  
            receiveProcessor.setPriority(p);
         setTransmitThreadPriority(getTransmitThreadPriority());
      }

      @Override
      public void setTransmitThreadPriority(int p) {
         super.setTransmitThreadPriority(p);
         if (coreSend != null)  
            coreSend.setThreadPriority(p);
         if (coreReceive != null)  
            coreReceive.setThreadPriority(p);
      }

      @Override
	  public void setTransmissionSpeed(int tempo) {
	   	 if (getTransmissionSpeed() == tempo) return;
	     super.setTransmissionSpeed(tempo);
	     if (isConnected()) {
	    	 setTempo(tempo);
	     }
	  }

	  @Override
      public void setConfirmTimeout (int timeout) {
         super.setConfirmTimeout(timeout);

         // create new ALIVE timeout controlling if defined
         if (getAlivePeriod() > 0 && isConnected()) {
            int period = getAlivePeriod() + getConfirmTimeout()/2;
            AliveEchoControlTask.createNew(ConnectionImpl.this, period);
         }
      }

      @Override
      public void setAlivePeriod (int period) {
         super.setAlivePeriod(period);
         if (isConnected()) {
        	 ConnectionImpl.this.setAlivePeriod(getAlivePeriod());
         }
      }

      @Override
      public void setIdleThreshold (int idleThreshold) {
         super.setIdleThreshold(idleThreshold);
         
         setCheckIdleState(idleThreshold > 0);
      }

      @Override
      public void setIdleCheckPeriod (int period) {
         super.setIdleCheckPeriod(period);
         
         setupCheckIdleState();
      }

      // methods with restricted accessibility (must be unconnected)
      
      @Override
      public void setParcelQueueCapacity (int parcelQueueCapacity) {
         if (isConnected()) 
            throw new IllegalStateException(rejectMsg);
         super.setParcelQueueCapacity(parcelQueueCapacity);
      }

      @Override
      public void setObjectQueueCapacity (int objectQueueCapacity) {
         if (isConnected()) 
            throw new IllegalStateException(rejectMsg);
         super.setObjectQueueCapacity(objectQueueCapacity);
      }

      @Override
      public void setCodingCharset (Charset charset) {
         if (isConnected()) 
            throw new IllegalStateException(rejectMsg);
         super.setCodingCharset(charset);
      }
   }
   
   // --------------- inner classes ----------------   
      
      /** This thread performs de-serialisation of objects received from remote
       * and puts the resulting objects into the objectReceiveQueue. 
       * It services solely the OBJECT channel.
       */
      private class ReceiveProcessor extends Thread {
         boolean operating;
         
         ReceiveProcessor () {
            super("Receive Processor ".concat(String.valueOf(getLocalAddress())));
            setDaemon(true);
         }
         
         @Override
         public void run() {
            operating = true;
            setPriority(parameters.getBaseThreadPriority());
            
            while (operating) {
               try {
                  // get next received parcel (blocking)
                  TransmissionParcel parcel = coreReceive.take();
                  long objectNr = parcel.getObjectID();

                  // look for the relevant parcel agglomeration from registry
                  ObjectAgglomeration agglom = objectReceptorMap.get(objectNr);                 
                  boolean isNewObject = agglom == null;
                  
                  // if agglomeration not found, create a new one
                  if (isNewObject) {
                     agglom = new ObjectAgglomeration(ConnectionImpl.this, objectNr, parcel.getPriority());
                  }
                  
                  // let agglomeration digest received parcel
                  agglom.digestParcel(parcel);
                  
                  // if object is finished
                  if (agglom.objectReady()) {
                     // put result it into output queue 
                     putObjectToReceiveQueue(new UserObject(agglom.getObject(), objectNr, agglom.getPriority()));
                     if (debug) {
                    	 System.out.println("--- OBJECT received (deserialised) to Queue: " + objectNr);
                     }

                     // remove done multi-part agglomeration from registry
                     if (!isNewObject) {
                        objectReceptorMap.remove(objectNr);
                        if (debug) {
                        	System.out.println("--- OBJECT agglomeration de-registered: " + objectNr);
                        }
                     }
                  }
                  
                  // insert new and multi-part agglomeration into registry
                  else if (isNewObject) {
                     objectReceptorMap.put(objectNr, agglom);
                     if (debug) {
                    	 System.out.println("--- NEW OBJECT agglomeration registered: " + objectNr);
                     }
                  }
                     
                  
               } catch (InterruptedException e) {
               } catch (Throwable e) {
                  e.printStackTrace();
                  close(e, 4);
               }
            }
         }
         
         public void terminate () {
            operating = false;
            interrupt();
         }
      }

      private static class ErrorObject {
    	  String message;
    	  int info;
    	  
    	  ErrorObject (int info, String message) {
    		  this.info = info;
    		  this.message = message;
    	  }
      }
      
      private static class UserObject implements Comparable<UserObject> {
    	  long objectID;
    	  Object object;
    	  SendPriority priority;
    	  
    	  public UserObject (Object object, long id, SendPriority priority) {
    		  objectID = id;
    		  this.object = object;
    		  this.priority = priority;
    	  }

    	  public UserObject (PingEcho echo) {
    		  objectID = echo.pingId();
    		  this.object = echo;
    		  this.priority = SendPriority.Normal;
    	  }

  	   Object getObject () {
		   return object;
	   }

	   long getObjectNr () {
		   return objectID;
	   }

		/** We compare natural for ordering a <code>PriorityQueue</code>,
		 * i.e. the lower element is the priorised element. 
		 */
		@Override
		public int compareTo (UserObject obj) {
	
			// we are higher if the compare is a PingEcho and we are not
			if (obj.object instanceof PingEcho) {
				if (!(object instanceof PingEcho)) {
					return 1;
				}
	
			// we are lower if we are a PingEcho and the compare is not
			} else {
				if (object instanceof PingEcho) {
					return -1;
				}				
			}

			// compare priority class
			if (priority.ordinal() < obj.priority.ordinal()) return -1;
			if (priority.ordinal() > obj.priority.ordinal()) return +1;

			// otherwise compare object numbers
			return objectID < obj.objectID ? -1 : 
				   objectID > obj.objectID ? +1 : 0;
		}

		@Override
		public boolean equals (Object obj) {
			if (obj == null || !(obj instanceof UserObject))
				return false;
			return compareTo((UserObject)obj) == 0;
		}


		@Override
		public int hashCode() {
			int code = (priority.hashCode() << 16) + (int)objectID;  
			return code;
		}
      }

	private static class AliveEchoControlTask extends TimerTask {
	      private ConnectionImpl connection;
	      private long confirmedTime;
	      private int period;
	      
	      /** Creates a new ALIVE ECHO control timer task for periodic checking
	       * of ALIVE-ECHO signals received from remote station. A previously 
	       * created task is cancelled. 
	       * 
	       * <p><small>If <code>period</code> is zero, a previous
	       * task is cancelled but no new one is created.</small>
	       * 
	       * @param con Connection
	       * @param period int ALIVE period in milliseconds
	       * @throws IllegalArgumentException if period is negative
	       */
	      public static void createNew (ConnectionImpl con, int period) {
	     	 if (con.aliveTimeoutTask != null) {
	    		 con.aliveTimeoutTask.cancel();
	    	 }

	    	 if (period > 0) {
	             // create a new timer task
	             con.aliveTimeoutTask = new AliveEchoControlTask(con, period);
	         } else {
	        	 con.aliveTimeoutTask = null;
	         }
	      }
	      
	      /** A new timer task for periodic ALIVE signalling to remote
	       * or controlling ALIVE timeout from remote (depending on execution
	       * modus). Cancels a previously scheduled task of the same type.
	       *  
	       * @param c Connection
	       * @param period int period in milliseconds (must be > 0)
	       * @throws IllegalArgumentException if period is 0 or negative          
	       */
	      private AliveEchoControlTask (ConnectionImpl c, int period) {
	         if (period <= 0) 
	            throw new IllegalArgumentException("period <= 0");
	         
	         connection = c;
	         this.period = period;
	
	         // log output
             if (debug) {
            	 System.out.println("CREATE ALIVE-TIMEOUT TASK for : " + c + ", period=" + period);
            	 System.out.println("             time marker = " + System.currentTimeMillis()/1000 + " sec");
             }
             
	         // schedule the new task
	         timer.schedule(this, period, period);
	      }
	      
	      @SuppressWarnings ("unused")
	      public int getPeriod () {
	         return period;
	      }
	      
	      /** Pushes the current time as actual ALIVE-ECHO confirmed time.
	       */
	      public void pushConfirmedTime () {
	    	  confirmedTime = System.currentTimeMillis();
	      }
	      
	      @Override
	      public void run() {
	    	 // cancel task if connection stopped
	         if (!connection.isConnected()) {
	        	cancel();
	            return;
	         }

	         // close connection if ALIVE-ECHO is missing over period time
	         if (connection.aliveSignalTask != null) {
                 if (debug) {
                	 System.out.println("-- CHECKING ALIVE ECHO (control task)");
                 }
	        	 long sendTime = connection.aliveSignalTask.getSendTime();
	        	 long delta = System.currentTimeMillis() - confirmedTime;
	        	 if (sendTime > 0 && delta > period-200) {
	        		 connection.close(new ConnectionTimeoutException("ALIVE signal timeout"), 5);
	        	 }
	         }
	      }
	   }

      
}
