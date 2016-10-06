/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;
import org.geoserver.rest.RestletException;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStoreFactorySpi;
import org.junit.Test;

public class DataStoreFileResourceTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("testDataAccessFactoryProvider.xml");
    }

    @Test
    public void testLookupDataStoreFactory() {
        Object bean = applicationContext.getBean("testDataAccessFactoryProvider");
        TestDataAccessFactoryProvider factoryProvider = (TestDataAccessFactoryProvider) bean;

        FileDataStoreFactorySpi dataAccessFactory = EasyMock
                .createMock(FileDataStoreFactorySpi.class);
        expect(dataAccessFactory.getFileExtensions()).andReturn(new String[] { ".CUSTOM" }).anyTimes();
        replay(dataAccessFactory);

        factoryProvider.setDataStoreFactories(Collections.singletonList(dataAccessFactory));

        DataAccessFactory factory = DataStoreFileResource.lookupDataStoreFactory("custom");
        assertSame("REST API did not find custom data store", dataAccessFactory, factory);
    }

    @Test
    public void testLookupDataStoreFactoryKnownExtension() throws Exception {
        DataAccessFactory factory = DataStoreFileResource.lookupDataStoreFactory("shp");
        assertEquals("Shapefile", factory.getDisplayName());
    }

    @Test
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
