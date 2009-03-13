/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package org.bluecove.tester.me;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;

/**
 * 
 */
public abstract class BaseTestMIDlet extends MIDlet {

	protected static BaseTestMIDlet instance;

	protected static Display display;

	private Displayable mainDisplayable;

	/** The messages are shown in this app this amount of time. */
	static final int ALERT_TIMEOUT = 10000;

	protected abstract Displayable createMainDisplayable();

	protected void startApp() throws MIDletStateChangeException {
		RuntimeDetect.detectCLDC();
		instance = this;
		display = Display.getDisplay(this);
		if (mainDisplayable == null) {
			mainDisplayable = createMainDisplayable();
		}
		showMain();
	}

	protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
	}

	protected void pauseApp() {
	}

	public static void showMain() {
		setCurrentDisplayable(instance.mainDisplayable);
	}

	public static void setCurrentDisplayable(Displayable nextDisplayable) {
		display.setCurrent(nextDisplayable);
	}

	public static void exit() {
		try {
			instance.destroyApp(true);
		} catch (MIDletStateChangeException e) {
		}
		instance.notifyDestroyed();
	}

	/** Show error message to user when something wrong with app */
	public static void error(final String message) {
		Logger.error(message);
		Alert al = new Alert("Error", message, null, AlertType.ERROR);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	public static void alert(final String title, final String message) {
		Logger.debug(title + " " + message);
		Alert al = new Alert(title, message, null, AlertType.INFO);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	public static void alert(String message) {
		alert("Message", message);
	}

	public static void message(final String title, final String message) {
		Logger.debug(title + " " + message);
		Alert al = new Alert(title, message, null, AlertType.INFO);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	private static void showAlertLater(final Alert al) {
		Runnable r = new Runnable() {
			public void run() {
				setCurrentDisplayable(al);
			}
		};
		display.callSerially(r);
	}

}
