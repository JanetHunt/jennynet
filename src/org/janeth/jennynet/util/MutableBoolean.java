package org.janeth.jennynet.util;

public class MutableBoolean {
	private boolean value;
	
	public MutableBoolean (boolean initial) {
		value = initial;
	}

	public boolean getValue () {
		return value;
	}
	
	public void setValue (boolean v) {
		value = v;
	}
	
}
