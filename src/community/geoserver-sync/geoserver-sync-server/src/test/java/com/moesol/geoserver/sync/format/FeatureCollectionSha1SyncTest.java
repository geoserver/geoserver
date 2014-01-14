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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.SampleDataAccessData;
import org.geotools.data.SampleDataAccessFactory;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import com.moesol.geoserver.sync.filter.Sha1SyncFilterFunction;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;
import com.moesol.geoserver.sync.samples.Samples;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class FeatureCollectionSha1SyncTest {

	private Samples m_samples = new Samples();
	private ByteArrayOutputStream m_output;
	private FeatureCollectionSha1Sync m_sha1Sync; 

	@Before
	public void setUp() throws Exception {
		System.setProperty("sha1.keep.name", "true");
		
    	Sha1SyncFilterFunction.clearThreadLocals();
    	
		m_output = new ByteArrayOutputStream();
		m_sha1Sync = new FeatureCollectionSha1Sync(m_output);
	}

	@After
	public void tearDown() throws Exception {
		System.clearProperty("sha1.keep.name");
	}

	@Test
	public void testParseAttributeList() {
		assertTrue(Arrays.deepEquals(
				new String[] {"attr1","attr2","attr3","attr4"}, 
				FeatureCollectionSha1Sync.parseAttributes("attr1,attr2 attr3,,,attr4"))); 
		assertTrue(Arrays.deepEquals(
				new String[0],
				FeatureCollectionSha1Sync.parseAttributes(null)));
		assertTrue(Arrays.deepEquals(
				new String[0],
				FeatureCollectionSha1Sync.parseAttributes("")));
	}
	
	@Test
	public void testSimpleFeature() throws ParseException, IOException {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");
		
		m_sha1Sync.setCollection(featureCollectionResponse);
		Sha1SyncJson json = m_sha1Sync.computeZero();
		
		assertEquals("78dc226e7d37ab5c800d8922857452bebea3709b", json.h.get(0).s);
	}
	
	@Test
	public void testSimpleFeatureAll() throws ParseException, IOException {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");
		
		m_sha1Sync.parseAttributesToInclude("-all");
		m_sha1Sync.setCollection(featureCollectionResponse);
		Sha1SyncJson json = m_sha1Sync.computeZero();
		
		assertEquals("f8aff4d2381ad249d96e74b16a4f9184e586fd15", json.h.get(0).s);

	}
	
	@Test
	public void testSimpleFeatureAllOneDiff() throws ParseException, IOException {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("UT");
		
		m_sha1Sync.parseAttributesToInclude("-all");
		m_sha1Sync.setCollection(featureCollectionResponse);
		Sha1SyncJson json = m_sha1Sync.computeZero();

		assertFalse( "5dcee332c8b73888d821b475f8bcd3450e277484".equals(json.h.get(0)) );
	}
	
	@Test
	public void testComplexFeature() throws IOException {
		DataAccess<FeatureType, Feature> dataAccess = DataAccessFinder.getDataStore(SampleDataAccessFactory.PARAMS);
		FeatureSource<FeatureType, Feature> featureSource = dataAccess.getFeatureSource(SampleDataAccessData.MAPPEDFEATURE_TYPE_NAME);
		FeatureCollection<FeatureType, Feature> featureCollection = featureSource.getFeatures();
		int count = 0;
		FeatureIterator<Feature> iterator = featureCollection.features();
		try {
			while (iterator.hasNext()) {
				Feature feature = iterator.next();
				// System.out.println(feature);
				m_sha1Sync.sha1One(feature);
				count++;
			}
		} finally {
			iterator.close();
		}
	}
	
	@Test
    public void testCompute_Level0() throws Exception {
		FeatureCollectionType server = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		FeatureCollectionResponse rserver = FeatureCollectionResponse.adapt(server);
		m_sha1Sync.setCollection(rserver);

		m_sha1Sync.parseSha1SyncJson("{l:0,h:[{p:'',s:'deadbeef'}]}"); // no matches
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(1, sync.level());
        assertEquals("result: " + sync, 4, sync.hashes().size());
        check(sync, 1, "99", "8870ef04dbc5c827b87c34bf582f39442a78bbd9");
        check(sync, 0, "88", "7ed2eeddc197ed77968204c8668ffc142b3f8878");
        check(sync, 2, "be", "755774c7760c6184f154b25e88f81712f65bc712");
        check(sync, 3, "d0", "55fb0480834cfd25152000bc6543d69f46ec3235");
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level0_Match() throws Exception {
		FeatureCollectionType server = make(f("F1", 0), f("F2", 1), f("F3", 2), f("F4", 3));
		FeatureCollectionResponse rserver = FeatureCollectionResponse.adapt(server);
		m_sha1Sync.setCollection(rserver);

		m_sha1Sync.parseSha1SyncJson("{l:0,h:[{p:'',s:'949ff963844796006fdfcab0a003b2d48f708771'}]}"); // no matches
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(1, sync.level());
        assertEquals("result: " + sync, 0, sync.hashes().size());
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level1() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");
		m_sha1Sync.setCollection(featureCollectionResponse);

		Sha1SyncJson client = new Sha1SyncJson().level(1).hashes(
				new Sha1SyncPositionHash().position("56").summary("deadbeef"),
				new Sha1SyncPositionHash().position("ed").summary("deadbeef")
		);
		m_sha1Sync.parseSha1SyncJson(client.toString());
		
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(2, sync.level());
        assertEquals("result: " + sync, 2, sync.hashes().size());
        check(sync, 0, "56e8", "23130043fa5b0d0aa126d4cab1e4553617bf7276");
        check(sync, 1, "ed3c", "279bd935a280bd6f5f5ff49fe17fc1feab25fb3a");
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level1_Partial() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");
		m_sha1Sync.setCollection(featureCollectionResponse);
		
		Sha1SyncJson client = new Sha1SyncJson().level(1).hashes(
				new Sha1SyncPositionHash().position("ed").summary("deadbeef")
		);
		m_sha1Sync.parseSha1SyncJson(client.toString());
		
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(2, sync.level());
        assertEquals("result: " + sync, 1, sync.hashes().size());
        check(sync, 0, "ed3c", "279bd935a280bd6f5f5ff49fe17fc1feab25fb3a");
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level1_Partial_Extra() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");
		m_sha1Sync.setCollection(featureCollectionResponse);
		Sha1SyncJson client = new Sha1SyncJson().level(1).hashes(
				new Sha1SyncPositionHash().position("ed").summary("deadbeef"),
				new Sha1SyncPositionHash().position("ff").summary("deadbeef")
		);
		m_sha1Sync.parseSha1SyncJson(client.toString());
		
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(2, sync.level());
        assertEquals("result: " + sync, 2, sync.hashes().size());
        check(sync, 0, "ed3c", "279bd935a280bd6f5f5ff49fe17fc1feab25fb3a");
        check(sync, 1, "ff", null);
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level1_Empty() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");

		// An empty client should produce 256 empty hashes
		List<Sha1SyncPositionHash> h = new ArrayList<Sha1SyncPositionHash>();
		for (int i = 0; i < 256; i++) {
			h.add(new Sha1SyncPositionHash().position(String.format("%02x", i)));
			
		}
		m_sha1Sync.setCollection(featureCollectionResponse);
		Sha1SyncJson client = new Sha1SyncJson().level(1).hashes(h);
		m_sha1Sync.parseSha1SyncJson(client.toString());
		
		Sha1SyncJson sync = m_sha1Sync.compute();
		System.out.println("send: " + client.toString().length());
		System.out.println("recv: " + sync.toString().length());
		
        assertEquals(2, sync.level());
        assertEquals("result: " + sync, 256, sync.hashes().size());
        for (int i = 0; i < 256; i++) {
        	switch (i) {
        	case 86:
                check(sync, i, "56e8", "23130043fa5b0d0aa126d4cab1e4553617bf7276");
                break;
        	case 237:
                check(sync, i, "ed3c", "279bd935a280bd6f5f5ff49fe17fc1feab25fb3a");
        		break;
        	default:
        		check(sync, i, String.format("%02x", i), null);
        	}
        }
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level2() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");

		m_sha1Sync.setCollection(featureCollectionResponse);
		m_sha1Sync.parseSha1SyncJson("{l:2,h:[{p:'56e8'},{p:'ed3c'}]}");
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(3, sync.level());
        assertEquals("result: " + sync, 2, sync.hashes().size());
        check(sync, 0, "56e82e", "23130043fa5b0d0aa126d4cab1e4553617bf7276");
        check(sync, 1, "ed3c37", "279bd935a280bd6f5f5ff49fe17fc1feab25fb3a");
        assertEquals(1L, sync.max());
    }

	@Test
    public void testComput_Level2_Partial() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");

		m_sha1Sync.setCollection(featureCollectionResponse);
		m_sha1Sync.parseSha1SyncJson("{l:2,h:[{p:'56e8'}]}");
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(3, sync.level());
        assertEquals("result: " + sync, 1, sync.hashes().size());
        check(sync, 0, "56e82e", "23130043fa5b0d0aa126d4cab1e4553617bf7276");
        assertEquals(1L, sync.max());
    }
    
	@Test
    public void testComput_Level2_Empty() throws Exception {
		FeatureCollectionResponse featureCollectionResponse = buildSomeFeatures("US");

		m_sha1Sync.setCollection(featureCollectionResponse);
		m_sha1Sync.parseSha1SyncJson("{l:2,h:[]}");
		Sha1SyncJson sync = m_sha1Sync.compute();
		
        assertEquals(3, sync.level());
        assertEquals("result: " + sync, 0, sync.hashes().size());
        assertEquals(1L, sync.max());
    }
    
    private void check(Sha1SyncJson sync, int i, String position, String summary) {
        assertEquals("at " + i, position, sync.hashes().get(i).position());
        assertEquals("at " + i, summary,  sync.hashes().get(i).summary());
    }
	
	/**
	 * Stress test...
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws ParseException, IOException {
		FeatureCollectionSha1SyncTest t = new FeatureCollectionSha1SyncTest();
		t.buildSomeFeatures("US");
		
	}
	private FeatureCollectionResponse buildSomeFeatures(String firstFlag) throws IOException, ParseException{
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(m_samples.getFlagType());
		builder.add(firstFlag);
		builder.add(1);
		builder.add(20.5);
		builder.add(makePoint("POINT(1 2)"));
		SimpleFeature f1 = builder.buildFeature("fid-0001");
		
		builder.add("JA");
		builder.add(2);
		builder.add(20.5);
		builder.add(makePoint("POINT (1 2)"));
		SimpleFeature f2 = builder.buildFeature("fid-0002");

        MemoryDataStore data = new MemoryDataStore();
        
        data.createSchema(m_samples.getFlagType());
        
        data.addFeature(f1);
        data.addFeature(f2);
        FeatureSource<SimpleFeatureType, SimpleFeature> fs = data.getFeatureSource(m_samples.getFlagType().getName());
        
        
        
        
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        addFeaturesFromSource(fct, fs);
        FeatureCollectionResponse fcr = FeatureCollectionResponse.adapt(fct);
		return fcr;		
	}

	@SuppressWarnings("unchecked")
	private void addFeaturesFromSource(FeatureCollectionType fct, FeatureSource<SimpleFeatureType, SimpleFeature> fs) throws IOException {
		fct.getFeature().add(fs.getFeatures());
	}

	private Point makePoint(String wellKnownText) throws ParseException {
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );

		WKTReader reader = new WKTReader( geometryFactory );
		Point point = (Point) reader.read(wellKnownText);
		return point;
	}

	private Feature f(String fid, int classification) throws ParseException {
		return m_samples.buildSimpleFeature(fid, classification);
	}

	private FeatureCollectionType make(Feature... features) throws IOException {
        MemoryDataStore data = new MemoryDataStore();
        data.createSchema(m_samples.getFlagType());

        for (Feature f : features) {
        	data.addFeature((SimpleFeature) f);
        }
        FeatureSource<SimpleFeatureType, SimpleFeature> fs = data.getFeatureSource(m_samples.getFlagType().getName());
        
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
		addFeatureToCollection(fct, fs);
		return fct;
	}

	@SuppressWarnings("unchecked")
	private void addFeatureToCollection(FeatureCollectionType fct,
			FeatureSource<SimpleFeatureType, SimpleFeature> fs)
			throws IOException {
		fct.getFeature().add(fs.getFeatures());
	}
}
