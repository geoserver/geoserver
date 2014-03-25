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




import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.Feature;
import org.opengis.filter.identity.Identifier;

public class FeaturesChangedAdaptor implements FeatureChangeListener, RoundListener {
	private final List<Feature> creates = new ArrayList<Feature>();
	private final List<Feature> updates = new ArrayList<Feature>();
	private final List<Feature> deletes = new ArrayList<Feature>();
	private final FeaturesChangedListener targetListener;
	
	public FeaturesChangedAdaptor(FeaturesChangedListener target) {
		this.targetListener = target;
	}
	
	@Override
	public void beforeRound(int r) { }

	@Override
	public void afterRound(int r) { }

	@Override
	public void afterSynchronize() {
		targetListener.featuresCreate(creates.toArray(new Feature[creates.size()]));
		targetListener.featuresUpdate(updates.toArray(new Feature[updates.size()]));
		targetListener.featuresDelete(deletes.toArray(new Feature[deletes.size()]));
		
		creates.clear();
		updates.clear();
		deletes.clear();
	}

	@Override
	public void sha1Collision() { }

	@Override
	public void featureCreate(Identifier fid, Feature feature) {
		creates.add(feature);
	}

	@Override
	public void featureUpdate(Identifier fid, Feature feature) {
		updates.add(feature);
	}

	@Override
	public void featureDelete(Identifier fid, Feature feature) {
		deletes.add(feature);
	}

}
