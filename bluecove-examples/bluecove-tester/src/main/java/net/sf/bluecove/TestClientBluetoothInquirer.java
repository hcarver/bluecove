/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package net.sf.bluecove;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.StringUtils;
import org.bluecove.tester.util.TimeUtils;

import net.sf.bluecove.util.BluetoothTypesInfo;

public class TestClientBluetoothInquirer implements DiscoveryListener {

	private final TestClientConfig config;

	boolean stoped = false;

	boolean inquiring;

	boolean inquiringDevice;

	boolean searchingServices;

	boolean deviceDiscoveryError;

	Vector devices = new Vector();

	Vector serverURLs = new Vector();

	public int[] attrIDs;

	public final UUID L2CAP = new UUID(0x0100);

	public final UUID RFCOMM = new UUID(0x0003);

	private UUID searchUuidSet[];

	private UUID searchUuidSet2[];

	DiscoveryAgent discoveryAgent;

	int servicesSearchTransID;

	private String servicesOnDeviceName = null;

	private String servicesOnDeviceAddress = null;

	private boolean servicesFound = false;

	boolean anyServicesFound = false;

	private int anyServicesFoundCount;

	public TestClientBluetoothInquirer(TestClientConfig config) {
		this.config = config;
		inquiringDevice = false;
		inquiring = false;
		if (this.config.searchOnlyBluecoveUuid) {
			if (Configuration.useServiceClassExtUUID.booleanValue()) {
				searchUuidSet = new UUID[] { L2CAP, RFCOMM, Configuration.blueCoveUUID(), Consts.uuidSrvClassExt };
			} else {
				searchUuidSet = new UUID[] { L2CAP, RFCOMM, Configuration.blueCoveUUID() };
			}
			if ((Configuration.supportL2CAP) && (Configuration.testL2CAP.booleanValue())) {
				if (Configuration.useServiceClassExtUUID.booleanValue()) {
					searchUuidSet2 = new UUID[] { L2CAP, Configuration.blueCoveL2CAPUUID(), Consts.uuidSrvClassExt };
				} else {
					searchUuidSet2 = new UUID[] { L2CAP, Configuration.blueCoveL2CAPUUID() };
				}
			}
		} else {
			searchUuidSet = new UUID[] { Configuration.discoveryUUID };
		}
		if (!Configuration.testServiceAttributes.booleanValue()) {
			attrIDs = null;
		} else if (Configuration.testAllServiceAttributes.booleanValue()) {
			int allSize = ServiceRecordTester.allTestServiceAttributesSize();
			attrIDs = new int[allSize + 1];
			attrIDs[0] = Consts.TEST_SERVICE_ATTRIBUTE_INT_ID;
			for (int i = 0; i < allSize; i++) {
				attrIDs[1 + i] = Consts.SERVICE_ATTRIBUTE_ALL_START + i;
			}
		} else if (Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) {
			attrIDs = new int[] { Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, Consts.TEST_SERVICE_ATTRIBUTE_URL_ID,
					Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID, Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID,
					Consts.SERVICE_ATTRIBUTE_BYTES_SERVER_INFO };
		} else {
			attrIDs = new int[] {
					0x0009, // BluetoothProfileDescriptorList
					0x0100, // Service name
					Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, Consts.TEST_SERVICE_ATTRIBUTE_STR_ID,
					Consts.TEST_SERVICE_ATTRIBUTE_URL_ID, Consts.TEST_SERVICE_ATTRIBUTE_LONG_ID,
					Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID, Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID,
					Consts.SERVICE_ATTRIBUTE_BYTES_SERVER_INFO, 0x0303, // SupportedFormatList
			};
		}
	}

	public boolean hasServers() {
		return ((serverURLs != null) && (serverURLs.size() >= 1));
	}

	public void shutdown() {
		stoped = true;
		if (inquiring && (discoveryAgent != null)) {
			cancelInquiry();
			cancelServiceSearch();
		}
	}

	private void cancelInquiry() {
		try {
			if (discoveryAgent != null) {
				if (discoveryAgent.cancelInquiry(this)) {
					Logger.debug("Device inquiry was canceled");
				} else if (inquiringDevice) {
					Logger.debug("Device inquiry was not canceled");
				}
			}
		} catch (Throwable e) {
			Logger.error("Cannot cancel Device inquiry", e);
		}
	}

