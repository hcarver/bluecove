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
package net.sf.bluecove.swing;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.se.BlueCoveSpecific;
import net.sf.bluecove.se.FileStorage;
import net.sf.bluecove.se.JavaSECommon;
import net.sf.bluecove.se.UIHelper;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.log.LoggerAppender;
import org.bluecove.tester.util.TimeUtils;

/**
 *
 * 
 */
public class Main extends JFrame implements LoggerAppender {

	private static final long serialVersionUID = 1L;

	private JTextArea output = null;

	private int outputLines = 0;

	private Vector logLinesQueue = new Vector();

	private boolean logUpdaterRunning = false;

	JMenuItem debugOn;

	public static void main(String[] args) {
		JavaSECommon.initOnce();
		Configuration.storage = new FileStorage();

		Main app = new Main();
		app.setVisible(true);

		Logger.debug("Stated app");
		Logger.debug("OS:" + System.getProperty("os.name") + "|" + System.getProperty("os.version") + "|"
				+ System.getProperty("os.arch"));
		Logger.debug("Java:" + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
	}

	Main() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				quit();
			}
		});

		Logger.addAppender(this);
		BlueCoveSpecific.addAppender(this);

		this.setTitle("BlueCove tester");

		final JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu menuBluetooth = new JMenu("Bluetooth");

		final JMenuItem serverStart = addMenu(menuBluetooth, "Server Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServer();
				updateTitle();
			}
		}, KeyEvent.VK_5);

		final JMenuItem serverStop = addMenu(menuBluetooth, "Server Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.serverShutdown();
			}
		}, KeyEvent.VK_6);

		final JMenuItem clientStart = addMenu(menuBluetooth, "Client Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClient();
				updateTitle();
			}
		}, KeyEvent.VK_2);

		final JMenuItem clientStop = addMenu(menuBluetooth, "Client Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.clientShutdown();
			}
		}, KeyEvent.VK_3);

		final JMenuItem tckStart;
		if (Configuration.likedTCKAgent) {
			tckStart = addMenu(menuBluetooth, "Start TCK Agent", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Switcher.startTCKAgent();
				}
			});
		} else {
			tckStart = null;
		}

		addMenu(menuBluetooth, "Discovery", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startDiscovery();
				updateTitle();
			}
		}, KeyEvent.VK_MULTIPLY);

		addMenu(menuBluetooth, "Services Search", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServicesSearch();
				updateTitle();
			}
		}, KeyEvent.VK_7);

		addMenu(menuBluetooth, "Client Stress Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClientStress();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Client selectService Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClientSelectService();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Client Last service Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClientLastURl();
				updateTitle();
			}
		});

		addMenu(menuBluetooth, "Client Last device Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClientLastDevice();
				updateTitle();
			}
		});

		final JMenuItem stop = addMenu(menuBluetooth, "Stop all work", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.clientShutdown();
				Switcher.serverShutdown();
			}
		}, KeyEvent.VK_S);

		addMenu(menuBluetooth, "Quit", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		}, KeyEvent.VK_X);

		menuBar.add(menuBluetooth);

		JMenu menuLogs = new JMenu("Logs");

		debugOn = addMenu(menuLogs, "BlueCove Debug ON", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean dbg = BlueCoveSpecific.changeDebug();
				if (dbg) {
					debugOn.setText("BlueCove Debug OFF");
				} else {
					debugOn.setText("BlueCove Debug ON");
				}
			}
		});

		addMenu(menuLogs, "Clear Log", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		}, KeyEvent.VK_Z);

		addMenu(menuLogs, "Print FailureLog", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UIHelper.printFailureLog();
			}
		}, KeyEvent.VK_4);

		addMenu(menuLogs, "Clear Stats", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UIHelper.clearStats();
			}
		});

		if (JavaSECommon.isJava5()) {
			addMenu(menuLogs, "ThreadDump", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JavaSECommon.threadDump();
				}
			});
		}

		menuBar.add(menuLogs);

		output = new JTextArea("");
		output.setEditable(false);
		Font logFont = new Font("Monospaced", Font.PLAIN, 12);
		output.setFont(logFont);

		JScrollPane scroll = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		getContentPane().add(scroll);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.height < 400) {
			Configuration.screenSizeSmall = true;
			Font smallLogFont = new Font("Monospaced", Font.PLAIN, 9);
			output.setFont(smallLogFont);
		}

		if (screenSize.width > 600) {
			screenSize.setSize(240, 320);
		}
		if (this.isResizable()) {
			Rectangle b = this.getBounds();
			b.x = Integer.valueOf(Configuration.getStorageData("main.x", "0")).intValue();
			b.y = Integer.valueOf(Configuration.getStorageData("main.y", "0")).intValue();
			b.height = Integer.valueOf(Configuration.getStorageData("main.height", String.valueOf(screenSize.height)))
					.intValue();
			b.width = Integer.valueOf(Configuration.getStorageData("main.width", String.valueOf(screenSize.width)))
					.intValue();
			this.setBounds(b);
		}
	}

	private JMenuItem addMenu(JMenu menu, String name, ActionListener l) {
		return addMenu(menu, name, l, 0);
	}

	private JMenuItem addMenu(JMenu menu, String name, ActionListener l, int key) {
		JMenuItem menuItem = new JMenuItem(name);
		menuItem.addActionListener(l);
		menu.add(menuItem);
		if (key != 0) {
			// menuItem.setShortcut(new MenuShortcut(key, false));
		}
		return menuItem;
	}

	private void updateTitle() {
		this.setTitle(UIHelper.getMainWindowTitle());
	}

	private void clear() {
		if (output == null) {
			return;
		}
		output.setText("");
		outputLines = 0;
	}

	private void quit() {
		Logger.debug("quit");
		Switcher.clientShutdown();
		Switcher.serverShutdownOnExit();

		Rectangle b = this.getBounds();
		Configuration.storeData("main.x", String.valueOf(b.x));
		Configuration.storeData("main.y", String.valueOf(b.y));
		Configuration.storeData("main.height", String.valueOf(b.height));
		Configuration.storeData("main.width", String.valueOf(b.width));

		Logger.removeAppender(this);
		BlueCoveSpecific.removeAppender();

		// this.dispose();
		System.exit(0);
	}

	public void appendLog(int level, String message, Throwable throwable) {
		if (output == null) {
			return;
		}
		final StringBuffer buf = new StringBuffer();

		if (Configuration.logTimeStamp) {
			String time = TimeUtils.timeNowToString();
			buf.append(time).append(" ");
		}

		switch (level) {
		case Logger.ERROR:
			// errorCount ++;
			buf.append("e.");
			break;
		case Logger.WARN:
			buf.append("w.");
			break;
		case Logger.INFO:
			buf.append("i.");
			break;
		}
		buf.append(message);
		if (throwable != null) {
			buf.append(' ');
			String className = throwable.getClass().getName();
			buf.append(className.substring(1 + className.lastIndexOf('.')));
			if (throwable.getMessage() != null) {
				buf.append(':');
				buf.append(throwable.getMessage());
			}
		}
		buf.append("\n");
		boolean createUpdater = false;
		synchronized (logLinesQueue) {
			if (logLinesQueue.isEmpty()) {
				createUpdater = true;
			} else {
				createUpdater = !logUpdaterRunning;
			}
			logLinesQueue.addElement(buf.toString());
		}
		if (createUpdater) {
			try {
				EventQueue.invokeLater(new AwtLogUpdater());
			} catch (NoSuchMethodError java1) {
				(new AwtLogUpdater()).run();
			}
		}
	}

	private class AwtLogUpdater implements Runnable {

		private String getNextLine() {
			synchronized (logLinesQueue) {
				if (logLinesQueue.isEmpty()) {
					return null;
				}
				String line = (String) logLinesQueue.firstElement();
				logLinesQueue.removeElementAt(0);
				return line;
			}
		}

		public void run() {
			int oneCallCount = 0;
			synchronized (logLinesQueue) {
				logUpdaterRunning = true;
			}
			String line;
			try {
				while ((line = getNextLine()) != null) {
					output.append(line);
					outputLines++;
					if (outputLines > 5000) {
						clear();
					}
					oneCallCount++;
					if (oneCallCount > 40) {
						break;
					}
				}
			} finally {
				synchronized (logLinesQueue) {
					logUpdaterRunning = false;
				}
			}
		}
	}
}
