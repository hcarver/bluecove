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
package org.bluecove.tester.util;

import java.util.Calendar;
import java.util.Date;

/**
 * 
 */
public abstract class TimeUtils {

    private static Calendar singleCalendar;
    
    private static Date singleDate;
    
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
		long bps = ((1000 * 8 * size) / (duration));
		return StringUtils.formatLong(bps) + " bit/s";
	}

	public static String timeNowToString() {
	    return timeToString(System.currentTimeMillis(), false);
	}
	

	public static String timeStampNowToString() {
		return timeToString(System.currentTimeMillis(), true);
	}
	
	public static String timeToString(long timeStamp) {
        return timeToString(timeStamp, false);
    }
	
	public static String timeToString(long timeStamp, boolean millisecond) {
        StringBuffer sb = new StringBuffer();
        appendTime(sb, timeStamp, false);
        return sb.toString();
    }

	public static StringBuffer appendTimeStampNow(StringBuffer sb, boolean millisecond) {
	    return appendTime(sb,System.currentTimeMillis(),  millisecond);
	}
	
	public static synchronized StringBuffer appendTime(StringBuffer sb, long timeStamp, boolean millisecond) {
        if (timeStamp == 0) {
            sb.append("n/a");
            return sb;

        }
        if (singleCalendar == null) {
            singleCalendar = Calendar.getInstance();
            singleDate = new Date();
        }
        singleDate.setTime(timeStamp);
        singleCalendar.setTime(singleDate);

        StringUtils.appendD00(sb, singleCalendar.get(Calendar.HOUR_OF_DAY)).append(':');
        StringUtils.appendD00(sb, singleCalendar.get(Calendar.MINUTE)).append(':');
        StringUtils.appendD00(sb, singleCalendar.get(Calendar.SECOND));
        if (millisecond) {
            sb.append('.');
           StringUtils.appendD000(sb, singleCalendar.get(Calendar.MILLISECOND));
        }

        return sb;
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
