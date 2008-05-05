package com.ibm.oti.vm;

import java.io.IOException;

/**
 * Stub for IBM J9 MIDP implementation.
 * <p>
 * Licensed Materials - Property of IBM, (c) Copyright IBM Corp. 2000, 2002 All
 * Rights Reserved
 * <p>
 * This class is not distributed with BlueCove binary distribution: bluecove.jar
 */

public class VM {

	private static final String message = "STUB Should not be used";

	/**
	 * Stub for IBM J9 MIDP implementation. Loads the system library specified
	 * by the libname argument.
	 * 
	 * @param libname
	 * @throws IOException
	 */
	public static synchronized void loadLibrary(String libname) throws IOException {
		throw new Error(message);
	}

	/**
	 * Stub for IBM J9 MIDP implementation. Registers a new virtual-machine
	 * shutdown hook.
	 */
	public static void addShutdownClass(Runnable hook) {
		throw new Error(message);
	}
}
