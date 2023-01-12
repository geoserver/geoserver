/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.integration;

import static org.junit.Assert.assertEquals;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests version negotiation for GetCapabilities Requests (KVP). */
public class VersionNegotiationKVPIntegrationTest extends GeoServerSystemTestSupport {

    /**
     * Executes a GetCapabilities request with the given version and acceptsVersion. this goes to
     * the "ows" endpoint
     */
    public Document doGetCapOWS(String version, String acceptVersions, String service)
            throws Exception {
        String url = "ows?service=" + service + "&request=GetCapabilities";
        if (version != null) url += "&version=" + version;
        if (acceptVersions != null) url += "&acceptVersions=" + acceptVersions;

        Document doc = getAsDOM(url);
        return doc;
    }

    public Document doGetCapURL(String url) throws Exception {
        Document doc = getAsDOM(url);
        return doc;
    }

    /**
     * Executes a GetCapabilities request with the given version and acceptsVersion. this goes to
     * the service specific endpoint (.../wfs?...)
     */
    public Document doGetCapService(String version, String acceptVersions, String service)
            throws Exception {
        String url = service.toLowerCase() + "?service=" + service + "&request=GetCapabilities";
        if (version != null) url += "&version=" + version;
        if (acceptVersions != null) url += "&acceptVersions=" + acceptVersions;

        Document doc = getAsDOM(url);
        return doc;
    }

    /**
     * test when they don't put a service=... in the request (but use the "service" endpoint)
     *
     * @throws Exception
     */
    @Test
    public void testServiceNoService() throws Exception {
        // wfs
        String url = "wfs?request=GetCapabilities&version=1.0.0";
        Document doc = doGetCapURL(url);
        assertVersion(doc, "1.0.0");

        url = "wfs?request=GetCapabilities&acceptVersions=2.0.0";
        doc = doGetCapURL(url);
        assertVersion(doc, "2.0.0");

        // wms
        url = "wms?request=GetCapabilities&version=1.3.0";
        doc = doGetCapURL(url);
        assertVersion(doc, "1.3.0");

        url = "wms?request=GetCapabilities&acceptVersions=1.1.1";
        doc = doGetCapURL(url);
        assertVersion(doc, "1.1.1");

        // wcs
        url = "wcs?request=GetCapabilities&version=1.1.1";
        doc = doGetCapURL(url);
        assertVersion(doc, "1.1.1");

        url = "wcs?request=GetCapabilities&acceptVersions=1.1.1";
        doc = doGetCapURL(url);
        assertVersion(doc, "1.1.1");
    }

    /**
     * checks that the capabilities document has an attribute called "version" with the correct
     * version number.
     */
    public static void assertVersion(Document doc, String version) {
        assertEquals(version, doc.getDocumentElement().getAttribute("version"));
    }

