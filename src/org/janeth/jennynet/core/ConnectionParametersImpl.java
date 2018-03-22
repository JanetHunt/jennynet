package org.janeth.jennynet.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.janeth.jennynet.intfa.ConnectionParameters;

/**
 * This class serves as a means to set, retrieve and administer a set of parameters
 * for a <code>Connection</code>. By creating a new instance, parameter values equal 
 * their counterparts of the global layer level (specified via <code>JennyNet</code>
 * class). No action of the user is required; a connection always owns a valid set
 * of parameters. 
 *   
 */
class ConnectionParametersImpl implements Cloneable, ConnectionParameters {
   /** The minimum data transport capacity of a transmission parcel. */
   protected static final int PARCEL_SIZE_MIN = 128;

   private int baseThreadPriority = JennyNet.getBaseThreadPriority();
   private int transmitThreadPriority = JennyNet.getTransmitThreadPriority();
   private int transmissionParcelSize = JennyNet.getTransmissionParcelSize();
   private int parcelQueueCapacity = JennyNet.getParcelQueueCapacity();
   private int objectQueueCapacity = JennyNet.getObjectQueueCapacity();
   private int alivePeriod = JennyNet.getAlivePeriod();
   private int confirmTimeout = JennyNet.getConfirmTimeout();
   private int serialMethod = JennyNet.getDefaultSerialisationMethod();
   private File fileRootDir = JennyNet.getDefaultTransmissionRoot();
   private File fileTempDir = JennyNet.getDefaultTempDirectory();
   private Charset codingCharset = JennyNet.getCodingCharset();
   private int idleThreshold = JennyNet.getIdleThreshold();
   private int idleCheckPeriod = JennyNet.getDefaultIdleCheckPeriod();
   private int transmissionTempo = JennyNet.getDefaultTransmissionTempo();
   private int maxSerialiseSize = JennyNet.getDefaultMaxSerialiseSize();

   public ConnectionParametersImpl() {
   }
   
   @Override
   public Object clone () {
      try {
         return super.clone();
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
   
   @Override
   public int getBaseThreadPriority() {
      return baseThreadPriority;
   }

   @Override
   public void setBaseThreadPriority(int p) {
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY) {
         baseThreadPriority = p;
      } else {
         throw new IllegalArgumentException("illegal priority value: ".
               concat(String.valueOf(p)));
      }
   }

   @Override
   public int getTransmitThreadPriority() {
      return transmitThreadPriority;
   }

   @Override
   public void setTransmitThreadPriority(int p) {
      if (p >= Thread.MIN_PRIORITY & p <= Thread.MAX_PRIORITY) {
           transmitThreadPriority = p;
      } else {
           throw new IllegalArgumentException("illegal priority value: ".
                 concat(String.valueOf(p)));
      }
   }

   @Override
   public int getParcelQueueCapacity() {
      return parcelQueueCapacity;
   }

   @Override
   public void setParcelQueueCapacity(int parcelQueueCapacity) {
      if (parcelQueueCapacity < 10 | parcelQueueCapacity > 10000) 
         throw new IllegalArgumentException("queue capacity out of range (10..10000)");
      this.parcelQueueCapacity = parcelQueueCapacity;
   }

   @Override
   public int getObjectQueueCapacity() {
      return objectQueueCapacity;
   }

   @Override
   public void setObjectQueueCapacity(int objectQueueCapacity) {
      if (objectQueueCapacity < 1)
         throw new IllegalArgumentException("illegal capacity; minimum = 1");
      this.objectQueueCapacity = objectQueueCapacity;
   }

   @Override
   public int getAlivePeriod () {
      return alivePeriod;
   }
   
   @Override
   public void setAlivePeriod (int period) {
      if (period < 10000 & period != 0) { 
         period = 10000;
      } else if (period > 300000) {
    	  period = 300000;
      }
      alivePeriod = period;
   }
   
   @Override
   public int getConfirmTimeout() {
      return confirmTimeout;
   }

   @Override
   public void setConfirmTimeout(int timeout) {
      if (timeout < 1000) { 
         timeout = 1000;
      } 
      confirmTimeout = timeout;
   }

   @Override
   public int getSerialisationMethod() {
      return serialMethod;
   }

//   @Override
//   public void setSerialisationMethod(int method) {
//      // we only use method 0 = kryo this time
////      this.serialMethod = serialMethod;
//   }

   @Override
   public File getFileRootDir() {
      return fileRootDir;
   }

   @Override
   public void setFileRootDir (File dir) throws IOException {
      if (dir == null) {
         fileRootDir = null;
      } else {
         if (!dir.isDirectory()) {
            throw new IllegalArgumentException("parameter is not a directory");
         }
         fileRootDir = dir.getCanonicalFile();
      }
   }

   @Override
   public File getTempDirectory () {
      return fileTempDir;
   }

   @Override
   public void setTempDirectory (File dir) throws IOException {
      if (dir == null) {
         dir = JennyNet.getDefaultTempDirectory();
      }
      if (!dir.isDirectory())
         throw new IllegalArgumentException("parameter is not a directory");

      fileTempDir = dir.getCanonicalFile();
   }
   
   @Override
   public int getTransmissionParcelSize () {
      return transmissionParcelSize;
   }

   @Override
   public void setTransmissionParcelSize (int size) {
      transmissionParcelSize = Math.min( Math.max(size, 
            JennyNet.MIN_TRANSMISSION_PARCEL_SIZE), JennyNet.MAX_TRANSMISSION_PARCEL_SIZE );
   }
   
   @Override
   public Charset getCodingCharset () {
      return codingCharset;
   }

   @Override
   public void setCodingCharset (Charset charset) {
      if (charset == null)
         throw new NullPointerException();
      this.codingCharset = charset;
   }

   @Override
   public void setIdleThreshold (int idleThreshold) {
      this.idleThreshold = Math.max(idleThreshold, 0);
   }

   @Override
   public int getIdleThreshold () {
      return idleThreshold;
   }

   @Override
   public int getIdleCheckPeriod () {
      return idleCheckPeriod;
   }

   @Override
   public void setIdleCheckPeriod (int period) {
      idleCheckPeriod = Math.max(period, 1000);
   }

	@Override
	public int getTransmissionSpeed() {
		return transmissionTempo;
	}
	
	@Override
	public void setTransmissionSpeed(int tempo) {
		transmissionTempo = Math.max(-1, tempo);
	}

	@Override
	public int getMaxSerialisationSize() {
		return maxSerialiseSize;
	}

	@Override
	public void setMaxSerialisationSize(int size) {
		maxSerialiseSize = Math.max(size, JennyNet.MIN_SERIALISE_SIZE);
	}

}
