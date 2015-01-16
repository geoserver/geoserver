/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.filter.IllegalFilterException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * This test class simply ensures that disabling ADVANCED PROJECTION HANDLING will not return a blank image
 * 
 * @author Nicola Lagomarsini
 */
public class RenderedImageMapOutputFormatExtendedTest extends WMSTestSupport {

    private static final QName MOSAIC_HOLES = new QName(MockData.SF_URI, "mosaic_holes",
            MockData.SF_PREFIX);

    @BeforeClass
    public static void setup() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "false");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(MOSAIC_HOLES, "mosaic_holes.zip", null, properties,
                RenderedImageMapOutputFormatExtendedTest.class, getCatalog());
    }

    @Test
    public void testMosaicNoProjection() throws IOException, IllegalFilterException, Exception {
        // Request
        MockHttpServletResponse response = getAsServletResponse("wms?BBOX=6.40284375,36.385494140625,12.189662109375,42.444494140625"
                + "&styles=&layers=sf:mosaic_holes&Format=image/png"
                + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        checkImage(response);
    }

    @AfterClass
    public static void after() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "true");
    }
}
