package org.geoserver.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

public class MissingStylesTest extends GeoServerSystemTestSupport {

    Catalog catalog;
    XStreamPersister xp;
    protected GeoServer geoServer;

    @Before
    public void setUp() throws Exception {
        geoServer = getGeoServer();
        catalog = getCatalog();
    }

    @Test
    /**
     * Test for GEOS-9756 Startup fails if .xml file for a style is missing
     *
     * @throws Exception
     */
    public void testMissingStyleFile() throws Exception {

        File dir = testData.getDataDirectoryRoot();
        File f = new File(dir, "styles/foostyle.xml");
        assertFalse(f.exists());
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename("foostyle.sld");
        catalog.add(s);
        assertTrue(f.exists());
        assertTrue(catalog.getStyles().contains(s));
        f.delete();
        geoServer.reload();
        assertFalse(f.exists());
        assertFalse(catalog.getStyles().contains(s));
    }
}
