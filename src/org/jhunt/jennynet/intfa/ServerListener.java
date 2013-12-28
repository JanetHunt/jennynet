package org.jhunt.jennynet.intfa;


public interface ServerListener {

   public void connectionAvailable (ServerConnection connection);
   
   public void connectionAdded (Connection connection);
   
   public void connectionRemoved (Connection connection);
   
   public void serverClosed ();
}
