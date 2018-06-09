/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import static org.junit.Assert.*;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;

public class LegacyLoggingImporterTest {

    GeoServer gs;
    LegacyLoggingImporter importer;

    @Before
    public void setUp() throws Exception {
        gs = new GeoServerImpl();

        importer = new LegacyLoggingImporter();
        importer.imprt(URLs.urlToFile(getClass().getResource("services.xml")).getParentFile());
    }

    @Test
    public void test() throws Exception {
        assertEquals("DEFAULT_LOGGING.properties", importer.getConfigFileName());
        assertFalse(importer.getSuppressStdOutLogging());
        assertEquals("logs/geoserver.log", importer.getLogFile());
    }
}