	private void cancelServiceSearch() {
		try {
			if ((servicesSearchTransID != 0) && (discoveryAgent != null)) {
				discoveryAgent.cancelServiceSearch(servicesSearchTransID);
				servicesSearchTransID = 0;
			}
		} catch (Throwable e) {
		}
	}

	public boolean runDeviceInquiry() {
		boolean needToFindDevice = Configuration.clientContinuousDiscoveryDevices
				|| ((devices.size() == 0) && (serverURLs.size() == 0));
		try {
			if (this.config.useDiscoveredDevices) {
				copyDiscoveredDevices();
				this.config.useDiscoveredDevices = false;
			} else if (needToFindDevice) {
				Logger.debug("Starting Device inquiry");
				deviceDiscoveryError = false;
				devices.removeAllElements();
				long start = System.currentTimeMillis();
				inquiring = true;
				inquiringDevice = true;
				try {
					discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
					boolean started = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
					if (!started) {
						Logger.error("Inquiry was not started (may be because the accessCode is not supported)");
						return false;
					}
				} catch (BluetoothStateException e) {
					Logger.error("Cannot start Device inquiry", e);
					return false;
				}
				// By this time inquiryCompleted maybe already been called,
				// because we are too fast
				while (inquiringDevice) {
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e) {
							return false;
						}
					}
				}
				inquiringDevice = false;
				if (this.stoped) {
					return true;
				}
				cancelInquiry();
				Logger.debug("  Device inquiry took " + TimeUtils.secSince(start));
				RemoteDeviceInfo.discoveryInquiryFinished(TimeUtils.since(start));
				if (deviceDiscoveryError && (devices.size() == 0)) {
					return false;
				}
			}

