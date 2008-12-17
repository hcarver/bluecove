/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.util.Enumeration;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sf.bluecove.util.StorageRMS;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.me.LoggerCanvas;
import org.bluecove.tester.util.ThreadUtils;
import org.bluecove.tester.util.TimeUtils;

public class BlueCoveTestCanvas extends LoggerCanvas implements CommandListener {

	static final Command exitCommand = new Command("Exit", Command.EXIT, 0);

	static final Command printStatsCommand = new Command("1-Print Stats", Command.ITEM, 1);

	static final Command startDiscoveryCommand = new Command("*-Discovery", Command.ITEM, 2);

	static final Command startServicesSearchCommand = new Command("7-Services Search", Command.ITEM, 2);

	static final Command startClientCommand = new Command("2-Client Start", Command.ITEM, 2);

	static final Command stopClientCommand = new Command("3-Client Stop", Command.ITEM, 3);

	static final Command startServerCommand = new Command("5-Server Start", Command.ITEM, 4);

	static final Command stopServerCommand = new Command("6-Server Stop", Command.ITEM, 5);

	static final Command startSwitcherCommand = new Command("8-Switcher Start", Command.ITEM, 6);

	static final Command stopSwitcherCommand = new Command("9-Switcher Stop", Command.ITEM, 7);

	static final Command clearCommand = new Command("#-Clear", Command.ITEM, 8);

	static final Command printFailureLogCommand = new Command("4-Print FailureLog", Command.ITEM, 8);

	static final Command startClientStressCommand = new Command("Client Stress Start", Command.ITEM, 9);

	static final Command startClientLastServiceCommand = new Command("Client Last service Start", Command.ITEM, 10);

	static final Command startClientLastDeviceCommand = new Command("Client Last device Start", Command.ITEM, 11);

	static final Command startTCKAgentCommand = new Command("TCK Agent", Command.ITEM, 12);

	static final Command configurationCommand = new Command("Options...", Command.ITEM, 13);

	static final Command obexPutCommand = new Command("ObexPut", Command.ITEM, 14);

	private Switcher switcher;

	private int errorCount = 0;

	public BlueCoveTestCanvas() {
		super();
		super.setTitle("BlueCoveT");

		addCommand(exitCommand);
		addCommand(startDiscoveryCommand);
		addCommand(startServicesSearchCommand);
		addCommand(startClientCommand);
		addCommand(stopClientCommand);
		addCommand(startServerCommand);
		addCommand(stopServerCommand);
		addCommand(printStatsCommand);
		addCommand(startSwitcherCommand);
		addCommand(stopSwitcherCommand);
		addCommand(printFailureLogCommand);
		addCommand(clearCommand);
		addCommand(startClientStressCommand);
		addCommand(startClientLastServiceCommand);
		addCommand(startClientLastDeviceCommand);
		if (Configuration.likedTCKAgent) {
			addCommand(startTCKAgentCommand);
		}
		addCommand(configurationCommand);
		if (TestOBEXCilent.obexEnabled) {
			addCommand(obexPutCommand);
		}
		setCommandListener(this);

		Configuration.storage = new StorageRMS();
	}

	protected String getCanvasTitleText() {
		return "BlueCove Tester";
	}

	protected String getCanvasStatusText() {
		StringBuffer msg = new StringBuffer();
		msg.append("(");
		msg.append("srv:").append((Switcher.isRunningServer()) ? "ON" : "off").append(" ").append(
				Switcher.serverStartCount);
		msg.append(" cli:").append((Switcher.isRunningClient()) ? "ON" : "off").append(" ").append(
				Switcher.clientStartCount);
		msg.append(" X:").append((Switcher.isRunning()) ? "ON" : "off");
		msg.append(" dc:").append(TestResponderClient.discoveryCount);
		msg.append(" er:").append(errorCount);
		msg.append(")");
		return msg.toString();
	}

