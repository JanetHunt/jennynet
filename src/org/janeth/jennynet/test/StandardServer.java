package org.janeth.jennynet.test;

import java.io.IOException;
import java.net.SocketAddress;

import org.janeth.jennynet.core.DefaultServerListener;
import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;
import org.janeth.jennynet.util.Util;

class StandardServer extends Server {
	
	/** Creates a standard server of the given socket address. The given 
	 * listener is added to each accepted connection. The server adds
	 * a standard Reporter listener to each connection.
	 * 
	 * @param address SocketAddress
	 * @param listener ConnectionListener
	 * @throws IOException
	 */
	StandardServer (SocketAddress address, final ConnectionListener listener) 
				throws IOException {
		this(address, listener, null);
	}
	
	StandardServer (SocketAddress address, 
			final ConnectionListener listener1,
			final ConnectionListener listener2) 
			throws IOException {
		super(address);
		addListener(new DefaultServerListener() {
	
			@Override
			public void connectionAvailable(IServer server, ServerConnection con) {
				try {
					if (listener1 != null) {
						con.addListener(listener1);
					}
					if (listener2 != null) {
						con.addListener(listener2);
					}
					con.addListener(new EventReporter());
					con.start();
					Util.sleep(10);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
}

}