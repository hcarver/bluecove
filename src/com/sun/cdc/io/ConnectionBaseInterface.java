package com.sun.cdc.io;

import java.io.IOException;

import javax.microedition.io.Connection;

public interface ConnectionBaseInterface {
	public Connection openPrim(String name, int mode, boolean timeouts)
			throws IOException;
}