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

import static com.moesol.geoserver.sync.client.Features.asMap;
import static com.moesol.geoserver.sync.client.Features.f;
import static com.moesol.geoserver.sync.client.Features.featuresEq;
import static com.moesol.geoserver.sync.client.Features.make;
import static com.moesol.geoserver.sync.client.Features.makeConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.wfs.WFSTestSupport;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.identity.FeatureIdImpl;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.xml.sax.SAXException;

import com.moesol.geoserver.sync.core.FeatureSha1;
import com.moesol.geoserver.sync.core.FeatureSha1Mapper;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.vividsolutions.jts.io.ParseException;

public class GeoserverClientSynchronizerIntegrationTest extends WFSTestSupport {
	
	@Test
	public void testParseWfs() throws IOException, SAXException, ParserConfigurationException {
		GeoserverClientSynchronizer clientSynchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "", SimulatedRequestBuilder.POST_TEMPLATE);
		InputStream is = loadResource("buildings.xml");
		FeatureCollection features = (FeatureCollection) clientSynchronizer.parseWfs(is);
		assertEquals(2, features.size());
		FeatureIterator<?> it = features.features();
		try {
			int i = 0;
			while (it.hasNext()) {
				Feature feature = it.next();
				switch (i) {
				case 0: assertEquals("Buildings.1107531701010", feature.getIdentifier().getID()); break;
				case 1: assertEquals("Buildings.1107531701011", feature.getIdentifier().getID()); break;
				}
				i++;
			}
		} finally {
			it.close();
		}
	}

	@Test
	public void testProcessGmlResponse() throws IOException, SAXException, ParserConfigurationException {
		FeatureChangeListener listener = mock(FeatureChangeListener.class);
		GeoserverClientSynchronizer clientSynchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "", SimulatedRequestBuilder.POST_TEMPLATE);
		clientSynchronizer.setListener(listener);
		
		TestResponse gml = makeGmlResponse();
		clientSynchronizer.processGmlResponse(gml);
		
		verify(listener).featureCreate(eq(fid("Buildings.1107531701010")), (Feature)notNull());
		verify(listener).featureCreate(eq(fid("Buildings.1107531701011")), (Feature)notNull());
		verifyNoMoreInteractions(listener);
	}

	private FeatureId fid(String fid) {
		return new FeatureIdImpl(fid);
	}

	@Test
	public void test_Client0_with_Server3() throws IOException, SAXException, ParserConfigurationException, ParseException {
//		GeoserverClientSynchronizer.TRACE_POST = new PrintStream(new FileOutputStream("run.log"));
//		FeatureCollectionSha1Sync.TRACE_RESPONSE = GeoserverClientSynchronizer.TRACE_POST;
		
		FeatureCollectionType client = make();		
		FeatureCollectionType server = make(f("F1", 0), f("F2", 1), f("F3", 2));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(3, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		assertEquals(3, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void testSameSets_3() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2));
		FeatureCollectionType server = make(f("F1", 0), f("F2", 1), f("F3", 2));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(3, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}
	
	@Test
	public void testUpdateOneFeature() throws ParseException, IOException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2));		
		FeatureCollectionType server = make(f("F1", 0), f("F2", 3), f("F3", 2));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// updated F2
		assertEquals(3, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 3, "F2");
		check(clientMap, 2, "F3");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(1, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());

		
		server = make(f("F1", 4), f("F2", 3), f("F3", 2));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// updated F1
		assertEquals(3, clientMap.size());
		check(clientMap, 4, "F1");
		check(clientMap, 3, "F2");
		check(clientMap, 2, "F3");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(1, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());

		
		server = make(f("F1", 4), f("F2", 3), f("F3", 5));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		// updated F3
		assertEquals(3, clientMap.size());
		check(clientMap, 4, "F1");
		check(clientMap, 3, "F2");
		check(clientMap, 5, "F3");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(1, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}

	@Test
	public void testCreateOneFeature() throws ParseException, IOException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2));		
		FeatureCollectionType server = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// created F4
		assertEquals(4, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		check(clientMap, 3, "F4");
		assertEquals(1, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
		
		
		server = make(f("F0", -1), f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// created F0
		assertEquals(5, clientMap.size());
		check(clientMap, -1, "F0");
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		check(clientMap, 3, "F4");
		assertEquals(1, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
		
		
		server = make(f("F0", -1), f("F1", 0), f("F2", 1), f("F2.1", 22), f("F3", 2), f("F4", 3));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// created F2.1
		assertEquals(6, clientMap.size());
		check(clientMap, -1, "F0");
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 22, "F2.1");
		check(clientMap, 2, "F3");
		check(clientMap, 3, "F4");
		assertEquals(1, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}

	@Test
	public void test_Client3_with_Server0() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2));
		FeatureCollectionType server = make();
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(0, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(3, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client0_with_Server0() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make();
		FeatureCollectionType server = make();
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(0, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client0_with_Server1() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make();
		FeatureCollectionType server = make(f("F1", 0));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(1, clientMap.size());
		assertEquals(1, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client1_with_Server0() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0));
		FeatureCollectionType server;
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		server = make();
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(0, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client1_with_Server1_Updated() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0));
		FeatureCollectionType server = make(f("F1", 1));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(1, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(1, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client1_with_Server1_DifferentKey() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0));
		FeatureCollectionType server = make(f("F2", 1));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(1, clientMap.size());
		assertEquals(1, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client1_with_Server1_Same() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0));
		FeatureCollectionType server;
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		server = make(f("F1", 0));
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(1, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}
	
	/**
	 * Map the SHA1's so that we have two SHA1's with all the same prefix bytes and only the last byte differs by one.
	 * 
	 * @throws ParseException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Test
	public void testDeleteOneFeatureInSameBucket() throws ParseException, IOException, SAXException, ParserConfigurationException  {
		FeatureSha1Mapper old = FeatureSha1.MAPPER;
		try {
			_testDeleteOneFeatureInSameBucket();
		} finally {
			FeatureSha1.MAPPER = old;
		}
	}
	private void _testDeleteOneFeatureInSameBucket() throws ParseException, IOException, SAXException, ParserConfigurationException  {
		FeatureSha1.MAPPER = new FeatureSha1Mapper() {
			@Override
			public Sha1Value map(Sha1Value old) {
				if ("e76becbcfbe3b847478a3aa706def57382d8b884".equals(old.toString())) {
					return old;
				}
				if ("ac93e7bcebb89813f206d9b618c94e1531cc6a0d".equals(old.toString())) {
					return new Sha1Value("ba5a86a5a1c34f30e7e3a9e8eef485f025c8d606");
				}
				if ("88bfad9cfffeafd299a44d4daf979d57419a2621".equals(old.toString())) {
					return old;
				}
				if ("2a7bc94a06f3221293677515044b0a9dd3960f4e".equals(old.toString())) {
					return new Sha1Value("88bfad9cfffeafd299a44d4daf979d57419a2622");
				}
				throw new IllegalStateException("Mapping of input SHA1's has changed unable to setup test conditions: " + old);
			}
		};
		
		FeatureCollectionType client = make(f("F0", -1), f("F1", 0));
		FeatureCollectionType server;
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE) {
			@Override
			protected HashAndFeatureValue makeHashAndFeatureValue(Feature f) {
				HashAndFeatureValue hashFeature = super.makeHashAndFeatureValue(f);
				System.out.println("sha1: " + hashFeature);
				return hashFeature;
			}
		};
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);

		server = make(f("F0", -1));
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(1, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(21, synchronizer.getNumRounds());
	}
	
	@Test
	public void testSha1Collision() throws ParseException, IOException, SAXException, ParserConfigurationException  {
		FeatureSha1Mapper old = FeatureSha1.MAPPER;
		try {
			_testSha1Collison();
		} finally {
			FeatureSha1.MAPPER = old;
		}
	}
	private void _testSha1Collison() throws ParseException, IOException, SAXException, ParserConfigurationException  {
		FeatureSha1.MAPPER = new FeatureSha1Mapper() {
			@Override
			public Sha1Value map(Sha1Value old) {
				if ("e76becbcfbe3b847478a3aa706def57382d8b884".equals(old.toString())) {
					return old;
				}
				if ("ac93e7bcebb89813f206d9b618c94e1531cc6a0d".equals(old.toString())) {
					return new Sha1Value("ba5a86a5a1c34f30e7e3a9e8eef485f025c8d605");
				}
				if ("88bfad9cfffeafd299a44d4daf979d57419a2621".equals(old.toString())) {
					return old;
				}
				if ("2a7bc94a06f3221293677515044b0a9dd3960f4e".equals(old.toString())) {
					return new Sha1Value("88bfad9cfffeafd299a44d4daf979d57419a2621");
				}
				throw new IllegalStateException("Mapping of input SHA1's has changed unable to setup test conditions.");
			}
		};
		
//		GeoserverClientSynchronizer.TRACE_POST = System.out;
//		FeatureCollectionSha1Sync.TRACE_RESPONSE = GeoserverClientSynchronizer.TRACE_POST;

		FeatureCollectionType client = make(f("F0", -1), f("F1", 0));
		FeatureCollectionType server = make(f("F0", -1));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE) {
			@Override
			protected HashAndFeatureValue makeHashAndFeatureValue(Feature f) {
				HashAndFeatureValue hashFeature = super.makeHashAndFeatureValue(f);
				System.out.println("sha1: " + hashFeature);
				return hashFeature;
			}
		};
		RoundListener roundListener = mock(RoundListener.class);
		synchronizer.setRoundListener(roundListener);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);

		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		verify(roundListener).sha1Collision();
	}

	@Test
	public void testDeleteOneFeature() throws ParseException, IOException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F0", -1), f("F1", 0), f("F2", 1), f("F2.1", 22), f("F3", 2), f("F4", 3));
		FeatureCollectionType server = make(f("F0", -1), f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		

		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// deleted F2.1
		assertEquals(5, clientMap.size());
		check(clientMap, -1, "F0");
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		check(clientMap, 3, "F4");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
		
		
		server = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		// deleted F0
		assertEquals(4, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		check(clientMap, 3, "F4");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
		
		// delete F4
		server = make(f("F1", 0), f("F2", 1), f("F3", 2));
		builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(3, clientMap.size());
		check(clientMap, 0, "F1");
		check(clientMap, 1, "F2");
		check(clientMap, 2, "F3");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(1, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client0_with_Server1K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make();		
		FeatureCollectionType server = make(makeManyFeatures(1000));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(1000, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 999, "fid.999");
		assertEquals(1000, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(4, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client0_with_Server5K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		int N = 5000;
		FeatureCollectionType client = make();		
		FeatureCollectionType server = make(makeManyFeatures(N));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(N, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 4999, "fid.4999");
		assertEquals(N, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(4, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client0_with_Server10K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make();		
		FeatureCollectionType server = make(makeManyFeatures(10000));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(10000, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 9999, "fid.9999");
		assertEquals(10000, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(5, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client70K_with_Server100K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make(makeManyFeatures(70000));		
		FeatureCollectionType server = make(makeManyFeatures(100000));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(100000, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 99999, "fid.99999");
		assertEquals(30000, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(5, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client100K_with_Server70K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make(makeManyFeatures(100000));		
		FeatureCollectionType server = make(makeManyFeatures(70000));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(70000, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 69999, "fid.69999");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(30000, synchronizer.getNumDeletes());
		assertEquals(5, synchronizer.getNumRounds());
	}
	
	@Test
	public void test_Client10K_with_Server0() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make(makeManyFeatures(10000));		
		FeatureCollectionType server = make();
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(0, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(10000, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}

	@Test
	public void testSameSets_10K() throws IOException, SAXException, ParserConfigurationException, ParseException {
		int N = 10000;
		FeatureCollectionType client = make(makeManyFeatures(N));
		FeatureCollectionType server = make(makeManyFeatures(N));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);
		
		assertEquals(N, clientMap.size());
		check(clientMap, 0, "fid.0");
		check(clientMap, 1, "fid.1");
		check(clientMap, 2, "fid.2");
		check(clientMap, 9999, "fid.9999");
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(0, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(1, synchronizer.getNumRounds());
	}

	@Test
	public void testBulkListenerAdaptor() throws IOException, SAXException, ParserConfigurationException, ParseException {
		FeatureCollectionType client = make(f("U1", 21), f("U2", 22), f("D1", 91), f("D2", 92), f("D3", 93));
		FeatureCollectionType server = make(f("C1", 1), f("C2", 2), f("C3", 3), f("U1", 121), f("U2", 122));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		FeaturesChangedListener target = mock(FeaturesChangedListener.class);
		FeaturesChangedAdaptor adaptor = new FeaturesChangedAdaptor(target);
		synchronizer.setListener(adaptor);
		synchronizer.setRoundListener(adaptor);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		synchronizer.synchronize(clientMap);

		verify(target).featuresCreate(featuresEq(f("C1", 1), f("C2", 2), f("C3", 3)));
		verify(target).featuresUpdate(featuresEq(f("U1", 121), f("U2", 122)));
		verify(target).featuresDelete(featuresEq(f("D1", 91), f("D2", 92), f("D3", 93)));
		
		assertEquals(3, synchronizer.getNumCreates());
		assertEquals(2, synchronizer.getNumUpdates());
		assertEquals(3, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void testAgainstOldServer() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1", 0));
		FeatureCollectionType server = make(f("F1", 1));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		builder.forceVersion(VersionFeatures.VERSION1);
		synchronizer.setRequestBuilder(builder);
		try {
			synchronizer.synchronize(clientMap);
			fail("expected exception");
		} catch (IllegalStateException e) {
			assertTrue(true);
		}
	}
	
	private void check(Map<Identifier, FeatureAccessor> clientMap, int expected, String fid) {
		assertEquals(expected, byFid(clientMap, new FeatureIdImpl(fid)).getProperty("classification").getValue());
	}

	private Feature byFid(Map<Identifier, FeatureAccessor> clientMap, FeatureId fid) {
		FeatureAccessor featureAccessor = clientMap.get(fid);
		if (featureAccessor == null) {
			return null;
		}
		return featureAccessor.getFeature();
	}

	private TestResponse makeGmlResponse() throws FileNotFoundException {
		TestResponse testResponse = new TestResponse();
		testResponse.setResultStream(loadResource("buildings.xml"));
		return testResponse;
	}

	private InputStream loadResource(String name) throws FileNotFoundException {
		InputStream is = getClass().getResourceAsStream(name);
		if (is == null) {
			throw new FileNotFoundException(name);
		}
		return is;
	}
	
	private Feature[] makeManyFeatures(int numberToMake) throws ParseException {
		List<Feature> results = new ArrayList<Feature>(numberToMake);
		for (int i = 0; i < numberToMake; i++) {
			results.add(f("fid." + i, i));
		}
		return results.toArray(new Feature[results.size()]);
	}
	
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, ParseException {
		GeoserverClientSynchronizerIntegrationTest test = new GeoserverClientSynchronizerIntegrationTest();
		for (int i = 0; i < 100; i++) {
			test.test_Client0_with_Server1K();
			test.test_Client0_with_Server5K();
			test.test_Client0_with_Server10K();
		}
		System.out.println("done...");
		System.in.read();
	}

}
