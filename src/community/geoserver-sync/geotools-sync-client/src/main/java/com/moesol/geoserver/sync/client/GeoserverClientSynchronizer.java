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




import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.input.CountingInputStream;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.feature.Feature;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.core.ClientReconciler;
import com.moesol.geoserver.sync.core.FeatureSha1;
import com.moesol.geoserver.sync.core.IdAndValueSha1Comparator;
import com.moesol.geoserver.sync.core.ReconcilerDelete;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.grouper.Sha1JsonLevelGrouper;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

/**
 * Synchronizes this client with geoserver using sha1Sync filter and sha1Sync outputFormat.
 * @author hastings
 */
public class GeoserverClientSynchronizer {
	private static final Logger LOGGER = Logging.getLogger(GeoserverClientSynchronizer.class.getName());
	private static final int INITIAL_LIST_SIZE = 4096;
	private static final int MAX_ROUNDS = 21;
	private static final String SHA1_SYNC_OUTPUT_FORMAT = "SyncChecksum";
	private static final String GML3_OUTPUT_FORMAT = "GML3";
	public static PrintStream TRACE_POST = null;
	private final FeatureSha1 m_featureSha1Sync = new FeatureSha1();
	private final Configuration m_configuration;
	private final String m_url;
	private final String m_postTemplate;
	private Thread m_thread;
	private VersionFeatures versionFeatures = VersionFeatures.VERSION2;
	private int m_numRounds;
	private int m_numCreates;
	private int m_numUpdates;
	private int m_numDeletes;
	private long m_rxBytes;
	private long m_rxGml;
	private long m_txBytes; // approximate, does not include HTTP overhead or UTF-8 escape sequences (if any).
	private long m_parseMillis;
	private RequestBuilder m_builder;
	private String m_attributesToInclude = "-all";
	private String m_lastOutputFormat;
	private Sha1SyncJson m_server;
	private List<HashAndFeatureValue> m_featureSha1s = new ArrayList<HashAndFeatureValue>(INITIAL_LIST_SIZE);
	private Map<Identifier, FeatureAccessor> m_features = new HashMap<Identifier, FeatureAccessor>();
	private Set<Identifier> m_potentialDeletes = new HashSet<Identifier>();
	private FeatureChangeListener m_listener = new FeatureChangeListener() {
		@Override
		public void featureCreate(Identifier fid, Feature feature) {
			m_features.put(fid, wrap(feature));
		}
		@Override
		public void featureUpdate(Identifier fid, Feature feature) {
			m_features.put(fid, wrap(feature));
		}
		@Override
		public void featureDelete(Identifier fid, Feature feature) {
			m_features.remove(fid);
		}
		private FeatureAccessor wrap(Feature f) {
			return new FeatureAccessorImpl(f);
		}
	};
	private ReconcilerDelete m_deleter = new ReconcilerDelete() {
		@Override
		public void deleteGroup(Sha1SyncPositionHash group) {
			deleteInPosition(group);
		}
	};
	private RoundListener m_roundListener = new RoundListener() {
		@Override
		public void beforeRound(int r) { }
		@Override
		public void afterRound(int r) { }
		@Override
		public void afterSynchronize() { }
		@Override
		public void sha1Collision() { }
	};
	
	/**
	 * Run the geoserver to client init-sync algorithm using sha1Sync filter and outputFormats plugged
	 * into geoserver.
	 * 
	 * The postTemplate should include the sha1Sync function like so:
	 * <pre>
	 * &lt;wfs:GetFeature 
     *  service="WFS" 
     *  version="1.1.0"
     *  outputFormat="${outputFormat}"
     *  xmlns:cdf="http://www.opengis.net/cite/data"
     *  xmlns:ogc="http://www.opengis.net/ogc"
     *  xmlns:wfs="http://www.opengis.net/wfs"&gt;
     *   &lt;wfs:Query typeName="cite:Buildings"&gt;
     *    &lt;ogc:Filter&gt;
     *     &lt;ogc:PropertyIsEqualTo&gt;
     *      &lt;ogc:Function name="sha1Sync"&gt;
     *       &lt;ogc:Literal&gt;${attributes}&lt;/ogc:Literal&gt;
     *       &lt;ogc:Literal&gt;${sha1Sync}&lt;/ogc:Literal&gt;
     *      &lt;/ogc:Function&gt;
     *      &lt;ogc:Literal&gt;true&lt;/ogc:Literal&gt;
     *     &lt;/ogc:PropertyIsEqualTo&gt;
     *    &lt;/ogc:Filter&gt;
     *   &lt;/wfs:Query&gt; 
     *  &lt;/wfs:GetFeature&gt;
	 * </pre> 
	 * @param configuration
	 * @param builder
	 * @param url
	 * @param postTemplate 
	 */
	public GeoserverClientSynchronizer(Configuration configuration, String url, String postTemplate) {
		m_configuration = configuration;
		m_builder = new URLConnectionRequestBuilder();
		m_url = url;
		m_postTemplate = postTemplate;
		m_thread = Thread.currentThread();
	}
	
