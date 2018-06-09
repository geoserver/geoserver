/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

public class GMLOutputFormatTest extends WFSTestSupport {

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
}
