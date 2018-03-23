package org.janeth.jennynet.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_Events {

	Timer timer = new Timer();
	
	private class ClosureListener extends DefaultConnectionListener {
		int counter;

		@Override
		public void disconnected(Connection connection, int cause, String message) {
			counter++;
		}
	}
	
	@Test
	public void client_closure () throws IOException {
		Server sv = null;
		Client cl = null;
		
//		final Object lock = new Object();
		final ClosureListener clientListener = new ClosureListener();
		final ObjectReceptionListener receptionListener = new ObjectReceptionListener();
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), 
				new EventReporter(), receptionListener);
		sv.getParameters().setAlivePeriod(0);
		sv.start();
		
		// set up a running connection
		cl = new Client();
		cl.getParameters().setAlivePeriod(0);
		cl.addListener(clientListener);
		cl.connect(100, sv.getSocketAddress());
		System.out.println("-- connection established " + cl.toString());
		Util.sleep(50);
		
		// send an object
		byte[] data = Util.randBytes(200000);
		cl.sendData(data, 0, data.length, SendPriority.Normal);
//		Util.sleep(1000);
		
		// close connection and wait
		cl.close();
		long mark = System.currentTimeMillis();
		cl.waitForDisconnect(0);
		long time = System.currentTimeMillis() - mark;
		
		// test client state
		assertTrue("client not closed after shutdown", cl.isClosed());
		assertFalse("client still connected after shutdown", cl.isConnected());
		
		// test for object received
		assertTrue("object not delivered to remote", receptionListener.getSize() == 1);
		
		// test for proper sequence of events
		assertTrue("disconnect event out of sequence", clientListener.counter == 1);
		System.out.println("-- waited for DISCONNECT: " + time + " ms");
		
		// shutdown net systems
		} catch (InterruptedException e) {
			fail("wait interrupted");
			
		} finally {
			System.out.println("--- System Memory : " + Runtime.getRuntime().freeMemory() + " / " 
					+ Runtime.getRuntime().totalMemory());
			if (sv != null) {
				sv.closeAllConnections();
				sv.close();
				Util.sleep(50);
			}
		}
		
	}
	
	
	
}