	public RequestBuilder getRequestBuilder() {
		return m_builder;
	}

	public void setRequestBuilder(RequestBuilder builder) {
		m_builder = builder;
	}

	public Map<Identifier, FeatureAccessor> getFeatures() {
		return m_features;
	}
	public void setFeatures(Map<Identifier, FeatureAccessor> features) {
		m_features = features;
	}
	
	public String getAttributesToInclude() {
		return m_attributesToInclude;
	}
	/**
	 * Comma separated list of property names to include in SHA-1 or "-all" for
	 * all. Currently Geometry properties are never included in the SHA-1 because
	 * of coordinate ordering issues.
	 * 
	 * @param attributesToInclude
	 */
	public void setAttributesToInclude(String attributesToInclude) {
		m_attributesToInclude = attributesToInclude;
	}

	public FeatureChangeListener getListener() {
		return m_listener;
	}
	/**
	 * Listener for changes needed to features.
	 * The default implementation calls put/remove on the feature map
	 * to synchronize it.
	 * 
	 * @param listener
	 */
	public void setListener(FeatureChangeListener listener) {
		m_listener = listener;
	}
	
	/**
	 * @return Listener for before/after a sync round.
	 * The default implementation does nothing
	 */
	public RoundListener getRoundListener() {
		return m_roundListener;
	}

	public void setRoundListener(RoundListener m_roundListener) {
		this.m_roundListener = m_roundListener;
	}

	/**
	 * Try to synchronize with the server. This method either succeeds or throws an IOException.
	 * @param features Map of feature id to object that is read to determine what client already has.
	 * The feature listener is called to notify clients what operations are needed to update the 
	 * features. Note that the default feature listener will put/remove features to make it match the server.
	 *  
	 * @throws IOException
	 * @throws ParserConfigurationException if provided configuration has an issue
	 * @throws SAXException 
	 */
	public void synchronize(Map<Identifier, FeatureAccessor> features) throws IOException, SAXException, ParserConfigurationException {
		m_features = features;
		checkThread();
		resetCounters();
		m_potentialDeletes.clear();
		
		m_featureSha1Sync.parseAttributesToInclude(m_attributesToInclude);
		computeSha1s();
		m_server = new Sha1SyncJson().level(0).max(Long.MAX_VALUE);

		long s = System.currentTimeMillis();
		try {
			for (int i = 0; i < MAX_ROUNDS; i++) {
				if (processRound(i)) {
					return;
				}
			}
			LOGGER.log(Level.WARNING, "Failed after {0} rounds, SHA-1 collision?", MAX_ROUNDS);
			m_roundListener.sha1Collision();
		} finally {
			realizePotentialDeletes();
			long totalMillis = System.currentTimeMillis() - s;
			LOGGER.log(Level.INFO, "total({0}ms), parse({1}ms), rounds({2}), creates({3}), updates({4}), deletes({5}), tx({6}), rx({7}), gml({8})",
					new Object[] { totalMillis, m_parseMillis, m_numRounds, m_numCreates, m_numUpdates, m_numDeletes,
						m_txBytes, m_rxBytes, m_rxGml
					}
			);
			m_roundListener.afterSynchronize();
		}
	}

	private void checkThread() {
		if (m_thread != Thread.currentThread()) {
			LOGGER.log(Level.WARNING, "Thread changed was({0}), now({1})", 
					new Object[] { m_thread, Thread.currentThread() });
			m_thread = Thread.currentThread();
		}
	}

	private void resetCounters() {
		m_numRounds = 0;
		m_numCreates = 0;
		m_numUpdates = 0;
		m_numDeletes = 0;
		m_parseMillis = 0L;
		m_rxBytes = 0L;
		m_rxGml = 0L;
		m_txBytes = 0L;
	}

