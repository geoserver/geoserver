/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

public class RetypingDataStoreTest {

    static final String RENAMED = "houses";

    RetypingDataStore rts;

    private File data;

    private Id fidFilter;

    private String fid;

    @Before
    public void setUp() throws Exception {
        data = File.createTempFile("retype", "data", new File("./target"));
        data.delete();
        data.mkdir();

        final String fileName = MockData.BUILDINGS.getLocalPart() + ".properties";
        URL properties = MockData.class.getResource(fileName);
        IOUtils.copy(properties.openStream(), new File(data, fileName));

        PropertyDataStore pds = new PropertyDataStore(data);
        rts =
                new RetypingDataStore(pds) {
                    @Override
                    protected String transformFeatureTypeName(String originalName) {
                        if (originalName.equals(MockData.BUILDINGS.getLocalPart())) return RENAMED;
                        else return super.transformFeatureTypeName(originalName);
                    }
                };

        // build a filter that will retrieve one feature only
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        fid = RENAMED + ".1107531701011";
        fidFilter = ff.id(Collections.singleton(ff.featureId(fid)));
    }

    @After
    public void tearDown() throws Exception {
        IOUtils.delete(data);
    }

    @Test
    public void testLookupFeatureType() throws Exception {
        try {
            rts.getSchema(MockData.BUILDINGS.getLocalPart());
            fail("The original type name should not be visible");
        } catch (IOException e) {
            // cool, as expected
        }

        final SimpleFeatureType schema = rts.getSchema(RENAMED);
        assertNotNull(schema);
        assertEquals(RENAMED, schema.getName().getLocalPart());
    }

    @Test
    public void testGetFeaturesFeatureSource() throws Exception {
        // check the schemas in feature source and feature collection
        SimpleFeatureSource fs = rts.getFeatureSource(RENAMED);
        assertEquals(RENAMED, fs.getSchema().getName().getLocalPart());
        SimpleFeatureCollection fc = fs.getFeatures();
        assertEquals(RENAMED, fc.getSchema().getName().getLocalPart());
        assertTrue(fc.size() > 0);

        // make sure the feature schema is good as well
        FeatureIterator<SimpleFeature> it = fc.features();
        SimpleFeature sf = it.next();
        it.close();

        assertEquals(RENAMED, sf.getFeatureType().getName().getLocalPart());

        // check the feature ids have been renamed as well
        assertTrue(
                "Feature id has not been renamed, it's still " + sf.getID(),
                sf.getID().startsWith(RENAMED));
    }

    @Test
    public void testGetFeaturesReader() throws Exception {
        FeatureReader<SimpleFeatureType, SimpleFeature> fr;
        fr = rts.getFeatureReader(new Query(RENAMED), Transaction.AUTO_COMMIT);
        SimpleFeature sf = fr.next();
        fr.close();

        assertEquals(RENAMED, sf.getFeatureType().getName().getLocalPart());

        // check the feature ids have been renamed as well
        assertTrue(
                "Feature id has not been renamed, it's still " + sf.getID(),
                sf.getID().startsWith(RENAMED));
    }

    @Test
    public void testFeautureSourceFidFilter() throws Exception {
        // grab the last feature in the collection (there are more than one)
        SimpleFeatureSource fs = rts.getFeatureSource(RENAMED);

        // build a filter that will retrieve that feature only
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        final String fid = RENAMED + ".1107531701011";
        Filter fidFilter = ff.id(Collections.singleton(ff.featureId(fid)));

        SimpleFeatureCollection fc = fs.getFeatures(new Query(RENAMED, fidFilter));
        assertEquals(RENAMED, fc.getSchema().getName().getLocalPart());
        assertEquals(1, fc.size());
        FeatureIterator<SimpleFeature> it = fc.features();
        assertTrue(it.hasNext());
        SimpleFeature sf = it.next();
        assertFalse(it.hasNext());
        it.close();
        assertEquals(fid, sf.getID());
    }

    @Test
    public void testFeautureReaderFidFilter() throws Exception {
        FeatureReader<SimpleFeatureType, SimpleFeature> fr;
        fr = rts.getFeatureReader(new Query(RENAMED, fidFilter), Transaction.AUTO_COMMIT);
        assertEquals(RENAMED, fr.getFeatureType().getName().getLocalPart());
        assertTrue(fr.hasNext());
        SimpleFeature sf = fr.next();
        assertFalse(fr.hasNext());
        fr.close();
        assertEquals(fid, sf.getID());
    }

    @Test
    public void testDelete() throws Exception {
        final Query queryAll = new Query(RENAMED);

        SimpleFeatureStore store;
        store = (SimpleFeatureStore) rts.getFeatureSource(RENAMED);
        int count = store.getCount(queryAll);
        store.removeFeatures(fidFilter);

        assertEquals(count - 1, store.getCount(queryAll));
    }

