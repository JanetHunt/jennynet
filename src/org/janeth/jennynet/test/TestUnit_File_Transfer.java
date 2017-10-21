package org.janeth.jennynet.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.TransmissionEvent;
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
		public void objectReceived(Connection con, long objNr, Object obj) {
			System.out.println("*** OBJECT RECEIVED: ID " + objNr + ", type = " + obj.getClass());
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
		
//		public void reset (int unlockSize) {
//			received.clear();
//			unlockThreshold = unlockSize;
//		}

		public void reset () {
			received.clear();
		}
	}
		
	
	@Test
	public void transfer_client_single_plain () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);

	try {
		System.out.println("\nTEST TRANSFER CLIENT TO SERVER: SINGLE, PLAIN");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
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
			// CASE 1 : 50,000 data file
			System.out.println("\nCASE 1 : single data file 50,000 - no target");

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

			// control received file content
			File file = receptionListener.getReceived().get(0);
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission (1)", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 3000 & time < 5000);
			
			// CASE 2 : transmit file (no speed limit)
			System.out.println("\nCASE 2 : single file - no speed limit");
			receptionListener.reset();
			cl.setTempo(-1);
			cl.sendFile(src, null);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait();
			time = System.currentTimeMillis() - stamp;

			// control received file content
			file = receptionListener.getReceived().get(0);
			rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission (2)", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 0 & time < 200);
			
			// CASE 3 : empty data file
			System.out.println("\nCASE 3 : single empty file - no target");
			receptionListener.reset();
			src = Util.getTempFile();
			cl.sendFile(src, null);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(10000);
			time = System.currentTimeMillis() - stamp;
			assertFalse("no file received", receptionListener.getReceived().isEmpty());

			// control received file
			file = receptionListener.getReceived().get(0);
			assertTrue("transmitted file should be empty but has length " + file.length(), 
					file.length() == 0);
			assertTrue("transmission time failure", time > 0 & time < 1000);
			
			// wait (test)
//			Util.sleep(12000);
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
	public void transfer_client_single_target_existing () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);

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
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
		System.out.println("-- (test) connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			// CASE 1 : 50,000 data file
			System.out.println("\nCASE 1 : single data file 50,000 - target filename");

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

			// control received file content
			File file = receptionListener.getReceived().get(0);
			assertTrue("file target not met, filename: " + file.getName(), filename.equals(file.getName()));
			assertTrue("file target not met, directory: [" + file.getParent() + "]", tardir.equals(file.getParentFile()));
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in file transmission", Util.equalArrays(rece, data));
			assertTrue("transmission time failure", time > 3000 & time < 5000);
			
			// CASE 2 : empty data file
			System.out.println("\nCASE 2 : single empty file - target filename");
			receptionListener.reset();
			src = Util.getTempFile();
//			filename = "eselsbruecke.data";
			cl.sendFile(src, filename);
			stamp = System.currentTimeMillis();
			
			// wait for completion
			lock.wait(10000);
			time = System.currentTimeMillis() - stamp;
			assertFalse("no file received", receptionListener.getReceived().isEmpty());

			// control received file
			file = receptionListener.getReceived().get(0);
			assertTrue("file target not met, filename: " + file.getName(), filename.equals(file.getName()));
			assertTrue("file target not met, directory: [" + file.getParent() + "]", tardir.equals(file.getParentFile()));
			assertTrue("transmitted file should be empty but has length " + file.length(), file.length() == 0);
			assertTrue("transmission time failure", time > 0 & time < 1000);
			
			// wait (test)
//			Util.sleep(12000);
		}		
	} finally {
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
		public void transfer_client_single_target_nonexisting () throws IOException, InterruptedException {
			Server sv = null;
			Client cl = null;
			
			final Object lock = new Object();
			final FileReceptionListener receptionListener = new FileReceptionListener(lock, 1);
	
		try {
			System.out.println("\nTEST TRANSFER CLIENT TO SERVER: SINGLE, TARGET DIR NON-EXISTING");
			sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
			sv.start();
	
			// set up a running connection
			cl = new Client();
			cl.getParameters().setAlivePeriod(0);
			cl.getParameters().setTransmissionParcelSize(8*1024);
			cl.connect(100, sv.getSocketAddress());
			cl.setTempo(15000);
			System.out.println("-- (test) connection established " + cl.toString());
			Util.sleep(20);
			
			synchronized (lock) {
				// CASE 1 : 50,000 data file
				System.out.println("\nCASE 1 : single data file 50,000 - target non-existing");
				File tempDir = new File(System.getProperty("java.io.tmpdir"));
	
				// prepare data and source file
				int length = 50000;
				byte[] data = Util.randBytes(length);
				File src = Util.getTempFile(); 
				Util.makeFile(src, data);
				
				// transmit file
				String filename = "kannenschleifer.data";
				cl.sendFile(src, filename);
				long stamp = System.currentTimeMillis();
				
				// wait for completion
				lock.wait(10000);
				long time = System.currentTimeMillis() - stamp;
				assertFalse("no file received", receptionListener.getReceived().isEmpty());
	
				// control received file content
				File file = receptionListener.getReceived().get(0);
				assertFalse("file not a random file: " + file.getName(), filename.equals(file.getName()));
				assertTrue("file not in TEMP directory: [" + file.getParent() + "]", tempDir.equals(file.getParentFile()));
				byte [] rece = Util.readFile(file);
				assertTrue("data integrity error in file transmission", Util.equalArrays(rece, data));
				assertTrue("transmission time failure", time > 3000 & time < 5000);
				
				// CASE 2 : empty data file
				System.out.println("\nCASE 2 : single empty file - target non-existing");
				receptionListener.reset();
				src = Util.getTempFile();
				cl.sendFile(src, filename);
				stamp = System.currentTimeMillis();
				
				// wait for completion
				lock.wait(10000);
				time = System.currentTimeMillis() - stamp;
				assertFalse("no file received", receptionListener.getReceived().isEmpty());
	
				// control received file
				file = receptionListener.getReceived().get(0);
				assertFalse("file not a random file: " + file.getName(), filename.equals(file.getName()));
				assertTrue("file not in TEMP directory: [" + file.getParent() + "]", tempDir.equals(file.getParentFile()));
				assertTrue("transmitted file should be empty but has length " + file.length(), file.length() == 0);
				assertTrue("transmission time failure", time > 0 & time < 1000);
				
				// wait (test)
	//			Util.sleep(12000);
			}		
		} finally {
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
		public void transfer_client_muti_plain () throws IOException, InterruptedException {
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
				// CASE 1 : 50,000 data file
				System.out.println("\nCASE 1 : multi data file 50,000 - no target");
	
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
				System.out.println("--- waiting ..");
				lock.wait(20000);
				long time = System.currentTimeMillis() - stamp;
				assertTrue("no / not all files received", receptionListener.getReceived().size() == 3);

				// control received file content
				File file = receptionListener.getReceived().get(0);
				byte [] rece = Util.readFile(file);
				assertTrue("data integrity error in file-1 transmission", Util.equalArrays(rece, data1));

				file = receptionListener.getReceived().get(1);
				rece = Util.readFile(file);
				assertTrue("data integrity error in file-2 transmission", Util.equalArrays(rece, data2));

				file = receptionListener.getReceived().get(2);
				rece = Util.readFile(file);
				assertTrue("data integrity error in file-3 transmission", Util.equalArrays(rece, data3));
				assertTrue("transmission time failure", time > 9000 & time < 12000);
				
//				// CASE 2 : empty data file
//				System.out.println("\nCASE 2 : single empty file - no target");
//				receptionListener.reset();
//				src = Util.getTempFile();
//				cl.sendFile(src, null);
//				stamp = System.currentTimeMillis();
//				
//				// wait for completion
//				lock.wait(10000);
//				time = System.currentTimeMillis() - stamp;
//				assertFalse("no file received", receptionListener.getReceived().isEmpty());
//	
//				// control received file
//				file = receptionListener.getReceived().get(0);
//				assertTrue("transmitted file should be empty but has length " + file.length(), 
//						file.length() == 0);
//				assertTrue("transmission time failure", time > 0 & time < 1000);
				
				// wait (test)
	//			Util.sleep(12000);
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
	public void transfer_client_server_cross () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final SemaphorLock lock = new SemaphorLock(6);
		final FileReceptionListener receptionListener = new FileReceptionListener(lock);

	try {
		System.out.println("\nTEST TRANSFER CLIENT/SERVER CROSS: MULTI-FILE, PLAIN");
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setTransmissionParcelSize(12*1024);
		sv.getParameters().setAlivePeriod(5000);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		FileReceptionListener clientListener = new FileReceptionListener(lock); 
		cl.addListener(clientListener);
		cl.connect(100, sv.getSocketAddress());
		cl.setTempo(15000);
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
			cl.sendFile(src1, null);
			cl.sendFile(src2, null);
			cl.sendFile(src3, null);
			scon.sendFile(src4, null);
			scon.sendFile(src5, null);
			scon.sendFile(src6, null);
			long stamp = System.currentTimeMillis();
			
			// wait for completion
			System.out.println("--- waiting ..");
			lock.lock_wait(30000);
			long time = System.currentTimeMillis() - stamp;
			assertTrue("no / not all files received", receptionListener.getReceived().size() == 3);

			// control received file content (server received)
			File file = receptionListener.getReceived().get(0);
			byte [] rece = Util.readFile(file);
			assertTrue("data integrity error in client file-1 transmission", Util.equalArrays(rece, cdata1));

			file = receptionListener.getReceived().get(1);
			rece = Util.readFile(file);
			assertTrue("data integrity error in client file-2 transmission", Util.equalArrays(rece, cdata2));

			file = receptionListener.getReceived().get(2);
			rece = Util.readFile(file);
			assertTrue("data integrity error in client file-3 transmission", Util.equalArrays(rece, cdata3));
			System.out.println("--- time elapsed for all: " + time);
			
			// control received file content (client received)
			file = clientListener.getReceived().get(0);
			rece = Util.readFile(file);
			assertTrue("data integrity error in server file-1 transmission", Util.equalArrays(rece, sdata1));

			file = clientListener.getReceived().get(1);
			rece = Util.readFile(file);
			assertTrue("data integrity error in server file-2 transmission", Util.equalArrays(rece, sdata2));

			file = clientListener.getReceived().get(2);
			rece = Util.readFile(file);
			assertTrue("data integrity error in server file-3 transmission", Util.equalArrays(rece, sdata3));
			System.out.println("--- time elapsed for all: " + time);
			assertTrue("transmission time failure", time > 18000 & time < 21000);
			
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
