package org.janeth.jennynet.appl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.intfa.TransmissionEvent.TransmissionEventType;
import org.janeth.jennynet.util.Util;
    
   public class SendClientA {

//   static Timer timer = new Timer();
   
   public static void main(String[] args) throws IOException {
    
       if (args.length != 3) {
           System.err.println("Usage: <program> <server> <port number> <file>");
           System.err.println("       server = target (server) IP address\n       port number = " +
                 "target (server) port\n       file = receive-file");
           System.exit(1);
       }
    
       String serverName = args[0];
       int portNumber = Integer.parseInt(args[1]);
       File outFile = new File(args[2]);
       File rootDir = outFile.getParentFile();
       if (!rootDir.isDirectory()) {
          System.err.println("*** root-directory does not exist: ".concat(rootDir.getAbsolutePath()));
          System.exit(1);
       }

        try {
           // create client socket
           Socket client = new Socket();
           
           InetSocketAddress serverAddress = new InetSocketAddress(serverName, portNumber);
           client.connect(serverAddress, 5000);
           System.out.println("SEND-CLIENT-TEST A (receive file), local = " + client.getLocalSocketAddress());
           System.out.println("--- CONNECTED TO SERVER : " + client.getRemoteSocketAddress());

           BufferedInputStream in = null;
           OutputStream out = null;
           
           try {
              // open source file input stream
              in = new BufferedInputStream(client.getInputStream(), 1024*16);
              
              // open output socket
              out = new BufferedOutputStream(new FileOutputStream(outFile), 1024*16);
              
              // sending data (transmission)
              long startTime = System.currentTimeMillis();
              System.out.println("--- RECEIVING FILE from : " + client.getRemoteSocketAddress() + 
                    "\n    file = " + outFile.getAbsolutePath());
              Util.transferData(in, out, 1024*4);
              long duration = System.currentTimeMillis() - startTime;
              
              // report
              System.out.println("--- FILE RECEIVED (COMPLETED) from : " + client.getRemoteSocketAddress() +
                    "\n    duration = " + duration);
              
           } catch (IOException e) {
              System.out.println("*** TRANSMISSION ERROR");
              e.printStackTrace();
              
           } finally {
              try {
                 // close client socket and input file
                 client.close();
                 System.out.println("--- CONNECTION CLOSED to : " + client.getRemoteSocketAddress());
                 if (out != null) {
                    out.close();
                 }
              } catch (IOException e1) {
                 System.out.println("*** TASK TERMINATION ERROR");
                 e1.printStackTrace();
              }
           }
           

        } catch (Exception e) {
            System.err.println("*** SOCKET CONNECTION ERROR to PORT " + portNumber);
            e.printStackTrace();
            System.exit(-1);
        }
       }

/*   
      public static class AbortFiletransferTask extends TimerTask {
         private Connection connection;
         private long fileId;
         
         /** A new file transfer cancel task.
          * 
         * @param c IConnection
          * @param fileId long file transfer
          * @param time int delay in seconds
          
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
*/
}
   
   