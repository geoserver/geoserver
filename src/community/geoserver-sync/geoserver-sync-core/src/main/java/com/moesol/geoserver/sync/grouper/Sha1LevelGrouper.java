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


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.moesol.geoserver.sync.core.IdAndValueSha1Comparator;
import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;

public abstract class Sha1LevelGrouper {
	private static final Logger LOGGER = Logger.getLogger(Sha1JsonLevelGrouper.class.getName());
	private final MessageDigest m_sha1;
	private final List<? extends IdAndValueSha1s> m_sortedFeatureSha1s;
	protected final VersionFeatures versionFeatures;
	private long m_maxInAnyGroup = 0;

	/**
	 * Prepares grouper by sorting featureSha1s in place.
	 * @param featureSha1s list of SHA-1 checksums for features.
	 */
	public Sha1LevelGrouper(VersionFeatures vf, List<? extends IdAndValueSha1s> featureSha1s) {
		versionFeatures = vf;
		try {
			m_sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Unable to load SHA-1 message digest", e);
		}
		Collections.sort(featureSha1s, new IdAndValueSha1Comparator(versionFeatures));
		m_sortedFeatureSha1s = featureSha1s;
	}
	
	public void groupForLevel(int level) {
		begin(level);
		m_maxInAnyGroup = 0;
		try {
			if (level == 0) {
				sha1All();
				return;
			}
			sha1Groups(level);
		} finally {
			end(m_maxInAnyGroup);
		}
	}

	private void sha1All() {
		for (IdAndValueSha1s sha1 : m_sortedFeatureSha1s) {
			hashOne(sha1.getValueSha1());
			m_maxInAnyGroup++;
		}
		doGroupCompleted(new GroupPosition(0), new Sha1Value(m_sha1.digest()), m_maxInAnyGroup);
	}

	/**
	 * Walks all the feature sha1s and computes the groups of sha1s for {@code level}.
	 * @param level
	 */
	private void sha1Groups(int level) {
		GroupPosition prefix = new GroupPosition(level);
		long maxCurrentGroup = 0;
		
		// Hmm, this could be written in terms of NavigableSet operations...
		if (m_sortedFeatureSha1s.size() > 0) {
			IdAndValueSha1s zero = m_sortedFeatureSha1s.get(0);
			prefix.setFromSha1(versionFeatures.getBucketPrefixSha1(zero));
			hashOne(zero.getValueSha1());
			maxCurrentGroup++;
		}
		for (int i = 1; i < m_sortedFeatureSha1s.size(); i++) {
			IdAndValueSha1s sha1 = m_sortedFeatureSha1s.get(i);

			if (versionFeatures.getBucketPrefixSha1(sha1).isPrefixMatch(prefix)) {
				hashOne(sha1.getValueSha1());
				maxCurrentGroup++;
				continue;
			}
			
			if (i < m_sortedFeatureSha1s.size()) {
				doGroupCompleted(prefix, new Sha1Value(m_sha1.digest()), maxCurrentGroup);
				updateMaxInGroup(maxCurrentGroup);
				maxCurrentGroup = 0;
				hashOne(sha1.getValueSha1());
				maxCurrentGroup++;
				prefix.setFromSha1(versionFeatures.getBucketPrefixSha1(sha1));
			}
		}
		if (m_sortedFeatureSha1s.size() > 0) {
			doGroupCompleted(prefix, new Sha1Value(m_sha1.digest()), maxCurrentGroup);
			updateMaxInGroup(maxCurrentGroup);
			maxCurrentGroup = 0;
		}
	}

	protected void hashOne(Sha1Value sha1) {
		LOGGER.log(Level.FINER, "el({0})", sha1);
		m_sha1.update(sha1.get());
	}
	
	private void doGroupCompleted(GroupPosition position, Sha1Value sha1Value, long maxInGroup) {
		LOGGER.log(Level.FINER, "--group({0},{1},{2})", new Object[] {position, sha1Value, maxInGroup });
		groupCompleted(position, sha1Value);
	}

	private void updateMaxInGroup(long maxCurrentGroup) {
		if (m_maxInAnyGroup < maxCurrentGroup) {
			m_maxInAnyGroup = maxCurrentGroup;
		}
	}
	
	protected abstract void begin(int level);
	protected abstract void groupCompleted(GroupPosition position, Sha1Value sha1Value);
	protected abstract void end(long maxInAnyGroup);
}
