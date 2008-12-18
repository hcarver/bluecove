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
package org.bluecove.tester.obex;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.me.LoggerCanvas;
import org.bluecove.tester.util.ThreadUtils;

public class BlueCoveObexCanvas extends LoggerCanvas implements CommandListener, TestControl {

	static final Command exitCommand = new Command("Exit", Command.EXIT, 0);

	static final Command continueCommand = new Command("1-Continue", Command.ITEM, 1);

	static final Command startRunCommand = new Command("*-Run", Command.ITEM, 2);

	static final Command selectTestCommand = new Command("Select Test", Command.ITEM, 3);

	static final Command clearCommand = new Command("#-Clear", Command.ITEM, 8);

	private Object pauseLock = new Object();

	private boolean pauseOn = false;

	public BlueCoveObexCanvas() {
		super();
		super.setTitle("BlueCoveO");

		TestSelector.testControl = this;

		logTimeStamp = true;

		addCommand(exitCommand);
		addCommand(startRunCommand);
		addCommand(selectTestCommand);
		addCommand(clearCommand);

		setCommandListener(this);
	}

	protected String getCanvasTitleText() {
		return "BlueCoveO Tester";
	}

	public void pause(String mesage) {
		addCommand(continueCommand);
		Logger.info("pause: " + mesage);
		pauseOn = true;
		while (pauseOn) {
			synchronized (pauseLock) {
				try {
					pauseLock.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Pause Interrupted");
				}
			}
		}
	}

	protected void runContinue() {
		removeCommand(continueCommand);
		pauseOn = false;
		synchronized (pauseLock) {
			pauseLock.notifyAll();
		}
	}

	public void commandAction(final Command c, Displayable d) {
		Runnable r = new Runnable() {
			public void run() {
				if (c == exitCommand) {
					BlueCoveObexMIDlet.exit();
				} else if (c == clearCommand) {
					clearLog();
				} else if (c == startRunCommand) {
					TestSelector.runTest();
				} else if (c == selectTestCommand) {
					TestSelector.selectTest();
				} else if (c == continueCommand) {
					runContinue();
				} else {
					if (c != null) {
						Logger.info("Command " + c.getLabel() + " not found");
					}
				}
			}
		};
		ThreadUtils.invokeLater(r, c.getLabel());
	}

	protected void keyPressed(int keyCode) {
		switch (keyCode) {
		case '0':
			logScrollBottom();
			break;
		case '3':
			TestSelector.selectTest();
			break;
		case '*':
			Runnable r = new Runnable() {
				public void run() {
					TestSelector.runTest();
				}
			};
			ThreadUtils.invokeLater(r, "RunTests");
			break;
		case '1':
			runContinue();
			break;
		case '#':
			clearLog();
			break;
		default:
			logLinesMove(getGameAction(keyCode));
		}
		repaint();
	}

	protected void keyRepeated(int keyCode) {
		logLinesMove(getGameAction(keyCode));
	}

}
