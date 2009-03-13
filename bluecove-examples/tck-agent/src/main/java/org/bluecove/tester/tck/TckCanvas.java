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
package org.bluecove.tester.tck;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.me.BaseTestMIDlet;
import org.bluecove.tester.me.LoggerCanvas;
import org.bluecove.tester.util.ThreadUtils;

public class TckCanvas extends LoggerCanvas implements CommandListener {

	static final Command exitCommand = new Command("Exit", Command.EXIT, 0);

	static final Command startBluetoothTCKCommand = new Command("Start BT Agent", Command.ITEM, 1);

	static final Command startObexTCKCommand = new Command("Start Obex Agent", Command.ITEM, 2);

	static final Command stopAgentCommand = new Command("Stop Agent", Command.ITEM, 3);

	static final Command runGarbageCollectoCommand = new Command("Run GC", Command.ITEM, 4);

	static final Command clearCommand = new Command("#-Clear", Command.ITEM, 8);

	public TckCanvas() {
		super();
		super.setTitle("JSR-82 TCK agent");

		logTimeStamp = true;

		addCommand(exitCommand);
		addCommand(startBluetoothTCKCommand);
		addCommand(startObexTCKCommand);
		if (ThreadUtils.canInterruptThread()) {
			addCommand(stopAgentCommand);
		}
		addCommand(runGarbageCollectoCommand);
		addCommand(clearCommand);

		setCommandListener(this);
	}

	protected String getCanvasTitleText() {
		return "JSR-82 TCK agent";
	}

	public void commandAction(final Command c, Displayable d) {
		Runnable r = new Runnable() {
			public void run() {
				if (c == exitCommand) {
					BaseTestMIDlet.exit();
				} else if (c == clearCommand) {
					clearLog();
					Logger.runGarbageCollector();
				} else if (c == startBluetoothTCKCommand) {
					TckStarter.startBluetoothTCK();
				} else if (c == startObexTCKCommand) {
					TckStarter.startObexTCK(false);
				} else if (c == stopAgentCommand) {
					TckStarter.stopAgent();
				} else if (c == runGarbageCollectoCommand) {
				    Logger.runGarbageCollector();
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
		case '#':
			clearLog();
			Logger.runGarbageCollector();
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