	protected void keyPressed(int keyCode) {
		switch (keyCode) {
		case '1':
			printStats();
			break;
		case '4':
			printFailureLog();
			break;
		case '0':
			logScrollBottom();
			break;
		case '*':
			Switcher.startDiscovery();
			break;
		case '7':
			Switcher.startServicesSearch();
			break;
		case '2':
			Switcher.startClient();
			break;
		case '3':
			Switcher.clientShutdown();
			break;
		case '5':
			Switcher.startServer();
			break;
		case '6':
			Switcher.serverShutdown();
			break;
		case '8':
			startSwitcher();
			break;
		case '9':
			stopSwitcher();
			break;
		case '#':
			clear();
			break;
		default:
			logLinesMove(getGameAction(keyCode));
		}
		repaint();
	}

	protected void keyRepeated(int keyCode) {
		logLinesMove(getGameAction(keyCode));
	}

	private void stopSwitcher() {
		if (switcher != null) {
			switcher.shutdown();
			switcher = null;
		}
	}

	private void printStats() {
		Logger.info("--- discovery stats ---");
		int deviceCnt = 0;
		int deviceActiveCnt = 0;
		long activeDeadline = System.currentTimeMillis() - 1000 * 60 * 4;
		for (Enumeration iter = RemoteDeviceInfo.devices.elements(); iter.hasMoreElements();) {
			RemoteDeviceInfo dev = (RemoteDeviceInfo) iter.nextElement();
			deviceCnt++;
			StringBuffer buf = new StringBuffer();
			buf.append(TestResponderClient.niceDeviceName(dev.remoteDevice.getBluetoothAddress()));
			buf.append(" dc:").append(dev.serviceDiscovered.count);
			buf.append(" first:").append(TimeUtils.timeToString(dev.serviceDiscoveredFirstTime));
			buf.append(" last:").append(TimeUtils.timeToString(dev.serviceDiscoveredLastTime));
			Logger.info(buf.toString());
			buf = new StringBuffer();
			buf.append(" avg ddf:").append(dev.avgDiscoveryFrequencySec());
			buf.append(" sdf:").append(dev.avgServiceDiscoveryFrequencySec());
			buf.append(" ss:").append(dev.avgServiceSearchDurationSec());
			buf.append(" sss:").append(dev.serviceSearchSuccessPrc()).append("%");
			if (dev.serviceDiscoveredLastTime > activeDeadline) {
				deviceActiveCnt++;
				buf.append(" Active");
			} else {
				buf.append(" Down");
			}
			if (dev.variableData == 0) {
				buf.append(" No VarAttr");
			} else {
				buf.append(" srv:").append(dev.variableData);
				if (dev.variableDataUpdated) {
					buf.append(" var.OK");
				}
			}
			Logger.info(buf.toString());
		}
		StringBuffer buf = new StringBuffer();
		buf.append("all avg");
		buf.append(" srv:").append(TestResponderServer.avgServerDurationSec());
		buf.append(" di:").append(RemoteDeviceInfo.allAvgDeviceInquiryDurationSec());
		buf.append(" ss:").append(RemoteDeviceInfo.allAvgServiceSearchDurationSec());
		Logger.info(buf.toString());

		buf = new StringBuffer();
		buf.append("all max");
		buf.append(" srv:").append(TestResponderServer.allServerDuration.durationMaxSec());
		buf.append(" di:").append(RemoteDeviceInfo.deviceInquiryDuration.durationMaxSec());
		buf.append(" ss:").append(RemoteDeviceInfo.allServiceSearch.durationMaxSec());
		Logger.info(buf.toString());

		buf = new StringBuffer();
		buf.append("devices:").append(deviceCnt).append(" active:").append(deviceActiveCnt);
		buf.append(" threads:").append(Thread.activeCount());
		Logger.info(buf.toString());
		Logger.info("-----------------------");
		Logger.info("*Client Success:" + TestResponderClient.countSuccess + " Failure:"
				+ TestResponderClient.failure.countFailure);
		Logger.info("*Server Success:" + TestResponderServer.countSuccess + " Failure:"
				+ TestResponderServer.failure.countFailure);
		logScrollBottom();
	}

