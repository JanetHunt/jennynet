package org.janeth.jennynet.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.SendPriority;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.core.Signal;
import org.janeth.jennynet.core.SignalType;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.test.FakeConnection.Action;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_Server_Send {

   
private class TestServerListener extends DefaultServerListener  {
   private int errorCt;
   private Throwable lastError;
   private int errorTransaction;
   private Connection errorConnection;
   
   
   public int getErrorCt () {
      return errorCt;
   }

   public Throwable getLastError () {
      return lastError;
   }

   public int getErrorTransaction () {
      return errorTransaction;
   }

   public Connection getErrorConnection () {
      return errorConnection;
   }

   @Override
   public void errorOccurred (IServer server, Connection con, int transAction, Throwable e) {
      errorConnection = con;
      lastError = e;
      errorTransaction = transAction;
      errorCt++;
   }
}
      
private FakeConnection[] addConnectionsToServer (IServer server, int size) {
   FakeServerConnection con, conArr[];
   
   // create and add an array of connections
   List<ServerConnection> list = new ArrayList<ServerConnection>();
   for (int i = 0; i < size; i++) {
      con = new FakeServerConnection(server);
      list.add(con);
      server.addConnection(con);
   }
   conArr = list.toArray(new FakeServerConnection[list.size()]);
   assertTrue("server init error: set of connections", server.getConnections().length == size );
   return conArr;
}
   
@Test
public void test_multi_send_object () {
   IServer sv1=null;
   FakeConnection conArr[];
   TestServerListener svListener;
   int size, taNumber, nr2;
   
   try {
      // create server with an array of connections
      sv1 = new Server(3000);
      svListener = new TestServerListener();
      sv1.addListener(svListener);
      size = 10;
      conArr = addConnectionsToServer(sv1, size);
      
      // test sendObject relation
      Object sendObject = new Object();
      sv1.sendObjectToAll(sendObject, SendPriority.Normal);
      
      for (FakeConnection fc : conArr) {
         assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.OBJECT) == 1);
         assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendObject);
      }
      
      // test sendObject EXCEPT ONE relation
      sendObject = new Object();
      UUID exceptId = conArr[3].getUUID();
      sv1.sendObjectToAllExcept(exceptId, sendObject, SendPriority.Normal);
      
      for (FakeConnection fc : conArr) {
         if (fc.getUUID().equals(exceptId)) {
            // control not executed connection (one)
            assertTrue("incorrect send_object execution", 
                  fc.getCounterValue(Action.OBJECT) == 1);
            assertFalse("illegal object transmission", fc.getLastSendObject() == sendObject);
         } else {
            // control executed connections
            assertTrue("missing send_object execution in connection", 
                  fc.getCounterValue(Action.OBJECT) == 2);
            assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendObject);
         }
      }
      
      // test error reporting (single error)
      int conPos = 3;
      conArr[conPos].setErrorMaker(true);
      assertTrue("server listener error counter should be 0", svListener.getErrorCt() == 0);
      
      // reset connection array
      for (FakeConnection fc : conArr) {
         fc.resetActionCounter();
      }      
      
      // test sendObject relation
      sendObject = new Object();
      taNumber = sv1.sendObjectToAll(sendObject, SendPriority.Normal);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 1);
      assertTrue("failed server listener error connection", svListener.getErrorConnection()
            == conArr[conPos]);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == taNumber);
      assertTrue("failed server listener error Throwable", svListener.getLastError().getClass()
            .equals(IllegalStateException.class));
      
      int count = 0;
      for (FakeConnection fc : conArr) {
         if (count++ != conPos) {
            assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.OBJECT) == 1);
            assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendObject);
         }
      }
      
      assertTrue("false send_object execution in error loaded connection", 
            conArr[conPos].getCounterValue(Action.OBJECT) == 0);
      
      
      // test second sendObject and transaction number
      sendObject = new Object();
      nr2 = sv1.sendObjectToAll(sendObject, SendPriority.Normal);
      assertTrue("failing sequential transaction number", nr2 == taNumber+1);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 2);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == nr2);
      
   } catch (Exception e) {
      e.printStackTrace();
      fail("TEST MULTIPLEX SEND OBJECT: " + e);
   } finally {
      sv1.closeAllConnections();
      sv1.close();
      sleep(30);
   }
}

