/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.util.DataStoreUtils;

public class GeoPkgDataStoreFactoryInitializerTest {

    @Test
    public void testInitializer() {
        GeoServerResourceLoader resourceLoader = createMock(GeoServerResourceLoader.class);
        expect(resourceLoader.getBaseDirectory()).andReturn(new File("target")).once();
        replay(resourceLoader);

        GeoPkgDataStoreFactoryInitializer initializer = new GeoPkgDataStoreFactoryInitializer();
        initializer.setResourceLoader(resourceLoader);

        WebApplicationContext appContext = createNiceMock(WebApplicationContext.class);
        expect(appContext.getBeanNamesForType(DataStoreFactoryInitializer.class))
                .andReturn(new String[] {"geopkgDataStoreFactoryInitializer"})
                .anyTimes();
        expect(appContext.getBean("geopkgDataStoreFactoryInitializer"))
                .andReturn(initializer)
                .anyTimes();
        replay(appContext);

        new GeoServerExtensions().setApplicationContext(appContext);
        assertNotNull(DataStoreUtils.aquireFactory(new GeoPkgDataStoreFactory().getDisplayName()));

        verify(resourceLoader);
    }
}
