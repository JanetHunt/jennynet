package org.janeth.jennynet.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.IServer.SignalMethod;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.intfa.TransmissionEvent.TransmissionEventType;
    
   public class TestServer {

      static int clientCounter;
      static List<Connection> clientList = new ArrayList<Connection>();
      static Timer timer = new Timer();
      
      public static void main(String[] args) throws IOException {
    
       if (args.length != 1) {
           System.err.println("Usage: <program> <port number>");
           System.exit(1);
       }
    
//           JennyNet.setDefaultTransmissionRoot(new File("/media/422E-3CB5/"));
          
//           ClientListener clistener = new ClientListener();
           EventReporter reporter = new EventReporter();
           int portNumber = Integer.parseInt(args[0]);
           boolean listening = true;
            
           try {
              Server server = new Server();
              server.bind(portNumber);
              server.setSignalMethod(SignalMethod.Accept);
              server.setAcceptQueueCapacity(500);
              server.start();
              
              while (listening) {
                 ServerConnection con = server.accept(0);
                 con.addListener(reporter);
                 con.getParameters().setAlivePeriod(5000);
//                 con.getParameters().setAliveTimeout(10000);
//                 con.addListener(clistener);
                 clientList.add(con);
                 con.start();
                 
                 System.out.println("--- CONNECTION ACCEPTED from : " + con.getRemoteAddress() );
                 con.sendPing();
                 
//                  new ServerConnectionThread(server.accept()).start();
              }
           } catch (IOException e) {
               System.err.println("Could not listen on port " + portNumber);
               System.exit(-1);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }

      public static class AbortFiletransferTask extends TimerTask {
         private Connection connection;
         private long fileId;
         
         /** A new file transfer cancel task.
          * 
          * @param c IConnection
          * @param fileId long file transfer
          * @param time int delay in seconds
          */
         public AbortFiletransferTask (Connection c, long fileId, int time) {
            connection = c;
            this.fileId = fileId;
            timer.schedule(this, time * 1000);
         }
         
         @Override
         public void run() {
            System.out.println("TIMER-TASK: Cancelling incoming file transfer : " 
                  + connection.getRemoteAddress() + " FILE = " + fileId);
            connection.breakTransfer(fileId, 0);
         }
         
      }
      
      public static class ClientListener extends DefaultConnectionListener {

         @Override
         public void objectReceived(Connection connection, long objectNr, Object object) {
         }

         @SuppressWarnings("unused")
         @Override
         public void transmissionEventOccurred (TransmissionEvent event) {
            if (event.getType() == TransmissionEventType.FILE_INCOMING) {
               new AbortFiletransferTask(event.getConnection(), 
                     event.getObjectID(), 3);
            }
         }

         @Override
         public void disconnected(Connection connection, int info, String msg) {
            System.out.println("+++ Connection TERMINATED (" + connection.getUUID() + ") Address: " 
                          + connection.getRemoteAddress());
            if (info != 0) {
               System.out.print("    Reason = " + info + ",  ");
            }
            if (msg != null) {
               System.out.println("MSG = ".concat(msg));
            }
         }
         
      }
/*      
      public static class ServerConnectionThread extends Thread {
         private int id = ++clientCounter; 
         private Connection connection = null;
      
         public ServerConnectionThread(Connection con) {
             super("KKMultiServerThread");
             connection = con;
         }
          
         public void run() {
            InputStream socketInput;
            boolean closed = false;
            
            System.out.println("++++++ Connection +++++ (" + id + ") thread started: Address: " + socket.getRemoteSocketAddress() );
            
            connection.sendPing();
            connection.sendObject("Hello here is Testserver! What is your name?");

            try {
                 while (!closed) {
                    try {
                       // reads and reports all parcels sent from the client
//                       TransmissionParcel parcel = TransmissionParcel.readParcel(in);
//                       parcel.report(System.out);
                       
//                       if (parcel.getChannel() == TransmissionChannel.OBJECT) {
//                          Object obj = JennyNet.getGlobalSerialisation().deserialiseObject(parcel.getData());
//                          System.out.println(obj);
//                       }
                       
                    } catch (Exception e) {
                       closed = true;
                       if (!(e instanceof EOFException)) {
                          e.printStackTrace();
                       }
                    }
                 }
                 System.out.println("++ Connection TERMINATED (" + id + ") Address: " + socket.getRemoteSocketAddress() );
                 
             } catch (Exception e) {
                 System.out.println("++ Connection (" + id + ") EXCEPTION: " + e );
                 System.out.println();
                 e.printStackTrace();
             }
         }
     }
*/     
   }
   
   