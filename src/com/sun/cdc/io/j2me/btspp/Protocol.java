package com.sun.cdc.io.j2me.btspp;

import java.io.IOException;

import javax.microedition.io.Connection;

import com.intel.bluetooth.MicroeditionConnector;
import com.sun.cdc.io.ConnectionBaseInterface;

public class Protocol implements ConnectionBaseInterface {

	public Connection openPrim(String name, int mode, boolean timeouts)
			throws IOException {
		return MicroeditionConnector.open("btspp:" + name, mode, timeouts);
	}

}
