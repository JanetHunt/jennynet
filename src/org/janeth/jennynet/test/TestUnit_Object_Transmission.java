package org.janeth.jennynet.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.util.CRC32;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_Object_Transmission {

	public TestUnit_Object_Transmission() {
	}

	@Test
	public void tempo_sending_single_object () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 1);

	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			// CASE 1
			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);
			cl.setTempo(5000);
			assertTrue("error is TEMPO setting", cl.getTransmissionSpeed() == 5000);
			
			// send over connection
			long time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			int elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			assertFalse("no object received by server", receptionListener.getReceived().isEmpty());
			byte[] rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 1)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 1 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 18);
			
			// CASE 2
			// prepare random data block to transmit
			dataLen = 500000;
			block = Util.randBytes(dataLen);
			cl.setTempo(33000);
			cl.getParameters().setTransmissionParcelSize(16*1024);
			receptionListener.reset();
			assertTrue("error is TEMPO setting", cl.getTransmissionSpeed() == 33000);
			
			// send over connection
			time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 2)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 2 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 14);

			// CASE 3
			// prepare random data block to transmit
			dataLen = 1000000;
			block = Util.randBytes(dataLen);
			cl.setTempo(100000);
			cl.getParameters().setTransmissionParcelSize(20*1024);
			receptionListener.reset();
			assertTrue("error is TEMPO setting", cl.getTransmissionSpeed() == 100000);
			
			// send over connection
			time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 3)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 3 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 9);

			// CASE 4
			// prepare random data block to transmit
			dataLen = 1000000;
			block = Util.randBytes(dataLen);
			cl.setTempo(1000000);
			cl.getParameters().setTransmissionParcelSize(32*1024);
			receptionListener.reset();
			assertTrue("error is TEMPO setting", cl.getTransmissionSpeed() == 1000000);
			
			// send over connection
			time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 4)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 4 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time error", elapsed >= 0 & elapsed < 3);

			// CASE 5
			// prepare random data block to transmit
			dataLen = 2000000;
			block = Util.randBytes(dataLen);
			cl.setTempo(-1);
			cl.getParameters().setTransmissionParcelSize(64*1024);
			receptionListener.reset();
			assertTrue("error is TEMPO setting", cl.getTransmissionSpeed() == -1);
			
			// send over connection
			time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time);
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 4)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 5 verified, time elapsed " + elapsed + " ms");
			assertTrue("transmission time error", elapsed >= 0 & elapsed < 1000);
		}
	
	// shutdown net systems
	} finally {
		System.out.println("# transmission volume of Client : " + cl.getTransmissionVolume());
		if (sv.getConnections().length > 0) {
			System.out.println("# transmission volume of Server : " + 
				sv.getConnections()[0].getTransmissionVolume());
		}
		
		System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
				+ Runtime.getRuntime().totalMemory());
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
	public void tempo_sending_multi_object () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 3);
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			// CASE 1
			// prepare 3 random data blocks to transmit
			int dataLen1 = 100000;
			byte[] block1 = Util.randBytes(dataLen1);
			int dataLen2 = 50000;
			byte[] block2 = Util.randBytes(dataLen2);
			int dataLen3 = 120000;
			byte[] block3 = Util.randBytes(dataLen3);

			cl.setTempo(33000);
			
			// send over connection
			long time = System.currentTimeMillis();
			cl.sendData(block1, 0, block1.length, SendPriority.Normal);
			cl.sendData(block2, 0, block2.length, SendPriority.Normal);
			cl.sendData(block3, 0, block3.length, SendPriority.Normal);
			System.out.println("-- orders put, waiting for results ...");
			lock.wait();
			int elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data blocks
			int size = receptionListener.getReceived().size();
			assertFalse("no object received by server", size == 0);
			assertTrue("error in number of received objects", size == 3);
			byte[] rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 1)", Util.equalArrays(block1, rece));
			rece = receptionListener.getReceived().get(1);
			assertTrue("data integrity error (transmitted 2)", Util.equalArrays(block2, rece));
			rece = receptionListener.getReceived().get(2);
			assertTrue("data integrity error (transmitted 3)", Util.equalArrays(block3, rece));

			// check transmission time
			System.out.println("-- transmission verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time error", elapsed > 6 & elapsed < 10);
			
		}
	
	// shutdown net systems
	} finally {
		System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
				+ Runtime.getRuntime().totalMemory());
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
	public void tempo_receiving_single_object () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 1);
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), null);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(receptionListener);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			Connection scon = sv.getConnections()[0];

			// CASE 1
			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);
			cl.setTempo(5000);
			
			// send over connection
			long time = System.currentTimeMillis();
			scon.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			int elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			assertFalse("no object received by server", receptionListener.getReceived().isEmpty());
			byte[] rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 1)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 1 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 18);
			
			// CASE 2
			// prepare random data block to transmit
			dataLen = 500000;
			block = Util.randBytes(dataLen);
			cl.setTempo(33000);
			cl.getParameters().setTransmissionParcelSize(16*1024);
			receptionListener.reset();
			
			// send over connection
			time = System.currentTimeMillis();
			scon.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 2)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 2 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 14);
	
			// CASE 3
			// prepare random data block to transmit
			dataLen = 1000000;
			block = Util.randBytes(dataLen);
			cl.setTempo(100000);
			cl.getParameters().setTransmissionParcelSize(20*1024);
			receptionListener.reset();
			
			// send over connection
			time = System.currentTimeMillis();
			scon.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 3)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 3 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time too quick", elapsed >= 9);
	
			// CASE 4
			// prepare random data block to transmit
			dataLen = 1000000;
			block = Util.randBytes(dataLen);
			cl.setTempo(1000000);
			cl.getParameters().setTransmissionParcelSize(32*1024);
			receptionListener.reset();
			
			// send over connection
			time = System.currentTimeMillis();
			scon.sendData(block, 0, dataLen, SendPriority.Normal);
			lock.wait();
			elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			
			// check received data
			rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 4)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 4 verified, time elapsed " + elapsed + " sec");
			assertTrue("transmission time error", elapsed >= 0 & elapsed < 3);
		}
	
	// shutdown net systems
	} finally {
		System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
				+ Runtime.getRuntime().totalMemory());
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
	public void speed_settings () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 1);
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(receptionListener);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			ServerConnection scon = (ServerConnection)sv.getConnections()[0];
			assertTrue("false initial server speed setting", scon.getTransmissionSpeed() < 0);
			assertTrue("false initial client speed setting", cl.getTransmissionSpeed() < 0);

			// test setting by server (unpriorised)
			int speed = 20000;
			scon.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in server speed setting", scon.getTransmissionSpeed() == speed);
			assertTrue("client does not receive server speed setting", cl.getTransmissionSpeed() == speed);
			
			// test setting by client (unpriorised)
			speed = 50000;
			cl.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in client speed setting", scon.getTransmissionSpeed() == speed);
			assertTrue("server does not receive client speed setting", scon.getTransmissionSpeed() == speed);
			
			// test setting by server (priorised)
			scon.setTempoFixed(true);
			speed = 100000;
			scon.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in priorised server speed setting", scon.getTransmissionSpeed() == speed);
			assertTrue("client does not receive priorised server speed setting", cl.getTransmissionSpeed() == speed);
			
			// test rejected setting by client (server priorised)
			speed = 23000;
			int oldSpeed = scon.getTransmissionSpeed();
			cl.setTempo(speed);
			Util.sleep(50);
			assertFalse("server falsely adopts client speed setting (priorised)", scon.getTransmissionSpeed() == speed);
			assertTrue("server loses speed setting through client speed setting (priorised)", 
					scon.getTransmissionSpeed() == oldSpeed);
			assertFalse("client falsely adopts speed setting (priorised)", cl.getTransmissionSpeed() == speed);
			
			// test setting by server (unpriorised)
			scon.setTempoFixed(false);
			speed = 330000;
			scon.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in unpriorised server speed setting", scon.getTransmissionSpeed() == speed);
			assertTrue("client does not receive unpriorised server speed setting", cl.getTransmissionSpeed() == speed);
			
			// test setting by server (unpriorised)
			speed = 66000;
			cl.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in unpriorised client speed setting", cl.getTransmissionSpeed() == speed);
			assertTrue("server does not receive unpriorised client speed setting", scon.getTransmissionSpeed() == speed);
			
			// test speed unlimited (speed off)
			speed = -1;
			cl.setTempo(speed);
			Util.sleep(50);
			assertTrue("error in client speed setting (speed off)", cl.getTransmissionSpeed() == speed);
			assertTrue("server does not receive client speed setting (speed off)", scon.getTransmissionSpeed() == speed);
			
		}
		// shutdown net systems
		} finally {
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
	public void transmission_off () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener(lock, 1);
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), receptionListener);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(receptionListener);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			ServerConnection scon = (ServerConnection)sv.getConnections()[0];
			int speed = 10000;
			scon.setTempo(speed);
			Util.sleep(10);
			
			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);
			assertTrue("error in TEMPO setting (client)", cl.getTransmissionSpeed() == speed);
			
			// start sending from client
			long time = System.currentTimeMillis();
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
			Util.sleep(2000);
			
			// set transmission off and on again
			scon.setTempo(0);
			Util.sleep(20000);
			scon.setTempo(speed);

			lock.wait();
			int elapsed = (int)(System.currentTimeMillis() - time) / 1000;
			assertTrue("transmission off setting not working, elapsed: " + elapsed, elapsed >= dataLen/speed + 18);
			
			// check received data
			assertFalse("no object received by server", receptionListener.getReceived().isEmpty());
			byte[] rece = receptionListener.getReceived().get(0);
			assertTrue("data integrity error (transmitted 1)", Util.equalArrays(block, rece));
			System.out.println("-- transmission 1 verified, time elapsed " + elapsed + " sec");
		}
		// shutdown net systems
		} finally {
			System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
					+ Runtime.getRuntime().totalMemory());
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

	private static class TestObject_C1 {
		private enum Choice {eins, zwei, drei}

		private long serialNr = Util.nextRand(Integer.MAX_VALUE);
		private int testNr = Util.nextRand(600000);
		private String text = "Und sie konnten ihn kaum noch tragen!";
		private Choice choice = Choice.drei;
		
		
		public int getCrc() {
			CRC32 crc = new CRC32();
			crc.update(testNr);
			crc.update(serialNr);
			crc.update(text.getBytes());
			crc.update(choice.ordinal());
			return crc.getIntValue();
		}
	}

	/** A <code>ConnectionListener</code> to receive 
		 * <code>JennyNetByteBuffer</code> and return them via an output method
		 * as list of byte arrays. There is a lock as parameter which gets 
		 * notified with each reception event.
		 */
		private class RealObjectReceptionListener extends DefaultConnectionListener {
	
			private List<Object> received = new ArrayList<Object>();
			private Object lock;
			private int unlockThreshold;
	
			public RealObjectReceptionListener (Object lock, int unlockSize) {
				this.lock = lock;
				unlockThreshold = unlockSize;
			}
			
			@Override
			public void objectReceived(Connection con, long objNr, Object obj) {
				received.add(obj);
				
				if (received.size() == unlockThreshold)
				synchronized(lock) {
					lock.notify();
				}
			}
			
			public List<Object> getReceived () {
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
	public void transmit_real_objects () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		final Object lock = new Object();
		final RealObjectReceptionListener receptionListener = new RealObjectReceptionListener(lock, 1);
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), null);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(5000);
		cl.getParameters().setTransmissionParcelSize(8*1024);
		cl.addListener(receptionListener);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(20);
		
		synchronized (lock) {
			
			Connection scon = sv.getConnections()[0];
	
			// CASE 1
			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);

			// prepare object of class TestObject_C1
			TestObject_C1 obj1 = new TestObject_C1();
			int crc1 = obj1.getCrc();
			scon.getSendSerialization().registerClass(TestObject_C1.class);
			cl.getReceiveSerialization().registerClass(TestObject_C1.class);
			
			// send over connection
			long time = System.currentTimeMillis();
			scon.sendObject(obj1);
			lock.wait(1000);
			int elapsed = (int)(System.currentTimeMillis() - time);
			
			// check received data
			assertFalse("no object received by server", receptionListener.getReceived().isEmpty());
			Object object = receptionListener.getReceived().get(0);
			assertTrue("received object is not of sender class", object instanceof TestObject_C1);
			TestObject_C1 recObj = (TestObject_C1)object;
			assertTrue("data integrity error (transmitted 1)", recObj.getCrc() == crc1);
			System.out.println("-- transmission 1 verified, time elapsed " + elapsed + " ms");
			assertTrue("transmission time error", elapsed >= 0 & elapsed < 200);
			
		}
	
	// shutdown net systems
	} finally {
		System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
				+ Runtime.getRuntime().totalMemory());
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
	public void send_queue_full () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		try {
			sv = new StandardServer(new InetSocketAddress("localhost", 3000), null);
			sv.getParameters().setAlivePeriod(0);
			sv.start();
			
			// set up a running connection
			cl = new Client();
			cl.getParameters().setAlivePeriod(5000);
			cl.getParameters().setObjectQueueCapacity(50);
			cl.connect(100, sv.getSocketAddress());
			System.out.println("-- connection established " + cl.toString());
			Util.sleep(50);
			
			ServerConnection scon = (ServerConnection)sv.getConnections()[0];
			int speed = 20000;
			scon.setTempo(speed);
			Util.sleep(10);
			
			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);
			
			// start sending from client
			try {
				for (int i = 0; i < 100; i++) {
					cl.sendData(block, 0, dataLen, SendPriority.Normal);
				}
				fail("illegal state exception for send-queue (client) overflow expected");
			} catch (IllegalStateException e) {
			}
			
		// shutdown net systems
		} finally {
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
	public void send_serialisation_overflow () throws IOException, InterruptedException {
		Server sv = null;
		Client cl = null;
		
		try {
			sv = new StandardServer(new InetSocketAddress("localhost", 3000), null);
			sv.getParameters().setAlivePeriod(0);
			sv.start();
			
			// set up a running connection
			cl = new Client();
			cl.getParameters().setAlivePeriod(5000);
			cl.getParameters().setMaxSerialisationSize(1000);
			cl.connect(100, sv.getSocketAddress());
			System.out.println("-- connection established " + cl.toString());
			Util.sleep(50);
			
			ServerConnection scon = (ServerConnection)sv.getConnections()[0];
			int speed = 20000;
			scon.setTempo(speed);
			scon.getParameters().setMaxSerialisationSize(2000);
			Util.sleep(10);

			// prepare random data block to transmit
			int dataLen = 100000;
			byte[] block = Util.randBytes(dataLen);
			
			// start sending from client
			cl.sendData(block, 0, dataLen, SendPriority.Normal);
//				System.out.println("-- EXCEPTION thrown for SEND-DATA on client");
			
			// start sending from server
//			scon.sendData(block, 0, dataLen, SendPriority.Normal);
			Util.sleep(1000);
			
			assertTrue("expected connection closed (client)", cl.isClosed());
			assertTrue("expected connection closed (server)", scon.isClosed());
			
			// TODO test exception type via connection listener
			
		// shutdown net systems
		} finally {
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
