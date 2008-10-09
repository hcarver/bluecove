/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.bluetooth.BluetoothConnectionException;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.RemoteDeviceHelper;
import com.intel.bluetooth.Utils;

/**
 * @author vlads
 * 
 */
class ServiceListener {

	private static final String RFCOMM_PREFIX = "rfcomm-";

	private static final String L2CAP_PREFIX = "l2cap-";

	private String portID;

	private boolean rfcomm;

	private Object lock = new Object();

	private Device serverDevice;

	private static long connectionCount = 0;

	private boolean connected = false;

	private boolean closed = false;

	private boolean interrupted = false;

	private long connectionId = 0;

	private int serverSecurityOpt;

	private int serverReceiveMTU;

	static String rfPrefix(int channel) {
		return RFCOMM_PREFIX + channel;
	}

	static String l2Prefix(int pcm) {
		return L2CAP_PREFIX + Integer.toHexString(pcm);
	}

	ServiceListener(String portID) {
		this.portID = portID;
		this.rfcomm = this.portID.startsWith(RFCOMM_PREFIX);
	}

	String getPortID() {
		return this.portID;
	}

	long accept(Device serverDevice, boolean authenticate, boolean encrypt, int serverReceiveMTU) throws IOException {
		this.serverDevice = serverDevice;
		this.serverReceiveMTU = serverReceiveMTU;
		this.serverSecurityOpt = Utils.securityOpt(authenticate, encrypt);

		serverDevice.serviceListenerAccepting(this.portID);
		while ((!closed) && (!interrupted) && (!connected)) {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					throw new InterruptedIOException("accept interrupted");
				}
			}
		}
		if (closed || interrupted || !connected) {
			throw new InterruptedIOException("accept closed");
		}
		return connectionId;
	}

	long connect(Device clientDevice, boolean authenticate, boolean encrypt, int cilentReceiveMTU, long timeout)
			throws IOException {
		ConnectionBuffer cb = null;
		boolean clientConnected = false;
		try {
			int securityOpt = Utils.securityOpt(authenticate, encrypt);
			if (this.serverSecurityOpt > securityOpt) {
				securityOpt = this.serverSecurityOpt;
			}

			int bsize = DeviceManagerServiceImpl.configuration.getConnectionBufferSize();
			ConnectedInputStream cis = new ConnectedInputStream(bsize);
			ConnectedOutputStream sos = new ConnectedOutputStream(cis);

			ConnectedInputStream sis = new ConnectedInputStream(bsize);
			ConnectedOutputStream cos = new ConnectedOutputStream(sis);

			ConnectionBuffer sb;
			if (this.rfcomm) {
				cb = new ConnectionBufferRFCOMM(serverDevice.getDescriptor().getAddress(), cis, cos);
				sb = new ConnectionBufferRFCOMM(clientDevice.getDescriptor().getAddress(), sis, sos);
			} else {
				cb = new ConnectionBufferL2CAP(serverDevice.getDescriptor().getAddress(), cis, cos,
						this.serverReceiveMTU);
				sb = new ConnectionBufferL2CAP(clientDevice.getDescriptor().getAddress(), sis, sos, cilentReceiveMTU);
			}
			cb.connect(sb);
			cb.setSecurityOpt(securityOpt);
			sb.setSecurityOpt(securityOpt);

			sb.setServerSide(true);

			long id;
			synchronized (ServiceListener.class) {
				connectionCount++;
				id = connectionCount;
			}
			MonitorConnection monitor = new MonitorConnection(clientDevice.getDescriptor().getAddress(), serverDevice
					.getDescriptor().getAddress(), portID, id);
			cb.setMonitor(monitor.getClientBuffer());
			sb.setMonitor(monitor.getServerBuffer());

			serverDevice.addConnectionBuffer(id, sb);
			connectionId = id;
			connected = true;
			synchronized (lock) {
				lock.notifyAll();
			}
			long endOfDellay = System.currentTimeMillis() + timeout;
			while ((!sb.isServerAccepted()) && (!sb.isClosed())) {
				long timeleft = endOfDellay - System.currentTimeMillis();
				if (timeleft <= 0) {
					throw new BluetoothConnectionException(BluetoothConnectionException.TIMEOUT, "Service " + portID
							+ " not ready");
				}
				synchronized (sb) {
					try {
						sb.wait(timeleft);
					} catch (InterruptedException e) {
						throw new InterruptedIOException();
					}
				}
			}
			if (!sb.isServerAccepted()) {
				throw new BluetoothConnectionException(BluetoothConnectionException.FAILED_NOINFO,
						"Connection rejected");
			}

			MonitoringServiceImpl.registerConnection(monitor);
			clientDevice.addConnectionBuffer(id, cb);
			clientConnected = true;

			StringBuffer logMsg = new StringBuffer();
			logMsg.append(RemoteDeviceHelper.getBluetoothAddress(clientDevice.getDescriptor().getAddress()));
			logMsg.append(" connected to ");
			logMsg.append(RemoteDeviceHelper.getBluetoothAddress(serverDevice.getDescriptor().getAddress()));
			logMsg.append(" ").append(this.portID);
			DebugLog.debug(logMsg.toString());

			return id;
		} finally {
			if (!connected) {
				interrupted = true;
			}
			if (!clientConnected) {
				cb.close();
			}
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}

	void close() {
		closed = true;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
}
