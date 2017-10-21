package org.janeth.jennynet.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.ConnectionParameters;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.Serialization;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.util.Util;
import org.junit.Test;

public class TestUnit_Connection_Static {
   UUID randomUuid = UUID.randomUUID();
   
   static Server testServer;
   static int ListenerIdCounter;

   private static class TClient extends Client {

      @Override
      protected ConnectionListener[] getListeners () {
         return super.getListeners();
      }
      
      public int getListenerCount () {
         return getListeners().length;
      }
   }
   
   private static class ConListener extends DefaultConnectionListener {
      int id = ++ListenerIdCounter;
      
      @Override
      public void objectReceived (Connection connection, long objectNr, Object object) {
         if (object instanceof String) {
            System.out.println("-- (L" + id + ") RECEIVED STRING == [" + (String)object + "]");
         }
      }
   }

   private static class SvListener extends DefaultServerListener {
      /** operation modus for answering incoming connections.
       * 0 = reject; 1 = accept.
       */
      int modus;
      ConnectionListener conListener = new ConListener();
      
      /** milliseconds of delay before connection is started or rejected. 
       */
      int delay;
      
      public SvListener (int modus) {
         this(modus, 0);
      }
      
      public SvListener (int modus, int delay) {
         this.modus = modus;
         this.delay = delay;
      }
      
      @Override
      public void connectionAvailable (IServer server, ServerConnection connection) {
         try {
            Util.sleep(delay);
            
            if ( modus == 1 ) {
               connection.addListener(conListener);
               connection.start();
            } else {
               connection.reject();
            }
         } catch (Exception e) {
            System.out.println("*** SERVER-LISTENER ERROR: ***");
            e.printStackTrace();
         }
      }

      @Override
      public void errorOccurred (IServer server, Connection con, int transAction, Throwable e) {
         super.errorOccurred(server, con, transAction, e);
      }
      
   }
   
