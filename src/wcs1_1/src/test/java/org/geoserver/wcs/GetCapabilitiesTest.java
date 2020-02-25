/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class GetCapabilitiesTest extends WCSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog cat = getCatalog();
        CoverageStoreInfo csi = cat.getCoverageStoreByName(SystemTestData.WORLD.getLocalPart());
        csi.setEnabled(false);
        cat.save(csi);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    // @Override
    // protected void populateDataDirectory(MockData dataDirectory) throws Exception {
    // super.populateDataDirectory(dataDirectory);
    //
    // }

    // @Override
    // protected String getDefaultLogConfiguration() {
    // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
    // }

    @Before
    public void revertTasmaniaDem() throws IOException {
        getTestData().addDefaultRasterLayer(TASMANIA_DEM, getCatalog());
    }

    @Test
    public void testGetBasic() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        BASEPATH + "?request=GetCapabilities&service=WCS&acceptversions=1.1.1");
        assertEquals("text/xml", response.getContentType());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);

        // make sure we provided the store values
        assertXpathEvaluatesTo(
                "TrueFalse",
                "/wcs:Capabilities/ows:OperationsMetadata"
                        + "/ows:Operation[@name=\"GetCoverage\"]/ows:Parameter/ows:AllowedValues",
                dom);

        // make sure the disabled coverage store is really disabled
        assertXpathEvaluatesTo("0", "count(//ows:Title[text()='World'])", dom);
    }

    @Test
    public void testSkipMisconfigured() throws Exception {
        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        // manually misconfigure one layer
        CoverageInfo cvInfo = getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        ReferencedEnvelope BBox = cvInfo.getLatLonBoundingBox();
        try {
            cvInfo.setLatLonBoundingBox(null);
            getCatalog().save(cvInfo);

            // check we got everything but that specific layer, and that the output is still schema
            // compliant
            Document dom =
                    getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&version=1.1.1");
            checkValidationErrors(dom, WCS11_SCHEMA);
            // print(dom);
            int count = getCatalog().getCoverages().size();
            assertEquals(count - 2, dom.getElementsByTagName("wcs:CoverageSummary").getLength());
        } finally {
            cvInfo.setLatLonBoundingBox(BBox);
            getCatalog().save(cvInfo);
        }
    }

    @Test
    public void testIgnoreWCS10Version() throws Exception {
        // ows 1.1 has no version param at all, it should be just ignored, that's for
        // wcs 1.0 negotiation
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&version=9.9.9");
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);

        // make sure we provided the store values
        assertXpathEvaluatesTo(
                "TrueFalse",
                "/wcs:Capabilities/ows:OperationsMetadata"
                        + "/ows:Operation[@name=\"GetCoverage\"]/ows:Parameter/ows:AllowedValues",
                dom);

        // make sure the disabled coverage store is really disabled
        assertXpathEvaluatesTo("0", "count(//ows:Title[text()='World'])", dom);
    }

    @Test
    public void testNamespaceFilter() throws Exception {
        // try to filter on an existing namespace
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&acceptversions=1.1.1&namespace=wcs");
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(
                xpath.getMatchingNodes("//wcs:CoverageSummary/ows:Title[starts-with(., wcs)]", dom)
                                .getLength()
                        > 0);
        assertEquals(
                0,
                xpath.getMatchingNodes(
                                "//wcs:CoverageSummary/ows:Title[not(starts-with(., wcs))]", dom)
                        .getLength());

        // now filter on a missing one
        dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&acceptversions=1.1.1&namespace=NoThere");
        e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());
        assertEquals(0, xpath.getMatchingNodes("//wcs:CoverageSummary", dom).getLength());
    }

    @Test
    public void testNoServiceContactInfo() throws Exception {
        // alter geoserver state so that there is no contact information
        getGeoServer().getGlobal().getSettings().setContact(new ContactInfoImpl());

        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS");
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
    }

    @Test
    public void testPostBasic() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
    }

    @Test
    public void testUnsupportedVersionPost() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                        + " xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <ows:AcceptVersions>" //
                        + "    <ows:Version>9.9.9</ows:Version>" //
                        + "  </ows:AcceptVersions>" //
                        + "</wcs:GetCapabilities>";
        Document dom = postAsDOM(BASEPATH, request);
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkOws11Exception(dom);
        assertEquals("ows:ExceptionReport", dom.getFirstChild().getNodeName());
        assertXpathEvaluatesTo(
                "VersionNegotiationFailed",
                "ows:ExceptionReport/ows:Exception/@exceptionCode",
                dom);
    }

    @Test
    public void testUnsupportedVersionGet() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&acceptVersions=9.9.9,8.8.8");
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkOws11Exception(dom);
        assertXpathEvaluatesTo(
                "VersionNegotiationFailed",
                "ows:ExceptionReport/ows:Exception/@exceptionCode",
                dom);
    }

    @Test
    public void testSupportedVersionGet() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&acceptVersions=0.5.0,1.1.1");
        assertEquals("wcs:Capabilities", dom.getFirstChild().getNodeName());
    }

    @Test
    public void testSupportedVersionPost() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                        + " xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <ows:AcceptVersions>" //
                        + "    <ows:Version>0.5.0</ows:Version>" //
                        + "    <ows:Version>1.1.1</ows:Version>" //
                        + "  </ows:AcceptVersions>" //
                        + "</wcs:GetCapabilities>";
        Document dom = postAsDOM(BASEPATH, request);
        assertEquals("wcs:Capabilities", dom.getFirstChild().getNodeName());
    }

    @Test
    public void testUpdateSequenceInferiorGet() throws Exception {
        Document dom =
                getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&updateSequence=-1");
        checkValidationErrors(dom, WCS11_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:Capabilities", root.getNodeName());
        assertTrue(root.getChildNodes().getLength() > 0);
    }

    @Test
    public void testUpdateSequenceInferiorPost() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                        + " xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " updateSequence=\"-1\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        checkValidationErrors(dom, WCS11_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:Capabilities", root.getNodeName());
        assertTrue(root.getChildNodes().getLength() > 0);
    }

    @Test
    public void testUpdateSequenceEqualsGet() throws Exception {
        long updateIndex = getGeoServer().getGlobal().getUpdateSequence();
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&updateSequence="
                                + Long.toString(updateIndex));
        checkValidationErrors(dom, WCS11_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:Capabilities", root.getNodeName());
        assertEquals(0, root.getChildNodes().getLength());
    }

    @Test
    public void testUpdateSequenceEqualsPost() throws Exception {
        long updateIndex = getGeoServer().getGlobal().getUpdateSequence();
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                        + " xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " updateSequence=\""
                        + Long.toString(updateIndex)
                        + "\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        checkValidationErrors(dom, WCS11_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:Capabilities", root.getNodeName());
        assertEquals(0, root.getChildNodes().getLength());
    }

    @Test
    public void testUpdateSequenceSuperiorGet() throws Exception {
        long updateIndex = getGeoServer().getGlobal().getUpdateSequence() + 1;
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&updateSequence="
                                + Long.toString(updateIndex));
        checkValidationErrors(dom, WCS11_SCHEMA);
        // print(dom);
        checkOws11Exception(dom);
    }

    @Test
    public void testUpdateSequenceSuperiorPost() throws Exception {
        long updateIndex = getGeoServer().getGlobal().getUpdateSequence() + 1;
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                        + " xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " updateSequence=\""
                        + Long.toString(updateIndex)
                        + "\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        checkValidationErrors(dom, WCS11_SCHEMA);
        // print(dom);
        checkOws11Exception(dom);
    }

    @Test
    public void testSectionsBogus() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&sections=Bogus");
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkOws11Exception(dom);
        assertXpathEvaluatesTo(
                WcsExceptionCode.InvalidParameterValue.toString(),
                "/ows:ExceptionReport/ows:Exception/@exceptionCode",
                dom);
    }

    @Test
    public void testSectionsAll() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&sections=All");
        checkValidationErrors(dom, WCS11_SCHEMA);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:Contents)", dom);
    }

    @Test
    public void testSchemaLocation() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS");
        checkValidationErrors(dom, WCS11_SCHEMA);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:Capabilities[contains(@xsi:schemaLocation,'http://www.opengis.net/wcs/1.1.1 ')])",
                dom);
    }

    @Test
    public void testOneSection() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH + "?request=GetCapabilities&service=WCS&sections=ServiceProvider");
        checkValidationErrors(dom, WCS11_SCHEMA);
        assertXpathEvaluatesTo("0", "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("0", "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("0", "count(//wcs:Contents)", dom);
    }

    @Test
    public void testTwoSection() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=GetCapabilities&service=WCS&sections=ServiceProvider,Contents");
        checkValidationErrors(dom, WCS11_SCHEMA);
        assertXpathEvaluatesTo("0", "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("0", "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:Contents)", dom);
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        int all = getCatalog().getCoverageStores().size() - 1;
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        assertEquals(all, xpath.getMatchingNodes("//wcs:CoverageSummary", dom).getLength());

        int some = getCatalog().getCoverageStoresByWorkspace("cdf").size();
        assertTrue(some < all);

        dom = getAsDOM("cdf/wcs?request=GetCapabilities&service=WCS");
        assertEquals(some, xpath.getMatchingNodes("//wcs:CoverageSummary", dom).getLength());
    }

    @Test
    public void testLayerQualified() throws Exception {
        int all = getCatalog().getCoverageStores().size() - 1;
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        assertEquals(all, xpath.getMatchingNodes("//wcs:CoverageSummary", dom).getLength());

        dom = getAsDOM("wcs/BlueMarble/wcs?request=GetCapabilities&service=WCS");
        assertEquals(1, xpath.getMatchingNodes("//wcs:CoverageSummary", dom).getLength());
    }

    @Test
    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            Document dom = getAsDOM("wcs?request=GetCapabilities");
            assertXpathExists("//wcs:CoverageSummary[ows:Title='DEM']", dom);

            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            dom = getAsDOM("wcs?request=GetCapabilities");
            assertXpathNotExists("//wcs:CoverageSummary[ows:Title='DEM']", dom);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }

    @Test
    public void testMetadataLink() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/tasmania/dem.xml");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        Document dom = getAsDOM("wcs?request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        String xpathBase =
                "//wcs:CoverageSummary[wcs:Identifier = '"
                        + TASMANIA_DEM.getPrefix()
                        + ":"
                        + TASMANIA_DEM.getLocalPart()
                        + "']/ows:Metadata";
        assertXpathEvaluatesTo("http://www.geoserver.org", xpathBase + "/@about", dom);
        assertXpathEvaluatesTo("simple", xpathBase + "/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "http://www.geoserver.org/tasmania/dem.xml", xpathBase + "/@xlink:href", dom);
    }

    @Test
    public void testMetadataLinksTransormToProxyBaseURL() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("/metadata?key=value");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        String proxyBaseUrl = getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
        Document dom = getAsDOM("wcs?request=GetCapabilities");
        checkValidationErrors(dom, WCS11_SCHEMA);
        String xpathBase =
                "//wcs:CoverageSummary[wcs:Identifier = '"
                        + TASMANIA_DEM.getPrefix()
                        + ":"
                        + TASMANIA_DEM.getLocalPart()
                        + "']/ows:Metadata";
        assertXpathEvaluatesTo("http://www.geoserver.org", xpathBase + "/@about", dom);
        assertXpathEvaluatesTo("simple", xpathBase + "/@xlink:type", dom);
        assertXpathEvaluatesTo(
                proxyBaseUrl + "/metadata?key=value", xpathBase + "/@xlink:href", dom);
    }

    @Test
    public void testNoMetadataTypeAttribute() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/tasmania/dem.xml");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        Document dom = getAsDOM("wcs?request=GetCapabilities");
        checkValidationErrors(dom, WCS11_SCHEMA);
        String xpathBase =
                "//wcs:CoverageSummary[wcs:Identifier = '"
                        + TASMANIA_DEM.getLocalPart()
                        + "']/ows:Metadata";
        assertXpathNotExists(xpathBase + "/@metadataType", dom);
    }
}
