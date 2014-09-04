/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static java.util.Collections.*;
import static org.geoserver.gss.GSSCore.*;

import java.io.IOException;

import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class PostDiffTest extends GSSTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    }
    
    public void testConfictSchema() throws IOException {
         SimpleFeatureType ft = synchStore.getSchema("synch_conflicts");
         // we expect the pk to be exposed, as it's a multi column one
         assertEquals(7, ft.getAttributeCount());
    }

    public void testUnknownLayer() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffUnknown.xml"));
        validate(response);
        Document dom = dom(response);
        print(dom);
        checkOws10Exception(dom, "InvalidParameterValue", "typeName");
    }

    public void testInvalidFromVersion() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInvalidFrom.xml"));
        validate(response);
        Document dom = dom(response);
        checkOws10Exception(dom, "InvalidParameterValue", "fromVersion");
    }

    public void testEmptyRepeated() throws Exception {
        VersioningDataStore synch = (VersioningDataStore) getCatalog().getDataStoreByName("synch").getDataStore(null);
        VersioningFeatureSource restricted = (VersioningFeatureSource) synch.getFeatureSource("restricted");
        long revisionStart = getLastRevision(restricted);
        
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffEmpty.xml"));
        checkPostDiffSuccessResponse(response);
        
        // check the lasts known Central revision is updated
        assertEquals(new Long(12), getLastCentralRevision(synch, "restricted"));
        
        // check we did not eat away a local revision number
        long revisionAfterFirst = getLastRevision(restricted);
        assertEquals(revisionStart, revisionAfterFirst);
        
        // execute a second empty update
        response = postAsServletResponse(root(true), loadTextResource("PostDiffEmptySecond.xml"));
        checkPostDiffSuccessResponse(response);
        
        // check the lasts known Central revision is updated
        assertEquals(new Long(24), getLastCentralRevision(synch, "restricted"));
        
        // check we did not eat away a local revision number
        long revisionAfterSecond = getLastRevision(restricted);
        assertEquals(revisionStart, revisionAfterSecond);
    }

    Long getLastCentralRevision(VersioningDataStore synch, String tableName) throws IOException {
        FeatureIterator<SimpleFeature> fi = null;
        try {
            FeatureSource<SimpleFeatureType, SimpleFeature> fs = synch.getFeatureSource(SYNCH_HISTORY);
            PropertyIsEqualTo tableFilter = ff.equals(ff.property("table_name"), ff.literal(tableName));
            DefaultQuery q = new DefaultQuery(SYNCH_HISTORY, tableFilter);
            q.setSortBy(new SortBy[] {ff.sort("local_revision", SortOrder.DESCENDING), 
                    ff.sort("central_revision", SortOrder.DESCENDING)});
            q.setMaxFeatures(1);
            fi = fs.getFeatures(q).features();
            SimpleFeature f = (SimpleFeature) fi.next();
            
            return (Long) f.getAttribute("central_revision");
        } finally {
            if(fi != null) {
                fi.close();
            }
        }
    }
    
    long getLastRevision(VersioningFeatureSource fs) throws IOException {
        FeatureIterator<SimpleFeature> fi = null;
        try {
            fi = fs.getLog("LAST", "FIRST", null, null, 1).features();
            if(fi.hasNext()) {
                return (Long) fi.next().getAttribute("revision");
            } else {
                return -1;
            }
        } finally {
            fi.close();
        }
    }
    
    public void testEmptyTransaction() throws Exception {
        // a slightly different test for a failure encontered in real world testing
        // with a transaction element specified, but empty
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffEmptyTransaction.xml"));
        checkPostDiffSuccessResponse(response);

        // check the lasts known Central revision is updated
        VersioningDataStore synch = (VersioningDataStore) getCatalog().getDataStoreByName("synch")
                .getDataStore(null);
        FeatureSource<SimpleFeatureType, SimpleFeature> fs = synch.getFeatureSource(SYNCH_HISTORY);
        FeatureCollection fc = fs.getFeatures(ff.equals(ff.property("table_name"), ff
                .literal("restricted")));
        FeatureIterator fi = fc.features();
        SimpleFeature f = (SimpleFeature) fi.next();
        fi.close();

        assertEquals(12l, f.getAttribute("central_revision"));
    }

    /**
     * No local changes, no conflicts
     * @throws Exception
     */
    public void testFirstSynch() throws Exception {
        // grab the datastore so that we can assess the initial situation
        FeatureSource<SimpleFeatureType, SimpleFeature> restricted = synchStore
                .getFeatureSource("restricted");
        assertEquals(4, restricted.getCount(Query.ALL));

        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        checkPostDiffInitialChanges(restricted);

        // check there are no conflicts
        assertEquals(0, gss.getActiveConflicts("restricted").size());
    }
    
    /**
     * Local but not conflicting changes
     * @throws Exception
     */
    public void testLocalChangesNoConflict() throws Exception {
        // grab the datastore so that we can make some changes that will not generate conflicts
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // modify the fourth feature, change its cat from 400 to 450
        Id updateFilter = ff.id(singleton(ff.featureId("restricted.1b99be2b-2480-4742-ad52-95c294efda3b")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), 450, updateFilter);
        // remove the third feature
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a")));
        restricted.removeFeatures(removeFilter);
        assertEquals(3, restricted.getCount(Query.ALL));
        

        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        checkPostDiffInitialChanges(restricted);

        // check there are no conflicts
        assertEquals(0, gss.getActiveConflicts("restricted").size());
        
        // check the local changes are still there
        assertEquals(0, restricted.getCount(new DefaultQuery(null, removeFilter)));
        FeatureIterator<SimpleFeature> fi;
        fi = restricted.getFeatures(updateFilter).features();
        assertTrue(fi.hasNext());
        SimpleFeature f = fi.next();
        fi.close();
        assertEquals(450l, f.getAttribute("cat"));
    }
    
    /**
     * Sheer luck, the local changes are on the same feature, and are the same changes Central is pushing onto us
     * @throws Exception
     */
    public void testCleanMerge() throws Exception {
        // grab the datastore 
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // make the same changes as in the post diff
        Id updateFilter = ff.id(singleton(ff.featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), -48, updateFilter);
        // remove the third feature
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5")));
        restricted.removeFeatures(removeFilter);
        assertEquals(3, restricted.getCount(Query.ALL));
        

        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        checkPostDiffInitialChanges(restricted);

        // check there are no conflicts
        assertEquals(0, gss.getActiveConflicts("restricted").size());
    }

    public void testDeleteConflict() throws Exception {
        // grab the datastore so that we can make some changes that will generate conflicts
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        restricted.removeFeatures(ff.id(singleton(ff.featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4"))));
        assertEquals(3, restricted.getCount(Query.ALL));
     
        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        checkPostDiffInitialChanges(restricted);
        
        // check we actually have stored the deletion conflict (the deleted feature has been updated
        // by central
        FeatureCollection<SimpleFeatureType, SimpleFeature> activeConflicts = gss.getActiveConflicts("restricted");
        assertEquals(1, activeConflicts.size());
        FeatureIterator<SimpleFeature> fi = activeConflicts.features();
        SimpleFeature f = fi.next();
        fi.close();
        
        assertEquals("restricted", f.getAttribute("table_name"));
        assertEquals("be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4", f.getAttribute("feature_id"));
        assertEquals("c", f.getAttribute("state"));
        assertNull(f.getAttribute("local_feature"));
    }
    
    public void testUpdateConflict() throws Exception {
        // grab the datastore so that we can make some changes that will generate conflicts
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        Id fidFilter = ff.id(singleton(ff.featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), 123456, fidFilter);
        assertEquals(4, restricted.getCount(Query.ALL));
     
        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        checkPostDiffInitialChanges(restricted);
        
        // check we actually have stored the update conflict 
        FeatureCollection<SimpleFeatureType, SimpleFeature> activeConflicts = gss.getActiveConflicts("restricted");
        assertEquals(1, activeConflicts.size());
        FeatureIterator<SimpleFeature> fi = activeConflicts.features();
        SimpleFeature f = fi.next();
        fi.close();
        
        assertEquals("restricted", f.getAttribute("table_name"));
        assertEquals("be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4", f.getAttribute("feature_id"));
        assertEquals("c", f.getAttribute("state"));
        assertNotNull(f.getAttribute("local_feature"));
        // TODO: make sure the stored feature is the value before the rollback
        
        SimpleFeature preConflict = gss.fromGML3((String) f.getAttribute("local_feature"));
        assertEquals(123456l, preConflict.getAttribute("cat"));
    }
    
    /**
     * Checks the changes in PostDiffInitial.xml have all been applied successfully
     * @param restricted
     * @throws IOException
     */
    void checkPostDiffInitialChanges(
            FeatureSource<SimpleFeatureType, SimpleFeature> restricted) throws IOException {
        // check from the datastore we actually have applied the diff
        SimpleFeature deleted = gss.getFeatureById(restricted,
                "restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5");
        assertNull(deleted);
        SimpleFeature updated = gss.getFeatureById(restricted,
                "restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4");
        assertNotNull(updated);
        assertEquals(-48l, updated.getAttribute("cat"));
        SimpleFeature added = gss.getFeatureById(restricted,
                "restricted.e9cba212-d79d-4569-aa0a-48f6b80539ee");
        assertNotNull(added);
        assertEquals(123l, added.getAttribute("cat"));
    }
    
}
