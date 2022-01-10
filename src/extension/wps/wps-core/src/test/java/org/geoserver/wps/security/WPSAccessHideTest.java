/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import org.geoserver.security.CatalogMode;
import org.junit.Test;
import org.w3c.dom.Document;

public class WPSAccessHideTest extends AbstractWPSAccessTest {

    // Capabilities

    @Test
    public void testNotAuthenticatedCapabilitiesPermission() throws Exception {
        setRequestAuth(null, null);
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        assertXpathEvaluatesTo("0", "count(//wps:Process[ows:Identifier = 'JTS:buffer'])", d);
        assertXpathEvaluatesTo("1", "count(//wps:Process[ows:Identifier = 'JTS:intersects'])", d);
    }

    @Test
    public void testAuthenticatedCapabilitiesPermission() throws Exception {
        setRequestAuth("test", "test");
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        assertXpathEvaluatesTo("1", "count(//wps:Process[ows:Identifier = 'JTS:buffer'])", d);
        assertXpathEvaluatesTo("1", "count(//wps:Process[ows:Identifier = 'JTS:intersects'])", d);
    }

    // Describe process

    @Test
    public void testNotAuthenticatedDescribeProcessPermission() throws Exception {
        setRequestAuth(null, null);
        Document d = getAsDOM("wps?service=wps&request=describeprocess&identifier=JTS:buffer");
        assertXpathEvaluatesTo(
                "0", "count(//ProcessDescription[ows:Identifier = 'JTS:buffer'])", d);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Exception[contains(ows:ExceptionText,'No such process: JTS:buffer')])",
                d);
    }

    @Test
    public void testAuthenticatedDescribeProcessPermission() throws Exception {
        setRequestAuth("test", "test");
        Document d = getAsDOM("wps?service=wps&request=describeprocess&identifier=JTS:buffer");
        assertXpathEvaluatesTo(
                "1", "count(//ProcessDescription[ows:Identifier = 'JTS:buffer'])", d);
    }

    // Execute process

    @Test
    public void testNotAuthenticatedExecutePermission() throws Exception {
        setRequestAuth(null, null);
        Document d = postAsDOM("wps", executeRequestXml);
        checkValidationErrors(d);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Exception[contains(ows:ExceptionText,'Unknown process JTS:buffer')])",
                d);
    }

    @Test
    public void testAuthenticatedExecutePermission() throws Exception {
        setRequestAuth("test", "test");
        Document d = postAsDOM("wps", executeRequestXml);
        checkValidationErrors(d);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessSucceeded", d);
        assertXpathExists(
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/gml:Polygon",
                d);
    }

    @Override
    protected CatalogMode getMode() {
        return CatalogMode.HIDE;
    }
}
