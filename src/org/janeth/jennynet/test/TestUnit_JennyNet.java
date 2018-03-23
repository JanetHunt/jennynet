package org.janeth.jennynet.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_JennyNet {
	
@Test
public void global_shutdown () throws IOException, InterruptedException {
	Server sv = null;
	Client cl1, cl2;
	ObjectReceptionListener receptionListener = new ObjectReceptionListener();
	
	try {
		sv = new StandardServer(new InetSocketAddress("localhost", 3000), 
				new EventReporter(), receptionListener);
		sv.start();
		
		cl1 = new Client();
		cl2 = new Client(6782);
		
		cl1.connect(0, sv.getSocketAddress());
		
		assertTrue("error in number of active clients", JennyNet.getNrOfClients() == 1);
		assertTrue("error in number of active servers", JennyNet.getNrOfServers() == 1);
		
		// sending of data to charge connection
		byte[] data = Util.randBytes(200000);
		cl1.sendData(data, 0, data.length, SendPriority.Normal);
//		Util.sleep(1000);
		
		// test layer shutdown
		JennyNet.shutdownAndWait(5000);
		assertTrue("error in global client set after shutdown", JennyNet.getNrOfClients() == 0);
		assertTrue("error in global server set after shutdown", JennyNet.getNrOfServers() == 0);
		assertFalse("server still alive after shutdown", sv.isAlive());
		assertTrue("server not closed after shutdown", sv.isClosed());
		assertTrue("server has open connections after shutdown", sv.getConnections().length == 0);

		assertTrue("client not closed after shutdown", cl1.isClosed());
		assertFalse("client still connected after shutdown", cl1.isConnected());
		
		// test data arrival
		assertTrue("data from client has not arrived", receptionListener.getSize() > 0);
		byte[] rece = receptionListener.getReceived().get(0);
		assertTrue("data not transferred correctly", Util.equalArrays(data, rece));
		
	} finally {
		if (sv != null) {
			sv.close();
			sv.closeAllConnections();
			Util.sleep(50);
		}
	}
}
	
@Test
public void global_client_list () throws IOException {
	Server sv = null;
	
	try {
	// test zero state (no connections)
	assertNotNull("no global client list available, initial", JennyNet.getGlobalClientSet());

	sv = new Server(0);
	assertTrue("false initial size of global con-list, 1", JennyNet.getNrOfClients() == 0);
	sv.addListener(new DefaultServerListener() {

		@Override
		public void connectionAvailable(IServer server, ServerConnection connection) {
			try {
				connection.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	});
	
	Client cl1 = new Client();
	assertTrue("false initial size of global con-list after client creation, 1", JennyNet.getNrOfClients() == 0);
	
	Client cl2 = new Client(3045);
	assertTrue("false initial size of global con-list after client creation, 2", JennyNet.getNrOfClients() == 0);
	
	// start server
	sv.start();
	assertTrue("false initial size of global con-lis after server start", JennyNet.getNrOfClients() == 0);
	
	// test in connected states client-1
	cl1.connect(0, sv.getSocketAddress());
	if (cl1.isConnected()) {
		assertTrue("false size of global client list, 1", JennyNet.getNrOfClients() == 1);
		assertNotNull("no global client list available", JennyNet.getGlobalClientSet());
		assertTrue("global list does not contain connection, 1", JennyNet.getGlobalClientSet().get(0) == cl1);
		
	} else {
		fail("unconnected client");
	}

	// test in connected states client-2
	cl2.connect(0, sv.getSocketAddress());
	if (cl2.isConnected()) {
		assertTrue("false size of global client list, 2", JennyNet.getNrOfClients() == 2);
		assertTrue("global list does not contain connection, 1", JennyNet.getGlobalClientSet().contains(cl2));
		
	} else {
		fail("unconnected client");
	}

	// test closing connection client-1
	cl1.close();
	if (cl1.isClosed()) {
		assertTrue("false size of global client list, 3", JennyNet.getNrOfClients() == 1);
		assertNotNull("no global client list available", JennyNet.getGlobalClientSet());
		assertFalse("global list contains connection after close", JennyNet.getGlobalClientSet().contains(cl1));
		
	} else {
		fail("client not disconnected: cl1");
	}
	
	// test closing via server close
	sv.closeAllConnections();
	Util.sleep(50);
	if (cl2.isClosed()) {
		assertTrue("false size of global client list, 3", JennyNet.getNrOfClients() == 0);
		assertNotNull("no global client list available", JennyNet.getGlobalClientSet());
		assertFalse("global list contains connection after close", JennyNet.getGlobalClientSet().contains(cl2));
		
	} else {
		fail("client not disconnected: cl2");
	}
	
	
	} finally {
		if (sv != null && sv.isAlive()) {
			sv.close();
			sv.closeAllConnections();
			Util.sleep(50);
		}
	}
}

}
