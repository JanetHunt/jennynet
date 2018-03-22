package org.janeth.jennynet.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.intfa.TransmissionEvent.TransmissionEventType;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_File_Transfer {

	public TestUnit_File_Transfer() {
	}

	/** A <code>ConnectionListener</code> to receive 
	 * <code>JennyNetByteBuffer</code> and return them via an output method
	 * as list of byte arrays. There is a lock as parameter which gets 
	 * notified with each reception event.
	 */
	private static class FileReceptionListener extends DefaultConnectionListener {

		public enum Station {Client, Server}
		private Station station = Station.Server;
		
		private List<File> received = new ArrayList<File>();
		private List<TransmissionEvent> events = new ArrayList<TransmissionEvent>();
		private Object lock;
		private SemaphorLock semaphor;
		private TransmissionEventType signalType;
		private File errorFile;
		private int unlockThreshold;
		private int errorInfo;
		private boolean releaseOnFailure = true;

		/** A listener for the server station equipped with a lock. 
		 */
		public FileReceptionListener (Object lock, int unlockSize) {
			this.lock = lock;
			unlockThreshold = unlockSize;
		}
		
		/** A listener for any station equipped with a semaphore lock. 
		 */
		public FileReceptionListener (SemaphorLock semaphor, Station station) {
			this.semaphor = semaphor;
			this.station = station;
			unlockThreshold = semaphor.getCounter();
		}
		
		/** A listener for the client station. 
		 */
		public FileReceptionListener () {
			this.station = FileReceptionListener.Station.Client;
		}
		
		@Override
		public void objectReceived(Connection con, long objNr, Object obj) {
			System.out.println("*** OBJECT RECEIVED: ID " + objNr + ", type = " + obj.getClass());
		}
		
		@Override
		public void transmissionEventOccurred (TransmissionEvent evt) {

			PrintStream out = station == Station.Client ? System.err : System.out;
			signalType = evt.getType();
			events.add(evt);
			
			switch (signalType) {
			case FILE_ABORTED:
				out.println("*** FILE ABORTED: No. " + evt.getObjectID()
					+ " from " + evt.getConnection().getRemoteAddress() 
					+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath()
					+ ", file = " + evt.getFile());
				errorInfo = evt.getInfo();
				errorFile = evt.getFile();
				
				if (releaseOnFailure) {
					release_locks();
				}

				// decrease SEMAPHOR if defined
				if (semaphor != null) {
					semaphor.dec();
				}
				break;
				
			case FILE_FAILED:
				out.println("*** FILE FAILED: No. " + evt.getObjectID()
				+ " from " + evt.getConnection().getRemoteAddress() 
				+ ", info = " + evt.getInfo() + ", dest-path = " + evt.getPath()
				+ ", file = " + evt.getFile());
				errorInfo = evt.getInfo();
				errorFile = evt.getFile();

				if (releaseOnFailure) {
					release_locks();
				}

				// decrease SEMAPHOR if defined
				if (semaphor != null) {
					semaphor.dec();
				}
				break;
				
			case FILE_INCOMING:
				out.println("*** FILE INCOMING: No. " + evt.getObjectID() 
						+ " from " + evt.getConnection().getRemoteAddress() 
						+ ", length " + evt.getExpectedLength() + " bytes at "
						+ evt.getFile());
				if (evt.getPath() != null) {
					out.println("    Target: " + evt.getPath()); 
				}
				break;
				
			case FILE_RECEIVED:
				File f = evt.getFile();
				out.println("*** FILE RECEIVED: No. " + evt.getObjectID() 
						+ " from " + evt.getConnection().getRemoteAddress() 
						+ ", length " + evt.getTransmissionLength() + " bytes --> " + f);
				received.add(f);
				if (evt.getPath() != null) {
					out.println("    target was: " + evt.getPath()); 
				}
				
				// notify waiting thread on LOCK if target reached
				check_lock();
				
				// decrease SEMAPHOR if defined
				if (semaphor != null) {
					semaphor.dec();
				}
				break;
				
			case FILE_CONFIRMED:
				out.println("*** FILE CONFIRMED: No. " + evt.getObjectID() 
						+ " from " + evt.getConnection().getRemoteAddress() 
						+ ", length " + evt.getTransmissionLength() + " bytes --> "
						+ evt.getFile());
				if (evt.getPath() != null) {
					out.println("    Target: " + evt.getPath()); 
				}

				// decrease SEMAPHOR if defined
				if (semaphor != null) {
					semaphor.dec();
				}
				break;
				
			default:
				break;
			}

		}

		/** check the LOCK release condition and notify a waiting thread
		 * if the unlockThreshold (int) is reached. 
		 */
		private void check_lock () {
			if (lock != null && received.size() == unlockThreshold) {
				synchronized(lock) {
					lock.notify();
				}
			}
		}

		private void release_locks () {
			if (lock != null) {
				synchronized(lock) {
					lock.notify();
				}
			}
			if (semaphor != null) {
				semaphor.reset();
			}
		}
		
		public void set_release_locks_on_failure (boolean release) {
			releaseOnFailure = release;
		}
		
		public List<File> getReceived () {
			return received;
		}
		
		public List<TransmissionEvent> getEvents () {
			return events;
		}
		
		public int countEvents (TransmissionEventType type) {
			int count = 0;
			for (TransmissionEvent evt : getEvents()) {
				if (evt.getType() == type) {
					count++;
				}
			}
			return count; 
		}

		public boolean hasTransmissionEvent (TransmissionEventType type, int info) {
			for (TransmissionEvent evt : getEvents()) {
				if (evt.getType() == type && evt.getInfo() == info) {
					return true;
				}
			}
			return false; 
		}
		
		public TransmissionEvent getFirstEventOf (TransmissionEventType type) {
			for (TransmissionEvent evt : getEvents()) {
				if (evt.getType() == type) {
					return evt;
				}
			}
			return null;
		}
		
		public int getErrorInfo () {
			return errorInfo;
		}
		
		public File getErrorFile () {
			return errorFile;
		}
		
		public TransmissionEventType getSignalType() {
			return signalType;
		}
		
		public void reset () {
			received.clear();
			events.clear();
			errorInfo = 0;
			errorFile = null;
			signalType = null;
			
			if (semaphor != null) {
				semaphor.setCounter(unlockThreshold);
			}
		}
	}
		
	
	@Test
	public void client_single_no_target () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final SemaphorLock sendLock = new SemaphorLock(1);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);
		final FileReceptionListener sendListener = new FileReceptionListener(sendLock, FileReceptionListener.Station.Client);

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: SINGLE, NO-TARGET");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(sendListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 50,000 data file, speed limit 15.000
			System.out.println("\nCASE 1 : single file 50,000 - no target, speed limit 15.000");

			// prepare data and source file
			int length = 50000;
			byte[] data = Util.randBytes(length);
			File src = Util.getTempFile(); 
			Util.makeFile(src, data);
			
			// transmit file (speed limit)
			cl.sendFile(src, null);
			long stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait();
			long time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control received file content
			File file = receptionListener.getReceived().get(0);
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission (1)", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 3000 & time < 5000);
			
			// CASE 2 : transmit file (no speed limit)
			System.out.println("\nCASE 2 : single file 50,000 - no target, no speed limit");
			receptionListener.reset();
			sendListener.reset();
			cl.setTempo(-1);
			cl.sendFile(src, null);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait();
			time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control received file content
			file = receptionListener.getReceived().get(0);
			rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission (2)", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 0 & time < 200);
			
			// CASE 3 : empty data file
			System.out.println("\nCASE 3 : single empty file - no target");
			receptionListener.reset();
			sendListener.reset();
			src = Util.getTempFile();
			cl.sendFile(src, null);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(10000);
			time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control received file
			assertFalse("no file received", receptionListener.getReceived().isEmpty());
			file = receptionListener.getReceived().get(0);
			assertTrue("transmitted file should be empty but has length " + file.length(), 
					file.length() == 0);
			assertTrue("transmission time failure", time > 0 & time < 1000);
			
			// wait (test)
