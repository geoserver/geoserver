/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.style.PaletteStyleHandler;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class NcwmsIntegrationTest extends WMSTestSupport {
    
    QName RAIN = new QName(MockTestData.CITE_URI, "rain", MockTestData.CITE_PREFIX);
    String GRAY_BLUE_STYLE = "grayToBlue";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // only the data we register manually is needed
    }
    
    // @Override
    // protected String getLogConfiguration() {
    // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
    // }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(RAIN, "rain.tif", ".tif", null, NcwmsIntegrationTest.class, getCatalog());
        testData.addStyle(getCatalog().getDefaultWorkspace(), GRAY_BLUE_STYLE, "grayToBlue.palette", 
                NcwmsIntegrationTest.class, getCatalog(), Collections.singletonMap(StyleProperty.FORMAT, PaletteStyleHandler.FORMAT));
        
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(RAIN));
        ci.getDimensions().get(0).setRange(NumberRange.create(0d, 7000d));
        getCatalog().save(ci);
    }
    
    @Test
    public void testPlain() throws Exception {
        BufferedImage image = getAsImage(requestBase(), "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(37, 37, 236));
        // mid value here
        assertPixel(image, 120, 74, new Color(129, 129, 191));
        // dry here
        assertPixel(image, 160, 60, new Color(170, 170, 170));
    }
    
    @Test
    public void testOpacity() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&OPACITY=50", "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(37, 37, 236, 128));
        // mid value here
        assertPixel(image, 120, 74, new Color(129, 129, 191, 128));
        // dry here
        assertPixel(image, 160, 60, new Color(170, 170, 170, 128));
    }
    
    @Test
    public void testLogarithmic() throws Exception {
        // the log application skews the map a lot towards the blues
        BufferedImage image = getAsImage(requestBase() + "&COLORSCALERANGE=1,6000&LOGSCALE=true", "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(3, 3, 253));
        // mid value here
        assertPixel(image, 120, 74, new Color(25, 25, 243));
        // dry here
        assertPixel(image, 160, 60, new Color(91, 91, 209));
    }
    
    @Test
    public void testRange() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&COLORSCALERANGE=100,5000", "image/png");
        // RenderedImageBrowser.showChain(image);
        // cut away both low and high
        // heavy rain here
        assertPixelIsTransparent(image, 32, 74);
        // almost dry here
        assertPixelIsTransparent(image, 160, 60);
    }
    
    @Test
    public void testBeforeAfterColor() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&COLORSCALERANGE=100,4000&BELOWMINCOLOR=0xFF0000&ABOVEMAXCOLOR=0x00FF00", "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // cut away both low and high
        // heavy rain here
        assertPixel(image, 32, 74, Color.GREEN);
        // dry here
        assertPixel(image, 160, 60, Color.RED);
    }

    @Test
    public void testNumberColors() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&NUMCOLORBANDS=3", "image/png");
        assertUniqueColorCount(image, 3);
    }

    private String requestBase() {
        return "wms?service=WMS&version=1.1.1&request=GetMap&layers=" + getLayerId(RAIN) + "&styles=" + GRAY_BLUE_STYLE + "&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&transparent=true&width=320&height=160";
    }
    
    private void assertUniqueColorCount(BufferedImage image, int count) {
        int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        Set<Integer> colors = new HashSet<>();
        for (int i = 0; i < rgb.length; i++) {
            int curr = rgb[i];
            colors.add(curr);
        }
        
        assertEquals("Expected amount of colors not found", count, colors.size());
    }
}
