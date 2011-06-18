package org.geoserver.wfsv.security;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjects;
import org.geoserver.util.SecurityUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureStore;
import org.opengis.filter.Filter;

public class ReadOnlyVersioningDataStoreTest extends SecuredVersioningTest {

    private static final String SCHEMA = "testSchema";
    private VersioningDataStore mock;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // create a feature store
        VersioningFeatureStore mockStore = createNiceMock(VersioningFeatureStore.class);
        mockStore.removeFeatures(Filter.INCLUDE);
        expect(mockStore.getSchema()).andReturn(DataUtilities.createType(SCHEMA, "id:int"));
        replay(mockStore);

        // create a data store
        mock = createNiceMock(VersioningDataStore.class);
        expect(mock.getFeatureSource(SCHEMA)).andReturn(mockStore);
        expect(mock.isVersioned(SCHEMA)).andReturn(true);
        mock.setVersioned(SCHEMA, true, null, null);
        replay(mock);
    }

    public void testDataStoreHide() throws Exception {
        ReadOnlyVersioningDataStore secured = (ReadOnlyVersioningDataStore) SecuredObjects.secure(
                mock, WrapperPolicy.hide(null));
        assertTrue(secured.isVersioned(SCHEMA));
        try {
            secured.setVersioned(SCHEMA, true, null, null);
            fail("Should have thrown a unsupported operation exception...");
        } catch (UnsupportedOperationException e) {
            // fine
        }
        // make sure we get a read only wrapper, a source instead of a store
        ReadOnlyVersioningFeatureSource source = (ReadOnlyVersioningFeatureSource) secured
                .getFeatureSource(SCHEMA);
    }
    
    public void testDataStoreChallenge() throws Exception {
        ReadOnlyVersioningDataStore secured = (ReadOnlyVersioningDataStore) SecuredObjects.secure(
                mock, WrapperPolicy.readOnlyChallenge(null));
        assertTrue(secured.isVersioned(SCHEMA));
        try {
            secured.setVersioned(SCHEMA, true, null, null);        
            fail("Should have thrown a security exception...");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e)==false)
                fail("Should have thrown a security exception...");            
        }
        // make sure we get a read only wrapper, a source instead of a store
        ReadOnlyVersioningFeatureStore store = (ReadOnlyVersioningFeatureStore) secured
                .getFeatureSource(SCHEMA);
        
        try {
            store.removeFeatures(Filter.INCLUDE);
            fail("Should have thrown a security exception");
        } catch(Throwable e) {
            if (SecurityUtils.isSecurityException(e)==false)
                fail("Should have thrown a security exception...");            
        }
    }
}
