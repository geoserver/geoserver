/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DownloadMapProcessTest extends WPSTestSupport {

    protected static final QName WATERTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    public static QName GIANT_POLYGON = new QName(MockData.CITE_URI, "giantPolygon", MockData.CITE_PREFIX);
    protected static final String UNITS = "foot";
    protected static final String UNIT_SYMBOL = "ft";
    public static final String SAMPLES = "src/test/resources/org/geoserver/wps/gs/download/";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // disable entity resolver as it won't let the tests run in IntelliJ if also GeoTools is loaded...
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setXmlExternalEntitiesEnabled(true);
        getGeoServer().save(global);
        
        // add water temperature
        testData.addStyle("temperature", "temperature.sld", DownloadMapProcess.class, catalog);
        Map propertyMap = new HashMap();
        propertyMap.put(SystemTestData.LayerProperty.STYLE,"temperature");
        testData.addRasterLayer(WATERTEMP, "watertemp.zip", null, propertyMap, SystemTestData.class, catalog);
        
        // a world covering layer with no dimensions
        testData.addVectorLayer(GIANT_POLYGON, Collections.EMPTY_MAP, "giantPolygon.properties",
                SystemTestData.class, getCatalog());

        // setup dimensions for water temp
        setupRasterDimension(WATERTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATERTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        // add a decoration layout
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        FileUtils.copyURLToFile(getClass().getResource("watermarker.xml"), new File(layouts, "watermarker.xml"));
        FileUtils.copyURLToFile(getClass().getResource("geoserver.png"), new File(layouts, "geoserver.png"));

    }

    @Test
    public void testExecuteSingleLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimple.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES +  "mapSimple.png"), image, 100);
    }

    @Test
    public void testExecuteSingleLayerFilter() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimpleFilter.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleFilter.png"), image, 100);
    }

    @Test
    public void testExecuteSingleDecorated() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimpleDecorated.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES +  "watermarked.png"), image, 100);
    }

    @Test
    public void testExecuteMultiName() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiName.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }

    @Test
    public void testExecuteMultiLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        // not a typo, the output should indeed be the same as testExecuteMultiName
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }
    
    @Test
    public void testTimeFilter() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapTimeFilter.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        // same test as DimensionRasterGetMapTest#testTime
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));

        // making extra sure
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilter.png"), image, 100);
    }
    
}
