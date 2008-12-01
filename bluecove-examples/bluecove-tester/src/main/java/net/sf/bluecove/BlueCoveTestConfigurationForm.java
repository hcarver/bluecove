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
import java.util.Vector;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.TextField;

import net.sf.bluecove.util.BooleanVar;
import net.sf.bluecove.util.IntVar;
import net.sf.bluecove.util.StringVar;

public class BlueCoveTestConfigurationForm extends Form implements CommandListener {

	Command okCommand = new Command("Ok", Command.OK, 2);

	Command cancelCommand = new Command("Cancel", Command.CANCEL, 3);

	Vector configItems = new Vector();

	private static final String[] booleanStrings = new String[] { "on" };

	private class ConfigurationComponent {
		String name;

		Item guiComponent;

		Object configField;
	}

	BlueCoveTestConfigurationForm() {
		super("Configuration");

		addCommand(okCommand);
		addCommand(cancelCommand);
		setCommandListener(this);

		buildFields();

	}

	public void commandAction(Command c, Displayable d) {
		if (c == okCommand) {
			saveConfig();
			BlueCoveTestMIDlet.showMain();
		} else if (c == cancelCommand) {
			BlueCoveTestMIDlet.showMain();
		}

	}

	private void buildFields() {
		addConfig("deviceClassFilter", Configuration.deviceClassFilter);
		addConfig("discoverDevicesComputers", Configuration.discoverDevicesComputers);
		addConfig("discoverDevicesPhones", Configuration.discoverDevicesPhones);
		addConfig("getDeviceFriendlyName", Configuration.discoveryGetDeviceFriendlyName);
		addConfig("listedDevicesOnly", Configuration.listedDevicesOnly);
		// addConfig("discoveryUUID");

		addConfig("useShortUUID", Configuration.useShortUUID);
		addConfig("useServiceClassExtUUID", Configuration.useServiceClassExtUUID);
		addConfig("clientContinuous", Configuration.clientContinuous);
		addConfig("clientContinuousDiscovery", Configuration.clientContinuousDiscovery);
		// addConfig("clientContinuousDiscoveryDevices");
		// addConfig("clientContinuousServicesSearch");
		// addConfig("clientTestConnections");

		addConfig("srvConnectedAccept", Configuration.serverAcceptWhileConnected);

		addConfig("authenticate", Configuration.authenticate);
		addConfig("encrypt", Configuration.encrypt);
		// addConfig("authorize");
		addConfig("RFCOMM", Configuration.testRFCOMM);
		addConfig("TEST_CASE_FIRST", Configuration.TEST_CASE_FIRST);
		addConfig("TEST_CASE_LAST", Configuration.TEST_CASE_LAST);
		addConfig("STERSS_TEST_CASE", Configuration.STERSS_TEST_CASE);
		addConfig("L2CAP", Configuration.testL2CAP);
		addConfig("authenticateOBEX", Configuration.authenticateOBEX);
		addConfig("tcp obex", Configuration.testServerOBEX_TCP);
		addConfig("test Srvc Attr", Configuration.testServiceAttributes);
		addConfig("test Ignore Broken Srv Attr", Configuration.testIgnoreNotWorkingServiceAttributes);
		addConfig("test All Srv Attr", Configuration.testAllServiceAttributes);

	}

	private void addConfig(String name, Object var) {
		ConfigurationComponent cc = new ConfigurationComponent();
		cc.configField = var;
		cc.name = name;
		if (var instanceof BooleanVar) {
			ChoiceGroup c = new ChoiceGroup(name, Choice.MULTIPLE, booleanStrings, null);
			c.setSelectedIndex(0, ((BooleanVar) var).booleanValue());
			cc.guiComponent = c;
		} else if (var instanceof StringVar) {
			cc.guiComponent = new TextField(name, var.toString(), 128, TextField.ANY);
		} else if (var instanceof IntVar) {
			cc.guiComponent = new TextField(name, var.toString(), 5, TextField.DECIMAL);
		} else {
			Logger.error("Unsupported type " + cc.name + " " + var.getClass().getName());
			return;
		}
		configItems.addElement(cc);
		append(cc.guiComponent);
	}

	private void saveConfig() {
		for (Enumeration en = configItems.elements(); en.hasMoreElements();) {
			ConfigurationComponent cc = (ConfigurationComponent) en.nextElement();
			if (cc.configField instanceof BooleanVar) {
				((BooleanVar) cc.configField).setValue(((ChoiceGroup) cc.guiComponent).isSelected(0));
			} else if (cc.configField instanceof StringVar) {
				((StringVar) cc.configField).setValue(((TextField) cc.guiComponent).getString());
			} else if (cc.configField instanceof IntVar) {
				((IntVar) cc.configField).setValue(((TextField) cc.guiComponent).getString());
			}
		}
	}

}
