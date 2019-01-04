/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetPropertyValueTest extends WFS20TestSupport {

    @Test
    public void testPOST() throws Exception {
        String xml =
                "<wfs:GetPropertyValue service='WFS' version='2.0.0' "
                        + "xmlns:sf='"
                        + MockData.SF_URI
                        + "'    "
                        + "xmlns:fes='http://www.opengis.net/fes/2.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' valueReference='pointProperty'> "
                        + "<wfs:Query typeNames='sf:PrimitiveGeoFeature'/> "
                        + "</wfs:GetPropertyValue>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "3", "count(//wfs:member/sf:pointProperty/gml:Point)", dom);
    }

    @Test
    public void testGET() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetPropertyValue"
                                + "&typeNames=sf:PrimitiveGeoFeature&valueReference=pointProperty");

        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "3", "count(//wfs:member/sf:pointProperty/gml:Point)", dom);
    }

    @Test
    public void testGETAlternateNamespace() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetPropertyValue"
                                + "&typeNames=abcd:PrimitiveGeoFeature&valueReference=pointProperty&namespaces=xmlns(abcd,"
                                + MockData.SF_URI
                                + ")");

        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "3", "count(//wfs:member/sf:pointProperty/gml:Point)", dom);
    }

    @Test
    public void testEmptyValueReference() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetPropertyValue"
                                + "&typeNames=sf:PrimitiveGeoFeature&valueReference=");

        checkOws11Exception(dom, "2.0.0", "InvalidParameterValue", "valueReference");
    }

    @Test
    public void testGmlId() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetPropertyValue"
                                + "&typeNames=sf:PrimitiveGeoFeature&valueReference=@gml:id");
        print(dom);

        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("5", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo("5", "count(//wfs:member/gml:identifier)", dom);
    }
}
