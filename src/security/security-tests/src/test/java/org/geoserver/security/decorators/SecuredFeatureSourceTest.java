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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
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
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.junit.Test;

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
    public <T extends FeatureType, F extends Feature> void testReadOnlyFeatureSourceDataAccess()
            throws Exception {
        // build the mock up
        @SuppressWarnings("unchecked")
        DataAccess<T, F> da = createNiceMock(DataAccess.class);
        replay(da);
        @SuppressWarnings("unchecked")
        FeatureSource<T, F> fs = createNiceMock(FeatureSource.class);
        expect(fs.getDataStore()).andReturn(da);
        replay(fs);

        SecuredFeatureSource<T, F> ro =
                new SecuredFeatureSource<>(fs, WrapperPolicy.readOnlyChallenge(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataAccess);
    }

    @Test
    public <T extends FeatureType, F extends Feature>
            void testSecuredFeatureSourceLoggingWithComplex() throws Exception {
        // build up the mock
        @SuppressWarnings("unchecked")
        T schema = (T) createNiceMock(ComplexFeatureTypeImpl.class);
        expect(schema.getName()).andReturn(new NameImpl("testComplexFt"));
        @SuppressWarnings("unchecked")
        List<PropertyDescriptor> descriptors = createNiceMock(List.class);
        expect(descriptors.size()).andReturn(3).anyTimes();
        replay(descriptors);
        expect(schema.getDescriptors()).andReturn(descriptors).anyTimes();
        replay(schema);
        @SuppressWarnings("unchecked")
        DataAccess<T, F> store = createNiceMock(DataAccess.class);
        replay(store);
        @SuppressWarnings("unchecked")
        FeatureStore<T, F> fStore = createNiceMock(FeatureStore.class);
        expect(fStore.getSchema()).andReturn(schema).anyTimes();
        @SuppressWarnings("unchecked")
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
            SecuredFeatureStore ro =
                    new SecuredFeatureStore<>(fStore, WrapperPolicy.readOnlyHide(null));
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
}
