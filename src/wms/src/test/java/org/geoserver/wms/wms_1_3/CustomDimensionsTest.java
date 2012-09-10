/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class CustomDimensionsTest extends WMSTestSupport {
    
    private static final QName WATTEMP = new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);

    private static final String CAPABILITIES_REQUEST = "wms?request=getCapabilities&version=1.3.0";
    private static final String DIMENSION_NAME = CustomFormat.CUSTOM_DIMENSION_NAME;
    private static final String BBOX = "0,40,15,45";
    private static final String LAYERS = "gs:watertemp";
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        
        // add org.geoserver.catalog.testReader.CustomFormat coverage
        URL style = getClass().getResource("../temperature.sld");
        String styleName = "temperature";
        dataDirectory.addStyle(styleName, style);
        dataDirectory.addCoverageFromZip(WATTEMP, TestData.class.getResource("custwatertemp.zip"),
                        null, styleName);
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("", "http://www.opengis.net/wms");
        namespaces.put("wms", "http://www.opengis.net/wms");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    public void testCapabilitiesNoDimension() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);

        // check layer is present with no dimension
        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='gs:watertemp'])", dom);
        assertXpathEvaluatesTo("0", "count(//wms:Layer/wms:Dimension)", dom);
    }
    
    public void testCapabilities() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST);
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);        
        assertXpathEvaluatesTo(DIMENSION_NAME, "//wms:Layer/wms:Dimension/@name", dom);
        
        // check we have the dimension values
        assertXpathEvaluatesTo("CustomDimValueA,CustomDimValueB", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testGetMap() throws Exception {

        // check that we get no data when requesting an incorrect value for custom dimension
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + BBOX + "&styles="
                + "&layers=" + LAYERS + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&VALIDATESCHEMA=true"
                + "&DIM_" + DIMENSION_NAME + "=bad_dimension_value");
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertTrue(isEmpty(image));
        
        // check that we get data when requesting a correct value for custom dimension
        response = getAsServletResponse("wms?bbox=" + BBOX + "&styles="
                + "&layers=" + LAYERS + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&VALIDATESCHEMA=true"
                + "&DIM_" + DIMENSION_NAME + "=CustomDimValueB");
        image = ImageIO.read(getBinaryInputStream(response));
        assertFalse(isEmpty(image));
    }
    
    private void setupRasterDimension(String metadata, DimensionPresentation presentation) {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }
    
    private static boolean isEmpty(BufferedImage image) {
        final DataBuffer pixData = image.getRaster().getDataBuffer();
        final int bankCount = pixData.getNumBanks();
        final int size = pixData.getSize();
        for (int i = 0; i < bankCount; i++) {
            for (int j = 0; j < size; j++) {
                if (pixData.getElem(i, j) != 0xFF) return false;
            }
        }
        return true;
    }
}