	/**
	 * @param roundNumber round number
	 * @return true when synchronized
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	private boolean processRound(int roundNumber) throws IOException, SAXException, ParserConfigurationException {
		m_roundListener.beforeRound(roundNumber);
		long s = System.currentTimeMillis();
		try {
			Sha1SyncJson localSyncState = (roundNumber == 0) ? computeLevelZero() : computeNextLevel();
			Response response = post(localSyncState);
			return processResponse(response);
		} finally {
			m_numRounds++;
			LOGGER.log(Level.FINEST, "ms({0}), server.level({1})", 
					new Object[] {System.currentTimeMillis() - s, 
					m_server != null ? m_server.level() : "server=null?"});
			m_roundListener.afterRound(roundNumber);
		}
	}
	
	private Response post(Sha1SyncJson outputJson) throws IOException {
        m_lastOutputFormat = isReadyForGML(outputJson) ? GML3_OUTPUT_FORMAT : SHA1_SYNC_OUTPUT_FORMAT; 
        String json = new Gson().toJson(outputJson);
        
        String xmlRequest = m_postTemplate.replaceAll(Pattern.quote("${outputFormat}"), m_lastOutputFormat);
        xmlRequest = xmlRequest.replaceAll(Pattern.quote("${attributes}"), m_attributesToInclude);
        xmlRequest = xmlRequest.replaceAll(Pattern.quote("${sha1Sync}"), json); 

        // Unit test support to pass along outputJson
        if (m_builder instanceof RequestBuilderJUnit) {
			RequestBuilderJUnit reqJUnit = (RequestBuilderJUnit) m_builder;
			reqJUnit.prePost(m_lastOutputFormat, m_attributesToInclude, json);
		}
        LOGGER.log(Level.FINE, "outputFormat({0}), attributes({1}), json({2})", 
        		new Object[] {m_lastOutputFormat, m_attributesToInclude, json});
        if (TRACE_POST != null) {
        	outputJson.dumpSha1SyncJson("POST", TRACE_POST);
        }
        m_txBytes += xmlRequest.length();
        return m_builder.post(m_url, xmlRequest);
	}

	/**
	 * If either the server or the client have buckets with more
	 * than one SHA-1 in them, we must do another round.
	 * 
	 * @param outputJson
	 * @return true if ready for server to send back GML.
	 */
	private boolean isReadyForGML(Sha1SyncJson outputJson) {
		if (m_server.max() > 1) {
			return false;
		}
		if (outputJson.max() > 1) {
			return false;
		}
		return true;
	}
	
	/**
	 * @return true if synchronized
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	boolean processResponse(Response response) throws IOException, SAXException, ParserConfigurationException {
// TODO
//		if (response.getResponseCode() != 200) {
//			processError(response);
//		}
		if (m_lastOutputFormat == GML3_OUTPUT_FORMAT) {
			processGmlResponse(response);
			return true;
		}
		return processSha1SyncResponse(response);
	}

//	private void processError(Response response) throws IOException {
//		InputStream is = response.getResultStream();
//		try {
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			int c;
//			while ((c = is.read()) != -1) {
//				baos.write(c);
//			}
//			throw new RuntimeException("Failed: " + response.getResponseMessage() + baos.toString("UTF-8"));
//		} finally {
//			is.close();
//		}
//	}

	void processGmlResponse(Response response) throws IOException, SAXException, ParserConfigurationException {
		FeatureCollection<?, ?> features;
		if (response instanceof ResponseFeatureCollection) {
			ResponseFeatureCollection responseFeatures = (ResponseFeatureCollection) response;
			features = responseFeatures.getFeatureCollection();
		} else {
			CountingInputStream counter = new CountingInputStream(response.getResultStream());
			features = (FeatureCollection<?, ?>)parseWfs(counter);
			m_rxGml += counter.getByteCount();
		}
		
		FeatureIterator<?> it = features.features();
		try {
			while (it.hasNext()) {
				Feature feature = it.next();
				FeatureId fid = feature.getIdentifier();
				m_potentialDeletes.remove(fid);
				if (!m_features.containsKey(fid)) {
					m_listener.featureCreate(fid, feature);
					m_numCreates++;
				} else {
					m_listener.featureUpdate(fid, feature);
					m_numUpdates++;
				}
			}
		} finally {
			it.close();
		}
	}

	private void realizePotentialDeletes() {
		for (Identifier fid : m_potentialDeletes) {
			FeatureAccessor accessor = m_features.get(fid);
			Feature feature = accessor.getFeature();
			
			m_listener.featureDelete(fid, feature);
			m_numDeletes++;
		}
	}

	protected Object parseWfs(InputStream is) throws IOException, SAXException, ParserConfigurationException {
		long s = System.currentTimeMillis();
		try {
			Parser parser = new Parser(m_configuration);
			return parser.parse(is);
		} finally {
			long e = System.currentTimeMillis();
			m_parseMillis = e - s;
			is.close();
		}
	}

	private boolean processSha1SyncResponse(Response response) throws IOException {
		int expected = m_server.level() + 1;
		CountingInputStream counter = new CountingInputStream(response.getResultStream());
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(counter), UTF8.UTF8);
		try {
			m_server = new Gson().fromJson(reader, Sha1SyncJson.class);
			if (expected != m_server.level()) {
				throw new IllegalStateException("Level warp! expected("+expected+"), actual("+m_server.level()+")");
			}
			if (!versionFeatures.getToken().equals(m_server.version())) {
				throw new IllegalStateException("Version warp! expected("+versionFeatures.getToken()+"), actual("+m_server.version()+")");
			}
			if (isServerEmpty()) {
				clearLocal();
				return true;
			}
			if (isServerHashesEmpty()) {
				return true;
			}
			return false;
		} finally {
			m_rxBytes += counter.getByteCount();
			reader.close();
		}
	}

	private void clearLocal() {
		LOGGER.log(Level.INFO, "Server empty, deleting all");
		Sha1SyncJson levelZero = computeLevelZero();
		for (Sha1SyncPositionHash pos : levelZero.h) {
			m_deleter.deleteGroup(pos);
		}
	}

	private boolean isServerEmpty() {
		return m_server.level() == 1 && m_server.max() == 0;
	}

	private boolean isServerHashesEmpty() {
		return m_server.hashes() == null || m_server.hashes().size() == 0;
	}

	/**
	 * Delete all the features we have with this prefix
	 * This prefix is empty on the server.
	 * 
	 * @param position
	 */
	private void deleteInPosition(Sha1SyncPositionHash position) {
		Sha1Value prefix = new Sha1Value(position.position());
		HashAndFeatureValue find = new HashAndFeatureValue(prefix, null, null);
		// TODO, hmm, better search?
		int i = Collections.binarySearch(m_featureSha1s, find, new IdAndValueSha1Comparator(versionFeatures));
		if (i < 0) {
			i = -i - 1;
		}
		for ( ; i < m_featureSha1s.size(); i++) {
			HashAndFeatureValue value = m_featureSha1s.get(i);
			if (!versionFeatures.getBucketPrefixSha1(value).isPrefixMatch(prefix.get())) {
				break;
			}
			FeatureId fid = value.getFeature().getIdentifier();
			m_potentialDeletes.add(fid);
		}
		
	}

