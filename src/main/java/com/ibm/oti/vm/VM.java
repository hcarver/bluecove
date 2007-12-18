package com.ibm.oti.vm;

import java.io.IOException;

/**
 * Stub for IBM J9 implementation.
 * <p>
 * Licensed Materials - Property of IBM, (c) Copyright IBM Corp. 2000, 2002 All
 * Rights Reserved
 * <p>
 * This class is not distributed with BlueCove binary distribution: bluecove.jar
 */

public class VM {

	/**
	 * Stub for IBM J9 implementation. Loads the system library specified by the
	 * libname argument.
	 * 
	 * @param libname
	 * @throws IOException
	 */
	public static synchronized void loadLibrary(String libname) throws IOException {
		throw new IOException("Should not be used");
	}

}
