package org.janeth.jennynet.exception;

public class UnconnectedException extends JennyNetException {

	public UnconnectedException() {
	}

	public UnconnectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnconnectedException(String message) {
		super(message);
	}

	public UnconnectedException(Throwable cause) {
		super(cause);
	}

}