	private void printFailureLog() {
		Logger.info("*Client Success:" + TestResponderClient.countSuccess + " Failure:"
				+ TestResponderClient.failure.countFailure);
		Logger.debug("Client avg conn concurrent " + TestResponderClient.concurrentStatistic.avg());
		Logger.debug("Client max conn concurrent " + TestResponderClient.concurrentStatistic.max());
		Logger.debug("Client avg conn time " + TestResponderClient.connectionDuration.avg() + " msec");
		Logger.debug("Client avg conn retry " + TestResponderClient.connectionRetyStatistic.avgPrc());

		TestResponderClient.failure.writeToLog();
		Logger.info("*Server Success:" + TestResponderServer.countSuccess + " Failure:"
				+ TestResponderServer.failure.countFailure);
		Logger.debug("Server avg conn concurrent " + TestResponderServer.concurrentStatistic.avg());
		Logger.debug("Server avg conn time " + TestResponderServer.connectionDuration.avg() + " msec");

		TestResponderServer.failure.writeToLog();
		logScrollBottom();
	}

	private void clear() {
		clearLog();
		TestResponderClient.clear();
		TestResponderServer.clear();
		Switcher.clear();
		RemoteDeviceInfo.clear();
		repaint();
	}

	private void startSwitcher() {
		if (switcher == null) {
			switcher = new Switcher();
		}
		if (!switcher.isRunning) {
			(switcher.thread = new Thread(switcher)).start();
		} else {
			BlueCoveTestMIDlet.message("Warn", "Switcher isRunning");
		}
	}

	public void commandAction(final Command c, Displayable d) {
		Runnable r = new Runnable() {
			public void run() {
				if (c == exitCommand) {
					Switcher.clientShutdown();
					Switcher.serverShutdownOnExit();
					BlueCoveTestMIDlet.exit();
					return;
				} else if (c == printStatsCommand) {
					printStats();
				} else if (c == printFailureLogCommand) {
					printFailureLog();
				} else if (c == clearCommand) {
					clear();
				} else if (c == startDiscoveryCommand) {
					Switcher.startDiscovery();
				} else if (c == startServicesSearchCommand) {
					Switcher.startServicesSearch();
				} else if (c == startClientCommand) {
					Switcher.startClient();
				} else if (c == startClientStressCommand) {
					Switcher.startClientStress();
				} else if (c == startClientLastServiceCommand) {
					Switcher.startClientLastURl();
				} else if (c == startClientLastDeviceCommand) {
					Switcher.startClientLastDevice();
				} else if (c == stopClientCommand) {
					Switcher.clientShutdown();
				} else if (c == stopServerCommand) {
					Switcher.serverShutdown();
				} else if (c == startServerCommand) {
					Switcher.startServer();
				} else if (c == startSwitcherCommand) {
					startSwitcher();
				} else if (c == stopSwitcherCommand) {
					stopSwitcher();
				} else if (c == configurationCommand) {
					try {
						BlueCoveTestMIDlet.setCurrentDisplayable(new BlueCoveTestConfigurationForm());
					} catch (Throwable e) {
						Logger.error("Internal error", e);
					}
				} else if ((Configuration.likedTCKAgent) && (c == startTCKAgentCommand)) {
					Switcher.startTCKAgent();
				} else if ((TestOBEXCilent.obexEnabled) && (c == obexPutCommand)) {
					TestOBEXCilent.obexPut();
				} else {
					if (c != null) {
						Logger.info("Command " + c.getLabel() + " not found");
					}
				}
			}
		};
		ThreadUtils.invokeLater(r, c.getLabel());
	}

}
