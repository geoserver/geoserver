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

package com.moesol.geoserver.sync.json;




import com.google.gson.Gson;

/**
 * Java bean used to format and parse the JSON messages for SHA-1 synchronization.
 * @author hasting
 */
public class Sha1SyncPositionHash {
	/** prefix in hex */
	public String p;
	/** summary hash */
	public String s;
	
	public Sha1SyncPositionHash position(String position) {
		p = position;
		return this;
	}
	public String position() {
		return p;
	}
	public Sha1SyncPositionHash summary(String summary) {
		s = summary;
		return this;
	}
	public String summary() {
		return s;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p == null) ? 0 : p.hashCode());
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sha1SyncPositionHash other = (Sha1SyncPositionHash) obj;
		if (p == null) {
			if (other.p != null)
				return false;
		} else if (!p.equals(other.p))
			return false;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;
	}
}
