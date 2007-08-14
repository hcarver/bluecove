package com.ibm.oti.connection;

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * Stub for IBM J9 implemenation.
 * <p>
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2000, 2002  All Rights Reserved
 * <p>
 * This class is not distributed with BlueCove binary distrubution: bluecove.jar
 */
public interface CreateConnection extends Connection {

	public void setParameters(String spec, int access, boolean timeout) throws IOException;

	public void setParameters2(String spec, int access, boolean timeout) throws IOException;
	
}
