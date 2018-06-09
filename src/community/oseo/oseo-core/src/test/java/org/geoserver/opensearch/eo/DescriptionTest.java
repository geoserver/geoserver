/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.response.DescriptionResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescriptionTest extends OSEOTestSupport {

    @Test
    public void testExceptionInternalError() throws Exception {
        final GeoServer gs = getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        String storeId = service.getOpenSearchAccessStoreId();
        // this will raise an internal error
        service.setOpenSearchAccessStoreId(null);
        try {
            gs.save(service);

            // run a request that's going to fail
            Document dom = getAsOpenSearchException("oseo/description", 500);
            // print(dom);

            assertThat(
                    dom,
                    hasXPath(
                            "/rss/channel/item/title",
                            equalTo(
                                    "OpenSearchAccess is not configured in the OpenSearch for EO panel, please do so")));
            assertThat(
                    dom,
                    hasXPath(
                            "/rss/channel/item/description",
                            equalTo(
                                    "OpenSearchAccess is not configured in the OpenSearch for EO panel, please do so")));
        } finally {
            // reset old values
            service.setOpenSearchAccessStoreId(storeId);
            gs.save(service);
        }
    }

    @Test
    public void testExceptionInternalErrorVerbose() throws Exception {
        final GeoServer gs = getGeoServer();
        GeoServerInfo gsInfo = gs.getGlobal();
        gsInfo.getSettings().setVerboseExceptions(true);
        gs.save(gsInfo);
        OSEOInfo service = gs.getService(OSEOInfo.class);
        String storeId = service.getOpenSearchAccessStoreId();
        // this will raise an internal error
        service.setOpenSearchAccessStoreId(null);
        try {
            gs.save(service);

            // run a request that's going to fail
            Document dom = getAsOpenSearchException("oseo/description", 500);
            // print(dom);

            assertThat(
                    dom,
                    hasXPath(
                            "/rss/channel/item/title",
                            equalTo(
                                    "OpenSearchAccess is not configured in the OpenSearch for EO panel, please do so")));
            assertThat(
                    dom,
                    hasXPath(
                            "/rss/channel/item/description",
                            startsWith(
                                    "OpenSearchAccess is not configured in the OpenSearch for EO panel, please do so")));
            assertThat(
                    dom,
                    hasXPath(
                            "/rss/channel/item/description",
                            containsString("org.geoserver.platform.OWS20Exception")));
        } finally {
            // reset old values
            service.setOpenSearchAccessStoreId(storeId);
            gs.save(service);
            gsInfo.getSettings().setVerboseExceptions(false);
            gs.save(gsInfo);
        }
    }

    @Test
    public void testExceptionInvalidParentId() throws Exception {
        // run a request that's going to fail
        Document dom = getAsOpenSearchException("oseo/description?parentId=IAmNotThere", 400);
        // print(dom);

        assertThat(
                dom,
                hasXPath("/rss/channel/item/title", equalTo("Unknown parentId 'IAmNotThere'")));
    }

    @Test
    public void testGlobalDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("oseo/description");
        assertEquals(DescriptionResponse.OS_DESCRIPTION_MIME, response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // print(dom);

        // generic contents check
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:ShortName", equalTo("OSEO")));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:LongName",
                        equalTo("OpenSearch for Earth Observation")));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Description",
                        containsString("Earth Observation")));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Tags",
                        equalTo("EarthObservation OGC CEOS-OS-BP-V1.2/L1")));
        assertThat(
                dom,
                hasXPath("/os:OpenSearchDescription/os:LongName", containsString("OpenSearch")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:SyndicationRight", equalTo("open")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:AdultContent", equalTo("false")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Language", equalTo("en-us")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:OutputEncoding", equalTo("UTF-8")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:InputEncoding", equalTo("UTF-8")));

        // check the self link
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']"));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']/@template",
                        containsString("/oseo/description")));

        // check the result link
        String resultsBase =
                "/os:OpenSearchDescription/os:Url[@rel='collection'and @type='application/atom+xml']";
        assertThat(dom, hasXPath(resultsBase));
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString("/oseo/search?"), //
                                containsString("searchTerms={searchTerms?}"), //
                                containsString("lat={geo:lat?}"), //
                                containsString("timeStart={time:start?}"))));
        assertThat(dom, hasXPath(resultsBase + "/@indexOffset", equalTo("1")));

        // check some parameters have been described
        String paramBase = resultsBase + "/param:Parameter";
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='searchTerms' and @value='{searchTerms}' and @minimum='0']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='count' and @value='{count}' and @minimum='0' and  @minInclusive='0' and @maxInclusive='100']"));

        // search profiles
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='searchTerms']/atom:link[@rel='profile' and @href='http://localhost:8080/geoserver/docs/searchTerms.html']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/LINESTRING']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/POINT']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/POLYGON']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/MULTILINESTRING']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/MULTIPOINT']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='geometry']/atom:link[@rel='profile' and @href='http://www.opengis.net/wkt/MULTIPOLYGON']"));

        // check some EO parameter
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='wavelength' and @value='{eo:wavelength}' and @minimum='0']"));
        assertThat(
                dom,
                hasXPath(
                        paramBase
                                + "[@name='identifier' and @value='{eo:identifier}' and @minimum='0']"));

        // general validation
        checkValidOSDD(dom);
    }

    @Test
    public void testOpticalCollectionDescription() throws Exception {
        Document dom = getAsDOM("oseo/description?parentId=SENTINEL2");
        print(dom);

        // we got a opensearch descriptor
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));

        // self is there and uses the right parentId
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']"));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']/@template",
                        containsString("/oseo/description?parentId=SENTINEL2")));

        // check the results link is there
        String resultsBase =
                "/os:OpenSearchDescription/os:Url[@rel='results'and @type='application/atom+xml']";
        assertThat(dom, hasXPath(resultsBase));
        // ... and has the right parentId, and basic search params
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString("/oseo/search?"), //
                                containsString("parentId=SENTINEL2"), //
                                containsString("searchTerms={searchTerms?}"), //
                                containsString("lat={geo:lat?}"), //
                                containsString("timeStart={time:start?}"))));
        // ... and has generic EOP parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString(
                                        "productQualityStatus={eo:productQualityStatus?}"), //
                                containsString("processorName={eo:processorName?}"), //
                                containsString("modificationDate={eo:modificationDate?}"))));
        // ... and has OPT parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString("cloudCover={eo:cloudCover?}"), //
                                containsString("snowCover={eo:snowCover?}"))));
        // ... but no SAR parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        not(
                                anyOf(
                                        containsString(
                                                "polarisationMode={eo:polarisationMode?}"), //
                                        containsString(
                                                "polarisationChannels={eo:polarisationChannels?}")))));
    }

    @Test
    public void testRadarCollectionDescription() throws Exception {
        Document dom = getAsDOM("oseo/description?parentId=SENTINEL1");
        print(dom);

        // we got a opensearch descriptor
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));

        // self is there and uses the right parentId
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']"));
        assertThat(
                dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']/@template",
                        containsString("/oseo/description?parentId=SENTINEL1")));

        // check the results link is there
        String resultsBase =
                "/os:OpenSearchDescription/os:Url[@rel='results'and @type='application/atom+xml']";
        assertThat(dom, hasXPath(resultsBase));
        // ... and has the right parentId, and basic search params
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString("/oseo/search?"), //
                                containsString("parentId=SENTINEL1"), //
                                containsString("searchTerms={searchTerms?}"), //
                                containsString("lat={geo:lat?}"), //
                                containsString("timeStart={time:start?}"), //
                                containsString("timeEnd={time:end?}"), //
                                containsString("timeRelation={time:relation?}"))));
        // ... and has generic EOP parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString(
                                        "productQualityStatus={eo:productQualityStatus?}"), //
                                containsString("processorName={eo:processorName?}"), //
                                containsString("modificationDate={eo:modificationDate?}"))));
        // ... and SAR parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        allOf(
                                containsString("polarisationMode={eo:polarisationMode?}"), //
                                containsString(
                                        "polarisationChannels={eo:polarisationChannels?}"))));
        // ... but no OPT parameters
        assertThat(
                dom,
                hasXPath(
                        resultsBase + "/@template",
                        not(
                                anyOf(
                                        containsString("cloudCover={eo:cloudCover?}"), //
                                        containsString("snowCover={eo:snowCover?}")))));
    }
}
