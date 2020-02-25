/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WPSTestSupport {

    @Test
    public void testGetBasic() throws Exception { // Standard Test A.4.2.1
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        // print(d);
        basicCapabilitiesTest(d, null);
    }

    @Test
    public void testGetDisabledService() throws Exception {
        GeoServer gs = getGeoServer();
        WPSInfo info = gs.getService(WPSInfo.class);
        try {
            info.setEnabled(false);
            gs.save(info);
            Document dom = getAsDOM("wps?service=wps&request=getcapabilities");
            // print(dom);
            checkOws10Exception(dom);
        } finally {
            info.setEnabled(true);
            gs.save(info);
        }
    }

    @Test
    public void testInvalidWPSConfig() throws Exception {
        GeoServer gs = getGeoServer();
        WPSInfo wpsInfo = gs.getService(WPSInfo.class);

        // Simulates the config failing to load due to xstream error GEOS-7903
        gs.getFacade().remove(wpsInfo);
        try {
            Document dom = getAsDOM("wps?service=wps&request=getcapabilities");
            checkOws11Exception(dom);
        } finally {
            gs.getFacade().add(wpsInfo);
        }
    }

    @Test
    public void testGetBasicWorkspaceQualified() throws Exception {
        // this one did not report the workspace specific urls
        Document d = getAsDOM(MockData.CITE_PREFIX + "/ows?service=wps&request=getcapabilities");
        // print(d);

        basicCapabilitiesTest(d, MockData.CITE_PREFIX);

        // this one used to fail with a 404
        d = getAsDOM(MockData.CITE_PREFIX + "/wps?service=wps&request=getcapabilities");
        basicCapabilitiesTest(d, MockData.CITE_PREFIX);
    }

    @Test
    public void testProcesseListSorted() throws Exception { // Standard Test A.4.2.1
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        // print(d);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        int count = Integer.valueOf(xpath.evaluate("count(wps:Process)", d));
        String previous = null;
        for (int i = 1; i <= count; i++) {
            String curr = xpath.evaluate("(//wps:Process)[" + i + "]/ows:Identifier)", d);
            if (previous != null) {
                assertTrue(curr.compareTo(previous) >= 0);
            }
            previous = curr;
        }
    }

    @Test
    public void testPostBasic() throws Exception { // Standard Test A.4.2.2
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document d = postAsDOM(root(), request);
        basicCapabilitiesTest(d, null);
    }

    @Test
    public void testBasicGetAcceptVersions() throws Exception { // Standard Test A.4.2.3
        Document d = getAsDOM("wps?service=wps&request=getcapabilities&AcceptVersions=1.0.0");
        // print(d);
        basicCapabilitiesTest(d, null);
    }

    @Test
    public void testBasicGetLanguage() throws Exception { // Standard Test A.4.2.3
        Document d = getAsDOM("wps?service=wps&request=getcapabilities&language=en-US");
        // print(d);
        basicCapabilitiesTest(d, null);
    }

    @Test
    public void testBasicPostAcceptVersions() throws Exception { // Standard Test A.4.2.3
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "<ows:AcceptVersions>"
                        + "<ows:Version>1.0.0</ows:Version>"
                        + "</ows:AcceptVersions>"
                        + "</wps:GetCapabilities>";
        Document d = postAsDOM(root(), request);
        basicCapabilitiesTest(d, null);
    }

    @Test
    public void testBasicPostLanguage() throws Exception { // Standard Test A.4.2.3
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "language=\"en-US\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document d = postAsDOM(root(), request);
        basicCapabilitiesTest(d, null);
    }

    private void basicCapabilitiesTest(Document d, String workspace) throws Exception {
        // print(d);
        checkValidationErrors(d);

        // check the schema location is declared
        assertXpathEvaluatesTo(
                "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd",
                "/wps:Capabilities/@xsi:schemaLocation",
                d);

        assertEquals("wps:Capabilities", d.getDocumentElement().getNodeName());
        int np = d.getElementsByTagName("wps:Process").getLength();
        assertTrue(np > 0);

        // check the operation links
        String expectedOperationUrl = "http://localhost:8080/geoserver/wps";
        if (workspace != null) {
            expectedOperationUrl = "http://localhost:8080/geoserver/" + workspace + "/wps";
        }
        String[] operations = new String[] {"GetCapabilities", "DescribeProcess", "Execute"};
        for (String operation : operations) {
            String getPath =
                    "//ows:Operation[@name='"
                            + operation
                            + "']/ows:DCP/ows:HTTP/ows:Get/@xlink:href";
            assertXpathEvaluatesTo(expectedOperationUrl, getPath, d);
            String postPath =
                    "//ows:Operation[@name='"
                            + operation
                            + "']/ows:DCP/ows:HTTP/ows:Post/@xlink:href";
            assertXpathEvaluatesTo(expectedOperationUrl, postPath, d);
        }
    }

    @Test
    public void testUnsupportedVersionHighPost() throws Exception { // Standard Test A.4.2.5
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <ows:AcceptVersions>" //
                        + "    <ows:Version>9.9.9</ows:Version>" //
                        + "  </ows:AcceptVersions>" //
                        + "</wps:GetCapabilities>";
        Document dom = postAsDOM(root(), request);

        checkValidationErrors(dom);
        checkOws11Exception(dom, "VersionNegotiationFailed");
    }

    @Test
    public void testUnsupportedVersionLowPost() throws Exception { // Standard Test A.4.2.5
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <ows:AcceptVersions>" //
                        + "    <ows:Version>0.0.9</ows:Version>" //
                        + "  </ows:AcceptVersions>" //
                        + "</wps:GetCapabilities>";
        Document dom = postAsDOM(root(), request);

        checkValidationErrors(dom);
        checkOws11Exception(dom, "VersionNegotiationFailed");
    }

    @Test
    public void testUnsupportedVersionGet() throws Exception { // Standard Test A.4.2.5
        Document dom =
                getAsDOM(root() + "request=GetCapabilities&service=WPS&acceptVersions=9.9.9,8.8.8");

        checkValidationErrors(dom);
        checkOws11Exception(dom, "VersionNegotiationFailed");
    }

    @Test
    public void testSupportedVersionGet() throws Exception { // Standard Test A.4.2.5
        Document dom =
                getAsDOM(root() + "request=GetCapabilities&service=WPS&acceptVersions=0.5.0,1.0.0");
        assertEquals("wps:Capabilities", dom.getFirstChild().getNodeName());
    }

    @Test
    public void testSupportedVersionPost() throws Exception { // Standard Test A.4.2.5
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <ows:AcceptVersions>" //
                        + "    <ows:Version>0.5.0</ows:Version>" //
                        + "    <ows:Version>1.0.0</ows:Version>" //
                        + "  </ows:AcceptVersions>" //
                        + "</wps:GetCapabilities>";
        Document dom = postAsDOM(root(), request);
        // print( dom );
        assertEquals("wps:Capabilities", dom.getFirstChild().getNodeName());
    }

    /* TODO Update Sequence tests
        public void testUpdateSequenceInferiorGet() throws Exception { // Standard Test A.4.2.6
            Document dom = getAsDOM(root() + "request=GetCapabilities&service=WPS&updateSequence=-1");
            basicCapabilitiesTest(dom);
        }

        public void testUpdateSequenceInferiorPost() throws Exception { // Standard Test A.4.2.6
            String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                    + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " updateSequence=\"-1\"/>";
            Document dom = postAsDOM(root(), request);
            basicCapabilitiesTest(dom);
        }

        public void testUpdateSequenceEqualsGet() throws Exception { // Standard Test A.4.2.6
            Document dom = getAsDOM(root() + "request=GetCapabilities&service=WPS&updateSequence=0");
            checkValidationErrors(dom);
            final Node root = dom.getFirstChild();
            assertEquals("wps:Capabilities", root.getNodeName());
            assertEquals(0, root.getChildNodes().getLength());
        }

        public void testUpdateSequenceEqualsPost() throws Exception { // Standard Test A.4.2.6
            String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                    + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " updateSequence=\"0\"/>";
            Document dom = postAsDOM(root(), request);
            checkValidationErrors(dom);
            final Node root = dom.getFirstChild();
            assertEquals("wcs:Capabilities", root.getNodeName());
            assertEquals(0, root.getChildNodes().getLength());
        }

        public void testUpdateSequenceSuperiorGet() throws Exception { // Standard Test A.4.2.6
            Document dom = getAsDOM(root() + "request=GetCapabilities&service=WPS&updateSequence=1");
            checkValidationErrors(dom);
    //        print(dom);
            checkOws11Exception(dom);
        }

        public void testUpdateSequenceSuperiorPost() throws Exception { // Standard Test A.4.2.6
            String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                    + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " updateSequence=\"1\"/>";
            Document dom = postAsDOM(root(), request);
            checkValidationErrors(dom);
    //        print(dom);
            checkOws11Exception(dom);
        }
        */

    /* TODO Language Negotiation tests
    public void testGetLanguageGood() throws Exception { // Standard Test A.4.2.6
        Document d = getAsDOM( "wps?service=wps&request=getcapabilities&language=en-US" );
        print(d);
        basicCapabilitiesTest(d);
    }

    public void testGetLanguageBad() throws Exception { // Standard Test A.4.2.6
        Document d = getAsDOM( "wps?service=wps&request=getcapabilities&language=zz-ZZ" );
        print(d);
        basicCapabilitiesTest(d);
    }

    public void testPostLanguageGod() throws Exception { // Standard Test A.4.2.6
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "language=\"en-US\" "
                + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document d = postAsDOM(root(), request);
        basicCapabilitiesTest(d);
    }

    public void testPostLanguageBad() throws Exception { // Standard Test A.4.2.6
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:GetCapabilities service=\"WPS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "language=\"zz-ZZ\" "
                + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document d = postAsDOM(root(), request);
        basicCapabilitiesTest(d);
    }
    */
}
