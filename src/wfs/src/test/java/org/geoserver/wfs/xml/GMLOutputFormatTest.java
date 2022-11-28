/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathValuesEqual;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.wfs.v2_0.WFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GMLOutputFormatTest extends WFSTestSupport {

    private static final QName INVALID_CHARACTER =
            new QName(MockData.DEFAULT_URI, "invalid_character", MockData.DEFAULT_PREFIX);

    private static final QName INVALID_NAMESPACE =
            new QName("http://www.w3.org/1999/xhtml", "invalid_namespace", "xhtml");

    private static final QName INVALID_PREFIX =
            new QName("http://invalid/prefix", "BasicPolygons", "'");

    private int defaultNumDecimals = -1;
    private boolean defaultForceDecimal = false;
    private boolean defaultPadWithZeros = false;

    @Before
    public void saveDefaultFormattingOptions() {
        if (defaultNumDecimals < 0) {
            FeatureTypeInfo info =
                    getGeoServer()
                            .getCatalog()
                            .getResourceByName(
                                    new NameImpl(
                                            MockData.BASIC_POLYGONS.getPrefix(),
                                            MockData.BASIC_POLYGONS.getLocalPart()),
                                    FeatureTypeInfo.class);
            defaultNumDecimals = info.getNumDecimals();
            defaultForceDecimal = info.getForcedDecimal();
            defaultPadWithZeros = info.getPadWithZeros();
        }
    }

    @After
    public void restoreDefaultFormattingOptions() {
        FeatureTypeInfo info =
                getGeoServer()
                        .getCatalog()
                        .getResourceByName(
                                new NameImpl(
                                        MockData.BASIC_POLYGONS.getPrefix(),
                                        MockData.BASIC_POLYGONS.getLocalPart()),
                                FeatureTypeInfo.class);
        info.setNumDecimals(defaultNumDecimals);
        info.setForcedDecimal(defaultForceDecimal);
        info.setPadWithZeros(defaultPadWithZeros);
    }

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        super.setUpInternal(testData);
        testData.addVectorLayer(
                INVALID_CHARACTER, Collections.emptyMap(), getClass(), getCatalog());
        testData.addWorkspace(
                INVALID_NAMESPACE.getPrefix(), INVALID_NAMESPACE.getNamespaceURI(), getCatalog());
        testData.addVectorLayer(
                INVALID_NAMESPACE, Collections.emptyMap(), getClass(), getCatalog());
        testData.addWorkspace(
                INVALID_PREFIX.getPrefix(), INVALID_PREFIX.getNamespaceURI(), getCatalog());
        testData.addVectorLayer(
                INVALID_PREFIX, Collections.emptyMap(), MockData.class, getCatalog());
    }

    @Test
    public void testGML2() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNotNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull(getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML2CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml2&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "-2.0000,-1.0000 2.0000,6.0000",
                dom.getElementsByTagName("gml:coordinates").item(0).getTextContent());
    }

    private void enableCoordinatesFormatting() {
        FeatureTypeInfo info =
                getGeoServer()
                        .getCatalog()
                        .getResourceByName(
                                new NameImpl(
                                        MockData.BASIC_POLYGONS.getPrefix(),
                                        MockData.BASIC_POLYGONS.getLocalPart()),
                                FeatureTypeInfo.class);
        info.setNumDecimals(4);
        info.setForcedDecimal(true);
        info.setPadWithZeros(true);
        getGeoServer().getCatalog().save(info);
    }

    @Test
    public void testGML2GZIP() throws Exception {
        //        InputStream input = get(
        // "wfs?request=getfeature&version=1.0.0&outputFormat=gml2-gzip&typename=" +
        //            MockData.BASIC_POLYGONS.getPrefix() + ":" +
        // MockData.BASIC_POLYGONS.getLocalPart());
        //        GZIPInputStream zipped = new GZIPInputStream( input );
        //
        //        Document dom = dom( zipped );
        //        zipped.close();
        //
        //        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        //        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        //        assertNull( getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML3() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));

        dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
        assertNull(getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull(getFirstElementByTagName(dom, "gml:exterior"));
    }

    @Test
    public void testGML3CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml3&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "-1.0000 0.0000 0.0000 1.0000 1.0000 0.0000 0.0000 -1.0000 -1.0000 0.0000",
                dom.getElementsByTagName("gml:posList").item(0).getTextContent());
    }

    @Test
    public void testGML32() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(WFS.NAMESPACE, dom.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
    }

    @Test
    public void testGML32CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "0.0000 -1.0000 1.0000 0.0000 0.0000 1.0000 -1.0000 0.0000 0.0000 -1.0000",
                dom.getElementsByTagName("gml:posList").item(0).getTextContent());
    }

    @Test
    public void testGML2InvalidElementName() throws Exception {
        testInvalidResponse(INVALID_CHARACTER, 2, "INVALID_CHARACTER_ERR");
    }

    @Test
    public void testGML2InvalidNamespaceUri() throws Exception {
        testInvalidResponse(INVALID_NAMESPACE, 2, "NAMESPACE_ERR");
    }

    @Test
    public void testGML2InvalidNamespacePrefix() throws Exception {
        testInvalidResponse(INVALID_PREFIX, 2, "INVALID_CHARACTER_ERR");
    }

    @Test
    public void testGML3InvalidElementName() throws Exception {
        testInvalidResponse(INVALID_CHARACTER, 3, "INVALID_CHARACTER_ERR");
    }

    @Test
    public void testGML3InvalidNamespaceUri() throws Exception {
        testInvalidResponse(INVALID_NAMESPACE, 3, "NAMESPACE_ERR");
    }

    @Test
    public void testGML3InvalidNamespacePrefix() throws Exception {
        testInvalidResponse(INVALID_PREFIX, 3, "INVALID_CHARACTER_ERR");
    }

    @Test
    public void testGML32InvalidElementName() throws Exception {
        testInvalidResponse(INVALID_CHARACTER, 32, "INVALID_CHARACTER_ERR");
    }

    @Test
    public void testGML32InvalidNamespaceUri() throws Exception {
        testInvalidResponse(INVALID_NAMESPACE, 32, "NAMESPACE_ERR");
    }

    @Test
    public void testGML32InvalidNamespacePrefix() throws Exception {
        testInvalidResponse(INVALID_PREFIX, 32, "INVALID_CHARACTER_ERR");
    }

    private void testInvalidResponse(QName layer, int version, String message) throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&version=1.0.0&outputFormat=gml"
                                + version
                                + "&typename="
                                + layer.getPrefix()
                                + ":"
                                + layer.getLocalPart());
        assertXpathValuesEqual("1", "count(/ogc:ServiceExceptionReport/ogc:ServiceException)", dom);
        String text = dom.getElementsByTagName("ServiceException").item(0).getTextContent();
        assertThat(text, containsString(message));
    }
}
