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

package com.moesol.geoserver.sync.format;




import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.core.FeatureSha1;
import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.ServerReconciler;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.filter.Sha1SyncFilterFunction;
import com.moesol.geoserver.sync.format.FeatureCollectionSha1Sync;
import com.moesol.geoserver.sync.grouper.Sha1JsonLevelGrouper;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

/**
 * SHA-1 set check. The order of the features should not matter, just the set existence.
 * Therefore, we record the SHA-1 for each feature then sort the SHA-1's and finally
 * run SHA-1 over the SHA-1's.
 * 
 * Apartment model threading.
 * 
 * @author Moebius Solutions, Inc.
 */
public class FeatureCollectionSha1Sync {
	private static final Logger LOGGER = Logging.getLogger(FeatureCollectionSha1Sync.class.getName());
	private static final Sha1SyncJson EMPTY_SET = 
		new Sha1SyncJson().level(-1).hashes(new ArrayList<Sha1SyncPositionHash>());
	public static PrintStream TRACE_RESPONSE = null;
	
	private final Charset UTF8 = Charset.forName("UTF-8");
	private final PrintWriter m_out;
	private final FeatureSha1 m_featureSha1Sync = new FeatureSha1();
	private final int INITIAL_LIST_SIZE = 4096;
	private List<IdAndValueSha1s> m_featureSha1s = new ArrayList<IdAndValueSha1s>(INITIAL_LIST_SIZE);
	private VersionFeatures versionFeatures = VersionFeatures.VERSION1;

	/** collection we are working on */
	private FeatureCollectionResponse m_featureCollectionResponse;
	private Sha1SyncJson m_remoteSha1SyncJson = EMPTY_SET;

	public FeatureCollectionSha1Sync(OutputStream output) throws ServiceException {
		m_out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, UTF8)));
	}

	public void write(FeatureCollectionResponse featureCollection) {
		captureFilterParameters();
		setCollection(featureCollection);
	    Sha1SyncJson response = compute();
		LOGGER.log(Level.FINE, "output({0}})", response); 
	    if (TRACE_RESPONSE != null) {
	    	response.dumpSha1SyncJson("RESPONSE", TRACE_RESPONSE);
	    }
		writeAsJsone(response);
	    Sha1SyncFilterFunction.clearThreadLocals();	
	}

	private void captureFilterParameters() {
		String atts = Sha1SyncFilterFunction.getFormatOptions();
		Sha1SyncJson sha1SyncJson = Sha1SyncFilterFunction.getSha1SyncJson();
		
		LOGGER.log(Level.FINER, "filter options {0}, filter JSON {1}", 
				new Object[] { atts, sha1SyncJson });
		if (atts != null) {
			parseAttributesToInclude(atts);
		}
		if (sha1SyncJson != null) {
			setRemoteSha1SyncJson(sha1SyncJson);
		} else if (m_remoteSha1SyncJson != null) {
			// already set (unit test)
		} else {
			setRemoteSha1SyncJson(EMPTY_SET);
		}
	}

	public void setCollection(FeatureCollectionResponse featureCollectionResponse){
		m_featureCollectionResponse = featureCollectionResponse;
	}
	
	public void setRemoteSha1SyncJson(Sha1SyncJson remote) {
		this.m_remoteSha1SyncJson = remote;
		versionFeatures = VersionFeatures.fromSha1SyncJson(remote);
    }

	public Sha1SyncJson compute() {
		if (m_remoteSha1SyncJson.level() < 0) {
			return computeZero();
		}
		return computeServerResponse();
	}
	
	public Sha1SyncJson computeZero() {
		LOGGER.log(Level.FINE, "attributes={0}, sync={1}", 
				new Object[] { m_featureSha1Sync.getAttributesToInclude(), m_remoteSha1SyncJson});
		@SuppressWarnings("rawtypes")
		List<FeatureCollection> resultsList = m_featureCollectionResponse.getFeature();
	    processAllCollections(resultsList);
	    
	    Sha1JsonLevelGrouper grouper = new Sha1JsonLevelGrouper(versionFeatures, m_featureSha1s);
	    grouper.groupForLevel(0);
	    return grouper.getJson();
	}

	public Sha1SyncJson computeServerResponse() {
		@SuppressWarnings("rawtypes")
		List<FeatureCollection> resultsList = m_featureCollectionResponse.getFeature();
	    processAllCollections(resultsList);
	    
	    
	    Sha1JsonLevelGrouper grouper = new Sha1JsonLevelGrouper(versionFeatures, m_featureSha1s);
	    grouper.groupForLevel(m_remoteSha1SyncJson.level());
	    Sha1SyncJson localSha1SyncJson = grouper.getJson();
	    
	    grouper.groupForLevel(m_remoteSha1SyncJson.level() + 1);
	    Sha1SyncJson outputSha1SyncJson = grouper.getJson();
	    
	    ServerReconciler recon = new ServerReconciler(localSha1SyncJson, m_remoteSha1SyncJson);
	    recon.filterDownNextLevel(outputSha1SyncJson);
	    
		LOGGER.log(Level.FINER, "attributes({0}), local({1}), remote({2})", 
				new Object[] { m_featureSha1Sync.getAttributesToInclude(), localSha1SyncJson, m_remoteSha1SyncJson});
	    return outputSha1SyncJson;
	}
	
	private void processAllCollections(@SuppressWarnings("rawtypes") List<FeatureCollection> resultsList) {
		List<IdAndValueSha1s> sha1s = Sha1SyncFilterFunction.getFeatureSha1s();
		if (sha1s != null) {
			m_featureSha1s = sha1s;
			return; // Filter beat us to it, no work for us :-)
		}
	
		for (int i = 0; i < resultsList.size(); i++) {
			FeatureCollection<?,?> collection = resultsList.get(i);
			processOne(collection);
		}
	}

	private void processOne(FeatureCollection<?,?> collection) {
        FeatureIterator<?> iterator = collection.features();
        try {
            while (iterator.hasNext()) {
            	Feature feature = iterator.next();
            	sha1One(feature);

            }
        } finally {
        	iterator.close();
        }
	}

	public void sha1One(Feature feature) {
		Sha1Value idSha1 = m_featureSha1Sync.computeIdSha1(feature);
		Sha1Value valueSha1 = m_featureSha1Sync.computeValueSha1(feature);
		m_featureSha1s.add(new IdAndValueSha1s(idSha1, valueSha1));
	}
	
	private void writeAsJsone(Sha1SyncJson json) {
		Gson gson = new Gson();
		gson.toJson(json, m_out);
    	m_out.flush();
	}
	
	public void parseAttributesToInclude(String atts) {
		m_featureSha1Sync.parseAttributesToInclude(atts);
	}
	
	static String [] parseAttributes(String atts) {
		if (atts == null) {
			return new String[0];
		}
		String[] r = atts.split("[,\\s]+");
		if (r[0].isEmpty()) {
			return new String[0];
		}
		return r;
	}

	public void parseSha1SyncJson(String sha1SyncJson) {
		if (sha1SyncJson == null) {
			setRemoteSha1SyncJson(EMPTY_SET);
			return;
		}
		setRemoteSha1SyncJson(new Gson().fromJson(sha1SyncJson, Sha1SyncJson.class));
	}

}
