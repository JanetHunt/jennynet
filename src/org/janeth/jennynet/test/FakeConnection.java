package org.janeth.jennynet.test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.UUID;

import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Signal;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.ConnectionParameters;
import org.janeth.jennynet.intfa.Serialization;
import org.janeth.jennynet.util.Util;

public class FakeConnection implements Connection {

   public enum Action {OBJECT, FILE, TEMPO, PING}
   
   private UUID uuid = UUID.randomUUID();
   private byte[] shortId = Util.makeShortId(uuid);
   private String connectionName;
   private ConnectionParameters parameters = JennyNet.getParameters();
   private Properties properties; 
   private Serialization sendSerialisation = JennyNet.getGlobalSerialisation().copy();
   private Serialization receiveSerialisation = JennyNet.getGlobalSerialisation().copy();
   private InetSocketAddress remoteAddress;
   private InetSocketAddress localAddress;
   private boolean closed;
   private boolean makeError;
   
   private int[] counter = new int[Action.values().length];
   private Object lastSendObject;
   private int nextObjectNumber;
   private int nextPingNumber;
   
   public class PingObject {
	   long pingNr;
	   public PingObject (long id) {
		   pingNr = id;
	   }
   }
   
   public FakeConnection () {
   }
   
   public FakeConnection (InetSocketAddress local, InetSocketAddress remote) {
      remoteAddress = remote;
      localAddress = local;
   }
   
   /** Returns the occurrence counter value for the 
    * specified send action. (Test function)
    * 
    * @param a Action
    * @return int counter value
    */
   int getCounterValue(Action a) {
      return counter[a.ordinal()];
   }
   
   Object getLastSendObject() {
      return lastSendObject;
   }

   /** Sets whether this connection will create an error condition
    * on all send activities.
    * 
    * @param v boolean true == throw error
    */
   public void setErrorMaker (boolean v) {
      makeError = v;
   }
   
   /** Resets the test counter of send actions to zero values. */
   public void resetActionCounter () {
      counter = new int[Action.values().length];
   }
   
   @Override
   public ConnectionParameters getParameters () {
      return parameters;
   }

   @Override
   public void setParameters (ConnectionParameters parameters) 
         throws IOException {
      if (parameters != null) {
         this.parameters = (ConnectionParameters)parameters.clone();
      }
   }

   @Override
   public Serialization getSendSerialization () {
      return sendSerialisation;
   }

   @Override
   public Serialization getReceiveSerialization () {
      return receiveSerialisation;
   }

   @Override
   public UUID getUUID () {
      return uuid;
   }

   
   @Override
   public void setUUID(UUID uuid) {
	   this.uuid = uuid;
   }

@Override
   public byte[] getShortId () {
      return shortId;
   }

   @Override
   public boolean isConnected () {
      return !closed;
   }

   @Override
   public boolean isClosed () {
      return closed;
   }

   @Override
   public boolean isTransmitting () {
      return false;
   }

   @Override
   public long getLastSendTime () {
      return 0;
   }

   @Override
   public long getLastReceiveTime () {
      return 0;
   }

   @Override
   public long sendObject (Object object) {
      return sendObject(object, SendPriority.Normal);
   }

   @Override
   public long sendObject (Object object, SendPriority priority) {
      if (makeError) {
         throw new IllegalStateException("TEST EXEPTION");
      }
      lastSendObject = object;
      counter[Action.OBJECT.ordinal()]++;
      return ++nextObjectNumber;
   }

   @Override
   public long sendFile (File file, String remotePath, SendPriority priority) 
		   throws IOException {
      if (makeError) {
         throw new IOException("TEST EXEPTION");
      }
      lastSendObject = file;
      counter[Action.FILE.ordinal()]++;
      return ++nextObjectNumber;
   }

   @Override
   public long sendFile (File file, String remotePath) 
		   throws IOException {
	   return sendFile(file, remotePath, SendPriority.Normal);
   }
   
   @Override
   public long sendData (byte[] buffer, int start, int length, SendPriority priority) {
      return sendObject(null);
   }

   @Override
   public long sendPing () {
//      sendObject(Signal.newPingSignal(++nextPingNumber));
      sendObject(new PingObject(++nextPingNumber));
      counter[Action.PING.ordinal()]++;
      return nextPingNumber;
   }

   @Override
   public void setTempo (int baud) {
      sendObject(new Integer(baud));
      counter[Action.TEMPO.ordinal()]++;
   }

   @Override
   public void breakTransfer (long objectID, int direction) {
   }

   @Override
   public void close () {
      closed = true;
   }

   @Override
   public void addListener (ConnectionListener listener) {
   }

   @Override
   public void removeListener (ConnectionListener listener) {
   }

   @Override
   public InetSocketAddress getRemoteAddress () {
      return remoteAddress;
   }

   @Override
   public InetSocketAddress getLocalAddress () {
      return localAddress;
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
   public boolean isIdle () {
      return false;
   }

   @Override
   public Properties getProperties () {
      if (properties == null) {
         properties = new Properties();
      }
      return properties;
   }

   @Override
   public void setSendSerialization (Serialization s) {
   }

   @Override
   public void setReceiveSerialization (Serialization s) {
   }

	@Override
	public long getTransmissionVolume() {
		return 0;
	}
	
	@Override
	public int getTransmissionSpeed() {
		return -1;
	}

	@Override
	public long getCurrentSendLoad() {
		return 0;
	}

}
