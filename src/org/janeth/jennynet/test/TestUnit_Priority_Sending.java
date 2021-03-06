package org.janeth.jennynet.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.JennyNetByteBuffer;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_Priority_Sending {

	public TestUnit_Priority_Sending() {
	}

	/** A <code>ConnectionListener</code> to receive 
	 * <code>JennyNetByteBuffer</code> and return them via an output method
	 * as list of byte arrays. There is a lock as parameter which gets 
	 * notified with each reception event.
	 */
	private class FileReceptionListener extends DefaultConnectionListener {

		private List<File> received = new ArrayList<File>();
		private Object lock;
		private SemaphorLock semaphor;
		private int unlockThreshold;

		public FileReceptionListener (Object lock, int unlockSize) {
			this.lock = lock;
			unlockThreshold = unlockSize;
		}
		
		public FileReceptionListener (SemaphorLock semaphor) {
			this.semaphor = semaphor;
		}
		
		@Override
		public void transmissionEventOccurred (TransmissionEvent evt) {

			switch (evt.getType()) {
			case FILE_ABORTED:
				System.out.println("*** FILE ABORTED: No. " + evt.getObjectID()
					+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath());
				break;
			case FILE_FAILED:
				System.out.println("*** FILE FAILED: No. " + evt.getObjectID()
				+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath());
				break;
			case FILE_INCOMING:
				System.out.println("*** FILE INCOMING: No. " + evt.getObjectID() 
						+ " from " + evt.getConnection().getRemoteAddress() 
						+ ", length " + evt.getExpectedLength() + " bytes at "
						+ evt.getFile());
				if (evt.getPath() != null) {
					System.out.println("    Target: " + evt.getPath()); 
				}
				break;
			case FILE_RECEIVED:
				File f = evt.getFile();
				System.out.println("*** FILE RECEIVED: No. " + evt.getObjectID() 
						+ " from " + evt.getConnection().getRemoteAddress() 
						+ ", length " + evt.getTransmissionLength() + " bytes at " + f);
				received.add(f);
				if (evt.getPath() != null) {
					System.out.println("    Target: " + evt.getPath()); 
				}
				
				// notify waiting thread if target reached
				if (lock != null && received.size() == unlockThreshold) {
					synchronized(lock) {
						lock.notify();
					}
				}
				// notify waiting thread if target reached
				if (semaphor != null) {
					semaphor.dec();
				}
				break;
			default:
				break;
			}

		}

		public List<File> getReceived () {
			return received;
		}
		
		public void reset () {
			received.clear();
		}
	}

	/** A <code>ConnectionListener</code> to receive 
		 * <code>JennyNetByteBuffer</code> and return them via an output method
		 * as list of byte arrays. There is a lock as parameter which gets 
		 * notified with each reception event.
		 */
		private class ObjectReceptionListener extends DefaultConnectionListener {
	
			private List<byte[]> received = new ArrayList<byte[]>();
			private Object lock;
			private SemaphorLock semaphor;
			private int unlockThreshold;
	
			public ObjectReceptionListener (Object lock, int unlockSize) {
				this.lock = lock;
				unlockThreshold = unlockSize;
			}
			
			public ObjectReceptionListener (SemaphorLock semaphor) {
				this.semaphor = semaphor;
			}
			
			@Override
			public void objectReceived(Connection con, long objNr, Object obj) {
				if (obj instanceof JennyNetByteBuffer) {
					received.add(((JennyNetByteBuffer)obj).getData());
					
					// notify waiting thread if target reached
					if (lock != null && received.size() == unlockThreshold) {
						synchronized(lock) {
							lock.notify();
						}
					}
					// notify waiting thread if target reached
					if (semaphor != null) {
						semaphor.dec();
					}
				}
			}
			
			public List<byte[]> getReceived () {
				return received;
			}
			
			public void reset () {
				received.clear();
			}
		}

	/** A <code>ConnectionListener</code> to receive 
		 * <code>JennyNetByteBuffer</code> and return them via an output method
		 * as list of byte arrays. There is a lock as parameter which gets 
		 * notified with each reception event.
		 */
		private class MixedObjectReceptionListener extends DefaultConnectionListener {
	
			private List<Object> received = new ArrayList<Object>();
			private Object lock;
			private SemaphorLock semaphor;
			private int unlockThreshold;
	
			public MixedObjectReceptionListener (Object lock, int unlockSize) {
				this.lock = lock;
				unlockThreshold = unlockSize;
			}
			
			public MixedObjectReceptionListener (SemaphorLock semaphor) {
				this.semaphor = semaphor;
			}
			
			@Override
			public void objectReceived(Connection con, long objNr, Object obj) {
				if (obj instanceof JennyNetByteBuffer) {
					received.add(((JennyNetByteBuffer)obj).getData());
					
					// notify waiting thread if target reached
					if (lock != null && received.size() == unlockThreshold) {
						synchronized(lock) {
							lock.notify();
						}
					}
					// notify waiting thread if target reached
					if (semaphor != null) {
						semaphor.dec();
					}
				}
			}
			
			@Override
			public void transmissionEventOccurred (TransmissionEvent evt) {
			
				switch (evt.getType()) {
				case FILE_ABORTED:
					System.out.println("*** FILE ABORTED: No. " + evt.getObjectID()
						+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath());
					break;
				case FILE_FAILED:
					System.out.println("*** FILE FAILED: No. " + evt.getObjectID()
					+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath());
					break;
				case FILE_INCOMING:
					System.out.println("*** FILE INCOMING: No. " + evt.getObjectID() 
							+ " from " + evt.getConnection().getRemoteAddress() 
							+ ", length " + evt.getExpectedLength() + " bytes at "
							+ evt.getFile());
					if (evt.getPath() != null) {
						System.out.println("    Target: " + evt.getPath()); 
					}
					break;
				case FILE_RECEIVED:
					File f = evt.getFile();
					System.out.println("*** FILE RECEIVED: No. " + evt.getObjectID() 
							+ " from " + evt.getConnection().getRemoteAddress() 
							+ ", length " + evt.getTransmissionLength() + " bytes at " + f);
					received.add(f);
					if (evt.getPath() != null) {
						System.out.println("    Target: " + evt.getPath()); 
					}
					
					// notify waiting thread if target reached
					if (lock != null && received.size() == unlockThreshold) {
						synchronized(lock) {
							lock.notify();
						}
					}
					// notify waiting thread if target reached
					if (semaphor != null) {
						semaphor.dec();
					}
					break;
				default:
					break;
				}
			
			}

			public List<Object> getReceived () {
				return received;
			}
			
			public void reset () {
				received.clear();
			}

		}

	@Test
	public void filesend_priority () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 3);

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: MULTI-FILE, PLAIN");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(5000);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 100,000 data file
			System.out.println("\nCASE 1 : multi data file 100,000 - no target");

			// prepare data and source file
			int length = 100000;
			byte[] data1 = Util.randBytes(length);
			byte[] data2 = Util.randBytes(length);
			byte[] data3 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			File src3 = Util.getTempFile(); 
			Util.makeFile(src1, data1);
			Util.makeFile(src2, data2);
			Util.makeFile(src3, data3);
			long stamp = System.currentTimeMillis();
			
			// transmit file
			cl.sendFile(src1, null, SendPriority.Low);
			Util.sleep(3000);
			
			cl.sendFile(src2, null, SendPriority.Normal);
			Util.sleep(4000);
			
			cl.sendFile(src3, null, SendPriority.High);
			
			// wait for completion
			System.out.println("--- waiting ..");
			lock.wait(20000);
			long time = System.currentTimeMillis() - stamp;
			assertTrue("no / not all files received", receptionListener.getReceived().size() == 3);

			// control received file content
			File file = receptionListener.getReceived().get(2);
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in file-1 transmission", Util.equalArrays(rece, data1));

			file = receptionListener.getReceived().get(1);
			rece = Util.readFile(file);
			assertTrue("data integrity error in file-2 transmission", Util.equalArrays(rece, data2));

			file = receptionListener.getReceived().get(0);
			rece = Util.readFile(file);
			assertTrue("data integrity error in file-3 transmission", Util.equalArrays(rece, data3));
			assertTrue("transmission time failure with " + time, time > 19000 & time < 21000);
			
		}		
	} finally {
		System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
		if (sv.getConnections().length > 0) {
			System.out.println("# transmission volume of Server : " + 
				sv.getConnections()[0].getTransmissionVolume());
		}
		
		if (sv != null) {
			sv.closeAllConnections();
			sv.close();
			Util.sleep(10);
		}
		if (cl != null) {
			cl.close();
		}
	}
	}

	@Test
	public void object_send_priority () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 3);
	
	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: MULTI-OBJECT");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(5000);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 100,000 data file
			System.out.println("\nCASE 1 : multi data objects 100,000 - no target");
	
			// prepare data and source file
			int length = 100000;
			byte[] data1 = Util.randBytes(length);
			byte[] data2 = Util.randBytes(length);
			byte[] data3 = Util.randBytes(length);
			long stamp = System.currentTimeMillis();
			
			// transmit file
			cl.sendData(data1, 0, length, SendPriority.Low);
			Util.sleep(3000);
			
			cl.sendData(data2, 0, length, SendPriority.Normal);
			Util.sleep(3000);
			
			cl.sendData(data3, 0, length, SendPriority.High);
			
			// wait for completion
			System.out.println("--- waiting ..");
			lock.wait(20000);
			long time = System.currentTimeMillis() - stamp;
			assertTrue("no / not objects received", receptionListener.getReceived().size() == 3);
	
			// control received file content
			byte [] rece = receptionListener.getReceived().get(2);
			assertTrue("data integrity error in file-1 transmission", Util.equalArrays(rece, data1));
	
			rece = receptionListener.getReceived().get(1);
			assertTrue("data integrity error in file-2 transmission", Util.equalArrays(rece, data2));
	
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error in file-3 transmission", Util.equalArrays(rece, data3));
			assertTrue("transmission time failure with " + time, time > 19000 & time < 21000);
			
		}		
	} finally {
		System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
		if (sv.getConnections().length > 0) {
			System.out.println("# transmission volume of Server : " + 
				sv.getConnections()[0].getTransmissionVolume());
		}
		
		if (sv != null) {
			sv.closeAllConnections();
			sv.close();
			Util.sleep(10);
		}
		if (cl != null) {
			cl.close();
		}
	}
	}

	@Test
	public void file_object_mixed_priority () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final MixedObjectReceptionListener objectReceptionListener = new MixedObjectReceptionListener(lock, 4);
	
	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: MULTI-FILE, MULTI-OBJECT");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), objectReceptionListener);
		sv.getParameters().setAlivePeriod(5000);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 100,000 data file
			System.out.println("\nCASE 1 : multi data file 100,000 - no target");
	
			// prepare data and source file
			int length = 100000;
			byte[] data1 = Util.randBytes(length);
			byte[] data2 = Util.randBytes(length);
			byte[] data3 = Util.randBytes(length);
			byte[] data4 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			Util.makeFile(src1, data1);
			Util.makeFile(src2, data2);
			long stamp = System.currentTimeMillis();
			
			// transmit file
			cl.sendFile(src1, null, SendPriority.Higher);
			Util.sleep(3000);
			
			cl.sendFile(src2, null, SendPriority.Top);
			Util.sleep(3000);
			
			cl.sendData(data3, 0, length, SendPriority.Low);
			Util.sleep(3000);
			
			cl.sendData(data4, 0, length, SendPriority.Normal);
			Util.sleep(3000);

			// wait for completion
			System.out.println("--- waiting ..");
			lock.wait(20000);
			long time = System.currentTimeMillis() - stamp;
			assertTrue("no / not all files received", objectReceptionListener.getReceived().size() == 4);
	
			// control received object content
			Object object = objectReceptionListener.getReceived().get(0);
			assertTrue("expected byte[] object received", object instanceof byte[]);
			byte [] rece = (byte[])object;
			assertTrue("data integrity error in object-2 transmission", Util.equalArrays(rece, data4));
	
			object = objectReceptionListener.getReceived().get(1);
			assertTrue("expected byte[] object received", object instanceof byte[]);
			rece = (byte[])object;
			assertTrue("data integrity error in object-1 transmission", Util.equalArrays(rece, data3));
	
			// control received file content
			object = objectReceptionListener.getReceived().get(2);
			assertTrue("expected File object received", object instanceof File);
			rece = Util.readFile((File)object);
			assertTrue("data integrity error in file-2 transmission", Util.equalArrays(rece, data2));
	
			object = objectReceptionListener.getReceived().get(3);
			assertTrue("expected File object received", object instanceof File);
			rece = Util.readFile((File)object);
			assertTrue("data integrity error in file-1 transmission", Util.equalArrays(rece, data1));

			assertTrue("transmission time failure with " + time, time > 25000 & time < 28000);
			
		}		
	} finally {
		System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
		if (sv.getConnections().length > 0) {
			System.out.println("# transmission volume of Server : " + 
				sv.getConnections()[0].getTransmissionVolume());
		}
		
		if (sv != null) {
			sv.closeAllConnections();
			sv.close();
			Util.sleep(10);
		}
		if (cl != null) {
			cl.close();
		}
	}
	}
	
}