	private void computeSha1s() {
		LOGGER.log(Level.FINER, "attributes={0}, sync={1}", 
				new Object[] { m_featureSha1Sync.getAttributesToInclude(), m_server});
		
		m_featureSha1s.clear();
		for (FeatureAccessor a : m_features.values()) {
			Feature f = a.getFeature();
			m_featureSha1s.add(makeHashAndFeatureValue(f));
		}
		Collections.sort(m_featureSha1s, new IdAndValueSha1Comparator(versionFeatures));
	}

	protected HashAndFeatureValue makeHashAndFeatureValue(Feature f) {
		Sha1Value idSha1 = m_featureSha1Sync.computeIdSha1(f);
		Sha1Value valueSha1 = m_featureSha1Sync.computeValueSha1(f);
		return new HashAndFeatureValue(idSha1, valueSha1, f);
	}

	private Sha1SyncJson computeLevelZero() {
	    Sha1JsonLevelGrouper grouper = new Sha1JsonLevelGrouper(versionFeatures, m_featureSha1s);
	    grouper.groupForLevel(0);
	    return grouper.getJson();
	}
	
	private Sha1SyncJson computeNextLevel() {
	    Sha1JsonLevelGrouper grouper = new Sha1JsonLevelGrouper(versionFeatures, m_featureSha1s);
	    grouper.groupForLevel(m_server.level());
	    Sha1SyncJson localSha1SyncJson = grouper.getJson();
	    
	    Sha1SyncJson outputSha1SyncJson = new Sha1SyncJson().level(m_server.level());
	    // Copy over some of the local properties
	    outputSha1SyncJson.max(localSha1SyncJson.max());
	    outputSha1SyncJson.version(localSha1SyncJson.version());
	    
	    ClientReconciler recon = new ClientReconciler(localSha1SyncJson, m_server);
	    recon.setDelete(m_deleter);
	    recon.computeOutput(outputSha1SyncJson);
	    return outputSha1SyncJson;
	}

	public int getNumRounds() {
		return m_numRounds;
	}

	public int getNumCreates() {
		return m_numCreates;
	}

	public int getNumUpdates() {
		return m_numUpdates;
	}

	public int getNumDeletes() {
		return m_numDeletes;
	}

}
