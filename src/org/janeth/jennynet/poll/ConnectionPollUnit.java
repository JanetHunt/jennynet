package org.janeth.jennynet.poll;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.TransmissionEvent;

public class ConnectionPollUnit {

	public enum UnitType {
		UserObject,
		ConnectionEvent,
		TransmissionEvent,
		PingEcho
	}
	
	Connection connection;
	UnitType unitType;
	Object userObject;
	ConnectionEvent connectionEvent;
	TransmissionEvent transmissionEvent;
	PingEcho pingEcho;
	
	
	public ConnectionPollUnit (Connection connection, Object userObject) {
		if (userObject == null || connection == null)
			throw new NullPointerException();
		this.userObject = userObject;
		this.connection = connection;
		unitType = UnitType.UserObject;
	}

	public ConnectionPollUnit (TransmissionEvent transEvent) {
		if (transEvent == null)
			throw new NullPointerException();
		transmissionEvent = transEvent;
		this.connection = transEvent.getConnection();
		unitType = UnitType.TransmissionEvent;
	}

	public ConnectionPollUnit (PingEcho echo) {
		if (echo == null)
			throw new NullPointerException();
		pingEcho = echo;
		this.connection = echo.getConnection();
		unitType = UnitType.PingEcho;
	}

	public ConnectionPollUnit (ConnectionEvent event) {
		if (event == null)
			throw new NullPointerException();
		connectionEvent = event;
		this.connection = event.getConnection();
		unitType = UnitType.ConnectionEvent;
	}


	public ConnectionPollUnit() {
	}

	public Connection getConnection() {
		return connection;
	}

	public UnitType getType() {
		return unitType;
	}

	public Object getUserObject() {
		return userObject;
	}

	public TransmissionEvent getTransmissionEvent() {
		return transmissionEvent;
	}

	public PingEcho getPingEcho() {
		return pingEcho;
	}

	public ConnectionEvent getConnectionEvent() {
		return connectionEvent;
	}

	
	
}
