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




import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

/**
 * Java bean used to format and parse the JSON messages for SHA-1 synchronization.
 * @author hasting
 */
public class Sha1SyncJson {
	/** level */
	public int l;
	/** hashes */
	public List<Sha1SyncPositionHash> h;
	/** max in any group */
	public long m;
	/** Sha1SyncJson protocol version, either missing (version1 clients/servers), or 2 for version2 */
	public String v;
	
	public Sha1SyncJson level(int level) {
		l = level;
		return this;
	}
	public int level() {
		return l;
	}
	public Sha1SyncJson hashes(List<Sha1SyncPositionHash> hashes) {
		h = hashes;
		return this;
	}
	public Sha1SyncJson hashes(Sha1SyncPositionHash... hashes) {
		h = new ArrayList<Sha1SyncPositionHash>(Arrays.asList(hashes));
		return this;
	}
	public List<Sha1SyncPositionHash> hashes() {
		return h;
	}
	public Sha1SyncJson max(long max) {
		m = max;
		return this;
	}
	public long max() {
		return m;
	}
	public Sha1SyncJson version(String v) {
		this.v = v;
		return this;
	}
	public String version() {
		return v;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public void dumpSha1SyncJson(String type, PrintStream out) {
		List<Sha1SyncPositionHash> hashes = hashes();
		if (hashes == null) {
			hashes = Arrays.asList();
		}
		out.printf("%s: level=%d,h.size=%d,max=%d {%n", 
				type, level(), hashes.size(), max());
		for (int i = 0; i < hashes.size(); i++) {
			out.printf(" %d={%s,%s)%n", 
					i, hashes.get(i).position(), hashes.get(i).summary());
		}
		out.println("}");
	}
	public int getL() {
		return l;
	}
	public void setL(int l) {
		this.l = l;
	}
	public List<Sha1SyncPositionHash> getH() {
		return h;
	}
	public void setH(List<Sha1SyncPositionHash> h) {
		this.h = h;
	}
	public long getM() {
		return m;
	}
	public void setM(long m) {
		this.m = m;
	}
	public String getV() {
		return v;
	}
	public void setV(String v) {
		if(v != null) {
			this.v = v;
		} else {
			this.v = "1.0";
		}
	}
	
}
