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



import java.io.PrintStream;
import java.util.List;

import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.grouper.GroupPosition;
import com.moesol.geoserver.sync.grouper.Sha1LevelGrouper;

/**
 * Dump debug grouping information
 * 
 * @author hastings
 */
public class DebugPrintLevelGrouper extends Sha1LevelGrouper {
	
	private PrintStream out = System.out;
	private int m_count = 0;
	private boolean justGroupHash = false;

	public boolean isJustGroupHash() {
		return justGroupHash;
	}

	public void setJustGroupHash(boolean justGroupHash) {
		this.justGroupHash = justGroupHash;
	}

	public DebugPrintLevelGrouper(VersionFeatures vf, List<IdAndValueSha1s> featureSha1s) {
		super(vf, featureSha1s);
	}

	@Override
	protected void end(long maxInAnyGroup) {
	}

	@Override
	protected void begin(int level) {
	}

	@Override
	protected void hashOne(Sha1Value sha1) {
		super.hashOne(sha1);
		
		if (justGroupHash) {
			return;
		}
		out.print(m_count);
		out.print(": ");
		out.println(sha1);
		m_count++;
	}

	@Override
	protected void groupCompleted(GroupPosition prefix, Sha1Value sha1Value) {
		out.println("--" + prefix + "--" + sha1Value);
	}

}
