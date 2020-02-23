/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.style.GraphicalSymbol;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GeoServerDataDirectoryTest {

    ClassPathXmlApplicationContext ctx;

    GeoServerDataDirectory dataDir;
    CatalogFactory factory = new CatalogFactoryImpl(new CatalogImpl());

    @Before
    public void setUp() throws Exception {

        ctx =
                new ClassPathXmlApplicationContext(
                        "GeoServerDataDirectoryTest-applicationContext.xml", getClass());
        ctx.refresh();

        dataDir = new GeoServerDataDirectory(Files.createTempDirectory("data").toFile());
        dataDir.root().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    public void testNullWorkspace() {
        assertEquals(dataDir.get((WorkspaceInfo) null, "test").path(), dataDir.get("test").path());
        assertEquals(
                dataDir.getStyles((WorkspaceInfo) null, "test").path(),
                dataDir.getStyles("test").path());
        assertEquals(
                dataDir.getLayerGroups((WorkspaceInfo) null, "test").path(),
                dataDir.getLayerGroups("test").path());
    }

    @Test
    public void testParsedStyle() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external.sld");
        Files.copy(this.getClass().getResourceAsStream("external.sld"), styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(((ExternalGraphic) graphic).getLocation(), iconFile.toURI().toURL());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }

    @Test
    public void testParsedStyleExternalWithParams() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external_with_params.sld");
        Files.copy(
                this.getClass().getResourceAsStream("external_with_params.sld"),
                styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(
                ((ExternalGraphic) graphic).getLocation().getPath(),
                iconFile.toURI().toURL().getPath());

        assertEquals("param1=1", ((ExternalGraphic) graphic).getLocation().getQuery());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }

    /**
     * Test loading a parsed style with an external graphic URL that contains both ?queryParams and
     * a URL #fragment, and assert that those URL components are preserved.
     */
    @Test
    public void testParsedStyleExternalWithParamsAndFragment() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();

        // Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external_with_params_and_fragment.sld");
        Files.copy(
                this.getClass().getResourceAsStream("external_with_params_and_fragment.sld"),
                styleFile.toPath());

        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());

        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());

        Style s = dataDir.parsedStyle(si);
        // Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic =
                ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(
                ((ExternalGraphic) graphic).getLocation().getPath(),
                iconFile.toURI().toURL().getPath());

        assertEquals("param1=1", ((ExternalGraphic) graphic).getLocation().getQuery());
        assertEquals("textAfterHash", ((ExternalGraphic) graphic).getLocation().getRef());

        // GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }
}
