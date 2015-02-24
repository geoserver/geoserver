/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static java.util.Collections.*;
import static org.easymock.EasyMock.*;
import static org.geoserver.gss.GSSCore.*;
import static org.geotools.data.DataUtilities.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.WfsFactory;

import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.LiveDbmsData;
import org.geoserver.data.test.TestData;
import org.geoserver.gss.GSSInfo.GSSMode;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

/**
 * Tests the central manager
 */
public class SynchronizationManagerTest extends GeoServerAbstractTestSupport {

    static XpathEngine xpath;

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    DefaultGeoServerSynchronizationService gss;

    VersioningDataStore synchStore;

    SynchronizationManager synch;

    FeatureStore<SimpleFeatureType, SimpleFeature> fsUnits;

    FeatureStore<SimpleFeatureType, SimpleFeature> fsUnitTables;

    @Override
    public TestData buildTestData() throws Exception {
        File base = new File("./src/test/resources/");
        LiveDbmsData data = new LiveDbmsData(new File(base, "data_dir"), "unit", new File(base,
                "unit.sql"));
        List<String> filteredPaths = data.getFilteredPaths();
        filteredPaths.clear();
        filteredPaths.add("workspaces/topp/synch/datastore.xml");
        return data;
    }

    @Override
    protected void setUpInternal() throws Exception {
        // configure the GSS service
        GeoServer gs = getGeoServer();
        GSSInfo gssInfo = gs.getService(GSSInfo.class);
        gssInfo.setMode(GSSMode.Central);
        gssInfo.setVersioningDataStore(getCatalog().getDataStoreByName("synch"));
        gs.save(gssInfo);

        // initialize the GSS service
        Map gssBeans = applicationContext
                .getBeansOfType(DefaultGeoServerSynchronizationService.class);
        gss = (DefaultGeoServerSynchronizationService) gssBeans.values().iterator().next();
        gss.core.ensureCentralEnabled();

        // grab the synch manager
        synch = (SynchronizationManager) applicationContext.getBeansOfType(
                SynchronizationManager.class).values().iterator().next();

        // disable automated scheduling, we control how does what here
        Timer timer = (Timer) applicationContext.getBean("gssTimerFactory");
        timer.cancel();

        // make some tables synchronised
        synchStore = (VersioningDataStore) getCatalog().getDataStoreByName("synch").getDataStore(
                null);
        FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) synchStore
                .getFeatureSource(SYNCH_TABLES);
        long restrectedId = addFeature(fs, "restricted", "2");
        long roadsId = addFeature(fs, "roads", "2");
        synchStore.setVersioned("restricted", true, null, null);
        synchStore.setVersioned("roads", true, null, null);

        // add some units
        fsUnits = (FeatureStore<SimpleFeatureType, SimpleFeature>) synchStore
                .getFeatureSource(SYNCH_UNITS);
        long mangoId = addFeature(fsUnits, "unit1", "http://localhost:8081/geoserver/ows",
                null, null, null, null, 60, 10, false);

