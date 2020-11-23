package org.geoserver.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

public class MissingStylesTest extends GeoServerSystemTestSupport {

    Catalog catalog;
    XStreamPersister xp;
    protected GeoServer geoServer;
    private File dir;

    @Before
    public void setUp() throws Exception {
        geoServer = getGeoServer();
        catalog = getCatalog();
        dir = testData.getDataDirectoryRoot();
    }

    @Test
    /**
     * Test for GEOS-9756 Startup fails if .xml file for a style is missing
     *
     * @throws Exception
     */
    public void testMissingStyleFile() throws Exception {
        File f = new File(testData.getDataDirectoryRoot(), "styles/foostyle.xml");
        assertFalse(f.exists());

        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename("foostyle.sld");
        catalog.add(s);
        assertTrue(f.exists());

        LayerInfo layer = catalog.getLayerByName("Bridges");
        layer.getStyles().add(catalog.getStyleByName("foostyle"));
        assertFalse(layer.getStyles().isEmpty());
        catalog.save(layer);
        f.delete();
        assertFalse(f.exists());
        geoServer.reset();
        geoServer.reload();
        layer = catalog.getLayerByName("Bridges");
        assertTrue(layer.getStyles().isEmpty());
    }

    @Test
    public void testMissingDefaultStyle() throws Exception {
        LayerInfo layer = catalog.getLayerByName("Bridges");

        StyleInfo style = layer.getDefaultStyle();

        File f = new File(dir, "styles/" + style.getFilename().replace("sld", "xml"));
        assertTrue(f.exists());
        f.delete();
        geoServer.reset();
        geoServer.reload();
        assertFalse(f.exists());
        layer = catalog.getLayerByName("Bridges");
        style = layer.getDefaultStyle();
        assertNotNull(style);
    }
}
