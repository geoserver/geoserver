/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Dynaminc dimensions differ for custom ones in that the reader does not know about the until it's
 * generated, while simple custom dimensions are hard coded and known to the Format class as well
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DynamicDimensionsTest extends WMSTestSupport {

    private static final QName WATTEMP =
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);
    private static final String DIMENSION_NAME = "WAVELENGTH";
    private static final String CAPABILITIES_REQUEST = "wms?request=getCapabilities&version=1.1.1";
    private static final String BBOX = "0,40,15,45";
    private static final String LAYERS = "gs:watertemp";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add org.geoserver.catalog.testReader.CustomFormat coverage
        String styleName = "temperature";
        testData.addStyle(styleName, "../temperature.sld", getClass(), getCatalog());
        Map propertyMap = new HashMap();
        propertyMap.put(LayerProperty.STYLE, "temperature");
        testData.addRasterLayer(
                WATTEMP,
                "watertempDynamicDims.zip",
                null,
                propertyMap,
                SystemTestData.class,
                getCatalog());

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

    @After
    public void removeRasterDimensions() {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        info.getMetadata().remove(ResourceInfo.CUSTOM_DIMENSION_PREFIX + DIMENSION_NAME);
        getCatalog().save(info);
    }

    @Test
    public void testCapabilitiesNoDimension() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);

        // check layer is present with no dimension
        assertXpathEvaluatesTo("1", "count(//Layer[Name='gs:watertemp'])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Dimension)", dom);
    }

    @Test
    public void testCapabilities() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST, null, null);
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);

        // check we have the extent
        assertXpathEvaluatesTo(DIMENSION_NAME, "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("020", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("020,100", "//Layer/Extent", dom);
    }

    @Test
    public void testGetMapInvalidValue() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST, "nano meters", "nm");

        // check that we get no data when requesting an incorrect value for custom dimension
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + BBOX
                                + "&styles="
                                + "&layers="
                                + LAYERS
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&DIM_"
                                + DIMENSION_NAME
                                + "=bad_dimension_value");
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertTrue(isEmpty(image));
    }

    @Test
    public void testGetMapDefaultValue() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST, "nano meters", "nm");

        // check that we get data when requesting a correct value for custom dimension
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + BBOX
                                + "&styles="
                                + "&layers="
                                + LAYERS
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertFalse(isEmpty(image));
        // this pixel is red-ish with the default value, 020, but black with 100
        assertPixel(image, 337, 177, new Color(255, 197, 197));
    }

    @Test
    public void testGetMapValidValue() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST, "nano meters", "nm");

        // check that we get data when requesting a correct value for custom dimension
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + BBOX
                                + "&styles="
                                + "&layers="
                                + LAYERS
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&bgColor=0x0000FF"
                                + "&DIM_"
                                + DIMENSION_NAME
                                + "=100");
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertFalse(isEmpty(image));
        // this pixel is red-ish with the default value, 020, but blue with 100
        assertPixel(image, 337, 177, Color.BLUE);
    }

    @Test
    public void testGetMapCaseInsensitiveKey() throws Exception {
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST, "nano meters", "nm");

        // check that we get data when requesting a correct value for custom dimension
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + BBOX
                                + "&styles="
                                + "&layers="
                                + LAYERS
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&bgColor=0x0000FF"
                                + "&DIM_wAvElEnGtH=100");
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertFalse(isEmpty(image));
        // this pixel is red-ish with the default value, 020, but blue with 100
        assertPixel(image, 337, 177, Color.BLUE);
    }

    private void setupRasterDimension(
            String metadata, DimensionPresentation presentation, String units, String unitSymbol) {
        CoverageInfo info = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        di.setUnits(units);
        di.setUnitSymbol(unitSymbol);
        info.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + metadata, di);

        final Map customParameters = new HashMap();
        info.getParameters()
                .put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), Boolean.FALSE);
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
