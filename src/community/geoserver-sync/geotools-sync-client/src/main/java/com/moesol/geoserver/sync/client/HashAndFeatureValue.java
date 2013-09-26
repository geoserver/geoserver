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

package com.moesol.geoserver.sync.client;



import org.opengis.feature.Feature;

import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;

public class HashAndFeatureValue extends IdAndValueSha1s {
	private final Feature m_feature;
	
	public HashAndFeatureValue(Sha1Value idSha1, Sha1Value valueSha1, Feature feature) {
		super(idSha1, valueSha1);
		m_feature = feature;
	}
	
	public Feature getFeature() {
		return m_feature;
	}

	@Override
	public String toString() {
		return String.format("{ id: %s, value: %s, fid: %s }", 
				getIdSha1().toString(), getValueSha1().toString(), getFeature().getIdentifier().getID());
	}

}
