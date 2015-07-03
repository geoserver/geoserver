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




import java.util.Comparator;

/**
 * Compare pairs based on which sha1 is being used for the bucket prefix.
 * @author hasting
 */
public class IdAndValueSha1Comparator implements Comparator<IdAndValueSha1s> {
	private VersionFeatures versionFeatures;

	public IdAndValueSha1Comparator(VersionFeatures vf) {
		versionFeatures = vf;
	}
	
	@Override
	public int compare(IdAndValueSha1s left, IdAndValueSha1s right) {
		return versionFeatures.getBucketPrefixSha1(left).compareTo(versionFeatures.getBucketPrefixSha1(right));
	}
	
}
