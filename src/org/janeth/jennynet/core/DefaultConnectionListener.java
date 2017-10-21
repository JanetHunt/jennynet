package org.janeth.jennynet.core;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.TransmissionEvent;

/**
 * A default implementation of the <code>ConnectionListener</code> interface.
 * All methods of this class are doing nothing.
 */
public class DefaultConnectionListener implements ConnectionListener {

   @Override
   public void connected(Connection connection) {
   }

   @Override
   public void disconnected(Connection connection, int cause, String message) {
   }

   @Override
   public void idle(Connection connection, boolean idle) {
   }

   @Override
   public void objectReceived(Connection connection, long objectNr, Object object) {
   }

   @Override
   public void transmissionEventOccurred(TransmissionEvent event) {
   }

   @Override
   public void pingEchoReceived(PingEcho pingEcho) {
   }

}
