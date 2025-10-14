/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCapabilities;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.util.GrowableInternationalString;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Test for {@link GetCapabilities}
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCapabilitiesTest extends WCSTestSupport {

    @Before
    public void cleanupLimitedSRS() {
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().clear();
        getGeoServer().save(service);
    }

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");

        checkFullCapabilitiesDocument(dom);
    }

    @Test
    public void testCase() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void testLimitedSRS() throws Exception {
        // check we support a lot of SRS by default
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        NodeList list =
                xpath.getMatchingNodes("//wcs:ServiceMetadata/wcs:Extension/crs:CrsMetadata/crs:crsSupported", dom);
        assertTrue(list.getLength() > 1000);

        // setup limited list
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().add("4326");
        service.getSRS().add("32632");
        getGeoServer().save(service);

        dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        list = xpath.getMatchingNodes("//wcs:ServiceMetadata/wcs:Extension/crs:CrsMetadata/crs:crsSupported", dom);
        assertEquals(2, list.getLength());
    }

    @Test
    public void testSectionsBogus() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&sections=Bogus");
        Element root = dom.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName());
        assertEquals("2.0.0", root.getAttribute("version"));
        assertEquals("http://www.opengis.net/ows/2.0", root.getAttribute("xmlns:ows"));
        assertXpathEvaluatesTo(
                WcsExceptionCode.InvalidParameterValue.toString(),
                "/ows:ExceptionReport/ows:Exception/@exceptionCode",
                dom);
    }

    @Test
    public void testSectionsAll() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&sections=All");
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:Contents)", dom);
    }

    @Test
    public void testAcceptVersions20() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&acceptversions=2.0.1");

        // make sure no exception is thrown
        assertXpathEvaluatesTo("0", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("0", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "0", "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])", dom);
        assertXpathEvaluatesTo("0", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void testAcceptVersions11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCapabilities&service=WCS&acceptversions=1.1.0");
        assertEquals("text/xml", response.getContentType());

        // xmlunit is not setup to parse WCS 1.1.1 XML, use string checks
        assertThat(
                response.getContentAsString(),
                allOf(
                        containsString("<wcs:Capabilities"),
                        containsString("version=\"1.1.1\""),
                        containsString("xmlns:ows=\"http://www.opengis.net/ows/1.1\""),
                        containsString("<wcs:CoverageSummary")));
    }

    @Test
    public void testAcceptVersions10() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCapabilities&service=WCS&acceptversions=1.0.0");
        assertEquals("application/xml", response.getContentType());

        // xmlunit is not setup to parse WCS 1.0.0 XML, use string checks
        assertThat(
                response.getContentAsString(),
                allOf(
                        containsString("<wcs:WCS_Capabilities"),
                        containsString("version=\"1.0.0\""),
                        containsString("xmlns:ows=\"http://www.opengis.net/ows/1.1\""),
                        containsString("<wcs:CoverageOfferingBrief")));
    }

    @Test
    public void testMetadata() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        ci.setTitle("My Title");
        ci.setAbstract("My Abstract");
        ci.getKeywords().add(0, new Keyword("my_keyword"));
        MetadataLinkInfo mdl1 = catalog.getFactory().createMetadataLink();
        mdl1.setContent("http://www.geoserver.org/tasmania/dem.xml");
        mdl1.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl1);
        MetadataLinkInfo mdl2 = catalog.getFactory().createMetadataLink();
        mdl2.setContent("/metadata?key=value");
        mdl2.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl2);
        catalog.save(ci);
        Document dom = getAsDOM("wcs?service=WCS&version=2.0.1&request=GetCapabilities");
        // print(dom);

        checkValidationErrors(dom, getWcs20Schema());
        String base = "//wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = '";
        base += getLayerId(TASMANIA_DEM).replace(":", "__") + "']/";
        assertXpathEvaluatesTo("My Title", base + "ows:Title", dom);
        assertXpathEvaluatesTo("My Abstract", base + "ows:Abstract", dom);
        assertXpathEvaluatesTo("4", "count(" + base + "ows:Keywords/ows:Keyword)", dom);
        assertXpathEvaluatesTo("my_keyword", base + "ows:Keywords/ows:Keyword[1]", dom);
        assertXpathEvaluatesTo("2", "count(" + base + "ows:Metadata)", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[1]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[1]/@xlink:type", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org/tasmania/dem.xml", base + "ows:Metadata[1]/@xlink:href", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[2]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[2]/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "src/test/resources/geoserver/metadata?key=value", base + "ows:Metadata[2]/@xlink:href", dom);
    }

    @Test
    public void testAcceptLanguagesParameter() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        ci.setInternationalTitle(title);

        GrowableInternationalString abstractInfo = new GrowableInternationalString();
        abstractInfo.add(Locale.ENGLISH, "abstract");
        abstractInfo.add(Locale.ITALIAN, "italiano abstract");
        ci.setInternationalAbstract(abstractInfo);
        catalog.save(ci);

        Document dom = getAsDOM("wcs?service=WCS&version=2.0.1&request=GetCapabilities&AcceptLanguages=it");

        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/wcs?Language=it&", "//ows:DCP/ows:HTTP/ows:Get/@xlink:href", dom);
    }

    @Test
    public void testNullLocale() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(null, "null locale");
        ci.setInternationalTitle(title);

        catalog.save(ci);

        Document dom = getAsDOM("wcs?service=WCS&version=2.0.1&request=GetCapabilities");

        assertXpathEvaluatesTo("src/test/resources/geoserver/wcs?", "//ows:DCP/ows:HTTP/ows:Get/@xlink:href", dom);
    }
}
