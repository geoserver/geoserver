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

import java.io.IOException;
import java.util.HashMap;
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
import com.moesol.geoserver.sync.client.RequestBuilder;
import com.vividsolutions.jts.io.ParseException;

import static org.junit.Assert.*;

public class ClientSynchronizerStressIntegrationTest extends WFSTestSupport {

	@Test
	public void test_Server6_RunMany() throws IOException, SAXException, ParserConfigurationException, ParseException {
//		GeoserverClientSynchronizer.TRACE_POST = new PrintStream(new FileOutputStream("run.log"));
//		FeatureCollectionSha1Sync.TRACE_RESPONSE = GeoserverClientSynchronizer.TRACE_POST;
		Feature[] serverFeatures = new Feature[] {
			f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3), f("F5", 4), f("F6", 5)
		};
		
		FeatureCollectionType client = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3), f("F5", 4), f("F6", 5));
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		RecordingFeatureChangeListener listener = new RecordingFeatureChangeListener(synchronizer.getListener());
		synchronizer.setListener(listener);
		
		long lastOutput = 0;
		for (int i = 0; i < 10000; i++) {
			Map<Identifier, FeatureAccessor> oldClientMap = new HashMap<Identifier, FeatureAccessor>(clientMap);
			UpdateRecords changed = randomlyChangeFeatures(serverFeatures);
			listener.reset();
			FeatureCollectionType server = make(serverFeatures);
			
			RequestBuilder builder = new SimulatedRequestBuilder(server);
			synchronizer.setRequestBuilder(builder);
			
			synchronizer.synchronize(clientMap);
			
			if (System.currentTimeMillis() - lastOutput > 1000) {
				System.out.println("desc: " + asString(clientMap) + " n: " + synchronizer.getNumUpdates() + " i: " + i);
				lastOutput = System.currentTimeMillis();
			}
			assertEquals(6, clientMap.size());
			
			assertEquals("run: " + i, 0, synchronizer.getNumCreates());
			assertEquals("run: " + i, 0, synchronizer.getNumDeletes());
			if (changed.getFeaturesUpdated().size() != 4) {
				continue;
			}
			if (changed.getFeaturesUpdated().size() != synchronizer.getNumUpdates()) {
				System.out.println("run: " + i);
				System.out.println("changed: " + changed);
				System.out.println("old: " + asString(oldClientMap));
				System.out.println("new: " + asString(clientMap));
				System.out.println("listener: " + asString(listener));
				
				assertEquals("run: " + i + " changed: " + changed + " listener: " + asString(listener), 
						changed.getFeaturesUpdated().size(), synchronizer.getNumUpdates());
			}
		}
	}
	
	@Test
	public void testExtraUpdate1() throws IOException, ParseException, SAXException, ParserConfigurationException {
		FeatureCollectionType client = make(f("F1",162),f("F5",167),f("F4",169),f("F3",168),f("F2",157),f("F6",164));
		FeatureCollectionType server = make(f("F1",162),f("F5",167),f("F4",169),f("F3",170),f("F2",157),f("F6",164));
		
		GeoserverClientSynchronizer synchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "url", SimulatedRequestBuilder.POST_TEMPLATE);
		Map<Identifier, FeatureAccessor> clientMap = asMap(client);
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
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
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
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
		
		RequestBuilder builder = new SimulatedRequestBuilder(server);
		synchronizer.setRequestBuilder(builder);
		synchronizer.synchronize(clientMap);

		assertEquals(6, clientMap.size());
		assertEquals(0, synchronizer.getNumCreates());
		assertEquals(3, synchronizer.getNumUpdates());
		assertEquals(0, synchronizer.getNumDeletes());
		assertEquals(2, synchronizer.getNumRounds());
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
		ClientSynchronizerStressIntegrationTest test = new ClientSynchronizerStressIntegrationTest();
		for (int i = 0; i < 100; i++) {
			test.test_Server6_RunMany();
		}
		System.out.println("done...");
		System.in.read();
	}

}
