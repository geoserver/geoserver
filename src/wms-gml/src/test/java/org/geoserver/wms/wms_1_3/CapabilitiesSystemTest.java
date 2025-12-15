/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WMS 1.3 GetCapabilities system tests, following clauses in section 7.2 of <a
 * href="http://portal.opengeospatial.org/files/?artifact_id=14416">OGC document 06-042, OpenGIS Web Map Service (WMS)
 * Implementation Specification</a> as requirements specification.
 *
 * <p>These are system tests in the sense that they verify specific system requirements as stated in the various
 * specification clauses. Yet they base on the same integration testing framework established for other GeoServer tests
 * in order to be able of mocking up a given configuration and hence not needing a fully functional instance running
 * inside a servlet container in order to do some minimal system testing.
 *
 * <p>For tests that do not check adherence to specific spec clauses but that verify integration aspects under different
 * configuration situations use {@link CapabilitiesIntegrationTest} instead.
 *
 * @author Gabriel Roldan
 */
public class CapabilitiesSystemTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");

        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    /**
     * Tests that GeoServer performs the server side of version number negotiation as defined in section 6.2.4
     *
     * <p>This tests assumes the only versions supported are 1.1.1 and 1.3.0, and shall be modified accordingly whenever
     * support for another version is added, or support for one of the existing versions is removed
     */
    @Test
    public void testRequestVersionNumberNegotiation() throws Exception {
        /*
         * In response to a GetCapabilities request (for which the VERSION parameter is optional)
         * that does not specify a version number, the server shall respond with the highest version
         * it supports.
         */
        Document dom = getAsDOM("ows?service=WMS&request=GetCapabilities");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * A specific version is requested, the server responds with that specific version iif
         * supported
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.1.1");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);

        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.3.0");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * If a version unknown to the server and higher than the lowest supported version is
         * requested, the server shall send the highest version it supports that is less than the
         * requested version.
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.1.2");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);

        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.4.0");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * If a version lower than any of those known to the server is requested, then the server
         * shall send the lowest version it supports.
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.0.0");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);
    }
}