//			Util.sleep(12000);
		}		
	} finally {
		System.out.println();
		if (sv != null) {
			if (sv.getConnections().length > 0) {
				System.out.println("# transmission volume of Server : " + 
					sv.getConnections()[0].getTransmissionVolume());
			}
			
			if (sv != null) {
				sv.closeAllConnections();
				sv.close();
				Util.sleep(10);
			}
		}
		if (cl != null) {
			System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
			cl.close();
		}
	}
	}


	@Test
	public void client_single_target () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final SemaphorLock sendLock = new SemaphorLock(1);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);
		final FileReceptionListener sendListener = new FileReceptionListener(sendLock, FileReceptionListener.Station.Client);

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: SINGLE, TARGET DIR EXISTING");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.start();

		// set file transfer target directory
		File base = new File(System.getProperty("java.io.tmpdir"));
		File tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
		tardir.mkdirs();
		sv.getParameters().setFileRootDir(tardir);
		assertTrue("transmission target not created", tardir.isDirectory());
		System.out.println("-- (test) transmission target created: [" + tardir + "]");
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(sendListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- (test) connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 50,000 data file
			System.out.println("\nCASE 1 : single file 50,000 - target filename, new file");

			// prepare data and source file
			int length = 50000;
			byte[] data = Util.randBytes(length);
			File src = Util.getTempFile(); 
			Util.makeFile(src, data);
			
			// transmit file
			String filename = "eselsbruecke.data";
			cl.sendFile(src, filename);
			long stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait();
			long time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control sender confirm
			assertTrue("CONFIRM signal missing on sender", sendListener.getSignalType() 
					   == TransmissionEventType.FILE_CONFIRMED);
			
			// control received file content
			File file = receptionListener.getReceived().get(0);
			assertTrue("file target not met, filename: " + file.getName(), filename.equals(file.getName()));
			assertTrue("file target not met, directory: [" + file.getParent() + "]", tardir.equals(file.getParentFile()));
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 3000 & time < 5000);
			
			// CASE 2 : empty data file, overwrite
			System.out.println("\nCASE 2 : single empty file - target filename, overwrite");
			receptionListener.reset();
			sendListener.reset();
			src = Util.getTempFile();
			cl.sendFile(src, filename);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(10000);
			time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control sender confirm
			assertFalse("no file received", receptionListener.getReceived().isEmpty());
			assertTrue("CONFIRM signal missing on sender", sendListener.getSignalType() 
					   == TransmissionEventType.FILE_CONFIRMED);
			
			// control received file
			file = receptionListener.getReceived().get(0);
			assertTrue("file target not met, filename: " + file.getName(), filename.equals(file.getName()));
			assertTrue("file target not met, directory: [" + file.getParent() + "]", tardir.equals(file.getParentFile()));
			assertTrue("transmitted file should be empty but has length " + file.length(), file.length() == 0);
			assertTrue("transmission time failure", time > 0 & time < 1000);
			
			// CASE 3 : data file with complex target path
			System.out.println("\nCASE 3 : single file 50,000 speed limit - complex target path, new");
			data = Util.randBytes(length);
			src = Util.getTempFile(); 
			Util.makeFile(src, data);

			receptionListener.reset();
			sendListener.reset();
			filename = "laradara/broad/Kannengiesser.dat"; 
			cl.sendFile(src, filename);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(10000);
			time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control sender confirm
			assertFalse("no file received", receptionListener.getReceived().isEmpty());
			assertTrue("CONFIRM signal missing on sender", sendListener.getSignalType() 
					   == TransmissionEventType.FILE_CONFIRMED);
			
			// control received file
			file = receptionListener.getReceived().get(0);
			String filepath = file.getPath();
			rece = Util.readFile(file);
			assertTrue("file target not met, relative path: " + filepath, filepath.endsWith(filename));
			assertTrue("file target not met, directory: " + filepath, filepath.startsWith(tardir.getAbsolutePath()));
			assertTrue("data integrity error in file transmission", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 3000 & time < 5000);
			
			// CASE 4 : data file with complex target path, no speed limit, path variant
			System.out.println("\nCASE 4 : single file 50,000 no speed limit - complex target path, new");
			data = Util.randBytes(length);
			src = Util.getTempFile(); 
			Util.makeFile(src, data);
			cl.setTempo(-1);

			receptionListener.reset();
			sendListener.reset();
			filename = "/laradara/wide/Wasserstelle.dat"; 
			cl.sendFile(src, filename);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(2000);
			time = System.currentTimeMillis() - stamp;
			sendLock.lock_wait(1000);

			// control sender confirm
			assertFalse("no file received", receptionListener.getReceived().isEmpty());
			assertTrue("CONFIRM signal missing on sender", sendListener.getSignalType() 
					   == TransmissionEventType.FILE_CONFIRMED);
			
			// control received file
			file = receptionListener.getReceived().get(0);
			filepath = file.getPath();
			rece = Util.readFile(file);
			assertTrue("file target not met, relative path: " + filepath, filepath.endsWith(filename));
			assertTrue("file target not met, directory: " + filepath, filepath.startsWith(tardir.getAbsolutePath()));
			assertTrue("data integrity error in file transmission", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 0 & time < 1000);
			
			// wait (test)
