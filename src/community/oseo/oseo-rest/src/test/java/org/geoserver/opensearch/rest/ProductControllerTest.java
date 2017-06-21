/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.DocumentContext;

public class ProductControllerTest extends OSEORestTestSupport {

    @Test
    public void testGetProductsForNonExistingCollection() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/fooBar/products");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("fooBar"));
    }

    @Test
    public void testGetProducts() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/products", 200);
        assertEquals(19, json.read("$.products.*", List.class).size());
        // check the first (sorted alphabetically, it should be stable)
        assertEquals("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                json.read("$.products[0].id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                json.read("$.products[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                json.read("$.products[0].rss"));
    }

    @Test
    public void testGetProductsPaging() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products?offset=1&limit=1", 200);
        assertEquals(1, json.read("$.products.*", List.class).size());
        // check the first (sorted alphabetically, it should be stable)
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.products[0].id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.products[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.products[0].rss"));
    }

    @Test
    public void testGetProductsPagingValidation() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products?offset=-1");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("offset"));

        response = getAsServletResponse("/rest/oseo/collections/SENTINEL2/products?limit=-1");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("limit"));

        response = getAsServletResponse("/rest/oseo/collections/SENTINEL2/products?limit=1000");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("limit"));
    }

    @Test
    public void testNonExistingProduct() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/foobar");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("foobar"));
    }

    @Test
    public void testGetProduct() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                200);
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.properties['eop:identifier']"));
        assertEquals("SENTINEL2", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/ogcLinks",
                json.read("$.properties['ogcLinksHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/metadata",
                json.read("$.properties['metadataHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/description",
                json.read("$.properties['descriptionHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/thumbnail",
                json.read("$.properties['thumbnailHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/granules",
                json.read("$.properties['granulesHref']"));
    }

    @Test
    public void testGetProductLinks() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/ogcLinks",
                200);
        assertEquals("http://www.opengis.net/spec/owc/1.0/req/atom/wms",
                json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals("${BASE_URL}/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
    }

    @Test
    public void testGetProductMetadata() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/metadata");
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        assertThat(response.getContentAsString(), both(containsString("opt:EarthObservation")).and(
                containsString("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01")));
    }

    @Test
    public void testGetCollectionDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/description");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertThat(response.getContentAsString(), both(containsString("<table>"))
                .and(containsString("2016-01-17T10:10:30.743Z / 2016-01-17T10:10:30.743Z")));
    }

    @Test
    public void testGetProductThumbnail() throws Exception {
        // just checking we get an image indeed from the only product that has a thumb
        getAsImage(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01/thumbnail",
                "image/jpeg");
    }

    @Test
    public void testGetProductMissingThumbnail() throws Exception {
        // this one does not have a thumbnail
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/thumbnail");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(),
                containsString("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01"));
    }
}
