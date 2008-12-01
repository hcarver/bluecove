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
package net.sf.bluecove.util;

/**
 *
 */
public class CountStatistic {

	public long count;
	
	public long max;
	
	public long total;

	public void clear() {
		count = 0;
		max = 0;
		total = 0;
	}
	
	public void add(long itemCount) {
		count ++;
		total += itemCount;
		if (itemCount > max) {
			max = itemCount;
		}
	}
	
	public String avg() {
		if (count == 0) {
			return "0";
		}
		// No Float: ((float)total/(float)count)
		long m = total/count;
		long r = (10000 * (total%count))/count;
		return String.valueOf(m) + "." + StringUtils.d0000((int)r);
	}
	
	public String avgPrc() {
		if (count == 0) {
			return "0%";
		}
		long m = (100 * total)/count;
		long r = (10000 * ((100 * total)%count)/count);
		return String.valueOf(m) + "." + StringUtils.d0000((int)r) + "%";
	}

	public long max() {
		return max;
	}
}
