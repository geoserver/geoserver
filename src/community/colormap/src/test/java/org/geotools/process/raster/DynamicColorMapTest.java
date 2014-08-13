package org.geotools.process.raster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DynamicColorMapTest extends GeoServerSystemTestSupport {

    private static final String COVERAGE_NAME = "watertemp_dynamic";

    private static final double TOLERANCE = 0.01;

    GetMapKvpRequestReader requestReader;

    protected static Catalog catalog;
    protected static XpathEngine xp;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        //addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        URL zip = getClass().getResource("test-data/watertemp_dynamic.zip");
        InputStream is = null;
        byte[] bytes;
        try {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        URL style = getClass().getResource("test-data/style_rgb.sld");
        
        GeoServerResourceLoader loader = catalog.getResourceLoader();
        final String dataDir = loader.getBaseDirectory().getAbsolutePath();
        try {
            is = style.openStream();
            org.geoserver.data.util.IOUtils.copy(is, new File(dataDir + "/styles/style_rgb.sld"));
        } finally {
            IOUtils.closeQuietly(is);
        }

        // Check if the gs workspace exists
        WorkspaceInfo ws = catalog.getWorkspaceByName("gs");
        if (ws==null){
            throw new IllegalArgumentException("Workspace not present");
        }

        MockHttpServletResponse response = putAsServletResponse(
                "rest/workspaces/gs/coveragestores/watertemp_dynamic/file.imagemosaic", bytes,
                "application/zip");
        assertEquals(201, response.getStatusCode());
        
        // Configuring read as immediate instead of using JAI
//        LayerInfo layer = catalog.getLayerByName("watertemp_dynamic");
//        MetadataMap metadata = layer.getMetadata();
//        Map<String, Serializable> map = metadata.getMap();
//        String xml = 
//                "<coverage>" +
//                  "<name>watertemp_dynamic</name>" +
//                  " <parameters> <entry>" + 
//      " <string>USE_JAI_IMAGEREAD</string> " +
//      " <string>true</string>" +
//    " </entry> "+
//    " </parameters>" + 
//                "</coverage>";
//            MockHttpServletResponse responseUpdate =
//                putAsServletResponse("rest/workspaces/gs/coveragestores/watertemp_dynamic/coverages/watertemp_dynamic", xml, "text/xml");
//            assertEquals(200, responseUpdate.getStatusCode());
    }

    protected final void setUpUsers(Properties props) {
    }

    protected final void setUpLayerRoles(Properties properties) {
    }

    @Before
    @Ignore
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testGridCoverageStats() throws Exception {
       
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(COVERAGE_NAME);
        assertNotNull(storeInfo);
        CoverageInfo ci = catalog.getCoverageByName(COVERAGE_NAME);
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Test on the GridCoverageStats
        FilterFunction_gridCoverageStats funcStat = new FilterFunction_gridCoverageStats();

        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);
        double min = (Double) funcStat.evaluate(gridCoverage, "minimum");
        double max = (Double) funcStat.evaluate(gridCoverage, "maximum");
        assertEquals(min, 13.1369, TOLERANCE);
        assertEquals(max, 20.665, TOLERANCE);
        ImageIOUtilities.disposeImage(gridCoverage.getRenderedImage());
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/gs/coveragestores/watertemp_dynamic?recurse=true");
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testSvgColorMapFilterFunctionRGB() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap = (ColorMap) func.evaluate("rgb(0,0,255);rgb(0,255,0);rgb(255,0,0)", 10, 100);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        check(entries);
      }

    @Test
    public void testSvgColorMapFilterFunctionHEX() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap = (ColorMap) func.evaluate("#0000FF;#00FF00;#FF0000", 10, 100);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        check(entries);
      }

    private void check(ColorMapEntry[] entries) {
        assertEquals(5, entries.length);
        assertEquals(9.99, Double.parseDouble(entries[0].getQuantity().toString()),TOLERANCE);
        assertEquals(10.0, Double.parseDouble(entries[1].getQuantity().toString()),TOLERANCE);
        assertEquals(55.0, Double.parseDouble(entries[2].getQuantity().toString()),TOLERANCE);
        assertEquals(100.0, Double.parseDouble(entries[3].getQuantity().toString()),TOLERANCE);
        assertEquals(100.01, Double.parseDouble(entries[4].getQuantity().toString()),TOLERANCE);

        assertEquals("#FF", entries[0].getColor().toString().toUpperCase());
        assertEquals("#FF", entries[1].getColor().toString().toUpperCase());
        assertEquals("#FF00", entries[2].getColor().toString().toUpperCase());
        assertEquals("#FF0000", entries[3].getColor().toString().toUpperCase());
        assertEquals("#FF0000", entries[4].getColor().toString().toUpperCase());

        assertEquals(0.0, Double.parseDouble(entries[0].getOpacity().toString()), TOLERANCE);
        assertEquals(1.0, Double.parseDouble(entries[1].getOpacity().toString()), TOLERANCE);
        assertEquals(1.0, Double.parseDouble(entries[2].getOpacity().toString()), TOLERANCE);
        assertEquals(1.0, Double.parseDouble(entries[3].getOpacity().toString()), TOLERANCE);
        assertEquals(0.0, Double.parseDouble(entries[4].getOpacity().toString()), TOLERANCE);
    }
}
