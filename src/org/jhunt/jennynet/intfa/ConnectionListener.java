package org.jhunt.jennynet.intfa;


public interface ConnectionListener
{

   /** Called when the remote end has been connected. This will be invoked before
    *  any objects are received. This method runs in a dedicated thread and may 
    *  be delayed for any time required. However, other events of the same type 
    *  are not processed until it returns. 
    */
   public void connected (Connection connection);

   /** Called when the connection is below the 
    * {@link ConnectionImpl#setIdleThreshold(int) idle threshold}. 
    */ 
   public void idle (Connection connection);

   /** Called when the remote end is no longer connected. This method should not
    * be delayed for a long time. 
    */
   public void disconnected (Connection connection, int cause, String message);

   /** Called when the connection has been closed by the remote station.
    * This indicates that the given connection instance is out of use.
    * 
    * @param connection IConnection closed connection
    */
   public void closed (Connection connection, int cause, String message);
   
   /** Called when an object has been received from the remote end of the
    *  connection. This method should not block for long periods as other 
    *  network events might not be reported until it returns.
    */   
   public void objectReceived (Connection connection, long objectNr, Object object);

   /** Called when a PING - ECHO was received from the remote station.
    * 
    * @param pingEcho PingEcho
    */
   public void pingEchoReceived (PingEcho pingEcho);

   /** Called when an incoming file-transfer has been completed.
    * The received file is available at given file parameter.
    * The additionally received remote file path information may 
    * differ from the actual local file path.
    *  
    * @param file File received file (local file system)
    * @param pathInfo String remote file path information (may be null)
    */
//   public void fileReceived (IConnection connection, File file, String pathInfo);
   
   public void transmissionEventOccurred (TransmissionEvent event);
   
}