			if (Configuration.clientContinuousServicesSearch || serverURLs.size() == 0) {
				serverURLs.removeAllElements();
				try {
					return startServicesSearch();
				} finally {
					cancelServiceSearch();
				}
			} else {
				return true;
			}
		} finally {
			inquiring = false;
			inquiringDevice = false;
		}
	}

	private void copyDiscoveredDevices() {
		if (RemoteDeviceInfo.devices.size() == 0) {
			Logger.warn("No device in history, run Discovery");
		}
		for (Enumeration iter = RemoteDeviceInfo.devices.elements(); iter.hasMoreElements();) {
			RemoteDeviceInfo dev = (RemoteDeviceInfo) iter.nextElement();
			devices.addElement(dev.remoteDevice);
		}
		if (devices.size() == 0) {
			if (Configuration.storage == null) {
				Logger.warn("no storage");
				return;
			}
			String lastURL = Configuration.getLastServerURL();
			if (StringUtils.isStringSet(lastURL)) {
				Logger.info("Will used device from recent Connections");
				devices.addElement(new RemoteDeviceIheritance(BluetoothTypesInfo.extractBluetoothAddress(lastURL)));
			} else {
				Logger.warn("no recent Connections");
			}
		}
	}

	public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
		if (this.stoped) {
			return;
		}
		if (Configuration.listedDevicesOnly.booleanValue()
				&& !Configuration.isWhiteDevice(remoteDevice.getBluetoothAddress())) {
			Logger.debug("ignore device " + TestResponderClient.niceDeviceName(remoteDevice.getBluetoothAddress())
					+ " " + BluetoothTypesInfo.toString(cod) + " (not listed)");
			return;
		}
		if (Configuration.useMajorDeviceClass(cod.getMajorDeviceClass())) {
			devices.addElement(remoteDevice);
		} else {
			Logger.debug("ignore device " + TestResponderClient.niceDeviceName(remoteDevice.getBluetoothAddress())
					+ " " + BluetoothTypesInfo.toString(cod) + " (by code)");
			return;
		}
		String name = "";
		try {
			if ((Configuration.discoveryGetDeviceFriendlyName.booleanValue()) || RuntimeDetect.isBlueCove) {
				name = " [" + remoteDevice.getFriendlyName(false) + "]";
			}
		} catch (IOException e) {
			Logger.debug("er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
		}
		if (remoteDevice.isTrustedDevice()) {
			name += " Trusted";
		}
		RemoteDeviceInfo.deviceFound(remoteDevice);
		Logger.debug("deviceDiscovered " + TestResponderClient.niceDeviceName(remoteDevice.getBluetoothAddress())
				+ name + " " + remoteDevice.getBluetoothAddress() + " " + BluetoothTypesInfo.toString(cod));
	}

	private boolean startServicesSearch() {
		if (devices.size() == 0) {
			return true;
		}
		Logger.debug(this.config.logID + "Starting Services search " + TimeUtils.timeNowToString());
		long inquiryStart = System.currentTimeMillis();
		nextDevice: for (Enumeration iter = devices.elements(); iter.hasMoreElements();) {
			if (this.stoped) {
				break;
			}
			servicesFound = false;
			anyServicesFound = false;
			anyServicesFoundCount = 0;
			long start = System.currentTimeMillis();
			RemoteDevice remoteDevice = (RemoteDevice) iter.nextElement();
			String name = "";
			if ((Configuration.discoveryGetDeviceFriendlyName.booleanValue()) || RuntimeDetect.isBlueCove) {
				try {
					name = remoteDevice.getFriendlyName(false);
					if ((name != null) && (name.length() > 0)) {
						TestResponderClient.recentDeviceNames.put(remoteDevice.getBluetoothAddress().toUpperCase(),
								name);
					}
				} catch (Throwable e) {
					Logger.error(this.config.logID + "er.getFriendlyName," + remoteDevice.getBluetoothAddress(), e);
				}
			}
			servicesOnDeviceAddress = remoteDevice.getBluetoothAddress();
			servicesOnDeviceName = TestResponderClient.niceDeviceName(servicesOnDeviceAddress);
			if (servicesOnDeviceName.equals(name)) {
				name = "";
			}
			Logger.debug(this.config.logID + "Search Services on " + servicesOnDeviceAddress + " "
					+ servicesOnDeviceName + " " + name);

			int transID = -1;

			for (int uuidType = 1; uuidType <= 2; uuidType++) {
				UUID[] uuidSet = searchUuidSet;
				if (uuidType == 2) {
					if (searchUuidSet2 != null) {
						uuidSet = searchUuidSet2;
					} else {
						break;
					}
				}
				try {
					discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();

					int[] shortAttrSet;
					if ((TestResponderClient.sdAttrRetrievableMax != 0) && (attrIDs != null)
							&& (TestResponderClient.sdAttrRetrievableMax < attrIDs.length)) {
						shortAttrSet = new int[TestResponderClient.sdAttrRetrievableMax];
						for (int i = 0; i < TestResponderClient.sdAttrRetrievableMax; i++) {
							shortAttrSet[i] = attrIDs[i];
						}
						Logger.debug(this.config.logID + "search attr first " + shortAttrSet.length + " of "
								+ attrIDs.length);
					} else {
						shortAttrSet = attrIDs;
					}
					searchingServices = true;
					servicesSearchTransID = discoveryAgent.searchServices(shortAttrSet, uuidSet, remoteDevice, this);
					transID = servicesSearchTransID;
					if (transID <= 0) {
						Logger.warn(this.config.logID + "servicesSearch TransID mast be positive, " + transID);
					}
				} catch (BluetoothStateException e) {
					Logger.error(this.config.logID + "Cannot start searchServices on " + servicesOnDeviceName, e);
					if (!this.config.searchServiceRetry) {
						this.stoped = true;
						return false;
					}
					continue nextDevice;
				}
				// By this time serviceSearchCompleted maybe already been
				// called, because we are too fast
				while (searchingServices) {
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e) {
							break;
						}
					}
				}
				cancelServiceSearch();
			}

			RemoteDeviceInfo.searchServices(remoteDevice, servicesFound, TimeUtils.since(start));
			String msg = (anyServicesFound) ? "; " + anyServicesFoundCount + " service(s) found" : "; no services";
			Logger.debug(this.config.logID + "Services Search " + transID + " took " + TimeUtils.secSince(start) + msg);
		}
		String msg = "";
		if (serverURLs.size() > 0) {
			msg = "; BC Srv(s) " + serverURLs.size();
		}
		Logger.debug(this.config.logID + "Services search completed " + TimeUtils.secSince(inquiryStart) + msg);
		return true;
	}

	void populateAllservicesAttributes(ServiceRecord servRecord) {
		int lastId = 0xffff;
		for (int j = 0; j <= lastId; j += TestResponderClient.sdAttrRetrievableMax) {
			int max = TestResponderClient.sdAttrRetrievableMax;
			if (j + max > lastId) {
				max = lastId - j;
			}
			int[] shortAttrSet = new int[max];
			int id = j;
			for (int n = 0; n < max; n++, id++) {
				shortAttrSet[n] = id;
			}
			try {
				servRecord.populateRecord(shortAttrSet);
			} catch (IOException e) {
				Logger.error("Cannot populateRecord " + j, e);
				break;
			}
		}
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		if (this.stoped) {
			return;
		}
		for (int i = 0; i < servRecord.length; i++) {
			anyServicesFound = true;
			anyServicesFoundCount++;
			String url = servRecord[i].getConnectionURL(Configuration.getRequiredSecurity(), false);
			Logger.info("*found server " + url);
			if (this.config.discoveryOnce) {
				if (Configuration.testAllServiceAttributes.booleanValue()
						&& (TestResponderClient.sdAttrRetrievableMax != 0)) {
					// populateAllservicesAttributes(servRecord[i]);
				}
				Logger.debug(this.config.logID + "ServiceRecord " + (i + 1) + "/" + servRecord.length + "\n"
						+ BluetoothTypesInfo.toString(servRecord[i]));
			}
			if (url == null) {
				// Bogus service Record
				continue;
			}
			RemoteDeviceInfo.saveServiceURL(servRecord[i]);

			boolean isBlueCoveTestService;

			if (this.config.searchOnlyBluecoveUuid) {
				isBlueCoveTestService = ServiceRecordTester.testServiceAttributes(servRecord[i], servicesOnDeviceName,
						servicesOnDeviceAddress);
			} else {
				isBlueCoveTestService = ServiceRecordTester.hasServiceClassBlieCoveUUID(servRecord[i]);
				if (isBlueCoveTestService) {

					// Retive other service attributes
					if ((TestResponderClient.sdAttrRetrievableMax != 0) && (attrIDs != null)
							&& (TestResponderClient.sdAttrRetrievableMax < attrIDs.length)) {
						// int[] shortAttrSet;
						for (int ai = TestResponderClient.sdAttrRetrievableMax; ai < attrIDs.length; ai++) {
							try {
								servRecord[i].populateRecord(new int[] { attrIDs[ai] });
							} catch (IOException e) {
								Logger.error("populateRecord", e);
							}
						}
					}

					ServiceRecordTester.testServiceAttributes(servRecord[i], servicesOnDeviceName,
							servicesOnDeviceAddress);
				}
			}

			if (isBlueCoveTestService) {
				TestResponderClient.discoveryCount++;
				Logger.info(this.config.logID + "Found BlueCove SRV:"
						+ TestResponderClient.niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
			}

			if (this.config.searchOnlyBluecoveUuid || isBlueCoveTestService) {
				serverURLs.addElement(url);
			} else {
				Logger.info(this.config.logID + "is not TestService on "
						+ TestResponderClient.niceDeviceName(servRecord[i].getHostDevice().getBluetoothAddress()));
			}
			if (isBlueCoveTestService) {
				servicesFound = true;
			}
		}
	}

	public synchronized void serviceSearchCompleted(int transID, int respCode) {
		switch (respCode) {
		case SERVICE_SEARCH_ERROR:
			Logger.error(this.config.logID + "error occurred while processing the service search");
			break;
		case SERVICE_SEARCH_TERMINATED:
			Logger.info(this.config.logID + "SERVICE_SEARCH_TERMINATED");
			break;
		case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
			Logger.info(this.config.logID + "SERVICE_SEARCH_DEVICE_NOT_REACHABLE");
			break;
		}
		searchingServices = false;
		notifyAll();
	}

	public synchronized void inquiryCompleted(int discType) {
		switch (discType) {
		case INQUIRY_ERROR:
			Logger.error("device inquiry ended abnormally");
			deviceDiscoveryError = true;
			break;
		case INQUIRY_TERMINATED:
			Logger.info("Device discovery has been canceled by the application");
			break;
		case INQUIRY_COMPLETED:
		}
		inquiringDevice = false;
		notifyAll();
	}

}