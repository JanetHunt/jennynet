package org.janeth.jennynet.poll;

import org.janeth.jennynet.intfa.Connection;

/** Class to enclose information about a connection event in the context
 * of <code>ConnectionPollService</code>.
 * 
 */
public class ConnectionEvent {

	public enum EventType {
		connected,
		disconnected,
		closed,
		idle
	}
	
	private Connection connection;
	private EventType type;
	private int info;
	private String text;
	
	public ConnectionEvent (Connection connection, EventType type) {
		if (connection == null || type == null)
			throw new NullPointerException();
		this.connection = connection;
		this.type = type;
	}

	public ConnectionEvent (Connection connection, EventType type, int info, String msg) {
		if (connection == null || type == null)
			throw new NullPointerException();
		this.connection = connection;
		this.type = type;
		this.info = info;
		this.text = msg;
	}

	public EventType getType() {
		return type;
	}

	public int getInfo() {
		return info;
	}

	public String getText() {
		return text;
	}

	public Connection getConnection() {
		return connection;
	}

	
}
