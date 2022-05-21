/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.wfs.v2_0.WFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GMLOutputFormatTest extends WFSTestSupport {
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
}
