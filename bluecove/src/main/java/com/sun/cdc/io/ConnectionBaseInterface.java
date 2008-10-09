package com.sun.cdc.io;

import java.io.IOException;

import javax.microedition.io.Connection;

/*
 * This class is not distributed with BlueCove binary distribution: bluecove.jar
 */

public interface ConnectionBaseInterface {
	public Connection openPrim(String name, int mode, boolean timeouts)
			throws IOException;
}