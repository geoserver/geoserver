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




import com.moesol.geoserver.sync.json.Sha1SyncJson;

/**
 * Provide handling for different features in different versions.
 * @author hasting
 */
public enum VersionFeatures {
	VERSION1("1") {
		@Override
		public Sha1Value getBucketPrefixSha1(IdAndValueSha1s pair) { return pair.getValueSha1(); }
	},
	VERSION2("2") {
		@Override
		public Sha1Value getBucketPrefixSha1(IdAndValueSha1s pair) { return pair.getIdSha1(); }
	};
	
	private final String token;
	
	private VersionFeatures(String token) {
		this.token = token;
	}

	/**
	 * @return Token string for this version.
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Get the Sha1Value to use for bucket/position prefixes.
	 * <ul>
	 * <li>Version 1 uses value sha1 for bucket/position prefixes.
	 * <li>Version 2 uses ID sha1 for bucket/position prefixes.
	 * </ul>
	 * @param pair
	 * @return
	 */
	public abstract Sha1Value getBucketPrefixSha1(IdAndValueSha1s pair);
	
	public static VersionFeatures fromSha1SyncJson(Sha1SyncJson sha1SyncJson) {
		if (sha1SyncJson.v == null) {
			return VERSION1;
		}
		for (VersionFeatures f : VersionFeatures.values()) {
			if (f.getToken().equals(sha1SyncJson.v)) {
				return f;
			}
		}
		// TODO perhaps we can negotiate down to an older version
		throw new IllegalArgumentException("Unknown SHA-1 Sync JSON version: " + sha1SyncJson.v);
	}
}
