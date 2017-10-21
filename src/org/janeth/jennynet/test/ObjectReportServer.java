package org.janeth.jennynet.test;

import java.io.IOException;
import java.net.SocketAddress;

import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.intfa.TransmissionEvent;

public class ObjectReportServer extends Server {

   
   public ObjectReportServer() throws IOException {
   }

   public ObjectReportServer(SocketAddress address) throws IOException {
      super(address);
   }

   public ObjectReportServer(int port) throws IOException {
      super(port);
   }


//  ************ INNER CLASSES *************
   
private static class SvListener extends DefaultServerListener {
   ConnectionListener conListener = new ServerConListener();
   
   public SvListener () {
   }
   
   @Override
   public void connectionAvailable (IServer server, ServerConnection connection) {
      try {
         connection.addListener(conListener);
         connection.start();
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



private static class ServerConListener extends DefaultConnectionListener {

   @Override
   public void objectReceived (Connection connection, long objectNr, Object object) {
      if (object instanceof String) {
         System.out.println("-- RECEIVED STRING == [" + (String)object + "]");
      }
   }

   @Override
   public void disconnected (Connection connection, int cause, String message) {
   }

   @Override
   public void transmissionEventOccurred (TransmissionEvent event) {
   }

}

   
   
}
