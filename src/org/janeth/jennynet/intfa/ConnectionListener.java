package org.janeth.jennynet.intfa;


/** Interface for application software that listens to events of a 
 * JennyNet <code>Connection</code>. A <code>ConnectionListener</code> 
 * has to be registered at a specific <code>Connection</code> to receive 
 * its events. 
 * 
 * <p><b>Time Management</b>
 * <p>As a general rule, event
 * execution in the application should not take long amounts of time and any
 * method calls that could result in blocking operations should be avoided. In 
 * consequence of blocking user execution, the buffering capabilities of the 
 * incoming channel of a connection are under charge; the outgoing channels 
 * should be unaffected. 
 * 
 *  @see Connection
 *  
 */

public interface ConnectionListener
{

  /** Called when the remote end has been connected. This will be invoked
   *  before any objects are received. This method runs in a dedicated thread
   *  and may  be delayed for any time required. However, other events of the
   *  same type are not processed until it returns. 
   * 
   * @param connection <code>Connection</code> source connection
   */
   public void connected (Connection connection);

   /** Called when the connection falls below or mounts above the IDLE
    * THRESHOLD. This event only appears after an IDLE THRESHOLD has been
    * defined for the source connection.
    * 
    * @param connection <code>Connection</code>  source connection
    * @param idle boolean true == is idle, false == is busy
    */ 
   public void idle (Connection connection, boolean idle);

   /** Called when the remote end is no longer connected. 
    * This indicates that the given connection has gone out of use; 
    * a disconnected {@code Connection} cannot be re-connected.
    * 
    * @param connection <code>Connection</code>  source connection
    * @param cause int code for cause of disconnection if available
    * @param message String message about cause of disconnection if available
    */
   public void disconnected (Connection connection, int cause, String message);

   /** Called when the connection has been closed for user operations.
    * In the CLOSED state incoming object events are still possible
    * until the DISCONNECTED state is reached.
    * This call indicates that the given connection is no more available
    * for outgoing data communication. A closed connection cannot be re-opened.
    * 
    * @param connection <code>Connection</code>  source connection
    * @param cause int code for cause of disconnection if available
    * @param message String message about cause of disconnection if available
    */
   public void closed (Connection connection, int cause, String message);

   /** Called when an object has been received from the remote end of the
    *  connection. The given object is available here in same data state and
    *  with the same layer based identifier as sent on the remote end.
    *  
    * @param connection <code>Connection</code> source connection
    * @param objectNr long identifier of received object
    * @param object Object received object
    */   
   public void objectReceived (Connection connection, long objectNr, 
         Object object);

   /** Called when a PING-ECHO was received from the remote station.
    * 
    * @param pingEcho PingEcho
    * @see PingEcho
    */
   public void pingEchoReceived (PingEcho pingEcho);

   /** Called when an event has occurred on a file-transfer of the source
    * connection, incoming or outgoing. (More detail on the event types
    * can be found in the interface description of type <code>TransmissionEvent
    * </code>.)
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
    * @see TransmissionEvent
    */
   public void transmissionEventOccurred (TransmissionEvent event);
   
}