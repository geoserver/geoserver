/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.*;

import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class SecuredFeatureStoreTest extends SecureObjectsTest {

    @Test
    public void testUpdateTwiceComplex() throws Exception {
        // build up the mock
        FeatureStore fs = createMock(FeatureStore.class);
        Name[] names = new Name[] {new NameImpl("foo")};
        Object[] values = new Object[] {"abc"};
        Filter filter = Filter.INCLUDE;
        fs.modifyFeatures(eq(names), eq(values), eq(filter));
        expectLastCall().once();
        replay(fs);

        VectorAccessLimits limits =
                new VectorAccessLimits(
                        CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        SecuredFeatureStore store = new SecuredFeatureStore(fs, WrapperPolicy.readWrite(limits));
        store.modifyFeatures(names, values, filter);
        verify(fs);
    }

    @Test
    public void testUpdateTwiceSimple() throws Exception {
        // build up the mock
        SimpleFeatureStore fs = createMock(SimpleFeatureStore.class);
        String[] names = new String[] {"foo"};
        Object[] values = new Object[] {"abc"};
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
