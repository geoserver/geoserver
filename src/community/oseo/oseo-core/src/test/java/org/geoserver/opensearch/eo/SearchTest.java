/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.opensearch.eo.response.DescriptionResponse;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQL;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

public class SearchTest extends OSEOTestSupport {

    @Test
    public void testAllCollection() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("oseo/search?httpAccept=" + AtomSearchResponse.MIME);
        assertEquals(AtomSearchResponse.MIME, response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("5")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/at:author/at:name", equalTo("GeoServer")));
        assertThat(dom, hasXPath("/at:feed/at:updated"));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:link[@rel='search']/@href",
                        equalTo("http://localhost:8080/geoserver/oseo/search/description")));

        assertNoResults(dom);

        // check entries
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("5")));
        // ... sorted by date
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SAS1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[3]/at:title", equalTo("gsTestCollection")));
        assertThat(dom, hasXPath("/at:feed/at:entry[4]/at:title", equalTo("SENTINEL1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[5]/at:title", equalTo("LANDSAT8")));

        // check the sentinel2 one
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/at:id",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/search?uid=SENTINEL2&httpAccept=application%2Fatom%2Bxml")));
        assertThat(
                dom, hasXPath("/at:feed/at:entry[2]/at:updated", equalTo("2016-02-26T09:20:21Z")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/dc:date",
                        equalTo("2015-07-01T08:20:21Z/2016-02-26T09:20:21Z")));
        // ... mind the lat/lon order
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/georss:where/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                        equalTo("89 -179 89 179 -89 179 -89 -179 89 -179")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[2]/georss:box", equalTo("-89.0 -179.0 89.0 179.0")));
        // ... the links (self, metadata, search)
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/at:link[@rel='self' and  @type='application/atom+xml']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/search?uid=SENTINEL2&httpAccept=application%2Fatom%2Bxml")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/at:link[@rel='alternate' and @type='application/vnd.iso.19139+xml']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/metadata?uid=SENTINEL2&httpAccept=application%2Fvnd.iso.19139%2Bxml")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/at:link[@rel='search' and @type='"
                                + DescriptionResponse.OS_DESCRIPTION_MIME
                                + "']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/description?parentId=SENTINEL2")));

        // check the html description (right one, and param substitution in links
        XPath xPath = getXPath();
        String summary = xPath.compile("/at:feed/at:entry[2]/at:summary").evaluate(dom);
        assertThat(summary, containsString("Sentinel-2"));
        // parse html using JSoup (DOM not usable, HTML is not valid/well formed XML in general
        org.jsoup.nodes.Document sd = Jsoup.parse(summary);
        String isoHRef = sd.select("a[title=ISO format]").attr("href");
        assertThat(
                isoHRef,
                equalTo(
                        "http://localhost:8080/geoserver/oseo/metadata?uid=SENTINEL2&httpAccept=application%2Fvnd.iso.19139%2Bxml"));
        String atomHRef = sd.select("a[title=ATOM format]").attr("href");
        assertThat(
                atomHRef,
                equalTo(
                        "http://localhost:8080/geoserver/oseo/search?uid=SENTINEL2&httpAccept=application%2Fatom%2Bxml"));

        // check owc:offering
        assertThat(dom, hasXPath("count(/at:feed/at:entry/owc:offering)", equalTo("3")));
        // single offering check
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/owc:offering/@code",
                        equalTo("http://www.opengis.net/spec/owc/1.0/req/atom/wms")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/owc:offering/owc:operation/@code",
                        equalTo("GetCapabilities")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/owc:offering/owc:operation/@method", equalTo("GET")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/owc:offering/owc:operation/@type",
                        equalTo("application/xml")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[2]/owc:offering/owc:operation/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities")));

        // overall schema validation for good measure
        checkValidAtomFeed(dom);
    }

    @Test
    public void testAllCollectionCountZero() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("oseo/search?count=0&httpAccept=" + AtomSearchResponse.MIME);
        assertEquals(AtomSearchResponse.MIME, response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("5")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("0")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='0']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/at:author/at:name", equalTo("GeoServer")));
        assertThat(dom, hasXPath("/at:feed/at:updated"));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:link[@rel='search']/@href",
                        equalTo("http://localhost:8080/geoserver/oseo/search/description")));

        // check no entries
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("0")));
    }

    @Test
    public void testOgcLinksOuterJoin() throws Exception {
        // remove one OGC link
        DataStore dataStore =
                (DataStore) getCatalog().getDataStoreByName("oseo_jdbc").getDataStore(null);
        SimpleFeatureStore store =
                (SimpleFeatureStore) dataStore.getFeatureSource("collection_ogclink");
        store.removeFeatures(CQL.toFilter("href like '%landsat8%'"));

        // run request, we should get 3 feeds but only two links
        MockHttpServletResponse response =
                getAsServletResponse("oseo/search?httpAccept=" + AtomSearchResponse.MIME);
        assertEquals(AtomSearchResponse.MIME, response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);

        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("5")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry/owc:offering)", equalTo("2")));
    }

    protected XPath getXPath() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(namespaceContext);
        return xPath;
    }

    @Test
    public void testPagingNoResults() throws Exception {
        // first page
        Document dom = getAsDOM("oseo/search?uid=UnknownIdentifier");
        assertNoResults(dom);
    }

    private void assertNoResults(Document dom) {
        assertHasLink(dom, "self", 1, 10);
        assertHasLink(dom, "first", 1, 10);
        assertHasLink(dom, "last", 1, 10);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
    }

    @Test
    public void testPagingFullPages() throws Exception {
        // first page
        Document dom = getAsDOM("oseo/search?count=1");
        assertHasLink(dom, "self", 1, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "next", 2, 1);
        assertHasLink(dom, "last", 5, 1);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SAS1")));

        // second page
        dom = getAsDOM("oseo/search?count=1&startIndex=2");
        assertHasLink(dom, "self", 2, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 1, 1);
        assertHasLink(dom, "next", 3, 1);
        assertHasLink(dom, "last", 5, 1);
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));

        // third page
        dom = getAsDOM("oseo/search?count=1&startIndex=3");
        assertHasLink(dom, "self", 3, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 2, 1);
        assertHasLink(dom, "next", 4, 1);
        assertHasLink(dom, "last", 5, 1);
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("gsTestCollection")));

        // fourth page
        dom = getAsDOM("oseo/search?count=1&startIndex=4");
        assertHasLink(dom, "self", 4, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 3, 1);
        assertHasLink(dom, "next", 5, 1);
        assertHasLink(dom, "last", 5, 1);
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL1")));

        // fifth and last page
        dom = getAsDOM("oseo/search?count=1&startIndex=5");
        assertHasLink(dom, "self", 5, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 4, 1);
        assertHasLink(dom, "last", 5, 1);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
    }

    @Test
    public void testPagingPartialPages() throws Exception {
        // first page
        Document dom = getAsDOM("oseo/search?count=2");
        assertHasLink(dom, "self", 1, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "next", 3, 2);
        assertHasLink(dom, "last", 5, 2);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SAS1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL2")));

        // second page
        dom = getAsDOM("oseo/search?count=2&startIndex=3");
        print(dom);
        assertHasLink(dom, "self", 3, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "previous", 1, 2);
        assertHasLink(dom, "last", 5, 2);
        assertHasLink(dom, "next", 5, 2);
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("gsTestCollection")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL1")));

        // third and last page
        dom = getAsDOM("oseo/search?count=2&startIndex=5");
        print(dom);
        assertHasLink(dom, "self", 5, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "previous", 3, 2);
        assertHasLink(dom, "last", 5, 2);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
    }

    @Test
    public void testGeoUidCollectionQuery() throws Exception {
        Document dom = getAsDOM("oseo/search?uid=LANDSAT8&httpAccept=" + AtomSearchResponse.MIME);
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@geo:uid='LANDSAT8']"));

        assertNoResults(dom);

        // check entries
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));

        // overall schema validation for good measure
        checkValidAtomFeed(dom);
    }

    private void assertHasLink(Document dom, String rel, int startIndex, int count) {
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']"));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:link[@rel='" + rel + "']/@href",
                        containsString("startIndex=" + startIndex)));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:link[@rel='" + rel + "']/@href",
                        containsString("count=" + count)));
    }

    @Test
    public void testProductById() throws Exception {
        Document dom =
                getAsDOM(
                        "oseo/search?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04&httpAccept="
                                + AtomSearchResponse.MIME);
        // print(dom);

        // check that filtering worked and offerings have been properly grouped
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:id",
                        containsString(
                                "S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04")));

        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:title",
                        equalTo("S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry/owc:offering)", equalTo("3")));
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry/owc:offering[@code='http://www.opengis.net/spec/owc/1.0/req/atom/wcs']/owc:operation)",
                        equalTo("2")));
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry/owc:offering[@code='http://www.opengis.net/spec/owc/1.0/req/atom/wms']/owc:operation)",
                        equalTo("2")));
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry/owc:offering[@code='http://www.opengis.net/spec/owc/1.0/req/atom/wmts']/owc:operation)",
                        equalTo("2")));
    }

    @Test
    public void testAllSentinel2Products() throws Exception {
        Document dom =
                getAsDOM("oseo/search?parentId=SENTINEL2&httpAccept=" + AtomSearchResponse.MIME);
        // print(dom);

        assertFirstPageSentinel2Products(dom);
    }

    @Test
    public void testAllSentinel2EmptyParams() throws Exception {
        // a URL generated by a client following the opensearch template to the letter (without
        // omitting the keys for missing params like OpenSearch for EO suggests to)
        Document dom =
                getAsDOM(
                        "oseo/search?parentId=SENTINEL2&searchTerms=&startIndex=&count=&uid=&box="
                                + "&name=&lat=&lon=&radius=&geometry=&geoRelation="
                                + "&timeStart=&timeEnd=&timeRelation=&illuminationAzimuthAngle="
                                + "&illuminationZenithAngle=&illuminationElevationAngle=&resolution=&identifier="
                                + "&productQualityDegradationStatus=&archivingCenter=&parentIdentifier="
                                + "&productionStatus=&acquisitionSubtype=&acquisitionType=&productQualityStatus="
                                + "&processorName=&orbitDirection=&processingCenter=&sensorMode=&processingMode="
                                + "&swathIdentifier=&creationDate=&modificationDate=&processingDate="
                                + "&availabilityTime=&acquisitionStation=&orbitNumber=&track=&frame="
                                + "&startTimeFromAscendingNode=&completionTimeFromAscendingNode="
                                + "&cloudCover=&snowCover=&httpAccept=atom");
        // print(dom);

        assertFirstPageSentinel2Products(dom);
    }

    private void assertFirstPageSentinel2Products(Document dom) {
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("19")));

        // check that filtering worked
        assertThat(dom, hasXPath("/at:feed/at:entry/at:title", startsWith("S2A")));
        assertThat(dom, not(hasXPath("/at:feed/at:entry[at:title='S1A']")));
        assertThat(dom, not(hasXPath("/at:feed/at:entry[at:title='LS08']")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:link[@rel='search']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/search/description?parentId=SENTINEL2")));

        // there are two products only with links, verify, three offerings each
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04']/owc:offering)",
                        equalTo("3")));
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01']/owc:offering)",
                        equalTo("3")));

        // there are two products with download links
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04']/at:link[@rel='enclosure']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/scihub/sentinel2/S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04.zip")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04']/at:link[@rel='enclosure']/@type",
                        equalTo("application/zip")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01']/at:link[@rel='enclosure']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/scihub/sentinel2/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01.zip")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry[at:title='S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01']/at:link[@rel='enclosure']/@type",
                        equalTo("application/octet-stream"))); // this one has no type in the
        // database
        // just two enclosure links, the other products have nothing that can be downloaded
        assertThat(
                dom, hasXPath("count(/at:feed/at:entry/at:link[@rel='enclosure'])", equalTo("2")));
    }

    @Test
    public void testAllSentinel2ProductsCountZero() throws Exception {
        Document dom =
                getAsDOM(
                        "oseo/search?parentId=SENTINEL2&count=0&httpAccept="
                                + AtomSearchResponse.MIME);
        // print(dom);

        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("19")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("0")));

        // there are two products only with links, verify, three offerings each
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("0")));
    }

    @Test
    public void testSpecificProduct() throws Exception {
        Document dom =
                getAsDOM(
                        "oseo/search?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04&httpAccept="
                                + AtomSearchResponse.MIME);
        // print(dom);

        // check basics
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:title",
                        equalTo("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04")));
        assertThat(
                dom, hasXPath("/at:feed/at:entry/at:updated", equalTo("2017-03-08T17:54:21.026Z")));
        assertThat(dom, hasXPath("/at:feed/at:entry/dc:date", equalTo("2017-03-08T17:54:21.026Z")));

        // ... the links (self, metadata)
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:link[@rel='self' and  @type='application/atom+xml']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/search?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04&httpAccept=application%2Fatom%2Bxml")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:link[@rel='alternate' and @type='application/gml+xml']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/metadata?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04&httpAccept=application%2Fgml%2Bxml")));

        // check the HTML
        String summary = getXPath().compile("/at:feed/at:entry[1]/at:summary").evaluate(dom);
        // parse html using JSoup (DOM not usable, HTML is not valid/well formed XML in general
        org.jsoup.nodes.Document sd = Jsoup.parse(summary);
        String isoHRef = sd.select("a[title=O&M format]").attr("href");
        assertThat(
                isoHRef,
                equalTo(
                        "http://localhost:8080/geoserver/oseo/metadata?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04&httpAccept=application%2Fgml%2Bxml"));
        String atomHRef = sd.select("a[title=ATOM format]").attr("href");
        assertThat(
                atomHRef,
                equalTo(
                        "http://localhost:8080/geoserver/oseo/search?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04&httpAccept=application%2Fatom%2Bxml"));
        String quickLookRef = sd.select("a[title='View browse image'").attr("href");
        assertThat(
                quickLookRef,
                equalTo(
                        "http://localhost:8080/geoserver/oseo/quicklook?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04"));
    }

    @Test
    public void testSearchByBoxDatelineCrossing() throws Exception {
        Document dom = getAsDOM("oseo/search?parentId=SENTINEL2&box=170,33,-117,34");
        // print(dom);

        // only one feature should be matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:title",
                        equalTo("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04")));
    }

    @Test
    public void testSearchByBoxOutsideData() throws Exception {
        // look for data close to the south pole
        Document dom = getAsDOM("oseo/search?parentId=SENTINEL2&box=0,-89,1,-88");
        // print(dom);

        assertNoResults(dom);
    }

    @Test
    public void testSearchByDistance() throws Exception {
        // test distance search, this distance is just good enough
        Document dom = getAsDOM("oseo/search?parentId=SENTINEL2&lon=-117&lat=33&radius=150000");

        // only one feature should be matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:title",
                        equalTo("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04")));
    }

    @Test
    public void testSearchByDistanceWhereNoDataIsAvailable() throws Exception {
        // since H2 does not support well distance searches, use a point inside the data area
        Document dom = getAsDOM("oseo/search?parentId=SENTINEL2&lon=0&lat=-89&radius=10000");

        assertNoResults(dom);
    }

    @Test
    public void testSearchCollectionByTimeRange() throws Exception {
        // only LANDSAT matches
        Document dom = getAsDOM("oseo/search?timeStart=1988-01-01&timeEnd=2000-01-01");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:end='2000-01-01']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:start='1988-01-01']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only landsat
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
    }

    @Test
    public void testSearchCollectionByTimeRangeDuring() throws Exception {
        // search time range
        Document dom = getAsDOM("oseo/search?timeStart=2012-01-01&timeRelation=during");
        print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("4")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:start='2012-01-01']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:relation='during']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only SENTINEL ones match
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("4")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SAS1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[3]/at:title", equalTo("gsTestCollection")));
        assertThat(dom, hasXPath("/at:feed/at:entry[4]/at:title", equalTo("SENTINEL1")));
    }

    @Test
    public void testProductByTimeRange() throws Exception {
        // only LANDSAT matches
        Document dom =
                getAsDOM("oseo/search?parentId=SENTINEL2&timeStart=2017-03-08&timeEnd=2017-03-09");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:start='2017-03-08']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@time:end='2017-03-09']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only one feature matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:title",
                        equalTo("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04")));
    }

    @Test
    public void testSearchOptical() throws Exception {
        // sentinel-2 and landsat8 match, sentinel1 does not
        Document dom = getAsDOM("oseo/search?sensorType=OPTICAL");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("2")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:sensorType='OPTICAL']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only one feature matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("LANDSAT8")));
    }

    @Test
    public void testCustomClass() throws Exception {
        // only the GS_TEST collection matches
        Document dom = getAsDOM("oseo/search?sensorType=geoServer");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:sensorType='geoServer']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only one feature matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("gsTestCollection")));
    }

    @Test
    public void testSearchRadar() throws Exception {
        Document dom = getAsDOM("oseo/search?sensorType=RADAR");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:sensorType='RADAR']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only one feature matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL1")));
    }

    @Test
    public void testSearchOpticalRadar() throws Exception {
        Document dom = getAsDOM("oseo/search?sensorType=OPTICAL,RADAR");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("3")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:sensorType='OPTICAL,RADAR']"));
        assertThat(dom, hasXPath("/at:feed/at:updated"));

        // check entries, only one feature matching
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("3")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[3]/at:title", equalTo("LANDSAT8")));
    }

    @Test
    public void testProductByCloudCover() throws Exception {
        // match cloud cover < 2
        Document dom = getAsDOM("oseo/search?parentId=SENTINEL2&cloudCover=2]");
        // print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("12")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count]"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:cloudCover='2]']"));
    }

    @Test
    public void testProductByCustomProperty() throws Exception {
        // match test property
        Document dom = getAsDOM("oseo/search?parentId=gsTestCollection&test=abc");
        print(dom);

        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count]"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@eo:test='abc']"));
    }

    @Test
    public void testGetSentinel2Metadata() throws Exception {
        Document dom = getAsDOM("oseo/metadata?uid=SENTINEL2", 200, MetadataRequest.ISO_METADATA);
        // print(dom);

        // just check we got the right one
        assertThat(
                dom,
                hasXPath(
                        "/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString",
                        equalTo("EOP:CNES:PEPS:S2")));
    }

    @Test
    public void testGetSentinel1Metadata() throws Exception {
        Document dom = getAsDOM("oseo/metadata?uid=SENTINEL1", 200, MetadataRequest.ISO_METADATA);
        // print(dom);

        // just check we got the right one
        assertThat(
                dom,
                hasXPath(
                        "/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString",
                        equalTo("EOP:CNES:PEPS:S1")));
    }

    @Test
    public void testProductMetadata() throws Exception {
        String path =
                "oseo/metadata?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04&httpAccept=application/gml%2Bxml";
        Document dom = getAsDOM(path, 200, MetadataRequest.OM_METADATA);
        // print(dom);

        // just check we got the right one (the namespaces used here are different than the ones
        // used
        SimpleNamespaceContext ctx = new SimpleNamespaceContext();
        ctx.bindNamespaceUri("gml", "http://www.opengis.net/gml/3.2");
        ctx.bindNamespaceUri("opt", "http://www.opengis.net/opt/2.1");
        ctx.bindNamespaceUri("om", "http://www.opengis.net/om/2.0");
        assertThat(
                dom,
                Matchers.hasXPath(
                        "/opt:EarthObservation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition",
                        ctx,
                        equalTo("2016-09-29T10:20:22.026Z")));
    }

    @Test
    public void testGetCollectionMetadataInvalidFormat() throws Exception {
        Document dom =
                getAsOpenSearchException("oseo/metadata?uid=SENTINEL2&httpAccept=foo/bar", 400);
        assertThat(
                dom,
                hasXPath("/rss/channel/item/title", containsString(MetadataRequest.ISO_METADATA)));
    }

    @Test
    public void testGetProductMetadataInvalidFormat() throws Exception {
        Document dom =
                getAsOpenSearchException(
                        "oseo/metadata?parentId=SENTINEL2&uid=123&httpAccept=foo/bar", 400);
        assertThat(
                dom,
                hasXPath("/rss/channel/item/title", containsString(MetadataRequest.OM_METADATA)));
    }

    @Test
    public void testQuicklook() throws Exception {
        String path =
                "oseo/quicklook?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01";
        RenderedImage image = getAsImage(path, "image/jpeg");
        assertNotNull(image);
    }

    @Test
    public void testQuicklookInAtom() throws Exception {
        Document dom =
                getAsDOM(
                        "oseo/search?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01");
        // print(dom);

        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/at:link[@rel='icon']/@href",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/quicklook?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry/media:group)", equalTo("1")));
        assertThat(
                dom, hasXPath("count(/at:feed/at:entry/media:group/media:content)", equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "count(/at:feed/at:entry/media:group/media:content/media:category)",
                        equalTo("1")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/media:group/media:content/media:category",
                        equalTo("THUMBNAIL")));
        assertThat(
                dom,
                hasXPath(
                        "/at:feed/at:entry/media:group/media:content[@medium='image' and @type='image/jpeg']/@url",
                        equalTo(
                                "http://localhost:8080/geoserver/oseo/quicklook?parentId=SENTINEL2&uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01")));
    }

    @Test
    public void testAtmosphericSearchSpecies() throws Exception {
        // match 03, both have it
        Document dom = getAsDOM("oseo/search?parentId=SAS1&species=O3");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("2")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180227102021.02")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[2]/dc:identifier", equalTo("SAS1_20180226102021.01")));

        // match C02, only one has it
        dom = getAsDOM("oseo/search?parentId=SAS1&species=O3&species=CO2");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180226102021.01")));

        // match 03 and NO2, only one has it
        dom = getAsDOM("oseo/search?parentId=SAS1&species=O3&species=NO2");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180227102021.02")));
    }

    @Test
    public void testAtmosphericSearchVerticalRange() throws Exception {
        // 1000 and above, both have it
        Document dom = getAsDOM("oseo/search?parentId=SAS1&verticalRange=[1000");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("2")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180227102021.02")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[2]/dc:identifier", equalTo("SAS1_20180226102021.01")));

        // below 300, only the first has it
        dom = getAsDOM("oseo/search?parentId=SAS1&verticalRange=300]");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180226102021.01")));

        // between 300 and 700, only the second has it
        dom = getAsDOM("oseo/search?parentId=SAS1&verticalRange=[300,700]");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180227102021.02")));
    }

    @Test
    public void testAtmosphericCombinedFilter() throws Exception {
        // 1000 and above, both have it
        Document dom = getAsDOM("oseo/search?parentId=SAS1&verticalRange=[1000&species=CO2");
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(
                dom,
                hasXPath("/at:feed/at:entry[1]/dc:identifier", equalTo("SAS1_20180226102021.01")));
    }
}
