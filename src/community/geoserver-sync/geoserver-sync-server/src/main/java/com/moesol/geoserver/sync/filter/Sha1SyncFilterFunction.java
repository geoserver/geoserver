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

package com.moesol.geoserver.sync.filter;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.core.FeatureSha1;
import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

public class Sha1SyncFilterFunction extends FunctionExpressionImpl implements VolatileFunction {
	private static final Logger LOGGER = Logging.getLogger(Sha1SyncFilterFunction.class.getName());
	
	private static final String LITERAL_TRUE = "true";
	private static final Object LITERAL_FALSE = "false";
	private static final int INITIAL_LIST_SIZE = 4096;
	
	// TODO this is somewhat of a kludge, turns out we can get a filter instances from sha1 output format code
	// but the instance we get is not tied to the actual instance that was used in the filter.
	private static final ThreadLocal<String> FORMAT_OPTIONS = new ThreadLocal<String>();
	private static final ThreadLocal<Sha1SyncJson> REMOTE_SHA1_SYNC = new ThreadLocal<Sha1SyncJson>();
	private static final ThreadLocal<List<IdAndValueSha1s>> FEATURE_SHA1S = new ThreadLocal<List<IdAndValueSha1s>>();

	private final FeatureSha1 m_featureSha1Evaluator = new FeatureSha1();
	private final ArrayList<IdAndValueSha1s> m_featureSha1s = new ArrayList<IdAndValueSha1s>(INITIAL_LIST_SIZE);
	private VersionFeatures versionFeatures;
	
	private static final Comparator<Sha1SyncPositionHash> POSITION_COMPARATOR = new Comparator<Sha1SyncPositionHash>() {
		@Override
		public int compare(Sha1SyncPositionHash o1, Sha1SyncPositionHash o2) {
			return o1.position().compareTo(o2.position());
		}
	};
	
	public static FunctionName NAME = new FunctionNameImpl("sha1Sync", String.class, 
			parameter("attributes", String.class),
            parameter("sha1SyncJson", String.class));

	public Sha1SyncFilterFunction() {
		super(NAME);
		// NOTE: the use of thread locals relies on a new filter being created for every request 
		// AND the entire filter pass running on the same thread.
		// RH: Instead of caching the computed response in thread local variables perhaps
		// we should just have the format recompute the sha1 for any features that did not get filtered.
		clearThreadLocals();
	}

	/*** Communication between the filter and the formatted output ***/
	
	public static String getFormatOptions() {
		return FORMAT_OPTIONS.get();
	}
	public static Sha1SyncJson getSha1SyncJson() {
		return REMOTE_SHA1_SYNC.get();
	}
	public static List<IdAndValueSha1s> getFeatureSha1s() {
		return FEATURE_SHA1S.get();
	}
	public static void clearThreadLocals() {
		LOGGER.log(Level.FINE, "Clear: {0}/{1}/{2}", 
				new Object[] { FORMAT_OPTIONS.get(), REMOTE_SHA1_SYNC.get(), FEATURE_SHA1S.get() });
		
		List<IdAndValueSha1s> sha1s = FEATURE_SHA1S.get();
		if (sha1s != null) {
			sha1s.clear();
		}
		
		FORMAT_OPTIONS.set(null);
		REMOTE_SHA1_SYNC.set(null);
		FEATURE_SHA1S.set(null);
	}

	private void maybeOneTimeSetup() {
		if (FEATURE_SHA1S.get() != null) {
			return;
		}
		oneTimeSetup();
	}
	
	/*** Filter computation ***/
	
	private void oneTimeSetup() {
		List<Expression> args = getParameters();
		if (args.size() < 2) {
			throw new IllegalArgumentException("sha1Sync requires two arguments {attributes}, and {sha1SyncJson}");
		}
		String atts = args.get(0).toString();
		String json = args.get(1).toString();
		
		m_featureSha1Evaluator.parseAttributesToInclude(atts);
		Sha1SyncJson remoteSha1Sync = new Gson().fromJson(json, Sha1SyncJson.class);
		versionFeatures = VersionFeatures.fromSha1SyncJson(remoteSha1Sync);
		FORMAT_OPTIONS.set(atts);
		REMOTE_SHA1_SYNC.set(remoteSha1Sync);
		FEATURE_SHA1S.set(m_featureSha1s);
		LOGGER.log(Level.FINER, "Recorded: {0}/{1}/{2}", 
				new Object[] { FORMAT_OPTIONS.get(), REMOTE_SHA1_SYNC.get(), FEATURE_SHA1S.get() });
	}
	
	@Override
	public Object evaluate(Object object) {
		maybeOneTimeSetup(); // Collect sha1s over all filters checked.

		Feature feature = (Feature) object;
		
		Sha1Value idSha1 = m_featureSha1Evaluator.computeIdSha1(feature);
		Sha1Value valueSha1 = m_featureSha1Evaluator.computeValueSha1(feature);
		IdAndValueSha1s pair = new IdAndValueSha1s(idSha1, valueSha1);
		m_featureSha1s.add(pair);
		Sha1Value prefixSha1 = versionFeatures.getBucketPrefixSha1(pair);
		
		Sha1SyncJson remoteSha1Sync = REMOTE_SHA1_SYNC.get();
		if (remoteSha1Sync.max() > 1) {
			return LITERAL_TRUE; // Keep all features, we are not deep enough in search tree
		}
		if (remoteSha1Sync.hashes() == null) {
			return LITERAL_TRUE; // Missing all...
		}
		
		Sha1SyncPositionHash sha1Position = new Sha1SyncPositionHash().position(prefixSha1.toString());
		int idx = Collections.binarySearch(remoteSha1Sync.hashes(), sha1Position, POSITION_COMPARATOR);
		if (idx < 0) {
			idx = -idx - 2; // position on shorter prefix
		}
		if (idx < 0 || remoteSha1Sync.hashes().size() < idx) {
			return LITERAL_FALSE;
		}
		Sha1SyncPositionHash remoteGroup = remoteSha1Sync.hashes().get(idx);
		if (!prefixSha1.toString().startsWith(remoteGroup.position())) {
			return LITERAL_FALSE; // Missing position means remote side thinks position is synchronized.
		}
		Sha1Value sha1OfSha1 = m_featureSha1Evaluator.sha1OfSha1(valueSha1);
		if (sha1OfSha1.toString().equals(remoteGroup.summary())) {
			return LITERAL_FALSE; // Exact match, filter
		}
		return LITERAL_TRUE;
	}
	
}
