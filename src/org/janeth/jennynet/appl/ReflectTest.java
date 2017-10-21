package org.janeth.jennynet.appl;

import java.io.File;
import java.io.IOException;

import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.IServer.SignalMethod;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.intfa.TransmissionEvent;

public class ReflectTest {
   public static final int BUILD_VERSION = 4;
   public static final int HOUR = 60*60*1000;
   int serverPort;
   int hours = 1;
   String rootPath;
   Thread activity;

   public ReflectTest (String[] args) {
      
      if (setup_parameters (args)) {
         try {
            activity = new Thread(new SimpleReflectServer(serverPort, hours*HOUR, rootPath));
            activity.start();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   
   private boolean setup_parameters (String[] args) {
      if (args.length < 1) {
         printUsage("Missing arguments. ");
         return false;
      }

      try { 
         serverPort = Integer.parseInt(args[0]);
      } catch (Exception e) {
         printUsage("Illegal characters in argument 0; must be numeric!");
         return false;
      }
      
      if (args.length > 1) {
         try { 
            hours = Integer.parseInt(args[1]);
         } catch (Exception e) {
            printUsage("Illegal characters in argument 1; must be numeric!");
            return false;
         }
      }

      if (args.length > 2) {
         try {
            String path = args[2];
            if (new File(path).isDirectory()) {
               rootPath = path;
            } else {
               printUsage("directory not found: ".concat(path));
               return false;
            }
         } catch (Exception e) {
            printUsage("Illegal characters in argument 2; must be a valid file path!");
            return false;
         }
      }
      return true;
   }

   /**
    * @param args
    */
   public static void main (String[] args) {
      
      new ReflectTest(args);
      
   }

   /** Prints a parameter usage declaration together with an optional
    * error message.
    * 
    * @param errorMessage Message to print before usage; (null to ignore)
    */
   private static void printUsage (String errorMessage) {
      String outputMessage = "Usage Parameters: <serverport> [<hours> [<root-directory>]] ";

      if (errorMessage != null) {
         // log.info("Error: " + errorMessage);
         outputMessage = "JennyNet ReflectTest *** Error: " + errorMessage + "\n\n" 
                         + outputMessage;
      }

      System.out.println(outputMessage);
   }

//  **************  INNER CLASSES  ***********
   
public static class SimpleReflectServer implements Runnable {
   IServer server = new Server();
   ConnectionListener connectionListener = new SimpleConnectionListener();
   int duration = 30*60*1000;
   String rootPath;
   
   public SimpleReflectServer () throws IOException {
      server.setSignalMethod(SignalMethod.Listener);
      server.setName("SIMPLE_REFLECT_SERVER");
      server.addListener(new DefaultServerListener() {

         @Override
         public void serverClosed (IServer server) {
            System.out.println(server.getName() + " has been closed");
         }
         
         @Override
         public void connectionAvailable (IServer server, ServerConnection con) {
            con.addListener(connectionListener);
            con.setName("Simple Reflect Connection");
            try {
               con.start();
               con.sendPing();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      });
   }

   /**
    * 
    * @param port int port number
    * @param duration int milliseconds of server ALIVE time
    * @param rootPath String the (existing) root directory for incoming file transmissions
    * @throws IOException if binding the port fails
    */
   public SimpleReflectServer (int port, int duration, String rootPath) 
         throws IOException {
      this();
      this.duration = duration;
      this.rootPath = rootPath;
//      this.duration = 6000;
      server.bind(port);
      server.setName(server.getName() + " (" + port + ")");
      if (rootPath != null) {
         JennyNet.setDefaultTransmissionRoot(new File(rootPath));
      }
   }
   
   @Override
   public void run () {
      System.out.println(server.getName() + " Build " + BUILD_VERSION + " started operating");
      server.start();
      try {
         Thread.sleep(duration);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      server.close();
      System.exit(0);
   }
   
   private class SimpleConnectionListener extends DefaultConnectionListener {

      @Override
      public void connected (Connection con) {
         System.out.println(con.toString().concat(" has been connected"));
      }

      @Override
      public void disconnected (Connection con, int info, String message) {
         String explain =  "reason : info=" + info + 
               (message != null ? ", ".concat(message) : "");
         System.out.println(con.toString() + " has been disconnected \n" + explain);
      }

      @Override
      public void objectReceived (Connection con, long objectNr, Object object) {
         String msg = "-- object received from " + con.getRemoteAddress()  
               + "\nser=" + objectNr + ", type=" + object.getClass().getSimpleName();
         System.out.println(msg);
         
         try { 
            con.sendObject(object); 
            msg = "++ object reflected";
         } catch (Throwable e) {
            msg = "-- unable to reflect object; ERROR = ".concat(e.toString());
         }
         System.out.println(msg);
      }

      @Override
      public void pingEchoReceived (PingEcho echo) {
         Connection con = echo.getConnection();
         String msg = "-- PING received from " + con.getRemoteAddress()  
               + ", ser=" + echo.pingId() + ", ms-total=" + echo.duration();
               
         System.out.println(msg);
      }

      @Override
      public void transmissionEventOccurred (TransmissionEvent evt) {
         String msg = "-- transmission event (ignored)";
         
         switch (evt.getType()) {
         case FILE_INCOMING:
            msg = "-- file transfer incoming: obj=" + evt.getObjectID() +
                  ", length=" + evt.getExpectedLength() + ", buffer=" +
                  evt.getFile();
            break;
         case FILE_RECEIVED: 
            msg = "-- file received: obj=" + evt.getObjectID() +
                  ", length=" + evt.getTransmissionLength() + ", duration=" +
                  evt.getDuration() + "\n   storage=" + evt.getFile();
            try {
               evt.getConnection().sendFile(evt.getFile(), evt.getPath());
               msg = msg.concat("\n++ file reflected");
            } catch (IOException e1) {
               msg = msg.concat("\n** unable to reflect file, reason: " + e1);
            }
            break;
         case FILE_ABORTED:
            Throwable e = evt.getException();
            msg = "** file transfer aborted: obj=" + evt.getObjectID() +
            ", info=" + evt.getInfo() + ", exception=" + e;
            break;
         }
         System.out.println(msg);
      }
   }

}

}
