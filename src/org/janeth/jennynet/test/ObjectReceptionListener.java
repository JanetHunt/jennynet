package org.janeth.jennynet.test;

import java.util.ArrayList;
import java.util.List;

import org.janeth.jennynet.core.DefaultConnectionListener;
import org.janeth.jennynet.core.JennyNetByteBuffer;
import org.janeth.jennynet.intfa.Connection;

/** A <code>ConnectionListener</code> to receive 
 * <code>JennyNetByteBuffer</code> and return them via an output method
 * as list of byte arrays. There is an optional lock as parameter which
 * gets notified with each reception event.
 */
class ObjectReceptionListener extends DefaultConnectionListener {

	private List<byte[]> received = new ArrayList<byte[]>();
	private Object lock;
	private int unlockThreshold;

	/** Creates a new reception listener with an optional lock
	 * to be notified.
	 *  
	 * @param lock Object lock to be notified, may be null
	 * @param unlockSize int size of object list which triggers 
	 * 		  lock release 
	 */
	public ObjectReceptionListener (Object lock, int unlockSize) {
		this.lock = lock;
		unlockThreshold = unlockSize;
	}
	
	/** Creates a new reception listener without lock reference.
	 */
	public ObjectReceptionListener () {
	}
	
	@Override
	public void objectReceived(Connection con, long objNr, Object obj) {
		if (obj instanceof JennyNetByteBuffer) {
			received.add(((JennyNetByteBuffer)obj).getData());
			
			if (lock != null && received.size() == unlockThreshold) {
				synchronized(lock) {
					lock.notify();
				}
			}
		}
	}
	
	public List<byte[]> getReceived () {
		return received;
	}
	
	/** The size of the reception list.
	 * 
	 * @return int
	 */
	public int getSize () {
		return received.size();
	}
	
	public void reset (int unlockSize) {
		received.clear();
		unlockThreshold = unlockSize;
	}

	public void reset () {
		received.clear();
	}
}