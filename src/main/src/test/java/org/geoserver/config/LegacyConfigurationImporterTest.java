/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import org.geoserver.config.impl.GeoServerFactoryImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.LegacyConfigurationImporter;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;

public class LegacyConfigurationImporterTest {

    LegacyConfigurationImporter importer;

    @Before
    public void setUp() throws Exception {
        GeoServer gs = new GeoServerImpl();
        gs.setFactory(new GeoServerFactoryImpl(gs));

        importer = new LegacyConfigurationImporter(gs);
        importer.imprt(URLs.urlToFile(getClass().getResource("services.xml")).getParentFile());
    }

    @Test
    public void testGlobal() throws Exception {
        GeoServerInfo info = importer.getConfiguration().getGlobal();
        assertNotNull(info);

        LoggingInfo logging = importer.getConfiguration().getLogging();
        assertNotNull(logging);

        assertEquals("DEFAULT_LOGGING.properties", logging.getLevel());
        assertTrue(logging.isStdOutLogging());
        assertEquals("logs/geoserver.log", logging.getLocation());
        assertFalse(info.getSettings().isVerbose());
        assertFalse(info.getSettings().isVerboseExceptions());
        assertEquals(8, info.getSettings().getNumDecimals());
        assertEquals("UTF-8", info.getSettings().getCharset());
        assertEquals(3, info.getUpdateSequence());
    }
}
