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




import static com.moesol.geoserver.sync.client.Features.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.filter.identity.Identifier;
import org.xml.sax.SAXException;

import com.moesol.geoserver.sync.client.FeatureAccessor;
import com.moesol.geoserver.sync.client.GeoserverClientSynchronizer;
import com.moesol.geoserver.sync.client.RoundListener;
import com.vividsolutions.jts.io.ParseException;

import static org.junit.Assert.*;

/**
 * Inject changes on the server side to attempt to induce extra deletes being detected on client side.
 */
public class MidRoundServerChangesIntegrationTest extends WFSTestSupport {
	
	@Test
	public void test_Server6_RunManyUpdates() throws IOException, SAXException, ParserConfigurationException, ParseException {
//		GeoserverClientSynchronizer.TRACE_POST = new PrintStream(new FileOutputStream("run.log"));
//		FeatureCollectionSha1Sync.TRACE_RESPONSE = GeoserverClientSynchronizer.TRACE_POST;
		final Feature[] serverFeatures = new Feature[] {
			f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3), f("F5", 4), f("F6", 5)
		};
		
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3), f("F5", 4), f("F6", 5));
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		final List<Map<Identifier, FeatureAccessor>> changes = new ArrayList<Map<Identifier,FeatureAccessor>>();
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		RecordingFeatureChangeListener listener = new RecordingFeatureChangeListener(synchronizer.getListener());
		synchronizer.setListener(listener);
		
		long lastOutput = 0;
		for (int i = 0; i < 10000; i++) {
			Map<Identifier, FeatureAccessor> oldClientMap = new HashMap<Identifier, FeatureAccessor>(clientMap);
			UpdateRecords changed = randomlyChangeFeatures(serverFeatures);
			listener.reset();
			FeatureCollectionType server = make(serverFeatures);
			changes.clear();
			changes.add(asMap(server));
			
			final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
			synchronizer.setRoundListener(new RoundListener() {
				@Override
				public void beforeRound(int r) { }
				@Override
				public void afterRound(int r) {
					// 50/50 chance of change for each round
					if (random.nextBoolean()) { return; }
					try {
						randomlyChangeFeatures(serverFeatures);
						FeatureCollectionType changedFeatures = make(serverFeatures);
						changes.add(asMap(changedFeatures));
						builder.setServer(changedFeatures);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				@Override
				public void afterSynchronize() { }
				@Override
				public void sha1Collision() { }
			});
			synchronizer.setRequestBuilder(builder);
			
			synchronizer.synchronize(clientMap);
			
			if (System.currentTimeMillis() - lastOutput > 1000) {
				System.out.println("desc: " + asString(clientMap) + " n: " + synchronizer.getNumUpdates() + " i: " + i);
				lastOutput = System.currentTimeMillis();
			}
			if (clientMap.size() != 6) {
				System.out.println("run: " + i);
				System.out.println("changed: " + changed);
				System.out.println("old: " + asString(oldClientMap));
				for (int j = 0; j < changes.size(); j++) {
					System.out.printf("chg[%d]=%s", j, asString(changes.get(j))); 
				}
				System.out.println("new: " + asString(clientMap));
				System.out.println("deleted: " + synchronizer.getNumDeletes());
				System.out.println("listener: " + asString(listener));
				
				assertEquals("run: " + i + " changed: " + changed + " listener: " + asString(listener), 
						changed.getFeaturesUpdated().size(), synchronizer.getNumUpdates());
			}
			assertEquals(6, clientMap.size());
			
			assertEquals("run: " + i, 0, synchronizer.getNumCreates());
			assertEquals("run: " + i, 0, synchronizer.getNumDeletes());
			if (changed.getFeaturesUpdated().size() != 4) {
				continue;
			}
			// Some extra changes might get picked up, but no fewer.
			if (synchronizer.getNumUpdates() < changed.getFeaturesUpdated().size()) {
				fail("to few updates detected");
			}
		}
	}
	
//	public void testSimulatedUpdates() throws ParseException {
//		SimulationEngine sim = new SimulationEngine(10);
//		for (int i = 0; i < 1000; i++) {
//			sim.makeSomeChanges();
//			System.out.println(String.format("i[%d] %s", i, asString(Arrays.asList(sim.getFeatures()))));
//		}
//	}
	
	@Test
	public void test_Server6_RandomChanges() throws IOException, SAXException, ParserConfigurationException, ParseException {
//		GeoserverClientSynchronizer.TRACE_POST = new PrintStream(new FileOutputStream("run.log"));
//		FeatureCollectionSha1Sync.TRACE_RESPONSE = GeoserverClientSynchronizer.TRACE_POST;
		final SimulationEngine engine = new SimulationEngine(1000);
		FeatureCollectionType client = make();
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		RecordingFeatureChangeListener listener = new RecordingFeatureChangeListener(synchronizer.getListener());
		synchronizer.setListener(listener);
		
		System.out.println(",ec,sc,eu,su,ed,sd,e#,s#"); 
		long lastOutput = 0;
		for (int i = 0; i < 1000; i++) {
			engine.makeSomeChanges();
			listener.reset();
			FeatureCollectionType server = make(engine.getFeatures());
			
			final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
			synchronizer.setRoundListener(new RoundListener() {
				@Override
				public void beforeRound(int r) { }
				@Override
				public void afterRound(int r) {
					// 50/50 chance of change for each round
					if (random.nextBoolean()) { return; }
					try {
						engine.makeSomeChanges();
						builder.setServer(make(engine.getFeatures()));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				@Override
				public void afterSynchronize() { }
				@Override
				public void sha1Collision() { }
			});
			synchronizer.setRequestBuilder(builder);
			synchronizer.synchronize(clientMap);
			
			if (System.currentTimeMillis() - lastOutput > 1000) {
				System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d,%d%n", 
						i, engine.getNumCreated(), synchronizer.getNumCreates(), 
						engine.getNumUpdated(), synchronizer.getNumUpdates(),
						engine.getNumDeleted(), synchronizer.getNumDeletes(),
						engine.getFeatures().length, clientMap.size());
				lastOutput = System.currentTimeMillis();
			}
			engine.resetCounts();
		}
		
		synchronizer.setRoundListener(mock(RoundListener.class));
		synchronizer.synchronize(clientMap);
		assertFeaturesEq(Arrays.asList(engine.getFeatures()), clientMap);
	}
	
	@Test
	public void testExtraUpdate1() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",162),f("F5",167),f("F4",169),f("F3",168),f("F2",157),f("F6",164));
		FeatureCollectionType server = make(f("F1",162),f("F5",167),f("F4",169),f("F3",170),f("F2",157),f("F6",164));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("F1",171),f("F5",167),f("F4",169),f("F3",172),f("F2",157),f("F6",164)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(1, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}

	@Test
	public void testExtraUpdate2() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",39),f("F5",35),f("F4",33),f("F3",38),f("F2",36),f("F6",37));
		FeatureCollectionType server = make(f("F1",39),f("F5",40),f("F4",33),f("F3",41),f("F2",36),f("F6",37));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("F1",39),f("F5",140),f("F4",33),f("F3",141),f("F2",36),f("F6",37)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(2, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}

	@Test
	public void testExtraUpdate3() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",59),f("F5",60),f("F4",57),f("F3",58),f("F2",56),f("F6",61));
		FeatureCollectionType server = make(f("F1",63),f("F5",60),f("F4",64),f("F3",58),f("F2",56),f("F6",62));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("F1",163),f("F5",60),f("F4",164),f("F3",58),f("F2",56),f("F6",162)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(3, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}

	@Test
	public void testExtraUpdate3_6() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",59),f("F5",60),f("F4",57),f("F3",58),f("F2",56),f("F6",61));
		FeatureCollectionType server = make(f("F1",63),f("F5",60),f("F4",64),f("F3",58),f("F2",56),f("F6",62));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("F1",163),f("F5",160),f("F4",164),f("F3",158),f("F2",156),f("F6",162)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(3, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void testExtraUpdateF1() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",24203),f("F5",24206),f("F4",24208),f("F3",24207),f("F2",24209),f("F6",24201));
		FeatureCollectionType server = make(f("F1",24214),f("F5",24211),f("F4",24210),f("F3",24215),f("F2",24212),f("F6",24213));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("F1",24218),f("F5",24219),f("F4",24210),f("F3",24217),f("F2",24216),f("F6",24213)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(6, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
	}
	
	@Test
	public void testAddUpdateDeleteDuringRounds() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("U1",100),f("U2",200),f("U3",300),f("U4",400));
		FeatureCollectionType server = make(f("U1",101),f("U2",201),f("U3",301),f("U4",401));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		final SimulatedRequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.setRoundListener(new RoundListener() {
			@Override
			public void beforeRound(int r) { }
			@Override
			public void afterRound(int r) { 
				// After round one simulate a change on the server
				if (r != 1) { return; }
				try {
					builder.setServer(make(f("U1",102),f("U2",202),f("U4",402),f("C1", 502)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			@Override
			public void afterSynchronize() { }
			@Override
			public void sha1Collision() { }
		});
		synchronizer.synchronize(clientMap);

		assertEquals(4, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(4, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
		
		assertFeaturesEq(Arrays.asList(f("U1",101),f("U2",201),f("U3",301),f("U4",401)), clientMap);
		
		synchronizer.synchronize(clientMap);
		assertFeaturesEq(Arrays.asList(f("U1",102),f("U2",202),f("U4",402),f("C1", 502)), clientMap);
	}
	
	private void assertFeaturesEq(List<Feature> expected, Map<Identifier, FeatureAccessor> clientMap) {
		for (FeatureAccessor fa : clientMap.values()) {
			Feature f = fa.getFeature();
			assertTrue("exp: " + asString(expected) + " extra: " + asString(f), expected.contains(f));
		}
		for (Feature f : expected) {
			assertTrue("missing: " + asString(f) + " in " + asString(clientMap), clientMap.containsKey(f.getIdentifier()));
		}
	}

	private Random random = new Random();
	private int nextVersion = 6;
	private UpdateRecords randomlyChangeFeatures(Feature[] serverFeatures) throws ParseException {
		UpdateRecords result = new UpdateRecords();
		int numToChange = random.nextInt(serverFeatures.length) + 1;
//		int numToChange = 6;
		boolean alreadyChanged[] = new boolean[serverFeatures.length];
		int numChanged = 0;
		while (numChanged < numToChange) {
			int indexToChange = random.nextInt(serverFeatures.length);
			if (alreadyChanged[indexToChange]) {
				continue;
			}
			
			Feature feature = f("F" + (indexToChange + 1), nextVersion++);
			alreadyChanged[indexToChange] = true;
			serverFeatures[indexToChange] = feature;
			result.getFeaturesUpdated().add(asString(feature));
			numChanged++;
		}
		return result;
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, ParseException {
		MidRoundServerChangesIntegrationTest test = new MidRoundServerChangesIntegrationTest();
		for (int i = 0; i < 100; i++) {
			test.test_Server6_RunManyUpdates();
		}
		System.out.println("done...");
		System.in.read();
	}

}
