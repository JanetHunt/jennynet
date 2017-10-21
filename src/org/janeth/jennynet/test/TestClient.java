package org.janeth.jennynet.test;

import java.io.File;
import java.io.IOException;

import org.janeth.jennynet.core.Client;
import org.janeth.jennynet.core.JennyNet;
import org.janeth.jennynet.core.JennyNetByteBuffer;
import org.janeth.jennynet.util.Util;

public class TestClient {

   
   public static void main(String[] args) {
      File tFile;
      
      if (args.length != 2) {
          System.err.println("Usage: <program> <host address> <port number>");
          System.exit(1);
      }
      
      String hostname = args[0];
      int portNumber = Integer.parseInt(args[1]);

//      JennyNet.setSendAliveSignals(false);
      File rootDir = new File("test");
      try {
         JennyNet.setDefaultTransmissionRoot(rootDir);
      } catch (Exception e1) {
         System.out.println("*** cannot activate file-root-directory: " + rootDir.getAbsolutePath());
         System.out.println("    reason: ".concat(e1.toString()));
      }

      Client client = new Client();
      client.addListener(new EventReporter());
//      client.bind(2020);

      try {
         client.connect(10000, hostname, portNumber);
//         client.getParameters().setTransmissionParcelSize(32000);
//         client.getParameters().setTransmissionParcelSize(128);
//         client.getParameters().setAlivePeriod(5000);
         client.getParameters().setConfirmTimeout(5000);
         client.getParameters().setTransmissionParcelSize(64000);
         
         System.out.println("----- Connection Established ------  to Server Address: " + client.getRemoteAddress() );
         
         // send OBJECT on regular channel
         client.sendObject("Hello, give my friends my greetings!");

//         for (int i = 0; i < 50; i++) {
//            client.sendObject("Hello my Friend, I'm here!");
//         }

         // send test file 2
         tFile = new File("/home/wolfgang/Downloads/kryonet-2.20.zip");
//         tFile = new File("/home/wolfgang/Downloads/jre-6u39-linux-i586.bin");
         long fid2 = client.sendFile(tFile, "transmission/kryonet.zip");

         Thread.sleep(1500);
         
         // send large object
         JennyNetByteBuffer buf = new JennyNetByteBuffer(Util.randBytes(60000));
         client.sendObject("Hello, we are sending now a data aray with CRC = " + buf.getCRC());
         client.sendObject(buf);
         System.out.println("sent data block with CRC = " + buf.getCRC()) ;

         Thread.sleep(50);
         client.sendPing();
         
//         tFile = new File("/home/wolfgang/FL-Git.txt");
//         tFile = new File("/home/wolfgang/Downloads/jre-6u39-linux-i586.bin");
//         long fid1 = client.sendFile(tFile, "transmission/jre-6.bin");

//         tFile = new File("/home/wolfgang/Downloads/pascal-htmls.tar.gz");
//         long fid2 = client.sendFile(tFile, "transmission/pascal-docs.tar.gz");
//         
         Thread.sleep(2000);
//         client.breakOutgoingTransfer(fid1);
//         Thread.sleep(500);
         client.sendPing();
//         client.sendObject(new JennyNetByteBuffer(null));
         
//         client.waitForSendPerformed();
//         client.close();
         
      } catch (Exception e) {
         e.printStackTrace();
         if (client != null) {
            client.close();
         }
      }
        
   }
}
