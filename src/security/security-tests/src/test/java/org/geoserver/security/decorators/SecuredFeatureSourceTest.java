/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

public class SecuredFeatureSourceTest extends SecureObjectsTest {

    @Test
    public void testReadOnlyFeatureSourceDataStore() throws Exception {
        // build up the mock
        DataStore ds = createNiceMock(DataStore.class);
        replay(ds);
        SimpleFeatureSource fs = createNiceMock(SimpleFeatureSource.class);
        SimpleFeatureCollection fc = createNiceMock(SimpleFeatureCollection.class);
        expect(fc.getSchema()).andReturn(createNiceMock(SimpleFeatureType.class));
        replay(fc);
        expect(fs.getDataStore()).andReturn(ds);
        expect(fs.getFeatures()).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        replay(fs);

        SecuredFeatureSource<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureSource<>(fs, WrapperPolicy.hide(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataStore);
        ro.getFeatures();
        assertTrue(ro.policy.isHide());
        assertTrue(ro.getFeatures(Filter.INCLUDE) instanceof SecuredFeatureCollection);
        assertTrue(ro.getFeatures(new Query()) instanceof SecuredFeatureCollection);
    }

    @Test
    public void testReadOnlyFeatureStore() throws Exception {
        // build up the mock
        SimpleFeatureType schema = createNiceMock(SimpleFeatureType.class);
        expect(schema.getName()).andReturn(new NameImpl("testFT"));
        replay(schema);
        SimpleFeatureStore fs = createNiceMock(SimpleFeatureStore.class);
        expect(fs.getSchema()).andReturn(schema);
        replay(fs);

        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureStore<>(fs, WrapperPolicy.readOnlyChallenge(null));
        try {
            ro.addFeatures(createNiceMock(SimpleFeatureCollection.class));
            fail("This should have thrown a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }

    @Test
    public <T extends FeatureType, F extends Feature> void testReadOnlyFeatureSourceDataAccess() throws Exception {
        // build the mock up
        DataAccess<T, F> da = createNiceMock(DataAccess.class);
        replay(da);
        FeatureSource<T, F> fs = createNiceMock(FeatureSource.class);
        expect(fs.getDataStore()).andReturn(da);
        replay(fs);

        SecuredFeatureSource<T, F> ro = new SecuredFeatureSource<>(fs, WrapperPolicy.readOnlyChallenge(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataAccess);
    }

    @Test
    public <T extends FeatureType, F extends Feature> void testSecuredFeatureSourceLoggingWithComplex()
            throws Exception {
        // build up the mock
        @SuppressWarnings("unchecked")
        T schema = (T) createNiceMock(ComplexFeatureTypeImpl.class);
        expect(schema.getName()).andReturn(new NameImpl("testComplexFt"));
        List<PropertyDescriptor> descriptors = createNiceMock(List.class);
        expect(descriptors.size()).andReturn(3).anyTimes();
        replay(descriptors);
        expect(schema.getDescriptors()).andReturn(descriptors).anyTimes();
        replay(schema);
        DataAccess<T, F> store = createNiceMock(DataAccess.class);
        replay(store);
        FeatureStore<T, F> fStore = createNiceMock(FeatureStore.class);
        expect(fStore.getSchema()).andReturn(schema).anyTimes();
        FeatureCollection<T, F> fc = createNiceMock(FeatureCollection.class);
        expect(fStore.getDataStore()).andReturn(store);
        expect(fStore.getFeatures()).andReturn(fc).anyTimes();
        expect(fStore.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(fStore.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        expect(fc.getSchema()).andReturn(schema).anyTimes();
        replay(fStore);
        replay(fc);

        // custom LogHandler to intercept log messages
        class LogHandler extends Handler {
            List<String> messages = new ArrayList<>();

            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        }
        Logger logger = Logging.getLogger(SecuredFeatureSource.class);
        LogHandler customLogHandler = new LogHandler();
        logger.addHandler(customLogHandler);
        customLogHandler.setLevel(Level.SEVERE);
        logger.addHandler(customLogHandler);
        try {
            SecuredFeatureStore ro = new SecuredFeatureStore<>(fStore, WrapperPolicy.readOnlyHide(null));
            Query q = new Query("testComplextFt");
            List<PropertyName> pnames = new ArrayList<>(1);
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            pnames.add(ff.property("someProperty"));
            q.setProperties(pnames);
            ro.getFeatures(q);
            String notExpectedMessage =
                    "Complex store returned more properties than allowed by security (because they are required by the schema). Either the security setup is broken or you have a security breach";
            assertFalse(customLogHandler.messages.contains(notExpectedMessage));
        } finally {
            logger.removeHandler(customLogHandler);
        }
    }

    /**
     * This test ensures that the clip geometry is reprojected to the collection's actual coordinate reference system,
     * including axis order, before being intersected with the features, so only the feature actually covered by the
     * clip area is returned.
     */
    @Test
    public void testDecoratesForClippingReprojectsClipToCollectionCRS() throws Exception {
        CoordinateReferenceSystem latLonCRS = CRS.decode("urn:ogc:def:crs:EPSG::4326", false);

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("states");
        tb.add("geom", Polygon.class, latLonCRS);
        tb.add("name", String.class);
        SimpleFeatureType schema = tb.buildFeatureType();

        DefaultFeatureCollection delegateCollection = new DefaultFeatureCollection(null, schema);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        WKTReader reader = new WKTReader();

        // feature geometries as delivered by WFS 2.0.0, in (lat, lon) order
        Geometry texas = reader.read("POLYGON((30 -100, 30 -98, 32 -98, 32 -100, 30 -100))");
        fb.add(texas);
        fb.add("Texas");
        delegateCollection.add(fb.buildFeature("states.1"));
        fb.reset();

        Geometry northCarolina = reader.read("POLYGON((35 -80, 35 -78, 37 -78, 37 -80, 35 -80))");
        fb.add(northCarolina);
        fb.add("North Carolina");
        delegateCollection.add(fb.buildFeature("states.2"));

        // clip area as stored by GeoFence, in conventional (lon, lat) order, SRID 4326
        Geometry clip = reader.read("POLYGON((-106.8 24, -93 24, -93 36.6, -106.8 36.6, -106.8 24))");
        clip.setSRID(4326);

        VectorAccessLimits limits =
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        limits.setClipVectorFilter(clip);

        SimpleFeatureSource fs = createNiceMock(SimpleFeatureSource.class);
        expect(fs.getFeatures((Query) anyObject()))
                .andReturn(delegateCollection)
                .anyTimes();
        replay(fs);

        SecuredFeatureSource<SimpleFeatureType, SimpleFeature> secured =
                new SecuredFeatureSource<>(fs, WrapperPolicy.readOnlyHide(limits));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = secured.getFeatures(Query.ALL);

        assertEquals(1, result.size());
        try (SimpleFeatureIterator it = ((SimpleFeatureCollection) result).features()) {
            SimpleFeature f = it.next();
            assertEquals("Texas", f.getAttribute("name"));
        }
    }
}
