/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.integration;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests version negotiation for GetCapabilities Requests (XML post). */
public class VersionNegotiationXMLIntegrationTest extends GeoServerSystemTestSupport {

    /** */
    public Document doPOSTService(String service, String xml) throws Exception {
        String url = service.toLowerCase();
        Document doc = postAsDOM(url, xml);
        return doc;
    }

    public Document doPOSTOWS(String xml) throws Exception {
        String url = "ows";
        Document doc = postAsDOM(url, xml);
        return doc;
    }

    /**
     * NOTE: cf testWFS111 CITE tests use these two xml documents to get 1.1.0 and 1.0.0
     * capabilities documents. The only difference is the version=1.0.0. However, this is an
     * optional element. This is the CITE expectation.
     */
    @Test
    public void testWFS110() throws Exception {
        String xml =
                "<GetCapabilities\n"
                        + "         service=\"WFS\"\n"
                        + "         xmlns=\"http://www.opengis.net/wfs\"\n"
                        + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "/>";
        Document doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.0");
    }

    /**
     * NOTE: cf testWFS110 CITE tests use these two xml documents to get 1.1.0 and 1.0.0
     * capabilities documents. The only difference is the version=1.0.0. However, this is an
     * optional element. This is the CITE expectation.
     */
    @Test
    public void testWFS100() throws Exception {
        String xml =
                "<GetCapabilities\n"
                        + "         service=\"WFS\"\n"
                        + "         version=\"1.0.0\"\n"
                        + "         xmlns=\"http://www.opengis.net/wfs\"\n"
                        + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "/>";
        Document doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");
    }

    /**
     * note - this has a different schema namespace than the 1.0.0/1.1.1 requests
     *
     * @throws Exception
     */
    @Test
    public void testWFS200() throws Exception {
        String xml =
                "<GetCapabilities service=\"WFS\" \n"
                        + "   xmlns=\"http://www.opengis.net/wfs/2.0\"\n"
                        + "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n";
        Document doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.0");
    }
    /** taken from cite wfs20 */
    @Test
    public void testWFSAcceptVersion() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wfs:GetCapabilities  service=\"WFS\" "
                        + "    xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" "
                        + "    xmlns:ows=\"http://www.opengis.net/ows\" "
                        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + ">\n"
                        + "<ows:AcceptVersions>\n"
                        + "    <ows:Version>1.1.0</ows:Version>\n"
                        + "    <ows:Version>2.0.0</ows:Version>\n"
                        + "  </ows:AcceptVersions>\n"
                        + "</wfs:GetCapabilities>";
        Document doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.0");

        xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wfs:GetCapabilities  service=\"WFS\" xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\n"
                        + "<ows:AcceptVersions>\n"
                        + "    <ows:Version>10.0.0</ows:Version>\n"
                        + "    <ows:Version>2.0.0</ows:Version>\n"
                        + "    <ows:Version>1.1.0</ows:Version>\n"
                        + "  </ows:AcceptVersions>\n"
                        + "</wfs:GetCapabilities>";

        doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.0");

        xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wfs:GetCapabilities  service=\"WFS\" xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\n"
                        + "<ows:AcceptVersions>\n"
                        + "    <ows:Version>1.0.0</ows:Version>\n"
                        + "    <ows:Version>1.1.0</ows:Version>\n"
                        + "  </ows:AcceptVersions>\n"
                        + "</wfs:GetCapabilities>";

        doc = doPOSTService("wfs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");
    }

    @Test
    public void testWCS201() throws Exception {
        String xml =
                "<wcs:GetCapabilities  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xmlns:ows=\"http://www.opengis.net/ows/2.0\"\n"
                        + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                        + "  service=\"WCS\">\n"
                        + "  <ows:AcceptVersions>\n"
                        + "  <ows:Version>2.0.1</ows:Version>\n"
                        + "  </ows:AcceptVersions>\n"
                        + "</wcs:GetCapabilities>";
        Document doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.1");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "2.0.1");
    }

    @Test
    public void testWCS111() throws Exception {
        String xml =
                " <GetCapabilities xmlns=\"http://www.opengis.net/wcs/1.1.1\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd\" service = \"WCS\">\n"
                        + "                  <ows:AcceptVersions>\n"
                        + "                    <ows:Version>1.1.1</ows:Version>\n"
                        + "                  </ows:AcceptVersions>\n"
                        + "                </GetCapabilities>";
        Document doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        xml =
                " <GetCapabilities xmlns=\"http://www.opengis.net/wcs/1.1.1\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd\" service = \"WCS\">\n"
                        + "                  <ows:AcceptVersions>\n"
                        + "                    <ows:Version>0.0.0</ows:Version>\n"
                        + "                    <ows:Version>1.1.1</ows:Version>\n"
                        + "                  </ows:AcceptVersions>\n"
                        + "                </GetCapabilities>";
        doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");
    }

    // note - GS returns a 1.1.1 document for a 1.1.0 request
    @Test
    public void testWCS110() throws Exception {
        String xml =
                " <GetCapabilities xmlns=\"http://www.opengis.net/wcs/1.1.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"   service = \"WCS\">\n"
                        + "                  <ows:AcceptVersions>\n"
                        + "                    <ows:Version>1.1.0</ows:Version>\n"
                        + "                  </ows:AcceptVersions>\n"
                        + "                </GetCapabilities>";
        Document doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        xml =
                "<GetCapabilities xmlns=\"http://www.opengis.net/wcs/1.1.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd\" service = \"WCS\">\n"
                        + "                  <ows:AcceptVersions>\n"
                        + "                    <ows:Version>0.0.0</ows:Version>\n"
                        + "                    <ows:Version>1.1.0</ows:Version>\n"
                        + "                  </ows:AcceptVersions>\n"
                        + "                </GetCapabilities>";

        doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.1.1");
    }

    // from cite - no acceptVersions testing (old spec)
    @Test
    public void testWCS100() throws Exception {
        String xml =
                "<GetCapabilities xmlns=\"http://www.opengis.net/wcs\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd\" version=\"1.0.0\" service=\"WCS\">\n"
                        + "</GetCapabilities>";
        Document doc = doPOSTService("wcs", xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");

        doc = doPOSTOWS(xml);
        VersionNegotiationKVPIntegrationTest.assertVersion(doc, "1.0.0");
    }
}
