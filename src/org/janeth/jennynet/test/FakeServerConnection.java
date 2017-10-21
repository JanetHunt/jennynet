package org.janeth.jennynet.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.janeth.jennynet.core.Server;
import org.janeth.jennynet.intfa.IServer;
import org.janeth.jennynet.intfa.ServerConnection;

public class FakeServerConnection extends FakeConnection implements ServerConnection {

	IServer server;
	boolean started;
	boolean tempoFixed;
	
	public FakeServerConnection (IServer server) {
		this.server = server;
	}

	public FakeServerConnection (InetSocketAddress local, InetSocketAddress remote) {
		super(local, remote);
	}

	@Override
	public void start() throws IOException {
		started = true;
	}

	@Override
	public void reject() throws IOException {
	}

	@Override
	public void setTempoFixed (boolean isFixed) {
		tempoFixed = isFixed;
	}

	@Override
	public IServer getServer() {
		return server;
	}

	@Override
	public boolean getTempoFixed() {
		return tempoFixed;
	}

}
