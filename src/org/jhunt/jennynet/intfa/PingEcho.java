package org.jhunt.jennynet.intfa;


public interface PingEcho {

   public Connection getConnection();

   /** Returns the time when the PING was sent.
    * 
    * @return long "epoch" time value (milliseconds)
    */
   public long time_sent();

   /** Returns the time of the complete PING run (duration) 
    * from the time-point of sending to the time-point of 
    * reception of the ECHO.
    * 
    * @return int PING run time in milliseconds
    */
   public int duration();

   /** The PING ID number for this PING - ECHO relation.
    * (PING Ids are relative to their connection.)
    * 
    * @return long PING-Id
    */
   public long pingId();

   @Override
   public String toString();

}