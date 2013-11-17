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
import java.util.List;

import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

/**
 * Filter the next level deeper hashes so that the server is only reporting to the client
 * the differences that the client needs to report on its next round of communication.
 * <p>
 * We save bandwidth by sending nothing any time a group/bucket on the local and remote match.
 * When the remote side should delete all elements that match a position/prefix we 
 * send group with a null summary. This means that reconcile must look out for two special
 * cases
 * <ol>
 * <li>The position/prefix might be shorter than normal position length for the current level</li>
 * <li>The summary might be {@code null}</li>
 * </ol>
 * It is somewhat counter intuitive, but if the client does not ask about a group/bucket,
 * then the server thinks that group/bucket is in sync and we should send nothing. It is somewhat
 * difficult to determine if the client not asking about a group/bucket means the client does not
 * have the group/bucket or if it means the client thinks that group/bucket is in synch. At level
 * zero we make it so that the client is asking about all group/bucket's.
 * 
 * @author hastings
 */
public class ServerReconciler extends Reconciler {

	private Sha1SyncJson m_output;
	private int output_k  = 0; // output index
	private List<Sha1SyncPositionHash> m_result;
	
	/**
	 * Construct a reconciler that can reconcile local and remote. Local and remote must be at the same level.
	 * @param local
	 * @param remote
	 */
	public ServerReconciler(Sha1SyncJson local, Sha1SyncJson remote) {
		super(local, remote);
	}

	/**
	 * Remove elements from output that are in sync between local and remote.
	 * NOTE: the positions in {@code output} must be sorted.
	 * @param output
	 */
	public void filterDownNextLevel(Sha1SyncJson output) {
		m_output = output;
		m_result = new ArrayList<Sha1SyncPositionHash>(output.hashes().size());
		
		reconcile();
		
		output.hashes(m_result);
	}

	@Override
	protected void differentAtPosition(String position, Sha1SyncPositionHash local, Sha1SyncPositionHash remote) {
		copyWithSamePrefix(position);
	}

	@Override
	protected void matchAtPosition(String position) {
		skipWithSamePrefix(position);
	}

	@Override
	protected void localMissingPosition(String position, Sha1SyncPositionHash missing) {
		// Tell remote side this branch is empty...
		m_result.add(new Sha1SyncPositionHash().position(position));
	}

	@Override
	protected void remoteMissingPosition(String position, Sha1SyncPositionHash missing) {
		// Client thinks this position is synchronized, stop talking about it.
		skipWithSamePrefix(position);
	}

	private int copyWithSamePrefix(String prefix) {
		List<Sha1SyncPositionHash> result = m_result;
		while (output_k < m_output.h.size() && m_output.h.get(output_k).p.startsWith(prefix)) {
			result.add(m_output.h.get(output_k));
			output_k++;
		}
		return output_k;
	}

	private int skipWithSamePrefix(String prefix) {
		while (output_k < m_output.h.size() && m_output.h.get(output_k).p.startsWith(prefix)) {
			output_k++;
		}
		return output_k;
	}

}
