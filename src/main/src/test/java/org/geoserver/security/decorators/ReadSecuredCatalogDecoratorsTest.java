package org.geoserver.security.decorators;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.AbstractAuthorizationTest;

public class ReadSecuredCatalogDecoratorsTest extends AbstractAuthorizationTest {

    public void testSecuredLayerInfoFeatures() {
        SecuredLayerInfo ro = new SecuredLayerInfo(statesLayer,
                WrapperPolicy.hide(null));

        assertFalse(statesLayer.getResource() instanceof SecuredFeatureTypeInfo);
        assertTrue(ro.getResource() instanceof SecuredFeatureTypeInfo);
        assertSame(ro.policy,
                ((SecuredFeatureTypeInfo) ro.getResource()).policy);
    }

    public void testSecuredLayerInfoCoverages() {
        SecuredLayerInfo ro = new SecuredLayerInfo(arcGridLayer,
                WrapperPolicy.hide(null));

        assertFalse(arcGridLayer.getResource() instanceof SecuredCoverageInfo);
        assertTrue(ro.getResource() instanceof SecuredCoverageInfo);
        assertSame(ro.policy, ((SecuredCoverageInfo) ro.getResource()).policy);
    }

    public void testSecuredFeatureTypeInfoHide() throws Exception {
        SecuredFeatureTypeInfo ro = new SecuredFeatureTypeInfo(states,
                WrapperPolicy.hide(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(
                null, null);
        assertEquals(SecuredFeatureSource.class, fs.getClass());
        assertTrue(fs.policy.isHide());
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(((SecuredDataStoreInfo) store).policy.isHide());
    }

    public void testSecuredFeatureTypeInfoMetadata() throws Exception {
        SecuredFeatureTypeInfo ro = new SecuredFeatureTypeInfo(states,
                WrapperPolicy.metadata(null));
        try {
            ro.getFeatureSource(null, null);
            fail("This should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(((SecuredDataStoreInfo) store).policy.isMetadata());
    }

    public void testSecuredTypeInfoReadOnly() throws Exception {
        SecuredFeatureTypeInfo ro = new SecuredFeatureTypeInfo(states,
                WrapperPolicy.readOnlyChallenge(null));
        SecuredFeatureStore fs = (SecuredFeatureStore) ro.getFeatureSource(
                null, null);
        assertTrue(fs.policy.isReadOnlyChallenge());
        SecuredDataStoreInfo store = (SecuredDataStoreInfo) ro.getStore();
        assertTrue(((SecuredDataStoreInfo) store).policy.isReadOnlyChallenge());
    }

    public void testSecuredDataStoreInfoHide() throws Exception {
        SecuredDataStoreInfo ro = new SecuredDataStoreInfo(statesStore,
                WrapperPolicy.hide(null));
        ReadOnlyDataStore dataStore = (ReadOnlyDataStore) ro.getDataStore(null);
        assertTrue(dataStore.policy.isHide());
    }

    public void testSecuredDataStoreInfoMetadata() throws Exception {
        SecuredDataStoreInfo ro = new SecuredDataStoreInfo(statesStore,
                WrapperPolicy.metadata(null));
        try {
            ReadOnlyDataStore dataStore = (ReadOnlyDataStore) ro.getDataStore(null);
            fail("This should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
    }

}
