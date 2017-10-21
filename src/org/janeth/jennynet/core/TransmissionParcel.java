package org.janeth.jennynet.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.exception.BadTransmissionParcelException;
import org.janeth.jennynet.exception.StreamOutOfSyncException;
import org.janeth.jennynet.util.CRC32;
import org.janeth.jennynet.util.SchedulableTimerTask;
import org.janeth.jennynet.util.Util;

/** The elemental transmission unit of the JennyNet network layer.
 */
class TransmissionParcel extends JennyNetByteBuffer 
			implements Comparable<TransmissionParcel> {
   
   public static final int PARCEL_MARK = (int)JennyNet.PARCEL_MARKER;
   
   /** Returns an array of transmission parcels converted from the serialisation
    * data of a transmission object. The parcel-numbers start from 1.
    *  
    * @param serObj byte[] serialisation data of an object (complete)
    * @param objectNr int object ID
    * @param transmissionParcelSize int data segment size transmittable in 
    *        a single parcel
    * @return TransmissionParcel array 
    */
   public static TransmissionParcel[] createParcelArray (
         TransmissionChannel channel, 
         byte[] serObj, 
         long objectNr, 
         SendPriority priority,
         int transmissionParcelSize ) {

      // calculate number of parcels
      int dataLen = serObj.length;
      int parcelSize = transmissionParcelSize;
      int nrOfParcels = dataLen / parcelSize;
      int lastBit = dataLen % parcelSize; 
      if (lastBit > 0) nrOfParcels++;
      
      // create a list of parcels
      List<TransmissionParcel> list = new ArrayList<TransmissionParcel>();
      for (int i = 0; i < nrOfParcels; i++) {
         int segmentSize = i < nrOfParcels-1 ? transmissionParcelSize : lastBit;
         TransmissionParcel p = new TransmissionParcel(objectNr, i, 
               serObj, i*transmissionParcelSize, segmentSize);
         p.setPriority(priority);
         list.add(p);
      }

      // set object header values on first transmission parcel
      ObjectHeader header = list.get(0).getObjectHeader();
      header.setTransmissionSize( serObj.length );
      header.setNrOfParcels( nrOfParcels );
      
      // return collected parcels as array
      return list.toArray(new TransmissionParcel[list.size()]);
   }

   // parcel header data
   private ObjectHeader header;
   private TransmissionChannel channel;
   private SendPriority priority = SendPriority.Normal;
   private SchedulableTimerTask timerTask;
   private long objectID;
   private int sequencelNr;
   private int crc32;
   
   
   /** Creates a new transmission parcel for the OBJECT channel with the 
    * given data buffer and header information. (For other channels use
    * the <code>setChannel()</code> method.)  
    * This fully defines the parcel.
    * 
    * @param objectNr int the transmission object number
    * @param parcelNr int the parcel serial number
    * @param buffer byte[] object data segment
    * @param start int data start offset in buffer
    * @int length int data length in buffer
    */
   public TransmissionParcel (long objectNr, 
		   					  int parcelNr, 
		   					  byte[] buffer, 
		   					  int start, 
		   					  int length) {
      super(buffer, start, length);

      objectID = objectNr;
      sequencelNr = parcelNr;
      channel = TransmissionChannel.OBJECT;
      
      // parcel 0 has extended header information
      if (parcelNr == 0) {
         header = new ObjectHeader(objectNr);
      }
   }

   
   /** Creates a new transmission parcel for the OBJECT channel 
    * with the given data buffer and header information. 
    * This fully defines the parcel.
    * 
    * @param objectNr int the transmission object number
    * @param parcelNr int the parcel serial number
    * @param buffer byte[] object data segment
    */
   public TransmissionParcel (long objectNr, int parcelNr, byte[] buffer) {
      this(objectNr, parcelNr, buffer, 0, buffer.length);
   }
   
   /** Creates a transmission parcel for a transmission signal.
    * 
    * @param signal SignalType type of signal
    * @param object long referenced transmission object or 0
    * @param info signal operational info
    * @param text signal associated text (human readable additional info
    *         e.g. a cause for issuing the signal)
    */
   public TransmissionParcel (SignalType signal, long object, int info, String text) {
      channel = TransmissionChannel.SIGNAL;
      objectID = object;
//      sequencelNr = (info << 16) | signal.ordinal();
      sequencelNr = signal.ordinal();
  	  byte[] textData = text == null ? null : text.getBytes(JennyNet.getCodingCharset());
  	  if (textData != null || info != 0) {
  	  	  int dataLen = (textData == null ? 0 : textData.length) + 4;
  		  byte[] data = new byte[dataLen];
  		  Util.writeInt(data, 0, info);
  		  if (textData != null) {
  			  System.arraycopy(textData, 0, data, 4, textData.length);
  		  }
          setData(data);
  	  }
   }
   
   /** Creates an empty and invalid transmission parcel.
    */
   protected TransmissionParcel () {
   }

   /** Creates a parcel from an existing other parcel (identical settings).
    */
   protected TransmissionParcel (TransmissionParcel p) {
      channel = p.channel;
      priority = p.priority;
      objectID = p.objectID;
      sequencelNr = p.sequencelNr;
      header = p.header;
      setData(p.getData());
      crc32 = p.crc32;
   }

   /** Writes the transmit parcel data to the given output stream.
    * 
    * @param output OutputStream data sink
    * @throws IOException 
    */
   public void writeObject (OutputStream output) throws IOException {
      DataOutputStream out = new DataOutputStream(output);

      // ensure CRC is calculated
      getCRC();
      
      // prevent sending invalid parcels
      if ( !verify() ) {
         throw new IOException("invalid send parcel: object=" + objectID + ", parcel=" + sequencelNr);
      }
      
      // write basic parcel information
      out.writeInt( PARCEL_MARK );
      out.write( channel.ordinal() );
      out.writeByte( priority.ordinal() );
      out.writeLong( objectID );
      out.writeInt( sequencelNr );
      out.writeInt( getLength() );
      out.writeInt( crc32 );

      // for parcel number 0 we write extended header information
      if (sequencelNr == 0 & (channel == TransmissionChannel.OBJECT |
            channel == TransmissionChannel.FILE) ) {
         header.writeObject(out);
      }

      // write serial buffer if supplied
      if (getLength() > 0) {
         out.write(getData());
      }
   }

   public void readObject( InputStream socketInput ) throws IOException {
      DataInputStream in = new DataInputStream(socketInput);

      int mark = in.readInt();
      if (mark != PARCEL_MARK) {
         throw new StreamOutOfSyncException("bad parcel mark");
      }
      
      // read basic parcel information
      channel = TransmissionChannel.valueOf(in.read());
      priority = SendPriority.valueOf(in.readByte());
      objectID = in.readLong();
      sequencelNr = in.readInt();
      int dataLength = in.readInt();
      int crc = in.readInt();
      
      // for parcel number 0 we read extended header information
      if (sequencelNr == 0 & (channel == TransmissionChannel.OBJECT |
            channel == TransmissionChannel.FILE) ) {
         header = new ObjectHeader(objectID);
         header.readObject(in);
      }

      // read the serial buffer if it is supplied
      if (dataLength > 0) {
         byte[] buffer = new byte[dataLength];
         in.readFully(buffer);
         setData(buffer);
      }
      
      // check CRC value of the parcel
      if (crc != getCRC()) {
         throw new BadTransmissionParcelException("bad CRC value");
      }
   }
   
   @Override
   public void setData(byte[] block) {
      super.setData(block);
      crc32 = 0;
   }

   /** Returns the object header data record if available.
    * On each parcel number 0 the transmittable object's header
    * data is available, null otherwise.
    * 
    * @return <code>ObjectHeader</code> or null
    */
   public ObjectHeader getObjectHeader () {
      return header;
   }

   /** Returns a timer task that has been added to this parcel
    * and will be scheduled at the time of its sending.
    *  
    * @return TimerTask or null if undefined
    */
   public SchedulableTimerTask getTimerTask() {
      return timerTask;
   }


   /** Attributes a timer task to this parcel
    * that will be scheduled at the time of parcel sending.
    * 
    * @param timerTask TimerTask (may be null)
    */
   public void setTimerTask (SchedulableTimerTask timerTask) {
      this.timerTask = timerTask;
   }

   /** Returns whether this parcel is set up correctly 
    * an is ready for transmission or other reception.
    * 
    * @return boolean parcel validity 
    */
   public boolean verify () {
      boolean ok = channel != null;
      if (ok) {
         ok &= sequencelNr > -1 & objectID > -1;
         if (channel != TransmissionChannel.SIGNAL) {
            ok &= objectID > 0;
            if (sequencelNr == 0) {
               ok &= header != null && header.verify();
            }
         }
      }
      return ok;
   }
   
   /** Returns a CRC64 value for all information in this parcel.
    * This comprises buffer and header data.
    * 
    * @return long CRC64 value
    */
   @Override
   public int getCRC () {
      if (crc32 == 0) {
         CRC32 crc = new CRC32();
         if (getLength() > 0) {
            crc.update(getData());
         }
         crc.update(objectID);
         crc.update(sequencelNr);
         crc.update((byte)channel.ordinal());
         crc32 = (int)crc.getValue();
      }
      return crc32;
   }
   
   public void report ( int io, PrintStream out ) {
      out.println("++ " + (io==0 ? "REC":"SND") + "-PARCEL: obj=" + objectID + ", ser=" + sequencelNr + ", channel=" + channel);
      if (header != null) {
         out.println("              HEAD: parcels=" + header.getNumberOfParcels() + ", size=" 
               + header.getTransmissionSize() + ", mt=" + header.getSerialisationMethod());
         if (header.getPath() != null ) {
            out.println("              path=" + header.getPath());
         }
      }
   }


   public static TransmissionParcel readParcel(InputStream in) throws IOException {
      TransmissionParcel p = new TransmissionParcel();
      p.readObject(in);
      return p;
   }


   public long getObjectID() {
      return objectID;
   }


   public int getParcelSequencelNr() {
      return sequencelNr;
   }

   public int getSerialisedLength () {
      return getLength() + 25 +
            (header != null ? header.getSerialisedLength() : 0);
   }
   
   
   public TransmissionChannel getChannel() {
      return channel;
   }
   
   public void setChannel (TransmissionChannel channel) {
      this.channel = channel;
   }


	public SendPriority getPriority() {
	return priority;
	}


	public void setPriority(SendPriority priority) {
		this.priority = priority;
	}

	@Override
	public int compareTo (TransmissionParcel obj) {
		if (obj == null)
			throw new NullPointerException();
		
		if (channel.ordinal() < obj.channel.ordinal()) return -1;
		if (channel.ordinal() > obj.channel.ordinal()) return +1;
		if (priority.ordinal() > obj.priority.ordinal()) return -1;
		if (priority.ordinal() < obj.priority.ordinal()) return +1;
		if (objectID < obj.objectID) return -1;
		if (objectID > obj.objectID) return +1;
		if (sequencelNr < obj.sequencelNr) return -1;
		if (sequencelNr > obj.sequencelNr) return +1;
		return 0;
	}


	@Override
	public boolean equals (Object obj) {
		if (obj == null || !(obj instanceof TransmissionParcel))
			return false;
		return compareTo((TransmissionParcel)obj) == 0;
	}


	@Override
	public int hashCode() {
		int code = ((channel.hashCode() + priority.hashCode()) << 16) + 
					((int)objectID << 8) + sequencelNr;  
		return code;
	}


	public boolean isSignal() {
		return channel == TransmissionChannel.SIGNAL;
	}
   
}
