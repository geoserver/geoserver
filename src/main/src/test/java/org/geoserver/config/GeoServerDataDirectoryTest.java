package org.geoserver.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.markwkt.WKTMarkFactory;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.style.GraphicalSymbol;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.vividsolutions.jts.geom.Point;


public class GeoServerDataDirectoryTest {
    
    ClassPathXmlApplicationContext ctx;
    
    GeoServerDataDirectory dataDir;
    CatalogFactory factory = new CatalogFactoryImpl(new CatalogImpl());

    private static final SimpleFeature feature;
    private static final FilterFactory ff;

    static {
        try {
        ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName("TestType");
        featureTypeBuilder.add("geom", Point.class,
                DefaultGeographicCRS.WGS84);
        SimpleFeatureType featureType = featureTypeBuilder
                .buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
                featureType);
        feature = featureBuilder.buildFeature(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Before
    public void setUp() throws Exception {
        
        ctx = new ClassPathXmlApplicationContext(
            "GeoServerDataDirectoryTest-applicationContext.xml", getClass());
        ctx.refresh();
        
        dataDir = new GeoServerDataDirectory(Files.createTempDirectory("data").toFile());
        dataDir.root().deleteOnExit();
    }
    
    @After
    public void tearDown() throws Exception {
        ctx.destroy();
    }
    
    @Test
    public void testParsedStyle() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();
        
        //Copy the sld to the temp style dir
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
        //Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic = ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(((ExternalGraphic) graphic).getLocation(), iconFile.toURI().toURL());
        
        //GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }
    
    @Test
    public void testParsedStyleExternalWithParams() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();
        
        //Copy the sld to the temp style dir
        File styleFile = new File(styleDir, "external_with_params.sld");
        Files.copy(this.getClass().getResourceAsStream("external_with_params.sld"), styleFile.toPath());
        
        File iconFile = new File(styleDir, "icon.png");
        assertFalse(iconFile.exists());
        
        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("sld");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());
        
        Style s = dataDir.parsedStyle(si);
        //Verify style is actually parsed correctly
        Symbolizer symbolizer = s.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        assertTrue(symbolizer instanceof PointSymbolizer);
        GraphicalSymbol graphic = ((PointSymbolizer) symbolizer).getGraphic().graphicalSymbols().get(0);
        assertTrue(graphic instanceof ExternalGraphic);
        assertEquals(((ExternalGraphic) graphic).getLocation().getPath(), iconFile.toURI().toURL().getPath());
        
        assertEquals("param1=1", ((ExternalGraphic) graphic).getLocation().getQuery());
        
        //GEOS-7025: verify the icon file is not created if it doesn't already exist
        assertFalse(iconFile.exists());
    }
    
    @Test
    public void testWKTLibPath() throws IOException {
        File styleDir = new File(dataDir.root(), "styles");
        styleDir.mkdir();
        
        File styleFile = new File(styleDir, "symbols.properties");
        Files.copy(this.getClass().getResourceAsStream("wktlib.properties"), styleFile.toPath());
        
        StyleInfoImpl si = new StyleInfoImpl(null);
        si.setName("");
        si.setId("");
        si.setFormat("properties");
        si.setFormatVersion(new Version("1.0.0"));
        si.setFilename(styleFile.getName());
        String stylePath = "";
        
        dataDir.get(si, stylePath);
        
        WKTMarkFactory wmf = new WKTMarkFactory();
        Literal exp = ff.literal(WKTMarkFactory.WKTLIB_PREFIX + "symbols.properties#block");        
        try {
            //GEOS-7639 check that a shape defined in .properties file is loaded
            assertNotNull(wmf.getShape(null, exp, feature));
        } catch (Exception e) {
            assertTrue(false);
        }
        
    }
}
