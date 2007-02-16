package com.intel.bluetooth;

public class NotImplementedError extends Error {
	
	private static final long serialVersionUID = 1L;

	public static final boolean enabled = false;
	
	public NotImplementedError() {
		super("Not Implemented");
	}
}
