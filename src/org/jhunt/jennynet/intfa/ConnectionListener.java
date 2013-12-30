package org.jhunt.jennynet.intfa;


public interface ConnectionListener
{

   /** Called when the remote end has been connected. This will be invoked before
    *  any objects are received. This method runs in a dedicated thread and may 
    *  be delayed for any time required. However, other events of the same type 
    *  are not processed until it returns. 
    */
   public void connected (Connection connection);

   /** Called when the connection falls below or mounts above the IDLE threshold.
    * 
    * @param connection Connection connection qualifying
    * @param idle boolean true == is idle, false == is busy
    */ 
   public void idle (Connection connection, boolean idle);

   /** Called when the remote end is no longer connected. This method should not
    * be delayed for a long time. 
    */
   public void disconnected (Connection connection, int cause, String message);

   /** Called when the connection has been closed. This may be caused
    * by either the local layer or the remote end.
    * This indicates that the given instance is out of use; a closed
    * connection cannot be reconnected.
    * 
    * @param connection IConnection closed connection
    */
   public void closed (Connection connection, int cause, String message);
   
   /** Called when an object has been received from the remote end of the
    *  connection. This method should not block for long periods as other 
    *  network events might not be reported until it returns.
    */   
   public void objectReceived (Connection connection, long objectNr, Object object);

   /** Called when a PING-ECHO was received from the remote station.
    * 
    * @param pingEcho PingEcho
    */
   public void pingEchoReceived (PingEcho pingEcho);

   /** Called when an event has occurred on a file-transfer, incoming or 
    * outgoing.
    * 
    * <small>
    * <p>A received file is indicated with <code>event.getType() == 
    * TransmissionEventType.FILETRANSFER_RECEIVED</code>.
    * The received file is available at <code>event.getFile()</code>.
    * The additionally available "remote file path" information at <code>
    * event.getPath()</code> may differ from the actual local file path.
    * </small>
    *  
    * @param event File <code>TransmissionEvent</code>
    */
   public void transmissionEventOccurred (TransmissionEvent event);
   
}