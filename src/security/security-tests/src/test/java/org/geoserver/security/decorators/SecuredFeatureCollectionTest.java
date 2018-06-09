/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class SecuredFeatureCollectionTest extends SecureObjectsTest {

    private FeatureStore store;

    private SimpleFeature feature;

    @Before
    public void setUp() throws Exception {
        SimpleFeatureType schema = createNiceMock(SimpleFeatureType.class);
        expect(schema.getTypeName()).andReturn("testSchema").anyTimes();
        expect(schema.getName()).andReturn(new NameImpl("testSchema")).anyTimes();
        replay(schema);

        feature = createNiceMock(SimpleFeature.class);
        expect(feature.getID()).andReturn("testSchema.1").anyTimes();
        expect(feature.getType()).andReturn(schema).anyTimes();
        expect(feature.getFeatureType()).andReturn(schema).anyTimes();
        replay(feature);

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        store = createNiceMock(FeatureStore.class);
        expect(store.getSchema()).andReturn(schema).anyTimes();
        expect(store.getFeatures()).andReturn(fc).anyTimes();
        expect(store.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(store.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        replay(store);
        /*expect(fc.features()).andReturn(it).anyTimes();
        expect(fc.sort(sort)).andReturn(fc).anyTimes();
        expect(fc.subCollection(Filter.INCLUDE)).andReturn(fc).anyTimes();
        expect(fc.getSchema()).andReturn(schema).anyTimes();
        replay(fc);*/
    }

    @Test
    public void testHide() throws Exception {

        SecuredFeatureStore ro = new SecuredFeatureStore(store, WrapperPolicy.hide(null));

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.addFeatures(fc);
            fail("Should have failed with an UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // ok
        }

        try {
            ro.removeFeatures(Filter.INCLUDE);
            fail("Should have failed with an UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

    @Test
    public void testReadOnly() throws Exception {
        SecuredFeatureStore ro = new SecuredFeatureStore(store, WrapperPolicy.readOnlyHide(null));

        // let's check the iterator, should allow read but not remove
        FeatureCollection rofc = ro.getFeatures();
        FeatureIterator roit = rofc.features();
        roit.hasNext();
        roit.next();

        // check derived collections are still read only and share the same
        // challenge policy
        SecuredFeatureCollection sorted =
                (SecuredFeatureCollection) rofc.sort(SortBy.NATURAL_ORDER);
        assertEquals(ro.policy, sorted.policy);
        SecuredFeatureCollection sub =
                (SecuredFeatureCollection) rofc.subCollection(Filter.INCLUDE);
        assertEquals(ro.policy, sorted.policy);
    }

    @Test
    public void testChallenge() throws Exception {

        SecuredFeatureStore ro =
                new SecuredFeatureStore(store, WrapperPolicy.readOnlyChallenge(null));

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.addFeatures(fc);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        try {
            ro.removeFeatures(Filter.INCLUDE);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.removeFeatures(ECQL.toFilter("IN ('testSchema.1')"));
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.removeFeatures(Filter.EXCLUDE);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        // let's check the iterator, should allow read but not remove
        FeatureCollection rofc = ro.getFeatures();
        FeatureIterator roit = rofc.features();
        roit.hasNext();
        roit.next();

        // check derived collections are still read only and share the same
        // challenge policy
        SecuredFeatureCollection sorted =
                (SecuredFeatureCollection) rofc.sort(SortBy.NATURAL_ORDER);
        assertEquals(ro.policy, sorted.policy);
        SecuredFeatureCollection sub =
                (SecuredFeatureCollection) rofc.subCollection(Filter.INCLUDE);
        assertEquals(ro.policy, sorted.policy);
    }
}
