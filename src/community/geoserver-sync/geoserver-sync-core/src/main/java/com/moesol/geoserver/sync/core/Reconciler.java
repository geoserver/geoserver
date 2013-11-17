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
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

public abstract class Reconciler {

	protected final Sha1SyncJson m_local;
	protected final Sha1SyncJson m_remote;

	public Reconciler(Sha1SyncJson local, Sha1SyncJson remote) {
		m_local = local;
		m_remote = remote;
		if (m_local.l != m_remote.l) {
			throw new IllegalArgumentException(msg());
		}
	}

	String msg() {
		return String.format("Local level(%d) not equal to remote level(%d)", m_local.l, m_remote.l);
	}

	protected abstract void differentAtPosition(String position, Sha1SyncPositionHash local, Sha1SyncPositionHash remote);
	protected abstract void matchAtPosition(String position);
	/**
	 * Remote has this position but remote does not. Default implementation does nothing.
	 * @param position from {@link Sha1SyncPositionHash}. for example, a0, ba10, abcd0123, ... 
	 * @param missing TODO
	 */
	protected abstract void localMissingPosition(String position, Sha1SyncPositionHash missing);
	/**
	 * Local has the position but remote does not. Default implementation does nothing.
	 * @param position from {@link Sha1SyncPositionHash}. for example, a0, ba10, abcd0123, ... 
	 * @param missing TODO
	 */
	protected abstract void remoteMissingPosition(String position, Sha1SyncPositionHash missing);

	/**
	 * Reconciles local and remote {@link Sha1SyncJson} differences by calling
	 * {@link #differentAtPosition}, {@link #matchAtPosition}, {@link #localMissing}, or {@link #remoteMissing}
	 */
	protected void reconcile() {
		int i = 0; // local index
		int j = 0; // remote index
		int local_size = m_local.h.size();
		int remote_size = m_remote.h == null ? 0 : m_remote.h.size();
		while (i < local_size && j < remote_size) {
			Sha1SyncPositionHash localHashPos = m_local.h.get(i);
			Sha1SyncPositionHash remoteHashPos = m_remote.h.get(j);
			
			int cmp = localHashPos.position().compareTo(remoteHashPos.position());
			if (cmp < 0) {
				remoteMissingPosition(localHashPos.position(), localHashPos);
				i++;
				continue;
			}
			if (cmp > 0) {
				localMissingPosition(remoteHashPos.position(), remoteHashPos);
				j++;
				continue;
			}
			
			// prefix is same on both do hashes match?
			String position = localHashPos.position();
			if (localHashPos.summary().equals(remoteHashPos.summary())) {
				matchAtPosition(position);
			} else {
				differentAtPosition(position, localHashPos, remoteHashPos);
			}
	
			i++;
			j++;
		}
		for ( ; i < local_size; i++) {
			Sha1SyncPositionHash localHashPos = m_local.h.get(i);
			remoteMissingPosition(localHashPos.position(), localHashPos);
		}
		for ( ; j < remote_size; j++) {
			Sha1SyncPositionHash remoteHashPos = m_remote.h.get(j);
			localMissingPosition(remoteHashPos.position(), remoteHashPos);
		}
	}
}