//			Util.sleep(12000);
		}		
	} finally {
		System.out.println();
		if (sv != null) {
			if (sv.getConnections().length > 0) {
				System.out.println("# transmission volume of Server : " + 
					sv.getConnections()[0].getTransmissionVolume());
			}
			sv.closeAllConnections();
			sv.close();
			Util.sleep(10);
		}
		if (cl != null) {
			System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
			cl.close();
		}
	}

	}


	@Test
	public void client_single_target_fail () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final SemaphorLock sendLock = new SemaphorLock(1);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);
		final FileReceptionListener sendListener = new FileReceptionListener(sendLock, FileReceptionListener.Station.Client);

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: SINGLE, TARGET DIR UNDEFINED");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.start();

		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(sendListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- (test) connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 50,000 data file
			System.out.println("\nCASE 1 : single file 50,000 - target ");

			// prepare data and source file
			int length = 50000;
			byte[] data = Util.randBytes(length);
			File src = Util.getTempFile(); 
			Util.makeFile(src, data);
			
			// transmit file (filename target)
			String filename = "kannenschleifer.data";
			cl.sendFile(src, filename);
			
			// wait for completion
			lock.wait(10000);
			sendLock.lock_wait(1000);

			// control file-failed event (server)
			assertTrue("FILE_FAILED event expected", receptionListener.getSignalType() == TransmissionEventType.FILE_FAILED);
			assertTrue("error-info 102 expected", receptionListener.getErrorInfo() == 102);
			assertNotNull("file-info expected", receptionListener.getErrorFile());
			assertFalse("TEMP file exists, should be erased", receptionListener.getErrorFile().exists());
			assertTrue("file received, none expected", receptionListener.getReceived().isEmpty());

			// control file-failed event (client)
			assertTrue("FILE_FAILED event expected", sendListener.getSignalType() == TransmissionEventType.FILE_FAILED);
			assertTrue("error-info 101 expected", sendListener.getErrorInfo() == 101);
			assertNotNull("file-info expected", sendListener.getErrorFile());
			assertTrue("sender file erased", sendListener.getErrorFile().exists());
			
			// CASE 2 : empty data file
			System.out.println("\nCASE 2 : single empty file - target");
			receptionListener.reset();
			sendListener.reset();
			src = Util.getTempFile();
			cl.sendFile(src, filename);
			
			// wait for completion
			lock.wait(10000);
			sendLock.lock_wait(1000);

			// control file-failed event
			assertTrue("FILE_FAILED event expected", receptionListener.getSignalType() == TransmissionEventType.FILE_FAILED);
			assertTrue("error-info 102 expected", receptionListener.getErrorInfo() == 102);
			assertNotNull("file-info expected", receptionListener.getErrorFile());
			assertFalse("TEMP file exists, should be erased", receptionListener.getErrorFile().exists());
			assertTrue("file received, none expected", receptionListener.getReceived().isEmpty());

			// control file-failed event (client)
			assertTrue("FILE_FAILED event expected", sendListener.getSignalType() == TransmissionEventType.FILE_FAILED);
			assertTrue("error-info 101 expected", sendListener.getErrorInfo() == 101);
			assertNotNull("file-info expected", sendListener.getErrorFile());
			assertTrue("sender file erased", sendListener.getErrorFile().exists());
			
			
		}		
	} finally {
		System.out.println();
		if (sv != null) {
			if (sv.getConnections().length > 0) {
				System.out.println("# transmission volume of Server : " + 
					sv.getConnections()[0].getTransmissionVolume());
			}
			sv.closeAllConnections();
			sv.close();
			Util.sleep(10);
		}
		if (cl != null) {
			System.out.println("\n# transmission volume of Client : " + cl.getTransmissionVolume());
			cl.close();
		}
	}

	}

	/** Whether base contains data by byte values (equal-arrays).
	 * 
	 * @param base byte[][]
	 * @param data byte[]
	 * @return
	 */
	private boolean containsData (byte[][] base, byte[] data) {
		for (byte[] token : base) {
			if (Util.equalArrays(token, data)) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void client_multi_no_target () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 3);
		byte[][] results;

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: MULTI-FILE, NO TARGET");
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
			// CASE 1 : 3 x 50,000 data files, sent parallel
			System.out.println("\nCASE 1 : multi data file 50,000 - no target, priority Normal");

			// prepare data and source file
			int length = 50000;
			byte[] data1 = Util.randBytes(length);
			byte[] data2 = Util.randBytes(length);
			byte[] data3 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			File src3 = Util.getTempFile(); 
			Util.makeFile(src1, data1);
			Util.makeFile(src2, data2);
			Util.makeFile(src3, data3);
			
			// transmit file
			cl.sendFile(src1, null);
			cl.sendFile(src2, null);
			cl.sendFile(src3, null);
			long stamp = System.currentTimeMillis();
			
			// wait for completion
			System.out.println("--- all files sent (client), waiting ..");
			lock.wait(20000);
			long time = System.currentTimeMillis() - stamp;
			assertTrue("no / not all files received", receptionListener.getReceived().size() == 3);
			
			results = new byte[3][];

			// control received file content
			results[0] = Util.readFile(receptionListener.getReceived().get(0));
			results[1] = Util.readFile(receptionListener.getReceived().get(1));
			results[2] = Util.readFile(receptionListener.getReceived().get(2));

			assertTrue("data integrity error in file-1 transmission", containsData(results, data1));
			assertTrue("data integrity error in file-2 transmission", containsData(results, data2));
			assertTrue("data integrity error in file-3 transmission", containsData(results, data3));
			assertTrue("transmission time failure", time > 9000 & time < 12000);
			
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
	public void client_server_cross () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final SemaphorLock lock = new SemaphorLock(6);
		final SemaphorLock sendLock = new SemaphorLock(6);
		final FileReceptionListener serverListener = new FileReceptionListener(lock,
				FileReceptionListener.Station.Server);
		final FileReceptionListener clientListener = new FileReceptionListener(sendLock,
				FileReceptionListener.Station.Client);
		byte[][] results;

	try {
		System.out.println("\nTEST TRANSFER CLIENT/SERVER CROSS: MULTI-FILE, NO TARGET");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), serverListener);
		sv.getParameters().setTransmissionParcelSize(12*1024);
		sv.getParameters().setAlivePeriod(5000);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(clientListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(25000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		// get server connection
		Connection scon = sv.getConnections()[0];
		
			System.out.println("\nCASE 1 : multi data files, cross 50,000 - no target");

			// prepare data and source files (client and server)
			int length = 100000;
			byte[] cdata1 = Util.randBytes(length);
			byte[] cdata2 = Util.randBytes(length);
			byte[] cdata3 = Util.randBytes(length);
			byte[] sdata1 = Util.randBytes(length);
			byte[] sdata2 = Util.randBytes(length);
			byte[] sdata3 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			File src3 = Util.getTempFile(); 
			File src4 = Util.getTempFile(); 
			File src5 = Util.getTempFile(); 
			File src6 = Util.getTempFile(); 
			Util.makeFile(src1, cdata1);
			Util.makeFile(src2, cdata2);
			Util.makeFile(src3, cdata3);
			Util.makeFile(src4, sdata1);
			Util.makeFile(src5, sdata2);
			Util.makeFile(src6, sdata3);
			
			// transmit files
			cl.sendFile(src3, null, SendPriority.Low);
			scon.sendFile(src6, null, SendPriority.Low);
			Util.sleep(100);
			cl.sendFile(src2, null);
			scon.sendFile(src5, null);
			Util.sleep(100);
			cl.sendFile(src1, null, SendPriority.High);
			scon.sendFile(src4, null, SendPriority.High);
			long stamp = System.currentTimeMillis();
			
			// wait for completion
			System.out.println("--- waiting ..");
			lock.lock_wait(60000);
			sendLock.lock_wait(30000);
			long time = System.currentTimeMillis() - stamp;
			System.err.println("--> RECEIVED FILES after wait: server=" + serverListener.getReceived().size() 
					+ ", client=" + clientListener.getReceived().size());
			System.out.println("--- time elapsed for all: " + time);
			assertTrue("no / not all files received (server)", serverListener.getReceived().size() == 3);
			assertTrue("no / not all files received (client)", clientListener.getReceived().size() == 3);

			// control received file content (server received)
			results = new byte[3][];
			results[0] = Util.readFile(serverListener.getReceived().get(0));
			results[1] = Util.readFile(serverListener.getReceived().get(1));
			results[2] = Util.readFile(serverListener.getReceived().get(2));

			assertTrue("data integrity error in client file-1 transmission", containsData(results, cdata1));
			assertTrue("data integrity error in client file-2 transmission", containsData(results, cdata2));
			assertTrue("data integrity error in client file-3 transmission", containsData(results, cdata3));
			
			// control received file content (client received)
			results = new byte[3][];
			results[0] = Util.readFile(clientListener.getReceived().get(0));
			results[1] = Util.readFile(clientListener.getReceived().get(1));
			results[2] = Util.readFile(clientListener.getReceived().get(2));
			
			assertTrue("data integrity error in server file-1 transmission", containsData(results, sdata1));
			assertTrue("data integrity error in server file-2 transmission", containsData(results, sdata2));
			assertTrue("data integrity error in server file-3 transmission", containsData(results, sdata3));

			assertTrue("transmission time failure", time > 11000 & time < 13000);
			
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
	public void transfer_break () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		long stamp, time, fid;
		
		final SemaphorLock lock = new SemaphorLock(2);
		final SemaphorLock sendLock = new SemaphorLock(2);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 
							FileReceptionListener.Station.Server);
		final FileReceptionListener sendListener = new FileReceptionListener(sendLock, 
							FileReceptionListener.Station.Client);
	
		receptionListener.set_release_locks_on_failure(false);
		sendListener.set_release_locks_on_failure(false);
		
	try {
		System.out.println("\nTEST TRANSFER CROSS - BREAK TRANSFER");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setTransmissionParcelSize(12*1024);
		sv.getParameters().setAlivePeriod(0); // 5000
		File base = new File(System.getProperty("java.io.tmpdir"));
		File tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
		tardir.mkdirs();
		sv.getParameters().setFileRootDir(tardir);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);  // 5000
		cl.getParameters().setTransmissionParcelSize(8*1024);
		tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
		tardir.mkdirs();
		cl.getParameters().setFileRootDir(tardir);
		cl.addListener(sendListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		// get server connection
		Connection scon = sv.getConnections()[0];
		
			// prepare data and source files (client and server)
			int length = 100000;
			byte[] cdata1 = Util.randBytes(length);
			byte[] cdata2 = Util.randBytes(length);
			byte[] sdata1 = Util.randBytes(length);
			byte[] sdata2 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			File src4 = Util.getTempFile(); 
			File src5 = Util.getTempFile(); 
			Util.makeFile(src1, cdata1);
			Util.makeFile(src2, cdata2);
			Util.makeFile(src4, sdata1);
			Util.makeFile(src5, sdata2);
			
			System.out.println("\nCASE 1 : 2 files cross 100,000 - targets - BROKEN by receiver");
			
			// transmit files
			stamp = System.currentTimeMillis();
			cl.sendFile(src1, "client-file-1");
			fid = scon.sendFile(src4, "server-file-1");

			// break incoming transfer on client (causes error)
			Util.sleep(1000);
			System.out.println("-- breaking transfer for \"server-file-1\" on client (incoming");
			cl.breakTransfer(fid, 0);
			
			// wait for transfer completion (failure and success)
			System.out.println("--- waiting ..");
			lock.lock_wait(30000);
			sendLock.lock_wait(5000);
			time = System.currentTimeMillis() - stamp;

			System.out.println("--- time elapsed for preceding: " + time);
			assertTrue("transmission time failure", time > 6000 & time < 8000);

			// control received file content (server received)
			assertTrue("no / not all files received", receptionListener.getReceived().size() == 1);
			File file = receptionListener.getReceived().get(0);
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in client-file-1 transmission", Util.equalArrays(rece, cdata1));

			// control received transmission events
			TransmissionEvent evt = receptionListener.getFirstEventOf(TransmissionEventType.FILE_ABORTED);
			assertNotNull("ABORTED transmission event missing (server)", evt);
			assertTrue("false event info, 107 expected", evt.getInfo() == 107);
			assertTrue("false target path detected", "server-file-1".equals(evt.getPath()));
			assertTrue("false number of transmission events, 3 expected", receptionListener.getEvents().size() == 3);
			
			evt = sendListener.getFirstEventOf(TransmissionEventType.FILE_ABORTED);
			assertNotNull("ABORTED transmission event missing (server)", evt);
			assertTrue("false event info, 108 expected", evt.getInfo() == 108);
			assertTrue("false target path detected", "server-file-1".equals(evt.getPath()));
			assertTrue("false number of transmission events, 3 expected", sendListener.getEvents().size() == 3);

			System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
			System.out.println("\nCASE 2 : 2 files cross 100,000 - targets - BROKEN by sender");
			
			receptionListener.reset();
			sendListener.reset();
			
			// transmit files
			stamp = System.currentTimeMillis();
			fid = cl.sendFile(src2, "client-file-2");
			scon.sendFile(src5, "server-file-2");

			// break outgoing transfer on client (causes error)
			Util.sleep(1000);
			System.out.println("-- breaking transfer for \"client-file-2\" on client (outgoing");
			cl.breakTransfer(fid, 1);
			
			// wait for transfer completion (failure and success)
			System.out.println("--- waiting ..");
			lock.lock_wait(30000);
			sendLock.lock_wait(5000);
			time = System.currentTimeMillis() - stamp;

			System.out.println("--- time elapsed for preceding: " + time);
			assertTrue("transmission time failure", time > 6000 & time < 8000);
			
			// control received file content (server received)
			assertTrue("no files received", sendListener.getReceived().size() == 1);
			file = sendListener.getReceived().get(0);
			rece = Util.readFile(file);
			assertTrue("data integrity error in server-file-2 transmission", Util.equalArrays(rece, sdata2));

			// control received transmission events
			evt = receptionListener.getFirstEventOf(TransmissionEventType.FILE_ABORTED);
			assertNotNull("ABORTED transmission event missing (client)", evt);
			assertTrue("false event info, 106 expected", evt.getInfo() == 106);
			assertTrue("false target path detected", "client-file-2".equals(evt.getPath()));
			assertTrue("false number of transmission events, 3 expected", receptionListener.getEvents().size() == 3);
			
			evt = sendListener.getFirstEventOf(TransmissionEventType.FILE_ABORTED);
			assertNotNull("ABORTED transmission event missing (client)", evt);
			assertTrue("false event info, 105 expected", evt.getInfo() == 105);
			assertTrue("false target path detected", "client-file-2".equals(evt.getPath()));
			assertTrue("false number of transmission events, 3 expected", sendListener.getEvents().size() == 3);

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
	public void aborted_by_close () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		long stamp, time, fid;
		
		final SemaphorLock lock = new SemaphorLock(2);
		final SemaphorLock sendLock = new SemaphorLock(2);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 
							FileReceptionListener.Station.Server);
		final FileReceptionListener sendListener = new FileReceptionListener(sendLock, 
							FileReceptionListener.Station.Client);
	
		receptionListener.set_release_locks_on_failure(false);
		sendListener.set_release_locks_on_failure(false);
		
	try {
		System.out.println("\nTEST TRANSFER CROSS - ABORTED BY CLOSE");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setTransmissionParcelSize(12*1024);
		sv.getParameters().setAlivePeriod(0); // 5000
		File base = new File(System.getProperty("java.io.tmpdir"));
		File tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
		tardir.mkdirs();
		sv.getParameters().setFileRootDir(tardir);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);  // 5000
		cl.getParameters().setTransmissionParcelSize(8*1024);
		tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
		tardir.mkdirs();
		cl.getParameters().setFileRootDir(tardir);
		cl.addListener(sendListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		// get server connection
		Connection scon = sv.getConnections()[0];
		
			// prepare data and source files (client and server)
			int length = 100000;
			byte[] cdata1 = Util.randBytes(length);
			byte[] cdata2 = Util.randBytes(length);
			byte[] sdata1 = Util.randBytes(length);
			byte[] sdata2 = Util.randBytes(length);
			File src1 = Util.getTempFile(); 
			File src2 = Util.getTempFile(); 
			File src4 = Util.getTempFile(); 
			File src5 = Util.getTempFile(); 
			Util.makeFile(src1, cdata1);
			Util.makeFile(src2, cdata2);
			Util.makeFile(src4, sdata1);
			Util.makeFile(src5, sdata2);
			
			System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
			System.out.println("\nCASE 1 : 2 files cross 100,000 - targets - Close by receiver (server)");
			
//			receptionListener.reset();
//			sendListener.reset();
			
			// transmit files
			cl.sendFile(src1, "client-file-1");
			scon.sendFile(src4, "server-file-1");
			stamp = System.currentTimeMillis();

			// break outgoing transfer on client (causes error)
			Util.sleep(2000);
			System.out.println("-- closing connection by receiver (server)");
			scon.close();
			
			// wait for transfer completion (failure and success)
			System.out.println("--- waiting ..");
			lock.lock_wait(30000);
			sendLock.lock_wait(5000);
			time = System.currentTimeMillis() - stamp;

			// control received file content (server received)
			assertTrue("no files expected (server)", receptionListener.getReceived().size() == 0);
			assertTrue("no files expected (server)", sendListener.getReceived().size() == 0);

			// control received transmission events
			TransmissionEventType type = TransmissionEventType.FILE_ABORTED;
			assertTrue("false number of ABORTED events (server)", receptionListener.countEvents(type) == 2);
			assertTrue("false number of ABORTED events (client)", sendListener.countEvents(type) == 2);
			assertTrue("missing event ABORTED, info 113, on server side", receptionListener.hasTransmissionEvent(type, 113));
			assertTrue("missing event ABORTED, info 114, on server side", receptionListener.hasTransmissionEvent(type, 114));
			assertTrue("missing event ABORTED, info 113, on client side", sendListener.hasTransmissionEvent(type, 113));
			assertTrue("missing event ABORTED, info 114, on client side", sendListener.hasTransmissionEvent(type, 114));

			System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
			System.out.println("\nCASE 2 : 2 files cross 100,000 - targets - Close by sender (client)");
			
			receptionListener.reset();
			sendListener.reset();
			cl = new Client();
			cl.getParameters().setAlivePeriod(0);  // 5000
			cl.getParameters().setTransmissionParcelSize(8*1024);
			tardir = new File(base, "JN-Target-" + Util.nextRand(100000));
			tardir.mkdirs();
			cl.getParameters().setFileRootDir(tardir);
			cl.addListener(sendListener);
			cl.connect(100, sv.getSocketAddress());
			cl.setTempo(15000);
			System.out.println("-- connection established " + cl.toString());
			Util.sleep(50);
			
			// get server connection
			scon = sv.getConnections()[0];
			
			// transmit files
			cl.sendFile(src2, "client-file-2");
			scon.sendFile(src5, "server-file-2");
			stamp = System.currentTimeMillis();

			// break outgoing transfer on client (causes error)
			Util.sleep(2000);
			System.out.println("-- closing connection by sender (client)");
			cl.close();
			
			// wait for transfer completion (failure and success)
			System.out.println("--- waiting ..");
			lock.lock_wait(30000);
			sendLock.lock_wait(5000);
			time = System.currentTimeMillis() - stamp;

			// control received file content (server received)
			assertTrue("no files expected (server)", receptionListener.getReceived().size() == 0);
			assertTrue("no files expected (server)", sendListener.getReceived().size() == 0);

			// control received transmission events
			assertTrue("false number of ABORTED events (server)", receptionListener.countEvents(type) == 2);
			assertTrue("false number of ABORTED events (client)", sendListener.countEvents(type) == 2);
			assertTrue("missing event ABORTED, info 113, on server side", receptionListener.hasTransmissionEvent(type, 113));
			assertTrue("missing event ABORTED, info 114, on server side", receptionListener.hasTransmissionEvent(type, 114));
			assertTrue("missing event ABORTED, info 113, on client side", sendListener.hasTransmissionEvent(type, 113));
			assertTrue("missing event ABORTED, info 114, on client side", sendListener.hasTransmissionEvent(type, 114));

			
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
