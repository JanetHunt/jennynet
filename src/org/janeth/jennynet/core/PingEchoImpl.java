package org.janeth.jennynet.core;

import java.util.Date;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.util.Util;

/** Class to contain immutable information about a PING - ECHO
 * relation concerning a specific connection (<code>IConnection</code>).
 *  
 */
final class PingEchoImpl implements PingEcho {
   
   private Connection connection;
   private long pingID;
   private long time_sent;
   private int duration;
   
   public static PingEcho create (Connection con, long pingId, long sendTime, int duration) {
      // validate
      if (con == null)
         throw new NullPointerException();
      if (pingId <= 0)
         throw new IllegalArgumentException("pingId <= 0");
      if (sendTime <= 0)
         throw new IllegalArgumentException("sendTime <= 0");
      if (duration <= 0)
         throw new IllegalArgumentException("duration <= 0");

      PingEchoImpl pe = new PingEchoImpl();
      pe.pingID = pingId;
      pe.connection = con;
      pe.time_sent = sendTime;
      pe.duration = duration;
      return pe;
}
   
   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.PingEcho#getConnection()
    */
   @Override
   public Connection getConnection () {
      return connection;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.PingEcho#time_sent()
    */
   @Override
   public long time_sent () {
      return time_sent;
   }

   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.PingEcho#duration()
    */
   @Override
   public int duration () {
      return duration;
   }
   
   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.PingEcho#pingId()
    */
   @Override
   public long pingId () {
      return pingID;
   }
   
   /* (non-Javadoc)
    * @see org.janeth.jennynet.core.PingEcho#toString()
    */
   @Override
   public String toString () {
      String timestr = new Date(time_sent).toLocaleString();
      return Util.bytesToHex(connection.getShortId()) + " PING: " + pingID + 
             ", " + timestr + ", Duration = " + duration + " ms";
   }
}
