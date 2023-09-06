/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

public class SecuredFeatureTypeInfoTest
        extends SecuredResourceInfoTest<FeatureTypeInfo, SecuredFeatureTypeInfo> {

    @Override
    FeatureTypeInfo createDelegate() {
        FeatureTypeInfo info = new FeatureTypeInfoImpl(getCatalog());
        info.setStore(new DataStoreInfoImpl(getCatalog()));
        return info;
    }

    @Override
    SecuredFeatureTypeInfo createSecuredDecorator(FeatureTypeInfo delegate) {
        return new SecuredFeatureTypeInfo(delegate, policy);
    }

    @Override
    Class<FeatureTypeInfo> getDelegateClass() {
        return FeatureTypeInfo.class;
    }

    @Override
    Class<SecuredFeatureTypeInfo> getSecuredDecoratorClass() {
        return SecuredFeatureTypeInfo.class;
    }

    @Override
    Class<SecuredDataStoreInfo> getSecuredStoreInfoClass() {
        return SecuredDataStoreInfo.class;
    }

    @Override
    int getStackOverflowCount() {
        return 500;
    }

    static class TestVectorAccessLimits extends VectorAccessLimits {

        static final Query THE_QUERY = new Query();

        public TestVectorAccessLimits() {
            super(CatalogMode.HIDE, null, null, null, null, null);
        }

        @Override
        public Query getReadQuery() {
            return THE_QUERY;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCloneAccessLimits() throws Exception {
        TestVectorAccessLimits limits = new TestVectorAccessLimits();
        WrapperPolicy wp = WrapperPolicy.readOnlyHide(limits);

        // mocks for the getFeatureSource call
        FeatureTypeInfo fti = Mockito.mock(FeatureTypeInfo.class);
        FeatureSource fs = Mockito.mock(SimpleFeatureSource.class);
        Mockito.when(fti.getFeatureSource(null, null)).thenReturn(fs);

        // build the secured feature type and grab a secure source
        SecuredFeatureTypeInfo secured = new SecuredFeatureTypeInfo(fti, wp);
        SecuredFeatureSource securedSource =
                (SecuredFeatureSource) secured.getFeatureSource(null, null);
        assertNotNull(securedSource);

        // use Spring reflection support to access private field
        Field policyField = ReflectionUtils.findField(SecuredFeatureSource.class, "policy");
        policyField.setAccessible(true);
        WrapperPolicy fsPolicy = (WrapperPolicy) policyField.get(securedSource);

        // check the policy is a clone of the original one, instead of a plain VectorAccessLimits
        assertThat(fsPolicy.getLimits(), CoreMatchers.instanceOf(TestVectorAccessLimits.class));
    }
}
