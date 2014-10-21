/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static java.util.Collections.*;
import static org.geotools.data.DataUtilities.*;

import java.util.HashSet;
import java.util.Set;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureDiff;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.Transaction;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Id;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class MergingFeatureDiffReaderTest extends GSSTestSupport {

    public void testMergeSingle() throws Exception {
        // grab the datastore
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore
                .getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();

        // an update
        Id updateFilter = ff.id(singleton(ff
                .featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), -48, updateFilter);
        // a delete
        Id removeFilter = ff.id(singleton(ff
                .featureId("restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5")));
        restricted.removeFeatures(removeFilter);
        // and an insert
        WKTReader wkt = new WKTReader();
        SimpleFeature f = SimpleFeatureBuilder.build(schema, new Object[] { 123,
                wkt.read("POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))") }, null);
        restricted.addFeatures(collection(f));

        // grab a diff reader
        FeatureDiffReader reader = restricted.getDifferences("FIRST", "LAST", null, null);
        FeatureDiffReader reader2 = restricted.getDifferences("FIRST", "LAST", null, null);

        // build a merging one, it should report the same resuls
        MergingFeatureDiffReader merge = new MergingFeatureDiffReader(reader2);

        int count = 0;
        Set<Integer> states = new HashSet<Integer>();
        while (reader.hasNext()) {
            assertTrue(merge.hasNext());
            FeatureDiff fd = reader.next();
            FeatureDiff fdm = merge.next();
            assertEquals(fd, fdm);
            count++;
            states.add(fd.getState());
        }
        reader.close();
        merge.close();

        assertEquals(3, count);
        assertEquals(3, states.size());
    }

    public void testDeleteReinsert() throws Exception {
        // grab the datastore
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore
                .getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();

        // remove
        Id removeFilter = ff.id(singleton(ff
                .featureId("restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5")));
        Transaction t = new DefaultTransaction();
        restricted.setTransaction(t);
        restricted.removeFeatures(removeFilter);
        String v1 = restricted.getVersion();
        t.commit();

        // revert
        String v2 = restricted.getVersion();
        restricted.rollback("FIRST", null, null);
        t.commit();
        t.close();
        
        restricted.setTransaction(Transaction.AUTO_COMMIT);

        // grab the two readers, separate
        FeatureDiffReader r1 = restricted.getDifferences("FIRST", v1, null, null);
        FeatureDiffReader r2 = restricted.getDifferences(v1, v2, null, null);

        // build a merging one, it should report the same resuls
        MergingFeatureDiffReader merge = new MergingFeatureDiffReader(r1, r2);
        
        assertFalse(merge.hasNext());
        merge.close();
    }
    
    public void testMergeMulti() throws Exception {
        // grab the datastore
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore
                .getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();

        // first update
        Id updateFilter = ff.id(singleton(ff
                .featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        Transaction t = new DefaultTransaction();
        restricted.setTransaction(t);
        restricted.modifyFeatures(schema.getDescriptor("cat"), -48, updateFilter);
        String v1 = restricted.getVersion();
        t.commit();
        
        // second update
        WKTReader wkt = new WKTReader();
        Geometry g = wkt.read("MULTIPOLYGON(((0 0, 0 10, 10 10, 10 0, 0 0)))");
        restricted.modifyFeatures(schema.getDescriptor("the_geom"), g, updateFilter);
        String v2 = restricted.getVersion();
        t.commit();
        
        // clean up
        t.close();
        restricted.setTransaction(Transaction.AUTO_COMMIT);

        // grab the two readers, separate
        FeatureDiffReader r1 = restricted.getDifferences("FIRST", v1, null, null);
        FeatureDiffReader r2 = restricted.getDifferences(v1, v2, null, null);

        // build a merging one, it should report the same resuls
        MergingFeatureDiffReader merge = new MergingFeatureDiffReader(r1, r2);
        
        assertTrue(merge.hasNext());
        FeatureDiff fd = merge.next();
        assertEquals(2, fd.getChangedAttributes().size());
        assertEquals(-48l, fd.getFeature().getAttribute("cat"));
        assertTrue(g.equals((Geometry) fd.getFeature().getAttribute("the_geom")));
        assertFalse(merge.hasNext());
        merge.close();
    }
}
