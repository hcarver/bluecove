/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author vlads
 * 
 */
public abstract class TimeUtils {

	public static String secSince(long start) {
		if (start == 0) {
			return "n/a";
		}
		long msec = since(start);
		long sec = msec / 1000;
		long min = sec / 60;
		sec -= min * 60;
		long h = min / 60;
		min -= h * 60;

		StringBuffer sb;
		sb = new StringBuffer();
		if (h != 0) {
			sb.append(StringUtils.d00((int) h)).append(":");
		}
		if ((h != 0) || (min != 0)) {
			sb.append(StringUtils.d00((int) min)).append(":");
		}
		sb.append(StringUtils.d00((int) sec));
		if ((h == 0) && (min == 0)) {
			sb.append(" sec");
		}
		if ((h == 0) && (min == 0) && (sec <= 1)) {
			msec -= 1000 * sec;
			sb.append(" ");
			sb.append(StringUtils.d000((int) msec));
			sb.append(" msec");
		}
		return sb.toString();
	}

	public static long since(long start) {
		if (start == 0) {
			return 0;
		}
		return (System.currentTimeMillis() - start);
	}

	public static String bps(long size, long start) {
		long duration = TimeUtils.since(start);
		if (duration == 0) {
			return "n/a";
		}
		return "" + ((1000 * 8 * size) / (duration)) + " bit/s";
	}

	public static String timeNowToString() {
		return timeToString(System.currentTimeMillis());
	}

	public static String timeStampNowToString() {
		return timeStampToString(System.currentTimeMillis(), true);
	}

	public static String timeToString(Calendar calendar, boolean millisecond) {
		StringBuffer sb;
		sb = new StringBuffer();
		sb.append(StringUtils.d00(calendar.get(Calendar.HOUR_OF_DAY))).append(":");
		sb.append(StringUtils.d00(calendar.get(Calendar.MINUTE))).append(":");
		sb.append(StringUtils.d00(calendar.get(Calendar.SECOND)));
		if (millisecond) {
			sb.append(".");
			sb.append(StringUtils.d000(calendar.get(Calendar.MILLISECOND)));
		}
		return sb.toString();
	}

	public static String timeToString(long timeStamp) {
		return timeStampToString(timeStamp, false);
	}

	public static synchronized String timeStampToString(long timeStamp, boolean millisecond) {
		if (timeStamp == 0) {
			return "n/a";
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(timeStamp));
		return timeToString(calendar, millisecond);
	}

	public static boolean sleep(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
}
