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



import java.util.ArrayList;
import java.util.List;

import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

/**
 * Build json state of groups and their SHA-1 of SHA-1 values.
 * 
 * @author hastings
 */
public class Sha1JsonLevelGrouper extends Sha1LevelGrouper {
	private Sha1SyncJson m_json;

	public Sha1SyncJson getJson() {
		return m_json;
	}

	public Sha1JsonLevelGrouper(VersionFeatures vf, List<? extends IdAndValueSha1s> featureSha1s) {
		super(vf, featureSha1s);
	}

	@Override
	protected void end(long maxInAnyGroup) {
		m_json.m = maxInAnyGroup;
		m_json.v = versionFeatures.getToken();
	}

	@Override
	protected void begin(int level) {
		m_json = new Sha1SyncJson();
		m_json.l = level;
		m_json.h = new ArrayList<Sha1SyncPositionHash>();
	}

	@Override
	protected void groupCompleted(GroupPosition prefix, Sha1Value sha1Value) {
		Sha1SyncPositionHash hash = new Sha1SyncPositionHash();
		hash.p = prefix.toString();
		hash.s = sha1Value.toString();
		m_json.h.add(hash);
	}

}