@Test
public void test_multi_send_file () {
   IServer sv1=null;
   FakeConnection conArr[];
   TestServerListener svListener;
   int size, taNumber, nr2;
   String pathInfo;
   
   try {
      // create server with an array of connections
      sv1 = new Server(3000);
      svListener = new TestServerListener();
      sv1.addListener(svListener);
      size = 10;
      conArr = addConnectionsToServer(sv1, size);
      
      // test sendObject relation
      File sendFile = Util.getTempFile();
      pathInfo = "test/jenny.dot";
      sv1.sendFileToAll(sendFile, pathInfo, SendPriority.Normal);
      
      for (FakeConnection fc : conArr) {
         assertTrue("missing send_file execution in connection", 
               fc.getCounterValue(Action.FILE) == 1);
         assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendFile);
      }
      
      // test sendFile EXCEPT ONE relation
      sendFile = Util.getTempFile();
      UUID exceptId = conArr[3].getUUID();
      sv1.sendFileToAllExcept(exceptId, sendFile, pathInfo, SendPriority.Normal);
      
      for (FakeConnection fc : conArr) {
         if (fc.getUUID().equals(exceptId)) {
            // control not executed connection (one)
            assertTrue("incorrect send_object execution", 
                  fc.getCounterValue(Action.FILE) == 1);
            assertFalse("illegal object transmission", fc.getLastSendObject() == sendFile);
         } else {
            // control executed connections
            assertTrue("missing send_object execution in connection", 
                  fc.getCounterValue(Action.FILE) == 2);
            assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendFile);
         }
      }
      
      // test error reporting (single error)
      int conPos = 3;
      conArr[conPos].setErrorMaker(true);
      assertTrue("server listener error counter should be 0", svListener.getErrorCt() == 0);
      
      // reset connection array
      for (FakeConnection fc : conArr) {
         fc.resetActionCounter();
      }      
      
      // test sendObject relation
      sendFile = Util.getTempFile();
      taNumber = sv1.sendFileToAll(sendFile, pathInfo, SendPriority.Normal);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 1);
      assertTrue("failed server listener error connection", svListener.getErrorConnection()
            == conArr[conPos]);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == taNumber);
      assertTrue("failed server listener error Throwable", svListener.getLastError().getClass()
            .equals(IOException.class));
      
      int count = 0;
      for (FakeConnection fc : conArr) {
         if (count++ != conPos) {
            assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.FILE) == 1);
            assertTrue("missing or incorrect object transmission", fc.getLastSendObject() == sendFile);
         }
      }
      
      assertTrue("false send_object execution in error loaded connection", 
            conArr[conPos].getCounterValue(Action.FILE) == 0);
      
      
      // test second sendObject and transaction number
      nr2 = sv1.sendFileToAll(sendFile, pathInfo, SendPriority.Normal);
      assertTrue("failing sequential transaction number", nr2 == taNumber+1);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 2);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == nr2);
      
   } catch (Exception e) {
      e.printStackTrace();
      fail("TEST MULTIPLEX SEND FILE: " + e);
   } finally {
      sv1.closeAllConnections();
      sv1.close();
      sleep(30);
   }
}

