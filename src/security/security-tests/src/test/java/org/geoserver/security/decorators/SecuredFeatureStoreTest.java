/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class SecuredFeatureStoreTest extends SecureObjectsTest {

    @Test
    public void testUpdateTwiceComplex() throws Exception {
        // build up the mock
        SimpleFeatureStore fs = createMock(SimpleFeatureStore.class);
        Name[] names = {new NameImpl("foo")};
        Object[] values = {"abc"};
        Filter filter = Filter.INCLUDE;
        fs.modifyFeatures(eq(names), eq(values), eq(filter));
        expectLastCall().once();
        replay(fs);

        VectorAccessLimits limits =
                new VectorAccessLimits(
                        CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> store =
                new SecuredFeatureStore<>(fs, WrapperPolicy.readWrite(limits));
        store.modifyFeatures(names, values, filter);
        verify(fs);
    }

    @Test
    public void testUpdateTwiceSimple() throws Exception {
        // build up the mock
        SimpleFeatureStore fs = createMock(SimpleFeatureStore.class);
        String[] names = {"foo"};
        Object[] values = {"abc"};
        Filter filter = Filter.INCLUDE;
        fs.modifyFeatures(eq(names), eq(values), eq(filter));
        expectLastCall().once();
        replay(fs);

        VectorAccessLimits limits =
                new VectorAccessLimits(
                        CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        SecuredSimpleFeatureStore store =
                new SecuredSimpleFeatureStore(fs, WrapperPolicy.readWrite(limits));
        store.modifyFeatures(names, values, filter);
        verify(fs);
    }
}