    /**
     * Tests that setting an exact `version=...` in the GetCapabilities requests works. OGC 1.1.0
     * spec - "If the server implements the requested version number, the server must send that
     * version. "
     */
    @Test
    public void testExactVersionOWS() throws Exception {
        Document doc;

        // --wfs
        doc = doGetCapOWS("1.0.0", null, "wfs");
        assertVersion(doc, "1.0.0");

        doc = doGetCapOWS("1.1.0", null, "wfs");
        assertVersion(doc, "1.1.0");

        doc = doGetCapOWS("2.0.0", null, "wfs");
        assertVersion(doc, "2.0.0");

        // wcs
        doc = doGetCapOWS("2.0.1", null, "WCS");
        assertVersion(doc, "2.0.1");

        doc = doGetCapOWS("1.1.0", null, "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapOWS("1.1.1", null, "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapOWS("1.0.0", null, "WCS");
        assertVersion(doc, "1.0.0");

        // wms
        doc = doGetCapOWS("1.3.0", null, "wms");
        assertVersion(doc, "1.3.0");

        doc = doGetCapOWS("1.1.1", null, "wms");
        assertVersion(doc, "1.1.1");

        // wmts
        doc = doGetCapURL("gwc/service/wmts?service=WMTS&request=GetCapabilities");
        assertVersion(doc, "1.0.0");

        // tms
        doc = doGetCapURL("gwc/service/tms/1.0.0");
        assertVersion(doc, "1.0.0");

        // wmts-c
        doc = doGetCapURL("gwc/service/wms?request=GetCapabilities&version=1.1.1&tiled=true");
        assertVersion(doc, "1.1.1");
    }

    /**
     * Tests that setting an exact `version=...` in the GetCapabilities requests works. OGC 1.1.0
     * spec - "If the server implements the requested version number, the server must send that
     * version. "
     */
    @Test
    public void testExactVersionService() throws Exception {
        Document doc;

        // --wfs
        doc = doGetCapService("1.0.0", null, "wfs");
        assertVersion(doc, "1.0.0");

        doc = doGetCapService("1.1.0", null, "wfs");
        assertVersion(doc, "1.1.0");

        doc = doGetCapService("2.0.0", null, "wfs");
        assertVersion(doc, "2.0.0");

        // wcs
        doc = doGetCapService("2.0.1", null, "WCS");
        assertVersion(doc, "2.0.1");

        doc = doGetCapService("1.1.0", null, "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapService("1.1.1", null, "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapService("1.0.0", null, "WCS");
        assertVersion(doc, "1.0.0");

        // wms
        doc = doGetCapService("1.3.0", null, "wms");
        assertVersion(doc, "1.3.0");

        doc = doGetCapService("1.1.1", null, "wms");
        assertVersion(doc, "1.1.1");
    }

    /**
     * Tests that setting a single `AcceptVersions=...` in the GetCapabilities requests works (to
     * OWS endpoint)
     */
    @Test
    public void testSingleAcceptVersionsOWS() throws Exception {
        Document doc;

        // --wfs
        doc = doGetCapOWS(null, "1.0.0", "wfs");
        assertVersion(doc, "1.0.0");

        doc = doGetCapOWS(null, "1.1.0", "wfs");
        assertVersion(doc, "1.1.0");

        doc = doGetCapOWS(null, "2.0.0", "wfs");
        assertVersion(doc, "2.0.0");

        // wcs
        doc = doGetCapOWS(null, "2.0.1", "WCS");
        assertVersion(doc, "2.0.1");

        doc = doGetCapOWS(null, "1.1.0", "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapOWS(null, "1.1.1", "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapOWS(null, "1.0.0", "WCS");
        assertVersion(doc, "1.0.0");

        // wms
        doc = doGetCapOWS(null, "1.3.0", "wms");
        assertVersion(doc, "1.3.0");

        doc = doGetCapOWS(null, "1.1.1", "wms");
        assertVersion(doc, "1.1.1");
    }

    /**
     * Tests that setting a single `AcceptVersions=...` in the GetCapabilities requests works (to
     * service endpoint)
     */
    @Test
    public void testSingleAcceptVersionsService() throws Exception {
        Document doc;

        // --wfs
        doc = doGetCapService(null, "1.0.0", "wfs");
        assertVersion(doc, "1.0.0");

        doc = doGetCapService(null, "1.1.0", "wfs");
        assertVersion(doc, "1.1.0");

        doc = doGetCapService(null, "2.0.0", "wfs");
        assertVersion(doc, "2.0.0");

        // wcs
        doc = doGetCapService(null, "2.0.1", "WCS");
        assertVersion(doc, "2.0.1");

        doc = doGetCapService(null, "1.1.0", "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapService(null, "1.1.1", "WCS");
        assertVersion(doc, "1.1.1");

        doc = doGetCapService(null, "1.0.0", "WCS");
        assertVersion(doc, "1.0.0");

        // wms
        doc = doGetCapService(null, "1.3.0", "wms");
        assertVersion(doc, "1.3.0");

        doc = doGetCapService(null, "1.1.1", "wms");
        assertVersion(doc, "1.1.1");
    }

    @Test
    public void testAcceptVersions() throws Exception {
        Document doc;

        // --wfs with non-known (choose the one that exists)
        doc = doGetCapService(null, "1.0.0,6.6.6", "wfs");
        assertVersion(doc, "1.0.0");
        doc = doGetCapService(null, "6.6.6,1.0.0", "wfs");
        assertVersion(doc, "1.0.0");
        doc = doGetCapService(null, "6.6.6,1.0.0,6.6.6", "wfs");
        assertVersion(doc, "1.0.0");

        // choose first
        doc = doGetCapService(null, "1.0.0,2.0.0", "wfs");
        assertVersion(doc, "1.0.0");

        // choose first
        doc = doGetCapService(null, "2.0.0,1.0.0", "wfs");
        assertVersion(doc, "2.0.0");

        // choose best one below requested
        doc = doGetCapService(null, "1.5.0", "wfs");
        assertVersion(doc, "1.1.0");
    }

    @Test
    public void testVersion() throws Exception {
        Document doc;

        doc = doGetCapService("0.9.0", null, "wfs");
        assertVersion(doc, "1.0.0");

        doc = doGetCapService(null, null, "wfs");
        assertVersion(doc, "2.0.0");
    }
}
