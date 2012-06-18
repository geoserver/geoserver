package org.geoserver.catalog.rest;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.Collections;

import org.easymock.EasyMock;
import org.geoserver.rest.RestletException;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStoreFactorySpi;

public class DataStoreFileResourceTest extends CatalogRESTTestSupport {

    @Override
    protected String[] getSpringContextLocations() {
        String[] springContextLocations = super.getSpringContextLocations();
        String[] withCustomContext = Arrays.copyOf(springContextLocations,
                springContextLocations.length + 1);
        withCustomContext[springContextLocations.length] = "testDataAccessFactoryProvider.xml";
        return withCustomContext;
    }

    public void testLookupDataStoreFactory() {
        Object bean = applicationContext.getBean("testDataAccessFactoryProvider");
        TestDataAccessFactoryProvider factoryProvider = (TestDataAccessFactoryProvider) bean;

        FileDataStoreFactorySpi dataAccessFactory = EasyMock
                .createMock(FileDataStoreFactorySpi.class);
        expect(dataAccessFactory.getFileExtensions()).andReturn(new String[] { ".CUSTOM" });
        replay(dataAccessFactory);

        factoryProvider.setDataStoreFactories(Collections.singletonList(dataAccessFactory));

        DataAccessFactory factory = DataStoreFileResource.lookupDataStoreFactory("custom");
        assertSame("REST API did not find custom data store", dataAccessFactory, factory);
    }

    public void testLookupDataStoreFactoryKnownExtension() throws Exception {
        DataStoreFactorySpi factory = DataStoreFileResource.lookupDataStoreFactory("shp");
        assertEquals("Shapefile", factory.getDisplayName());
    }

    public void testLookupDataStoreFactoryUnknownExtension() throws Exception {
        try {
            DataStoreFileResource.lookupDataStoreFactory("unknown");
        } catch (RestletException e) {
            assertTrue("Exception expected", true);
            return;
        }
        fail("Restlet Exception should have been thrown for unknown format");
    }
}
