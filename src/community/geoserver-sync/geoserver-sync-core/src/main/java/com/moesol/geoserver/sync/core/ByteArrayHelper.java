/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.core;

public class ByteArrayHelper {
	private static final char[] charMap = "0123456789abcdef".toCharArray();
	
	public static String toHex(byte[] a) {
		char[] result = new char[2 * a.length];
		for (int i = 0; i < a.length; i++) {
			int high = (0xF0 & a[i]) >> 4;
			int low =  (0x0F & a[i]);
			result[2*i    ] = charMap[high];
			result[2*i + 1] = charMap[low];
		}
		return new String(result);
	}
	
//	public static int toInteger(byte[] a) {
//		int v = 0;
//		
//		for (byte b : a) {
//			v = (v << 8) | (0xFF &b);
//		}
//		return v;
//	}

	public static byte[] fromHex(String string) {
		if ((string.length() & 1) != 0) {
			throw new IllegalArgumentException("String length must be even");
		}
		int rl = string.length() / 2;
		byte[] result = new byte[rl];
		for (int i = 0; i < rl; i++) {
			String hex = string.substring(i * 2, i* 2 + 2);
			result[i] = (byte) Integer.parseInt(hex, 16);
		}
		return result;
	}
}
