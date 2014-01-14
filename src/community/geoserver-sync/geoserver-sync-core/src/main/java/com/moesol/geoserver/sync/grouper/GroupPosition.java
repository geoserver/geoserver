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
package com.moesol.geoserver.sync.grouper;



import com.moesol.geoserver.sync.core.ByteArrayHelper;
import com.moesol.geoserver.sync.core.Sha1Value;

/**
 * Stores prefix bytes that define the groups position.
 * @author hastings
 *
 */
public class GroupPosition {
	private static final int MAX_SHA1_LEN = 20;
	private final byte[] m_prefix;
	
	public GroupPosition(int level) {
		m_prefix = new byte[Math.min(level, MAX_SHA1_LEN)];
	}
	
	public void setFromSha1(Sha1Value sha1) {
		sha1.copyPrefixTo(m_prefix);
	}

	public String toString() {
		return ByteArrayHelper.toHex(m_prefix);
	}

	public byte[] get() {
		return m_prefix;
	}
}