    @Test
    public void testModify() throws Exception {
        final Query queryAll = new Query(RENAMED);

        SimpleFeatureStore store;
        store = (SimpleFeatureStore) rts.getFeatureSource(RENAMED);
        SimpleFeature original = store.getFeatures(fidFilter).features().next();
        String newAddress = ((String) original.getAttribute("ADDRESS")) + " xxx";

        store.modifyFeatures(new NameImpl("ADDRESS"), newAddress, fidFilter);
        SimpleFeature modified = store.getFeatures(fidFilter).features().next();
        assertEquals(newAddress, modified.getAttribute("ADDRESS"));
    }

    /**
     * This test is made with mock objects because the property data store does not generate fids in
     * the <type>.<id> form
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAppend() throws Exception {
        SimpleFeatureType type =
                DataUtilities.createType("trees", "the_geom:Point,FID:String,NAME:String");

        SimpleFeatureStore fs = createMock(SimpleFeatureStore.class);
        expect(fs.addFeatures(isA(FeatureCollection.class)))
                .andReturn(Collections.singletonList((FeatureId) (new FeatureIdImpl("trees.105"))));
        replay(fs);

        DataStore ds = createMock(DataStore.class);
        expect(ds.getTypeNames()).andReturn(new String[] {"trees"}).anyTimes();
        expect(ds.getSchema("trees")).andReturn(type).anyTimes();
        expect(ds.getFeatureSource("trees")).andReturn(fs);
        replay(ds);

        RetypingDataStore rts =
                new RetypingDataStore(ds) {
                    @Override
                    protected String transformFeatureTypeName(String originalName) {
                        return "oaks";
                    }
                };

        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(type);
        WKTReader reader = new WKTReader();
        sfb.set("the_geom", reader.read("POINT (0.002 0.0008)"));
        sfb.set("FID", "023");
        sfb.set("NAME", "Old oak");
        SimpleFeature feature = sfb.buildFeature(null);
        SimpleFeatureCollection fc = DataUtilities.collection(feature);

        SimpleFeatureStore store = (SimpleFeatureStore) rts.getFeatureSource("oaks");
        List<FeatureId> ids = store.addFeatures(fc);
        assertEquals(1, ids.size());
        String id = ((FeatureId) ids.iterator().next()).getID();
        assertTrue("Id does not start with " + "oaks" + " it's " + id, id.startsWith("oaks"));
    }

    @Test
    public void testLockUnlockFilter() throws Exception {
        SimpleFeatureLocking fl;
        fl = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        final FeatureLock lock = new FeatureLock("abc", 10 * 60 * 1000);
        Transaction t = new DefaultTransaction();
        t.addAuthorization(lock.getAuthorization());
        fl.setTransaction(t);
        fl.setFeatureLock(lock);

        SimpleFeatureLocking fl2;
        fl2 = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        fl.setFeatureLock(lock);
        fl2.setTransaction(new DefaultTransaction());

        assertEquals(1, fl.lockFeatures(fidFilter));
        assertEquals(0, fl2.lockFeatures(fidFilter));

        fl.unLockFeatures(fidFilter);
        assertEquals(1, fl2.lockFeatures(fidFilter));
    }

    @Test
    public void testLockUnlockQuery() throws Exception {
        SimpleFeatureLocking fl;
        fl = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        final FeatureLock lock = new FeatureLock("abc", 10 * 60 * 1000);
        Transaction t = new DefaultTransaction();
        t.addAuthorization(lock.getAuthorization());
        fl.setTransaction(t);
        fl.setFeatureLock(lock);

        SimpleFeatureLocking fl2;
        fl2 = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        fl.setFeatureLock(lock);
        fl2.setTransaction(new DefaultTransaction());

        Query q = new Query(RENAMED, fidFilter);
        assertEquals(1, fl.lockFeatures(q));
        assertEquals(0, fl2.lockFeatures(q));

        fl.unLockFeatures(q);
        assertEquals(1, fl2.lockFeatures(q));
    }

    @Test
    public void testQueryWithPropertyNames() throws Exception {
        // check the schemas in feature source and feature collection
        SimpleFeatureSource fs = rts.getFeatureSource(RENAMED);
        Query q = new Query(RENAMED, Filter.INCLUDE, new String[] {"ADDRESS"});
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = fs.getFeatures(q);
        assertEquals(1, fc.getSchema().getAttributeCount());

        // make sure the feature schema is good as well
        FeatureIterator<SimpleFeature> it = fc.features();
        SimpleFeature sf = it.next();
        it.close();

        assertEquals(1, sf.getAttributeCount());
        assertNull(sf.getAttribute("FID"));
        assertNotNull(sf.getAttribute("ADDRESS"));
    }
}
