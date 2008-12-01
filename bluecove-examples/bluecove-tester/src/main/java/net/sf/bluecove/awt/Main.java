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
package net.sf.bluecove.awt;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Consts;
import net.sf.bluecove.Logger;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.TestConcurrent;
import net.sf.bluecove.Logger.LoggerAppender;
import net.sf.bluecove.se.BlueCoveSpecific;
import net.sf.bluecove.se.FileStorage;
import net.sf.bluecove.se.JavaSECommon;
import net.sf.bluecove.se.LocalDeviceManager;
import net.sf.bluecove.se.RemoteDeviceManager;
import net.sf.bluecove.se.UIHelper;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.TimeUtils;

import com.intel.bluetooth.BlueCoveImpl;

/**
 *
 * 
 */
public class Main extends Frame implements LoggerAppender {

	private static final long serialVersionUID = 1L;

	private TextArea output = null;

	private int outputLines = 0;

	private Vector logLinesQueue = new Vector();

	private boolean logUpdaterRunning = false;

	int lastKeyCode;

	MenuItem debugOn;

	public static void main(String[] args) {
		// System.setProperty("bluecove.debug", "true");
		// System.getProperties().put("bluecove.debug", "true");

		// BlueCoveImpl.instance().getBluetoothPeer().enableNativeDebug(true);
		JavaSECommon.initOnce();
		Configuration.storage = new FileStorage();

		Main app = new Main();
		app.setVisible(true);
		Logger.debug("Stated app");
		Logger.debug("OS:" + System.getProperty("os.name") + "|" + System.getProperty("os.version") + "|"
				+ System.getProperty("os.arch"));
		Logger.debug("Java:" + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--stack")) {
				// This is used in WebStart when system properties can't be
				// defined.
				i++;
				try {
					BlueCoveImpl.instance().setBluetoothStack(args[i]);
				} catch (BluetoothStateException e) {
					Logger.error("can't init stack", e);
				}
				app.updateTitle();
			} else if (args[i].equalsIgnoreCase("--runonce")) {
				int rc = Switcher.runClient();
				Logger.debug("Finished app " + rc);
				System.exit(rc);
			}
		}
	}

	public Main() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				quit();
			}
		});

		Logger.addAppender(this);
		BlueCoveSpecific.addAppender(this);

		Logger.debug("Stating app");

		this.setTitle("BlueCove tester");

		final MenuBar menuBar = new MenuBar();
		Menu menuBluetooth = new Menu("Bluetooth");

		final MenuItem serverStart = addMenu(menuBluetooth, "Server Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startServer();
				updateTitle();
			}
		}, KeyEvent.VK_5);

		final MenuItem serverStop = addMenu(menuBluetooth, "Server Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.serverShutdown();
			}
		}, KeyEvent.VK_6);

		final MenuItem clientStart = addMenu(menuBluetooth, "Client Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startClient();
				updateTitle();
			}
		}, KeyEvent.VK_2);

		final MenuItem clientStop = addMenu(menuBluetooth, "Client Stop", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.clientShutdown();
			}
		}, KeyEvent.VK_3);

		final MenuItem tckStart;
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

		final MenuItem stop = addMenu(menuBluetooth, "Stop all work", new ActionListener() {
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

		Menu menuLogs = new Menu("Logs");

		debugOn = addMenu(menuLogs, "BlueCove Debug ON", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean dbg = BlueCoveSpecific.changeDebug();
				if (dbg) {
					debugOn.setLabel("BlueCove Debug OFF");
				} else {
					debugOn.setLabel("BlueCove Debug ON");
				}
			}
		});

		addMenu(menuLogs, "Clear Log", new ActionListenerRunnable() {
			public void run() {
				clear();
			}
		}, KeyEvent.VK_Z);

		addMenu(menuLogs, "Print FailureLog", new ActionListenerRunnable() {
			public void run() {
				UIHelper.printFailureLog();
			}
		}, KeyEvent.VK_4);

		addMenu(menuLogs, "Clear Stats", new ActionListenerRunnable() {
			public void run() {
				UIHelper.clearStats();
			}
		});

		if (JavaSECommon.isJava5()) {
			addMenu(menuLogs, "ThreadDump", new ActionListenerRunnable() {
				public void run() {
					JavaSECommon.threadDump();
				}
			});
		}

		addMenu(menuLogs, "Save to File", new ActionListenerRunnable() {
			public void run() {
				logSaveToFile();
			}
		});

		menuBar.add(menuLogs);

		Menu menuMore = new Menu("More");

		addMenu(menuMore, "Configuration", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				(new ConfigurationDialog(Main.this)).setVisible(true);
			}
		});

		addMenu(menuMore, "Client Connection", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				(new ClientConnectionDialog(Main.this)).setVisible(true);
			}
		});

		addMenu(menuMore, "OBEX Client Connection", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				(new ObexClientConnectionDialog(Main.this)).setVisible(true);
			}
		});

		addMenu(menuMore, "Two Clients Start", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Switcher.startTwoClients();
				updateTitle();
			}
		});

		addMenu(menuMore, "Concurrent Services Search", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestConcurrent.startConcurrentServicesSearchClients();
			}
		});

		Menu menuSpeedTests = new Menu("Speed tests");
		addMenu(menuSpeedTests, "RFCOMM Read test", new ActionListenerRunnable() {
			public void run() {
				UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_WRITE, false);
				Switcher.startClient();
			}
		});

		addMenu(menuSpeedTests, "RFCOMM Write test", new ActionListenerRunnable() {
			public void run() {
				UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_READ, false);
				Switcher.startClient();
			}
		});
		addMenu(menuSpeedTests, "L2CAP Read test", new ActionListenerRunnable() {
			public void run() {
				UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_WRITE, true);
				Switcher.startClient();
			}
		});

		addMenu(menuSpeedTests, "L2CAP Write test", new ActionListenerRunnable() {
			public void run() {
				UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_READ, true);
				Switcher.startClient();
			}
		});

		menuMore.add(menuSpeedTests);

		Menu menuLocalDevice = new Menu("LocalDevice");
		addMenu(menuLocalDevice, "Get discoverable", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.getDiscoverable();
			}
		});

		addMenu(menuLocalDevice, "Set NOT discoverable", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setNotDiscoverable();
			}
		});

		addMenu(menuLocalDevice, "Set discoverable GIAC", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setDiscoverableGIAC();
			}
		});

		addMenu(menuLocalDevice, "Set discoverable LIAC", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setDiscoverableLIAC();
			}
		});

		menuMore.add(menuLocalDevice);

		Menu menuRemoteDevice = new Menu("RemoteDevice");
		addMenu(menuRemoteDevice, "Retrieve CACHED", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RemoteDeviceManager.retrieveDevices(DiscoveryAgent.CACHED);
			}
		});
		addMenu(menuRemoteDevice, "Retrieve PREKNOWN", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RemoteDeviceManager.retrieveDevices(DiscoveryAgent.PREKNOWN);
			}
		});
		menuMore.add(menuRemoteDevice);

		Menu threadLocalStack = new Menu("ThreadLocalStack");

		MenuItem menuWinsock = addMenu(threadLocalStack, "Set 'winsock'", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setUseWINSOCK();
				updateTitle();
			}
		});
		menuWinsock.setEnabled(Configuration.windows);

		MenuItem menuWidcomm = addMenu(threadLocalStack, "Set 'widcomm'", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setUseWIDCOMM();
				updateTitle();
			}
		});
		menuWidcomm.setEnabled(Configuration.windows);

		addMenu(threadLocalStack, "Set 'deviceID=0'", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setUseDevice0();
				updateTitle();
			}
		});

		addMenu(threadLocalStack, "Set 'deviceID=1'", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.setUseDevice1();
				updateTitle();
			}
		});

		addMenu(threadLocalStack, "shutdown", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.shutdownThreadLocal();
			}
		});

		menuMore.add(threadLocalStack);

		addMenu(menuMore, "Shutdown BlueCove", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalDeviceManager.shutdown();
			}
		});

		menuBar.add(menuMore);

		setMenuBar(menuBar);

		// Create a scrolled text area.
		output = new TextArea("");
		output.setEditable(false);
		this.add(output);

		Runnable statusUpdateRunnable = new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					if (isMainFrameActive()) {
						serverStop.setEnabled(Switcher.isRunningServer());
						serverStart.setEnabled(!Switcher.isRunningServer());

						clientStop.setEnabled(Switcher.isRunningClient());
						clientStart.setEnabled(!Switcher.isRunningClient());
						stop.setEnabled(Switcher.isRunningClient() || Switcher.isRunningServer());
						if (tckStart != null) {
							tckStart.setEnabled(!Switcher.isTCKRunning());
						}
					}
				}
			}
		};
		Thread statusUpdate = Configuration.cldcStub.createNamedThread(statusUpdateRunnable, "StatusUpdate");
		statusUpdate.start();

		output.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				// Logger.debug("key:" + e.getKeyCode() + " " +
				// KeyEvent.getKeyText(e.getKeyCode()));
				Main.this.keyPressed(e.getKeyCode());
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}
		});

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.height < 400) {
			Configuration.screenSizeSmall = true;
		}
		Font logFont = new Font("Monospaced", Font.PLAIN, Configuration.screenSizeSmall ? 9 : 12);
		output.setFont(logFont);

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

	boolean isMainFrameActive() {
		try {
			return isActive();
		} catch (Throwable j13) {
			return true;
		}
	}

	private void updateTitle() {
		this.setTitle(UIHelper.getMainWindowTitle());
	}

	private MenuItem addMenu(Menu menu, String name, ActionListener l) {
		return addMenu(menu, name, l, 0);
	}

	private MenuItem addMenu(Menu menu, String name, ActionListener l, int key) {
		MenuItem menuItem = new MenuItem(name);
		menuItem.addActionListener(l);
		menu.add(menuItem);
		if (key != 0) {
			menuItem.setShortcut(new MenuShortcut(key, false));
		}
		return menuItem;
	}

	protected void keyPressed(int keyCode) {
		switch (keyCode) {
		case '1':
			// printStats();
			break;
		case '4':
			UIHelper.printFailureLog();
			break;
		case '0':
			// logScrollX = 0;
			// setLogEndLine();
			break;
		case '*':
		case KeyEvent.VK_MULTIPLY:
		case 119:
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
			// startSwitcher();
			break;
		case '9':
			// stopSwitcher();
			break;
		case '#':
		case 120:
			if (lastKeyCode == keyCode) {
				quit();
			}
			clear();
			break;
		default:
			// Logger.debug("keyCode " + keyCode);
		}
		lastKeyCode = keyCode;
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

	private void logSaveToFile() {
		SimpleDateFormat fmt = new SimpleDateFormat("MM-dd_HH-mm-ss");
		OutputStreamWriter out = null;
		try {
			File file = new File("BlueCoveTester-" + fmt.format(new Date()) + ".log");
			out = new FileWriter(file);
			out.write(output.getText());
			out.flush();
			out.close();
			out = null;
			Logger.info("Log saved to file " + file.getAbsolutePath());
		} catch (Throwable ignore) {
		} finally {
			IOUtils.closeQuietly(out);
		}
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
