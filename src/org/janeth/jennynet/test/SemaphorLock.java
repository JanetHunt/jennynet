package org.janeth.jennynet.test;

/** This is a counter based semaphore implementation. Any number of threads
 *  can wait on this semaphore until the counter becomes zero or optionally
 *  an amount of time has elapsed. On the falling flank of the counter
 *  becoming zero all waiting threads are awakened.
 *
 *  <p>Note: the "lock_wait" method is stabilised against Java's 
 *  "spurious" thread wakeup.
 */
public class SemaphorLock {

	private Object lock = new Object();
	private int counter;

	/** Creates a new <code>SemaphorLock</code> with an initial counter 
	 * value of zero.
	 */
	public SemaphorLock () {
	}
	
	/** Creates a new <code>SemaphorLock</code> with an initial counter 
	 * value as given by the parameter.
	 * 
	 * @param count int initial counter value
	 */
	public SemaphorLock (int count) {
		if (count < 0) 
			throw new IllegalArgumentException("illegal negative value");
		counter = count;
	}
	
	/** Increases the counter of this semaphore by 1.
	 */
	public void inc () {
		counter++;
	}
	
	/** Decreases the counter of this semaphore by 1 if its value is
	 * above zero. If the counter becomes zero on the falling flank in the 
	 * course of this action, the semaphore is reset.
	 */
	public void dec () {
		if (counter > 0) {
		   counter--;
		   if (counter == 0) {
			   reset();
		   }
		}
	}

	/** Returns the current counter state. 0 means the semaphore is unlocked.
	 * 
	 * @return int counter value
	 */
	public int getCounter () {
		return counter;
	}
	
	public void setCounter (int value) {
		if (value < 0) 
			throw new IllegalArgumentException("negative value");
		counter = value;
	}
	
	/** Releases all threads waiting on the lock of this semaphore and
	 * resets the counter to zero. This is automatically called by the 
	 * "dec" method when the counter value becomes zero.
	 */
	public void reset () {
		synchronized(lock) {
			lock.notifyAll();
			counter = 0;
		}
	}

	/** Waits the given amount of time for this semaphore's lock to
	 * open if and only if the counter's value is above zero. The
	 * lock opens if method "reset" is called or if the counter
	 * value becomes zero through the "dec" method.
	 *  
	 * @param time long milliseconds to wait; 0 for endless wait
	 * @throws InterruptedException
	 */
	public void lock_wait (long time) throws InterruptedException {
		if (counter > 0 && time > -1) {
			long elapsed = 0;
			long start = System.currentTimeMillis();
			
			synchronized(lock) {
				// we have to make all this effort bc. of the socalled "spurious" thread resumption
				while (elapsed-1 < time && counter > 0) {
					lock.wait(time-elapsed);
					elapsed = System.currentTimeMillis() - start;
				}
			}
		}
	}

}
