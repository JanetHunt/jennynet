package org.janeth.jennynet.test;

import java.io.File;
import java.io.PrintStream;

import org.janeth.jennynet.core.JennyNetByteBuffer;
import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.util.Util;

public class EventReporter implements ConnectionListener {

   private PrintStream out = System.out;
   
   /** Creates an event reporter which writes to the
    * <i>System.out</i> print stream.
    */
   public EventReporter () {
   }
   
   public EventReporter (PrintStream output) {
      out = output;
   }
   
   private void thisStatement (Connection con) {
      String idstr = Util.bytesToHex(con.getShortId());
      out.println("    THIS: " + idstr + ", NetAddr = " + con.getLocalAddress());
   }
   
   @Override
   public void connected (Connection con) {
      out.println("+++ Event: CONNECTED TO: " + con.getRemoteAddress() + 
            "  ");
      thisStatement(con);
   }

   @Override
   public void disconnected (Connection con, int cause, String message) {
      out.println("+++ Event: DISCONNECTED FROM: " + con.getRemoteAddress() + 
            ", Reason = " + cause);
      if (message != null) {
         out.println("    MSG: ".concat(message));
      }
      thisStatement(con);
   }

   @Override
   public void idle (Connection con, boolean idle) {
      out.println("+++ Event: CONNECTION IDLE STATUS (" + idle + "): " + con.getRemoteAddress() + 
            "  ");
      thisStatement(con);
   }

   @Override
   public void objectReceived (Connection con, long objectNr, Object object) {
      out.println("+++ Event: O B J E C T (" + objectNr + ") RECEIVED FROM: " + con.getRemoteAddress() + 
            "  ");
      thisStatement(con);
      out.println("    Class: " + object.getClass());
      if (object instanceof String) {
         out.println("    \"" + (String)object + "\"");
      }
      if (object instanceof JennyNetByteBuffer) {
         JennyNetByteBuffer buffer = (JennyNetByteBuffer)object;
         byte[] trunk = new byte[Math.min(buffer.getLength(), 500)];
         System.arraycopy(buffer.getData(), 0, trunk, 0, trunk.length);
         out.println("    JENNY-BUFFER: " + Util.bytesToHex(trunk) );
         out.println("    received block CRC = " + buffer.getCRC());
      }
   }

   @Override
   public void transmissionEventOccurred(TransmissionEvent event) {
      File file;
      long objectNr = event.getObjectID();
      
      out.println("+++ Event: TRANSMISSION EVENT OCCURRED, object = " + objectNr + ", remote = " +
            event.getConnection().getRemoteAddress());
      out.println("    TYPE = " + event.getType() + "  (info " + event.getInfo() + ")");
      thisStatement(event.getConnection());

      switch (event.getType()) {
      case FILE_INCOMING:
         out.println("--- FILE TRANSFER INCOMING, path = " + event.getPath() +
               ", size = " + event.getExpectedLength());
         out.println("    storing file = " + event.getFile());
         break;

      case FILE_RECEIVED:
         file = event.getFile();
         out.println("--- FILE RECEIVED (" + objectNr + "), path = " + event.getPath() +
               ", size = " + event.getTransmissionLength() + ", duration=" + event.getDuration());
//         String hstr = event.haveDestination() ? "DESTINATION" : "TEMP";
//         out.println("    available as " + hstr + " file = " + file);
         if ( file != null ) {
            out.println("    local file length is: " + (file.exists() ? file.length() : "- NIL -"));
         }
         break;
         
      case FILE_CONFIRMED:
         out.println("--- FILE TRANSFER CONFIRMED, path = " + event.getPath());
         file = event.getFile();
         if ( file != null ) {
            out.println("    File = " + file);
            out.println("    local file length is: " + (file.exists() ? file.length() : "- NIL -"));
         }
         break;
         
      case FILE_ABORTED:
         out.println("--- FILE TRANSFER ABORTED, path = " + event.getPath());
         file = event.getFile();
         if ( file != null ) {
            out.println("    File = " + file);
            out.println("    local file length is: " + (file.exists() ? file.length() : "- NIL -"));
         }
         break;
         
      case FILE_FAILED:
         out.println("--- FILE TRANSFER FAILED, path = " + event.getPath());
         file = event.getFile();
         if ( file != null ) {
            out.println("    File = " + file);
            out.println("    local file length is: " + (file.exists() ? file.length() : "- NIL -"));
         }
         break;
      }

   }

   @Override
   public void pingEchoReceived(PingEcho pingEcho) {
      Connection con = pingEcho.getConnection();
      out.println("+++ Event: PING-ECHO RECEIVED FROM: " + con.getRemoteAddress() + 
            "  ");
      thisStatement(con);
      out.println("    ECHO: ".concat(pingEcho.toString()));
   }

   
}
