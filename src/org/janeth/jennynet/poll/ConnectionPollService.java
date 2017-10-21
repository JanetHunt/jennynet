package org.janeth.jennynet.poll;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.janeth.jennynet.intfa.Connection;
import org.janeth.jennynet.intfa.ConnectionListener;
import org.janeth.jennynet.intfa.PingEcho;
import org.janeth.jennynet.intfa.TransmissionEvent;
import org.janeth.jennynet.poll.ConnectionEvent.EventType;

public class ConnectionPollService {

	private BlockingQueue<ConnectionPollUnit> queue; 
	private Connection connection;
	
	public ConnectionPollService (Connection connection, int capacity) {
		if (capacity == 0) {
			capacity = connection.getParameters().getObjectQueueCapacity();
		}
		queue  = new ArrayBlockingQueue<ConnectionPollUnit>(capacity, true);
		this.connection = connection;
		connection.addListener(new ConListener());
	}

	public ConnectionPollService (Connection connection) {
		this(connection, 0);
	}
	
	public Connection getConnection() {
		return connection;
	}

	private void putToQueue (ConnectionPollUnit unit) {
		try {
			queue.put(unit);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/** Returns the next available poll unit if available 
	 * or null after the given amount of time.
	 *  
	 * @param timeout long milliseconds wait time
	 * @return <code>ConnectionPollUnit</code> or null
	 * @throws InterruptedException if the thread is interrupted while waiting 
	 */
	public ConnectionPollUnit poll (long timeout) throws InterruptedException {
		return queue.poll(timeout, TimeUnit.MILLISECONDS);
	}

	/** Returns the next poll unit waiting if necessary until the next
	 * unit becomes available.  
	 *  
	 * @return <code>ConnectionPollUnit</code>
	 * @throws InterruptedException if the thread is interrupted while waiting 
	 */
	public ConnectionPollUnit take () throws InterruptedException {
		return queue.take();
	}

	
	private class ConListener implements ConnectionListener {
		@Override
		public void connected (Connection connection) {
			ConnectionEvent event = new ConnectionEvent(connection, EventType.connected);
			ConnectionPollUnit unit = new ConnectionPollUnit(connection, event);
			putToQueue(unit);
		}
	
		@Override
		public void disconnected(Connection connection, int cause, String message) {
			ConnectionEvent event = new ConnectionEvent(connection, EventType.connected,
					cause, message);
			ConnectionPollUnit unit = new ConnectionPollUnit(connection, event);
			putToQueue(unit);
		}
	
		@Override
		public void idle(Connection connection, boolean idle) {
			ConnectionEvent event = new ConnectionEvent(connection, EventType.idle, idle?1:0, null);
			ConnectionPollUnit unit = new ConnectionPollUnit(connection, event);
			putToQueue(unit);
		}
	
		@Override
		public void objectReceived (Connection connection, long objectNr, Object object) {
			ConnectionPollUnit unit = new ConnectionPollUnit(connection, object);
			putToQueue(unit);
		}
	
		@Override
		public void transmissionEventOccurred (TransmissionEvent event) {
			ConnectionPollUnit unit = new ConnectionPollUnit(event);
			putToQueue(unit);
		}
	
		@Override
		public void pingEchoReceived(PingEcho pingEcho) {
			ConnectionPollUnit unit = new ConnectionPollUnit(pingEcho);
			putToQueue(unit);
		}
	}

	
}
