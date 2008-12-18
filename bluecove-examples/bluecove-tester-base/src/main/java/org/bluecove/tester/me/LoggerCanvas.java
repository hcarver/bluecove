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
package org.bluecove.tester.me;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.log.LoggerAppender;
import org.bluecove.tester.util.TimeUtils;

public class LoggerCanvas extends Canvas implements LoggerAppender {

	protected boolean showLogDebug = true;

	public static boolean logTimeStamp = false;

	private int line;

	private int lineOffsetY;

	private int lineOffsetX;

	private Vector logMessages = new Vector();

	private int errorCount = 0;

	private int logLine = 0;

	private int logScrollX;

	private int logVisibleLines = 0;

	private int logMessagesSownSize = 0;

	private boolean movingCursor = false;

	private boolean logLastEvenVisible = true;

	protected int backgroundColor = 255;
	
	protected int fontColor = 0;
	
	public LoggerCanvas() {
		Logger.addAppender(this);
	}

	protected String getCanvasTitleText() {
		return null;
	}

	protected String getCanvasStatusText() {
		return null;
	}
	
	protected void paintBackground(Graphics g, int width, int height) {
	    
	}

	public int writeln(Graphics g, String s) {
		int h = (g.getFont().getHeight() + 1);
		int y = lineOffsetY + h * line;
		g.drawString(s, lineOffsetX, y, Graphics.LEFT | Graphics.TOP);
		line++;
		return y + h;
	}

	
	protected void paint(Graphics g) {
		lineOffsetY = 0;
		lineOffsetX = 0;
		line = 0;
		int width = getWidth();
		int height = getHeight();

		g.setGrayScale(backgroundColor);
		g.fillRect(0, 0, width, height);
		paintBackground(g, width, height);
		g.setColor(fontColor);
		int lastY = 0;
		String title = getCanvasTitleText();
		if (title != null) {
			lastY = writeln(g, title);
		}

		line = 0;
		lineOffsetY = lastY;
		Font font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		g.setFont(font);

		String status = getCanvasStatusText();
		if (status != null) {
			lastY = writeln(g, status);
		}

		int lineHeight = g.getFont().getHeight() + 1;
		logVisibleLines = (height - lastY) / lineHeight;
		lineOffsetX = logScrollX;

		if (!movingCursor && logLastEvenVisible) {
			if (((logLine + logVisibleLines) < logMessages.size())) {
				setLogEndLine();
			}
		}
		movingCursor = false;
		logMessagesSownSize = logMessages.size();
		int logIndex = logLine;
		while (((lastY) < height) && (logIndex < logMessagesSownSize)) {
			try {
				String message = (String) logMessages.elementAt(logIndex);
				lastY = writeln(g, message);
				logIndex++;
			} catch (ArrayIndexOutOfBoundsException e) {
				logLastEvenVisible = true;
				return;
			}
		}
		logLastEvenVisible = (logIndex == logMessagesSownSize);
	}

	protected void logScrollBottom() {
		logScrollX = 0;
		setLogEndLine();
	}

	protected void logLinesMove(int action) {
		switch (action) {
		case UP:
			if (logLine > 0) {
				logLine--;
				movingCursor = true;
			}
			break;
		case DOWN:
			if ((logLine + logVisibleLines - 1) < logMessages.size()) {
				logLine++;
			}
			break;
		case RIGHT:
			if (logScrollX > -500) {
				logScrollX -= 20;
			}
			break;
		case LEFT:
			if (logScrollX < 0) {
				logScrollX += 20;
			}
			break;
		default:
			return;
		}
	}

	public void appendLog(int level, String message, Throwable throwable) {
		if (!showLogDebug && (level == Logger.DEBUG)) {
			return;
		}
		StringBuffer buf = new StringBuffer();
		if (logTimeStamp) {
			buf.append(TimeUtils.timeStampNowToString()).append(" ");
		}
		switch (level) {
		case Logger.ERROR:
			errorCount++;
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
			buf.append(':');
			buf.append(throwable.getMessage());
		}
		String m = buf.toString().replace('\t', ' ');
		int cr = m.indexOf("\n");
		while (cr != -1) {
			logMessages.addElement(m.substring(0, cr));
			m = m.substring(cr + 1);
			cr = m.indexOf("\n");
		}
		logMessages.addElement(m);

		int logMax = 1000;
		if (logMessages.size() >= logMax) {
			Vector newLogMessages = new Vector();
			for (int i = logMax - 5; i < logMax; i++) {
				newLogMessages.addElement(logMessages.elementAt(i));
			}
			logMessages = newLogMessages;
			logLine = 0;
		}

		if (logLastEvenVisible) {
			// BlueCoveTestMIDlet.display.flashBacklight(0);
			repaint();
		}
	}

	private void setLogEndLine() {
		logLine = logMessages.size() - logVisibleLines;
		if (logLine < 0) {
			logLine = 0;
		}
	}

	protected void clearLog() {
		logMessages.removeAllElements();
		errorCount = 0;
		logLine = 0;
		logScrollX = 0;
	}
}
