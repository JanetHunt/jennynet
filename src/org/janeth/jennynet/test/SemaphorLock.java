package org.janeth.jennynet.test;

public class SemaphorLock {

	private Object lock = new Object();
	private int counter;
	
	public SemaphorLock (int count) {
		if (count < 0) 
			throw new IllegalArgumentException("illegal negative value");
		counter = count;
	}
	
	public void inc () {
		counter++;
	}
	
	public void dec () {
		if (counter > 0) {
		   counter--;
		}
		if (counter == 0) {
			lock_notify();
		}
	}
	
	public void lock_notify() {
		synchronized(lock) {
			lock.notify();
		}
	}

	public void lock_wait (long time) throws InterruptedException {
		if (counter > 0) {
			synchronized(lock) {
				lock.wait(time);
			}
		}
	}

}
