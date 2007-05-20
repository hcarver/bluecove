package com.ibm.oti.connection;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2000, 2002  All Rights Reserved
 * 
 * This class is not distributed with BlueCove binary distrubution: bluecove.jar
 */

import java.io.IOException;

import javax.microedition.io.Connection;

public interface CreateConnection extends Connection {

	public void setParameters(String spec, int access, boolean timeout) throws IOException;

}
