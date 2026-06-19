/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.util.logging.Logging;
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

    /**
     * Verifies that initialize() emits a WARNING when neither read_only nor immutable is set, alerting administrators
     * to the concurrent-access risk described in GEOS-12094.
     */
    @Test
    public void testWarningEmittedWhenNoReadOnlyOrImmutable() {
        Logger logger = Logging.getLogger(GeoPkgDataStoreFactoryInitializer.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);

        final boolean[] warningEmitted = {false};
        Handler capturingHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().equals(Level.WARNING)
                        && record.getMessage() != null
                        && record.getMessage().contains("immutable")) {
                    warningEmitted[0] = true;
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(capturingHandler);

        try {
            GeoServerResourceLoader resourceLoader = createMock(GeoServerResourceLoader.class);
            expect(resourceLoader.getBaseDirectory())
                    .andReturn(new File("target"))
                    .once();
            replay(resourceLoader);

            GeoPkgDataStoreFactoryInitializer initializer = new GeoPkgDataStoreFactoryInitializer();
            initializer.setResourceLoader(resourceLoader);
            initializer.initialize(new GeoPkgDataStoreFactory());

            assertTrue(
                    "Expected a WARNING log mentioning 'immutable' when neither read_only nor "
                            + "immutable is configured (GEOS-12094)",
                    warningEmitted[0]);

            verify(resourceLoader);
        } finally {
            logger.removeHandler(capturingHandler);
            logger.setLevel(originalLevel);
        }
    }
}