@Test
public void test_multi_send_ping () {
   IServer sv1=null;
   FakeConnection conArr[];
   TestServerListener svListener;
   int size, baud, taNumber, nr2;
   
   try {
      // create server with an array of connections
      sv1 = new Server(3000);
      svListener = new TestServerListener();
      sv1.addListener(svListener);
      size = 10;
      conArr = addConnectionsToServer(sv1, size);
      
      // test sendObject relation
      sv1.sendPingToAll();
      
      for (FakeConnection fc : conArr) {
         assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.PING) == 1);
         assertTrue("missing or incorrect TEMPO value transmission", 
               ((Signal)fc.getLastSendObject()).getSigType() == SignalType.PING); 
      }
      
      // test error reporting (single error)
      int conPos = 3;
      conArr[conPos].setErrorMaker(true);
      assertTrue("server listener error counter should be 0", svListener.getErrorCt() == 0);
      
      // reset connection array
      for (FakeConnection fc : conArr) {
         fc.resetActionCounter();
      }      
      
      // test sendObject relation
      taNumber = sv1.sendPingToAll();
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 1);
      assertTrue("failed server listener error connection", svListener.getErrorConnection()
            == conArr[conPos]);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == taNumber);
      assertTrue("failed server listener error Throwable", svListener.getLastError().getClass()
            .equals(IllegalStateException.class));
      
      int count = 0;
      for (FakeConnection fc : conArr) {
         if (count++ != conPos) {
            assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.PING) == 1);
            assertTrue("missing or incorrect object transmission", 
               ((Signal)fc.getLastSendObject()).getSigType() == SignalType.PING); 
         }
      }
      
      assertTrue("false set_tempo execution in error loaded connection", 
            conArr[conPos].getCounterValue(Action.PING) == 0);
      
      
      // test second sendObject and transaction number
      nr2 = sv1.sendPingToAll();
      assertTrue("failing sequential transaction number", nr2 == taNumber+1);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 2);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == nr2);
      
   } catch (Exception e) {
      e.printStackTrace();
      fail("TEST MULTIPLEX SEND PING: " + e);
   } finally {
      sv1.closeAllConnections();
      sv1.close();
      sleep(30);
   }
}

@Test
public void test_multi_send_tempo () {
   IServer sv1=null;
   FakeConnection conArr[];
   TestServerListener svListener;
   int size, baud, taNumber, nr2;
   
   try {
      // create server with an array of connections
      sv1 = new Server(3000);
      svListener = new TestServerListener();
      sv1.addListener(svListener);
      size = 10;
      conArr = addConnectionsToServer(sv1, size);
      
      // test sendObject relation
      baud = 2400;
      sv1.sendTempoToAll(baud);
      
      for (FakeConnection fc : conArr) {
         assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.TEMPO) == 1);
         assertTrue("missing or incorrect TEMPO value transmission", 
               ((Integer)fc.getLastSendObject()).intValue() == baud); 
      }
      
      // test error reporting (single error)
      int conPos = 3;
      conArr[conPos].setErrorMaker(true);
      assertTrue("server listener error counter should be 0", svListener.getErrorCt() == 0);
      
      // reset connection array
      for (FakeConnection fc : conArr) {
         fc.resetActionCounter();
      }      
      
      // test sendObject relation
      baud = 4800;
      taNumber = sv1.sendTempoToAll(baud);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 1);
      assertTrue("failed server listener error connection", svListener.getErrorConnection()
            == conArr[conPos]);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == taNumber);
      assertTrue("failed server listener error Throwable", svListener.getLastError().getClass()
            .equals(IllegalStateException.class));
      
      int count = 0;
      for (FakeConnection fc : conArr) {
         if (count++ != conPos) {
            assertTrue("missing send_object execution in connection", 
               fc.getCounterValue(Action.TEMPO) == 1);
            assertTrue("missing or incorrect object transmission", 
               ((Integer)fc.getLastSendObject()).intValue() == baud); 
         }
      }
      
      assertTrue("false set_tempo execution in error loaded connection", 
            conArr[conPos].getCounterValue(Action.TEMPO) == 0);
      
      
      // test second sendObject and transaction number
      nr2 = sv1.sendTempoToAll(baud);
      assertTrue("failing sequential transaction number", nr2 == taNumber+1);
      
      // verify server listener error reporting
      assertTrue("failed server listener error reporting", svListener.getErrorCt() == 2);
      assertTrue("failed server listener error transaction", svListener.getErrorTransaction()
            == nr2);
      
   } catch (Exception e) {
      e.printStackTrace();
      fail("TEST MULTIPLEX SEND TEMPO: " + e);
   } finally {
      sv1.closeAllConnections();
      sv1.close();
      sleep(30);
   }
}

private static void sleep (int millis) {
   try {
      Thread.sleep(millis);
   } catch (InterruptedException e) {
      e.printStackTrace();
   }
}


}
