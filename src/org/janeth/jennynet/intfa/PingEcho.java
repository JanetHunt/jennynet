package org.janeth.jennynet.intfa;

/**
 * Interface for a PING-ECHO. A PING-ECHO is released as
 * a connection event to <code>ConnectionListener</code> implementations.
 * It gives details about the connection involved, when the PING was started 
 * and its run-time (complete run) in milliseconds. The PING-ID makes a PING
 * traceable after sending. The ID name-space is unique for each connection
 * and separate from OBJECT-IDs. 
 * 
 * @see ConnectionListener
 * @see Connection
 */

public interface PingEcho {

   /** The connection involved in the PING / PING-ECHO action.
    * 
    * @return <code>Connection</code>
    */
   public Connection getConnection();

   /** Returns the time point when the PING was sent.
    * 
    * @return long "epoch" time value in milliseconds
    */
   public long time_sent();

   /** Returns the time (duration) of the complete PING run 
    * from the time-point of sending to the time-point of 
    * reception of the ECHO in the sending layer.
    * 
    * @return int PING run time in milliseconds
    */
   public int duration();

   /** The PING-ID number for this PING-ECHO.
    * (PING IDs are relative to their connection and cover a name-space
    * separate from OBJECT-IDs.)
    * 
    * @return long PING-Id
    */
   public long pingId();

   /**
    * Human readable representation of this PING-ECHO detailing its basic 
    * facts, including a short-ID of the connection involved.
    * 
    * @return String
    */
   @Override
   public String toString();

}