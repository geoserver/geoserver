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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

public class ClientReconciler extends Reconciler {
	private List<Sha1SyncPositionHash> m_result;
	private Set<String> m_priorLevelPrefix = new HashSet<String>();
	private ReconcilerDelete m_delete = new ReconcilerDelete() {
		@Override
		public void deleteGroup(Sha1SyncPositionHash group) {
		}
	};

	public ClientReconciler(Sha1SyncJson local, Sha1SyncJson remote) {
		super(local, remote);
	}

	@Override
	protected void differentAtPosition(String position, Sha1SyncPositionHash local, Sha1SyncPositionHash remote) {
		// Tell remote side this branch differs
		m_result.add(local);
	}

	@Override
	protected void matchAtPosition(String position) {
		// Matches can be safely skipped
	}

	@Override
	protected void localMissingPosition(String position, Sha1SyncPositionHash remote) {
		// Tell remote side this branch is empty unless remote thinks its empty too.
		if (remote.summary() == null) {
			return; // skip, remote thinks its empty
		}
		m_result.add(new Sha1SyncPositionHash().position(position));
	}

	@Override
	protected void remoteMissingPosition(String position, Sha1SyncPositionHash local) {
		// If server talks about prefix and we don't have this position this this is a delete
		// Otherwise, we skip this because server thinks we are in synch here.
		String prior = computePriorPrefix(position);
		if (m_priorLevelPrefix.contains(prior)) {
			m_delete.deleteGroup(local);
		}
	}

	public void computeOutput(Sha1SyncJson outputSha1SyncJson) {
	    m_result = new ArrayList<Sha1SyncPositionHash>();
	    computePriorLevelPrefixes();
	    reconcile();
		outputSha1SyncJson.hashes(m_result);
	}

	private void computePriorLevelPrefixes() {
		for (Sha1SyncPositionHash group : m_remote.hashes()) {
			m_priorLevelPrefix.add(computePriorPrefix(group.position()));
		}
	}

	/**
	 * Each level has a byte or 2 hex characters
	 * @param position
	 * @return
	 */
	private String computePriorPrefix(String position) {
		int length = position.length();
		if (length < 2) {
			return position;
		}
		return position.substring(0, length - 2);
	}

	public ReconcilerDelete getDelete() {
		return m_delete;
	}

	public void setDelete(ReconcilerDelete delete) {
		m_delete = delete;
	}

}
