package org.janeth.jennynet.core;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.intfa.ServerListener;

/** A <code>ServerListener</code> which does nothing.
 */
public class DefaultServerListener implements ServerListener {

   @Override
   public void connectionAvailable(IServer server, ServerConnection connection) {
   }

   @Override
   public void connectionAdded(IServer server, Connection connection) {
   }

   @Override
   public void connectionRemoved(IServer server, Connection connection) {
   }

   @Override
   public void serverClosed(IServer server) {
   }

   @Override
   public void errorOccurred (IServer server, Connection con, int transAction, Throwable e) {
   }

}
