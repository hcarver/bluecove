package com.intel.bluetooth;

import java.io.IOException;

public class NotImplementedIOException extends IOException {

	private static final long serialVersionUID = 1L;

	public static final boolean enabled = NotImplementedError.enabled;
	
	public NotImplementedIOException() {
		super("Not Implemented");
	}

}