        // link units and tables
        fsUnitTables = (FeatureStore<SimpleFeatureType, SimpleFeature>) synchStore
                .getFeatureSource(SYNCH_UNIT_TABLES);
        addFeature(fsUnitTables, mangoId, restrectedId, null, null, null, null);
        // addFeature(fsUnitTables, mangoId, roadsId, null, null, null, null);
    }

    /**
     * Utility method to add a feature in a store and get back the generated id (which is supposed
     * to be a integer/long number)
     * 
     * @param fs
     * @param attributes
     * @return
     * @throws IOException
     */
    long addFeature(FeatureStore<SimpleFeatureType, SimpleFeature> fs, Object... attributes)
            throws IOException {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(fs.getSchema());
        List<FeatureId> ids = fs.addFeatures(collection((fb.buildFeature(null, attributes))));
        String id = ids.get(0).getID();
        return Long.parseLong(id.substring(fs.getSchema().getTypeName().length() + 1));
    }
    
    SimpleFeature getSingleFeature(FeatureSource<SimpleFeatureType, SimpleFeature> fs, Filter f) throws IOException {
        FeatureIterator<SimpleFeature> fi = null;
        try {
            fi = fs.getFeatures(f).features();
            return fi.next();
        } finally {
            if(fi != null) {
                fi.close();
            }
        }
    }

    public void testConnectionFailure() throws Exception {
        // create mock objects that will simulate a connection failure
        GSSClient client = createMock(GSSClient.class);
        expect(client.getCentralRevision((QName) anyObject())).andThrow(
                new IOException("Host unreachable"));
        replay(client);
        GSSClientFactory factory = createMock(GSSClientFactory.class);
        expect(factory.createClient(new URL("http://localhost:8081/geoserver/ows"), null, null))
                .andReturn(client);
        replay(factory);

        synch.clientFactory = factory;

        // perform synch
        Date start = new Date();
        synch.synchronizeOustandlingLayers();
        Date end = new Date();

        // check we stored the last failure marker
        SimpleFeature f = getSingleFeature(fsUnitTables, ff.equal(ff.property("table_id"), ff.literal(1), false));
        Date lastFailure = (Date) f.getAttribute("last_failure");
        assertNotNull(lastFailure);
        assertTrue(lastFailure.compareTo(start) >= 0 && lastFailure.compareTo(end) <= 0);
        
        // check we marked the unit as failed
        f = getSingleFeature(fsUnits, ff.equal(ff.property("unit_name"), ff.literal("unit1"), false));
        assertTrue((Boolean) f.getAttribute("errors"));
    }
    
    public void testEmptyUpdates() throws Exception {
        QName typeName = new QName("http://www.openplans.org/spearfish", "restricted");
        
        // build a "no loca changes" post diff
        PostDiffType postDiff = new PostDiffType();
        postDiff.setFromVersion(-1);
        postDiff.setToVersion(-1);
        postDiff.setTypeName(typeName);
        postDiff.setTransaction(WfsFactory.eINSTANCE.createTransactionType());
        
        // create mock objects that will simulate a connection failure
        GSSClient client = createMock(GSSClient.class);
        expect(client.getCentralRevision((QName) anyObject())).andReturn(new Long(-1));
        client.postDiff(postDiff);
        expect(client.getDiff((GetDiffType) anyObject())).andReturn(new GetDiffResponseType());
        replay(client);
        GSSClientFactory factory = createMock(GSSClientFactory.class);
        expect(factory.createClient(new URL("http://localhost:8081/geoserver/ows"), null, null))
                .andReturn(client);
        replay(factory);
        
        synch.clientFactory = factory;
        
        // perform synch
        Date start = new Date();
        synch.synchronizeOustandlingLayers();
        Date end = new Date();

        // check we stored the last synch marker
        SimpleFeature f = getSingleFeature(fsUnitTables, ff.equal(ff.property("table_id"), ff.literal(1), false));
        Date lastSynch = (Date) f.getAttribute("last_synchronization");
        assertNotNull(lastSynch);
        assertTrue(lastSynch.compareTo(start) >= 0 && lastSynch.compareTo(end) <= 0);
        assertNull(f.getAttribute("last_failure"));
        
        // check we marked the unit as succeded
        f = getSingleFeature(fsUnits, ff.equal(ff.property("unit_name"), ff.literal("unit1"), false));
        assertFalse((Boolean) f.getAttribute("errors"));
    }
    
    public void testLocalChanges() throws Exception {
        // apply a local change on Central so that we'll get a non empty transaction sent to the client
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // remove the third feature
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a")));
        restricted.removeFeatures(removeFilter);
        assertEquals(3, restricted.getCount(Query.ALL));
        
        // build the expected PostDiff request
        QName typeName = new QName("http://www.openplans.org/spearfish", "restricted");
        PostDiffType postDiff = new PostDiffType();
        postDiff.setFromVersion(-1);
        postDiff.setToVersion(3);
        postDiff.setTypeName(typeName);
        TransactionType changes = WfsFactory.eINSTANCE.createTransactionType();
        DeleteElementType delete = WfsFactory.eINSTANCE.createDeleteElementType();
        delete.setTypeName(typeName);
        delete.setFilter(removeFilter);
        changes.getDelete().add(delete);
        postDiff.setTransaction(changes);
        
        // create mock objects that will check the calls are flowing as expected
        GSSClient client = createMock(GSSClient.class);
        expect(client.getCentralRevision((QName) anyObject())).andReturn(new Long(-1));
        client.postDiff(postDiff);
        expect(client.getDiff((GetDiffType) anyObject())).andReturn(new GetDiffResponseType());
        replay(client);
        GSSClientFactory factory = createMock(GSSClientFactory.class);
        expect(factory.createClient(new URL("http://localhost:8081/geoserver/ows"), null, null))
                .andReturn(client);
        replay(factory);
        
        synch.clientFactory = factory;
        
        // perform synch
        Date start = new Date();
        synch.synchronizeOustandlingLayers();
        Date end = new Date();

        // check we stored the last synch marker
        SimpleFeature f = getSingleFeature(fsUnitTables, ff.equal(ff.property("table_id"), ff.literal(1), false));
        Date lastSynch = (Date) f.getAttribute("last_synchronization");
        assertNotNull(lastSynch);
        assertTrue(lastSynch.compareTo(start) >= 0 && lastSynch.compareTo(end) <= 0);
        assertNull(f.getAttribute("last_failure"));
        
        // check we marked the unit as succeded
        f = getSingleFeature(fsUnits, ff.equal(ff.property("unit_name"), ff.literal("unit1"), false));
        assertFalse((Boolean) f.getAttribute("errors"));
    }
    
    
    public void testRemoteChanges() throws Exception {
        // make sure we start with 4 features
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        assertEquals(4, restricted.getCount(Query.ALL));
        
        // build a "no local changes" postdiff
        QName typeName = new QName("http://www.openplans.org/spearfish", "restricted");
        PostDiffType postDiff = new PostDiffType();
        postDiff.setFromVersion(-1);
        postDiff.setToVersion(-1);
        postDiff.setTypeName(typeName);
        postDiff.setTransaction(WfsFactory.eINSTANCE.createTransactionType());
        
        // build the expected GetDiff object
        GetDiffType getDiff = new GetDiffType();
        getDiff.setTypeName(typeName);
        getDiff.setFromVersion(-1);
        
        // build a GetDiffResponse that will trigger a deletion
        GetDiffResponseType gdr = new GetDiffResponseType();
        gdr.setFromVersion(-1);
        gdr.setFromVersion(6);
        gdr.setTypeName(typeName);
        TransactionType changes = WfsFactory.eINSTANCE.createTransactionType();
        DeleteElementType delete = WfsFactory.eINSTANCE.createDeleteElementType();
        delete.setTypeName(typeName);
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a")));
        delete.setFilter(removeFilter);
        changes.getDelete().add(delete);
        gdr.setTransaction(changes);
        
        // create mock objects that will check the calls are flowing as expected
        GSSClient client = createMock(GSSClient.class);
        expect(client.getCentralRevision((QName) anyObject())).andReturn(new Long(-1));
        client.postDiff(postDiff);
        expect(client.getDiff((GetDiffType) anyObject())).andReturn(gdr);
        replay(client);
        GSSClientFactory factory = createMock(GSSClientFactory.class);
        expect(factory.createClient(new URL("http://localhost:8081/geoserver/ows"), null, null))
                .andReturn(client);
        replay(factory);
        
        synch.clientFactory = factory;
        
        // perform synch
        Date start = new Date();
        synch.synchronizeOustandlingLayers();
        Date end = new Date();

        // check we stored the last synch marker
        SimpleFeature f = getSingleFeature(fsUnitTables, ff.equal(ff.property("table_id"), ff.literal(1), false));
        Date lastSynch = (Date) f.getAttribute("last_synchronization");
        assertNotNull(lastSynch);
        assertTrue(lastSynch.compareTo(start) >= 0 && lastSynch.compareTo(end) <= 0);
        assertNull(f.getAttribute("last_failure"));
        
        // check we marked the unit as succeded
        f = getSingleFeature(fsUnits, ff.equal(ff.property("unit_name"), ff.literal("unit1"), false));
        assertFalse((Boolean) f.getAttribute("errors"));
        
        // check the deletion actually happened locally
        assertEquals(3, restricted.getCount(Query.ALL));
        assertEquals(0, restricted.getCount(new DefaultQuery("restricted", removeFilter)));
    }

}