   static {
      try {
         // creates a test server with listener which starts connections
         testServer = new Server(new InetSocketAddress("localhost", 4000));
         testServer.start();
         testServer.addListener(new SvListener(1));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   /** Tests initial features of a server.
    * @param cli Client
    */
   private void initial_feature_test (Connection cli) {
      assertFalse("not in closed state", cli.isClosed());
      assertFalse("not in idle state", cli.isIdle());
      assertFalse("not in transmitting state", cli.isTransmitting());
      assertNull("getName should be null", cli.getName());
      assertTrue("equals itself", cli.equals(cli));
      assertNotNull("has UUID (initial)", cli.getUUID());
      assertNotNull("has short-ID (initial)", cli.getShortId());
      assertTrue("illegal short-ID length (initial)", cli.getShortId().length == 4);
      assertNotNull("has parameters (initial)", cli.getParameters());
      assertNotNull("has receive serialisation (initial)", cli.getReceiveSerialization());
      assertNotNull("has send serialisation (initial)", cli.getSendSerialization());
      }

   private void test_can_set_parameter_values (ConnectionParameters par, boolean connected) {
      String errorMsg = "cannot set parameter value";
      
      try {
         int newBaseThreadPrio = par.getBaseThreadPriority() + 2;
         par.setBaseThreadPriority(newBaseThreadPrio);
         assertTrue(errorMsg, par.getBaseThreadPriority() == newBaseThreadPrio);
         
         int newTransmitThreadPrio = par.getBaseThreadPriority() - 2;
         par.setTransmitThreadPriority(newTransmitThreadPrio);
         assertTrue(errorMsg, par.getTransmitThreadPriority() == newTransmitThreadPrio);
         
         int newTransmissionParcelSize = par.getTransmissionParcelSize() + 23000;
         par.setTransmissionParcelSize(newTransmissionParcelSize);
         assertTrue(errorMsg, par.getTransmissionParcelSize() == newTransmissionParcelSize);
         
         int newTransmissionSpeed = 23000;
         par.setTransmissionSpeed(newTransmissionSpeed);
         assertTrue(errorMsg, par.getTransmissionSpeed() == newTransmissionSpeed);
         
         int newIdleThreshold = par.getIdleThreshold() + 1000;
         par.setIdleThreshold(newIdleThreshold);
         assertTrue(errorMsg, par.getIdleThreshold() == newIdleThreshold);
         
         int newConfirmTimeout = par.getConfirmTimeout() + 11034;
         par.setConfirmTimeout(newConfirmTimeout);
         assertTrue(errorMsg, par.getConfirmTimeout() == newConfirmTimeout);
         
         File newTransferRoot = JennyNet.getDefaultTempDirectory(); 
         assertFalse("fails to find new root directory", newTransferRoot.equals(par.getFileRootDir()));
         par.setFileRootDir(newTransferRoot);
         assertTrue(errorMsg, par.getFileRootDir().equals(newTransferRoot));
         
         if (!connected) {
            int newObjectQueueCapacity = par.getObjectQueueCapacity() + 155; 
            par.setObjectQueueCapacity(newObjectQueueCapacity);
            assertTrue(errorMsg, par.getObjectQueueCapacity() == newObjectQueueCapacity);
   
            int newParcelQueueCapacity = par.getParcelQueueCapacity() + 55; 
            par.setParcelQueueCapacity(newParcelQueueCapacity);
            assertTrue(errorMsg, par.getParcelQueueCapacity() == newParcelQueueCapacity);
   
            int newAlivePeriod = par.getAlivePeriod() + 30200; 
            par.setAlivePeriod(newAlivePeriod);
            assertTrue(errorMsg, par.getAlivePeriod() == newAlivePeriod);
   
            Charset newCodingCharset = Charset.forName("ASCII"); 
            par.setCodingCharset(newCodingCharset);
            assertTrue(errorMsg, par.getCodingCharset().equals(newCodingCharset));
         }
         
      } catch (Exception e) {
         e.printStackTrace();
         fail("Exception in CAN SET PARAMETER VALUES");
      }
   }
   
   private void test_parameters_con(Connection cl1, boolean connected) throws IOException {
      // test that we can set a parameter set
      ConnectionParameters origPar = cl1.getParameters(); 
      assertNotNull("con has no parameters", origPar);
      ConnectionParameters par1 = (ConnectionParameters)origPar.clone();

      // test setting of new values (single mode)
      test_can_set_parameter_values(origPar, connected);
      
      // clone is data-separate
      assertFalse("parameter-clone is not data separate", origPar.getIdleThreshold() 
                  == par1.getIdleThreshold());
      assertFalse("parameter-clone is not data separate", origPar.getTransmissionParcelSize() 
            == par1.getTransmissionParcelSize());

      // in CONNECTED status: test parameter setting (set mode)
      if (!connected) {
         cl1.setParameters(par1);
         ConnectionParameters par2 = cl1.getParameters(); 
         
         // new parameter values (all options) are set on connection
         assertTrue("failed parameter set assignment", par2.getAlivePeriod() 
               == par1.getAlivePeriod());
         assertTrue("failed parameter set assignment", par2.getBaseThreadPriority() 
               == par1.getBaseThreadPriority());
         assertTrue("failed parameter set assignment", par2.getCodingCharset().equals 
               (par1.getCodingCharset()));
         assertTrue("failed parameter set assignment", par2.getConfirmTimeout() 
               == par1.getConfirmTimeout());
         if (par1.getFileRootDir() != null) {
            assertTrue("failed parameter set assignment", par1.getFileRootDir().equals 
                  (par2.getFileRootDir()));
         }
         assertTrue("failed parameter set assignment", par2.getIdleThreshold() 
               == par1.getIdleThreshold());
         assertTrue("failed parameter set assignment", par2.getObjectQueueCapacity() 
               == par1.getObjectQueueCapacity());
         assertTrue("failed parameter set assignment", par2.getParcelQueueCapacity() 
               == par1.getParcelQueueCapacity());
         assertTrue("failed parameter set assignment", par2.getSerialisationMethod() 
               == par1.getSerialisationMethod());
         assertTrue("failed parameter set assignment", par2.getTempDirectory() 
               .equals(par1.getTempDirectory()));
         assertTrue("failed parameter set assignment", par2.getTransmissionParcelSize() 
               == par1.getTransmissionParcelSize());
         assertTrue("failed parameter set assignment", par2.getTransmitThreadPriority() 
               == par1.getTransmitThreadPriority());
         assertTrue("failed parameter set assignment", par2.getTransmissionSpeed() 
                 == par1.getTransmissionSpeed());

         // assign-set is not equal to connection owned set
         assertFalse("connection falsely took over assign-set", par2 == par1);
      }
   }
   
   @Test
   public void test_parameters () {
      Client cl1 = null;
      
      try {
         cl1 = new Client();
         initial_feature_test(cl1);
         test_parameters_con(cl1, false);
         
         cl1.connect(0, "localhost", 4000);
         test_parameters_con(cl1, true);

         cl1.close();
      } catch (Exception e) {
         e.printStackTrace();
         fail("TEST PARAMETERS EXCEPTION");
      }
   }
   
   
   @Test
   public void test_parameters_failure () throws IOException {
      Client cl1 = null;
      cl1 = new Client();
      
      try {
         cl1.setParameters(null);
         fail("fails to throw exception on null parameter");
      } catch (Exception e) {
         assertTrue("false exception thrown (expected: NullPointerException)", 
               e instanceof NullPointerException);
      }
      
      cl1.connect(0, "localhost", 4000);
      
      // cannot set parameters when connection established
      try {
         cl1.setParameters(JennyNet.getParameters());
         fail("fails to throw exception on setParameters()");
      } catch (Exception e) {
         assertTrue("false exception thrown (expected: IllegalStateException)", 
               e instanceof IllegalStateException);
      }
      
      // cannot set some single parameters when connection established
      try {
         cl1.getParameters().setObjectQueueCapacity(23);
         fail("fails to throw exception on setParameters()");
      } catch (Exception e) {
         assertTrue("false exception thrown (expected: IllegalStateException)", 
               e instanceof IllegalStateException);
      }
      
      try {
         cl1.getParameters().setParcelQueueCapacity(24);
         fail("fails to throw exception on setParameters()");
      } catch (Exception e) {
         assertTrue("false exception thrown (expected: IllegalStateException)", 
               e instanceof IllegalStateException);
      }
      
      try {
         cl1.getParameters().setCodingCharset(null);
         fail("fails to throw exception on setParameters()");
      } catch (Exception e) {
         assertTrue("false exception thrown (expected: IllegalStateException)", 
               e instanceof IllegalStateException);
      }
      
      cl1.close();
   }

   @Test
   public void test_serialisation_setting () {
      Serialization sendSer1, sendSer2, recSer1, recSer2;
      Client cl1 = null;
      cl1 = new Client();
      
      try {
         // test we have default serialisations after instantiation 
         sendSer1 = cl1.getSendSerialization();
         recSer1 = cl1.getReceiveSerialization();
         assertNotNull("con has no default send serialisation", sendSer1);
         assertNotNull("con has no default receive serialisation", recSer1);

         // test we can set clones of some existing serialisations
         sendSer2 = JennyNet.getGlobalSerialisation().copy();
         recSer2 = sendSer2;
         assertFalse("should have new instance of ser (Global)", sendSer1 == sendSer2);
         assertFalse("should have new instance of ser (Global)", recSer1 == recSer2);
         
         // we also set some individual classes for serialisation
         Class<?> clas = Client.class;
         sendSer2.registerClass(clas);
         assertTrue("class not registered: Client", sendSer2.isRegisteredClass(clas));

         cl1.setSendSerialization(sendSer2);
         cl1.setReceiveSerialization(recSer2);
         sendSer1 = cl1.getSendSerialization();
         recSer1 = cl1.getReceiveSerialization();
         assertNotNull("con has not accepted new send serialisation", sendSer1);
         assertNotNull("con has no accepted new receive serialisation", recSer1);
         assertFalse("con does not copy serialisation (send)", sendSer1 == sendSer2);
         assertFalse("con does not copy serialisation (receive)", recSer1 == recSer2);
         assertFalse("should have different copies of sers", sendSer1 == recSer1);
         
         // verification of individual class registration (clone)
         assertTrue("class not registered: Client", sendSer1.isRegisteredClass(clas));
         assertTrue("class not registered: Client", recSer1.isRegisteredClass(clas));
         
         // test we have persistent ser instances in connection
         sendSer1 = cl1.getSendSerialization();
         sendSer2 = cl1.getSendSerialization();
         recSer1 = cl1.getReceiveSerialization();
         recSer2 = cl1.getReceiveSerialization();
         assertTrue("serialisation not persistent (send)", sendSer1 == sendSer2);
         assertTrue("serialisation not persistent (receive)", recSer1 == recSer2);
         
         // test we can add some individual classes in connection
         clas = Server.class;
         sendSer1.registerClass(clas);
         sendSer2 = cl1.getSendSerialization();
         recSer2 = cl1.getReceiveSerialization();
         assertTrue("class not registered in con: Server", sendSer2.isRegisteredClass(clas));
         assertFalse("class falsely registered in con (receive): Server", recSer2.isRegisteredClass(clas));
         assertFalse("verification on registration", sendSer2.isRegisteredClass(Socket.class));
         
      } catch (Exception e) {
         e.printStackTrace();
         fail("TEST SERIALISATION SETTING EXCEPTION");
      }
   }
   
   @Test
   public void test_listener_registry () {
      TClient cl1 = null, cl2 = null;
      cl1 = new TClient();
      cl2 = new TClient();

//      byte[] shortID1 = cl1.getShortId();
//      byte[] shortID2 = cl2.getShortId();
      
      try {

         assertTrue("false listener initial size", cl1.getListenerCount() == 0);
         assertTrue("false listener initial size", cl2.getListenerCount() == 0);
         
         // test we can add a single listener on many connections 
         ConnectionListener li1 = new ConListener();
         cl1.addListener(li1);
         cl2.addListener(li1);
         assertTrue("could not add connection listener (1)", cl1.getListenerCount() == 1);
         assertTrue("connection list entry failed (1)", 
               Arrays.asList(cl1.getListeners()).contains(li1));
         assertTrue("could not add connection listener (2)", cl2.getListenerCount() == 1);
         assertTrue("connection list entry failed (2)", 
               Arrays.asList(cl2.getListeners()).contains(li1));
         
         // test we can add more than one listener (and still keep what we've got)
         ConnectionListener li2 = new ConListener();
         cl1.addListener(li2);
         cl2.addListener(li2);
         assertTrue("could not add connection listener (12)", cl1.getListenerCount() == 2);
         assertTrue("connection list entry failed (12)", 
               Arrays.asList(cl1.getListeners()).contains(li2));
         assertTrue("connection list entry lost (1)", 
               Arrays.asList(cl1.getListeners()).contains(li1));
         assertTrue("could not add connection listener (22)", cl2.getListenerCount() == 2);
         assertTrue("connection list entry failed (22)", 
               Arrays.asList(cl2.getListeners()).contains(li2));
         assertTrue("connection list entry lost (2)", 
               Arrays.asList(cl2.getListeners()).contains(li1));
         
         // test we cannot add one listener more than once
         cl1.addListener(li1);
         assertTrue("falsely added one con-listener twice", cl1.getListenerCount() == 2);
         
         // test we can remove listeners correctly
         cl1.removeListener(li1);
         cl2.removeListener(li1);
         assertTrue("could not remove connection listener (1)", cl1.getListenerCount() == 1);
         assertTrue("could not remove connection listener (2)", cl2.getListenerCount() == 1);
         assertTrue("connection listener false entry removed (1)", 
               Arrays.asList(cl1.getListeners()).contains(li2));
         assertTrue("connection listener false entry removed (2)", 
               Arrays.asList(cl2.getListeners()).contains(li2));
         
         // test we can remove more than once (no-operation)
         cl1.removeListener(li1);
         assertTrue("false list condition after second remove attempt", cl1.getListenerCount() == 1);
         
         // neutral null test
         cl1.removeListener(null);
         assertTrue("false list condition after null remove", cl1.getListenerCount() == 1);
         cl1.addListener(null);
         assertTrue("false list condition after null adding", cl1.getListenerCount() == 1);
         
         
      } catch (Exception e) {
         e.printStackTrace();
         fail("TEST LISTENERS EXCEPTION");
      } finally {
         cl1.close();
         cl2.close();
      }
   }
   
   @Test
   public void test_minors () {
      Client cl1 = null, cl2 = null;
      cl1 = new Client();
      cl2 = new Client();

      UUID uuid = cl1.getUUID();
      byte[] shortID = cl1.getShortId();
      
      try {

         // initial minors
         assertFalse("not in closed state", cl1.isClosed());
         assertFalse("not in connected state", cl1.isConnected());
         assertFalse("not in idle state", cl1.isIdle());
         assertFalse("not in transmitting state", cl1.isTransmitting());
         assertNull("getName should be null", cl1.getName());
         assertNotNull("has UUID (initial)", cl1.getUUID());
         assertNotNull("has short-ID (initial)", cl1.getShortId());
         assertTrue("illegal short-ID length (initial)", cl1.getShortId().length == 4);
         assertNotNull("has Properties", cl1.getProperties());
         assertNull("has no local address (initial)", cl1.getLocalAddress());
         assertNull("has no remote address (initial)", cl1.getRemoteAddress());
         assertTrue("false initial transmission speed", cl1.getTransmissionSpeed() == -1);
         assertTrue("false initial transmission volume", cl1.getTransmissionVolume() == 0);
         assertTrue("has no last-send-time", cl1.getLastSendTime() == 0);
         assertTrue("has no last-receive-time", cl1.getLastReceiveTime() == 0);
         assertTrue("equals itself", cl1.equals(cl1));
         assertTrue("equals another null-defined connection", cl1.equals(cl2));
         assertTrue("hascode identical with another null-defined connection",  
                    cl1.hashCode() == cl2.hashCode()); 
         assertTrue("toString() makes a value", cl1.toString().length() > 10);

         // test after binding
         cl1.bind(0);
         assertFalse("not in closed state", cl1.isClosed());
         assertFalse("not in connected state", cl1.isConnected());
         assertFalse("not in idle state", cl1.isIdle());
         assertNotNull("has local address (initial)", cl1.getLocalAddress());
         assertNull("has no remote address (initial)", cl1.getRemoteAddress());
         assertTrue("equals itself", cl1.equals(cl1));
         assertFalse("not equals another null-defined connection", cl1.equals(cl2));
         assertFalse("hascode not identical with null-defined connection",  
               cl1.hashCode() == cl2.hashCode()); 
         
         // test after connecting
         cl1.connect(0, "localhost", 4000);
         cl2.connect(0, "localhost", 4000);
         assertFalse("not in closed state", cl1.isClosed());
         assertFalse("not in closed state", cl2.isClosed());
         assertTrue("in connected state", cl1.isConnected());
         assertTrue("in connected state", cl2.isConnected());
         assertFalse("not in idle state", cl1.isIdle());
         assertFalse("not in idle state", cl2.isIdle());
         assertFalse("not in transmitting state", cl1.isTransmitting());
         assertNull("getName should be null", cl1.getName());
         assertNotNull("has UUID (initial)", cl1.getUUID());
         assertNotNull("has short-ID (initial)", cl1.getShortId());
         assertTrue("illegal short-ID length (initial)", cl1.getShortId().length == 4);
         assertNotNull("has Properties", cl1.getProperties());
         assertTrue("false transmission speed", cl1.getTransmissionSpeed() == -1);
         assertTrue("false transmission volume", cl1.getTransmissionVolume() == 0);
         assertNotNull("has local address (initial)", cl1.getLocalAddress());
         assertNotNull("has remote address (initial)", cl1.getRemoteAddress());
         assertTrue("has no last-send-time", cl1.getLastSendTime() == 0);
         assertTrue("has no last-receive-time", cl1.getLastReceiveTime() == 0);
         assertTrue("equals itself", cl1.equals(cl1));
         assertFalse("not equals another connection", cl1.equals(cl2));
         assertFalse("hascode not identical with connection",  
               cl1.hashCode() == cl2.hashCode()); 

         // separation tests
         assertFalse("has different Properties instance", cl1.getProperties() 
               == cl2.getProperties());
         assertFalse("has different UUID", cl1.getUUID().equals(cl2.getUUID())); 
         assertFalse("has different Short-ID", Util.equalArrays(cl1.getShortId(), cl2.getShortId())); 
         assertFalse("has different local address", cl1.getLocalAddress().
               equals(cl2.getLocalAddress())); 
         assertTrue("has same remote address", cl1.getRemoteAddress().
               equals(cl2.getRemoteAddress())); 
         assertFalse("has different toString rendering", cl1.toString().equals(cl2.toString())); 

         // test set name
         String name = "Ohuwabohu";
         cl1.setName(name);
         cl2.setName("Oxenheimer");
         assertTrue("remembers name setting", name.equals(cl1.getName()));
         
         // test Properties
         cl1.getProperties().setProperty("key1", "Hello");
         cl2.getProperties().setProperty("key1", "NoNo");
         assertTrue("Properties remembers key-value pair", cl1.getProperties().
               getProperty("key1").equals("Hello"));
         assertTrue("Properties remembers key-value pair", cl2.getProperties().
               getProperty("key1").equals("NoNo"));
         
         // send action
         long timeNow = System.currentTimeMillis();
         cl1.sendObject("Hello Other End!");
         cl2.sendObject("My Fair Lady");
         Util.sleep(60);
         
         // test after sending
         assertFalse("not in closed state", cl1.isClosed());
         assertTrue("in connected state", cl1.isConnected());
         assertFalse("not in idle state", cl1.isIdle());
         assertTrue("in transmitting state", cl1.isTransmitting());
         assertTrue("has last-send-time", cl1.getLastSendTime() > 0);
         assertTrue("credible last-send time", cl1.getLastSendTime()-timeNow <= 100);
         assertTrue("has no last-receive-time", cl1.getLastReceiveTime() == 0);

         cl1.close();
         cl2.close();
         Util.sleep(50);
         
         // test after closing
         assertTrue("in closed state", cl1.isClosed());
         assertFalse("not in connected state", cl1.isConnected());
         assertFalse("not in idle state", cl1.isIdle());
         assertFalse("not in transmitting state", cl1.isTransmitting());
         assertTrue("has last-send-time", cl1.getLastSendTime() > 0);
         assertTrue("has no last-receive-time", cl1.getLastReceiveTime() == 0);
         
         assertTrue("name persists close", cl1.getName().equals(name));
         assertNotNull("has UUID (closed)", cl1.getUUID());
         assertTrue("same UUID (closed)", cl1.getUUID().equals(uuid));
         assertNotNull("has short-ID (closed)", cl1.getShortId());
         assertTrue("same short-ID (closed)", Util.equalArrays(cl1.getShortId(), shortID));
         assertNotNull("has Properties", cl1.getProperties());
         assertNotNull("has local address (closed)", cl1.getLocalAddress());
         assertNotNull("has remote address (closed)", cl1.getRemoteAddress());
         assertTrue("equals itself", cl1.equals(cl1));
         assertFalse("not equals another closed connection", cl1.equals(cl2));
         assertFalse("hashcode not identical with another closed connection",  
                    cl1.hashCode() == cl2.hashCode()); 
         assertTrue("toString() makes a value (closed)", cl1.toString().length() > 10);
         
      } catch (Exception e) {
         e.printStackTrace();
         fail("TEST MINORS EXCEPTION");
      } finally {
         cl1.close();
         cl2.close();
      }
   }
   
}
