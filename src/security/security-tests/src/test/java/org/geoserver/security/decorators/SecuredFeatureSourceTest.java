/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.*;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class SecuredFeatureSourceTest extends SecureObjectsTest {

    @Test
    public void testReadOnlyFeatureSourceDataStore() throws Exception {
        // build up the mock
        DataStore ds = createNiceMock(DataStore.class);
        replay(ds);
        FeatureSource fs = createNiceMock(FeatureSource.class);
        FeatureCollection fc = createNiceMock(FeatureCollection.class);
        expect(fs.getDataStore()).andReturn(ds);
        expect(fs.getFeatures()).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        replay(fs);

        SecuredFeatureSource ro = new SecuredFeatureSource(fs, WrapperPolicy.hide(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataStore);
        SecuredFeatureCollection collection = (SecuredFeatureCollection) ro.getFeatures();
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
        FeatureStore fs = createNiceMock(FeatureStore.class);
        expect(fs.getSchema()).andReturn(schema);
        replay(fs);

        SecuredFeatureStore ro = new SecuredFeatureStore(fs, WrapperPolicy.readOnlyChallenge(null));
        try {
            ro.addFeatures(createNiceMock(FeatureCollection.class));
            fail("This should have thrown a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }

    @Test
    public void testReadOnlyFeatureSourceDataAccess() throws Exception {
        // build the mock up
        DataAccess da = createNiceMock(DataAccess.class);
        replay(da);
        FeatureSource fs = createNiceMock(FeatureSource.class);
        expect(fs.getDataStore()).andReturn(da);
        replay(fs);

        SecuredFeatureSource ro =
                new SecuredFeatureSource(fs, WrapperPolicy.readOnlyChallenge(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataAccess);
    }

    @Test
    public void testSecuredFeatureSourceLoggingWithComplex() throws Exception {
        // build up the mock
        ComplexFeatureTypeImpl schema = createNiceMock(ComplexFeatureTypeImpl.class);
        expect(schema.getName()).andReturn(new NameImpl("testComplexFt"));
        List<PropertyDescriptor> descriptors = createNiceMock(List.class);
        expect(descriptors.size()).andReturn(3).anyTimes();
        replay(descriptors);
        expect(schema.getDescriptors()).andReturn(descriptors).anyTimes();
        replay(schema);
        DataStore store = createNiceMock(DataStore.class);
        replay(store);
        FeatureStore fStore = createNiceMock(FeatureStore.class);
        expect(fStore.getSchema()).andReturn(schema).anyTimes();
        FeatureCollection fc = createNiceMock(FeatureCollection.class);
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
                    new SecuredFeatureStore(fStore, WrapperPolicy.readOnlyHide(null));
            Query q = new Query("testComplextFt");
            List<PropertyName> pnames = new ArrayList<PropertyName>(1);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
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
