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

/**
 * @author vlads
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
