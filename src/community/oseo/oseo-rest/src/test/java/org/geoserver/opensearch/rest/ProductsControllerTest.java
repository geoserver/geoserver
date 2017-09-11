/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass.EOP_GENERIC;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.rest.ProductsController.ProductPart;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import com.vividsolutions.jts.geom.Envelope;

import net.sf.json.JSONObject;

public class ProductsControllerTest extends OSEORestTestSupport {

    @Override
    protected boolean populateGranulesTable() {
        return true;
    }

    @Override
    protected String getLogConfiguration() {
        // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
        return super.getLogConfiguration();
    }

    @Before
    public void cleanupTestProduct() throws IOException {
        DataStoreInfo ds = getCatalog().getDataStoreByName("oseo");
        OpenSearchAccess access = (OpenSearchAccess) ds.getDataStore(null);
        FeatureStore store = (FeatureStore) access.getProductSource();
        store.removeFeatures(FF.and(
                FF.equal(FF.property(new NameImpl(EOP_GENERIC.getPrefix(), "parentIdentifier")),
                        FF.literal("SENTINEL2"), true),
                FF.equal(FF.property(new NameImpl(EOP_GENERIC.getPrefix(), "identifier")),
                        FF.literal(
                                "S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04"),
                        true)));
    }

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
    public void testCreateProduct() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products", getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                response.getHeader("location"));

        // check it's really there
        assertProductCreated();
    }

    private void assertProductCreated() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                200);
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("SENTINEL2", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals("NOMINAL", json.read("$.properties['eop:acquisitionType']"));
        assertEquals(Integer.valueOf(65), json.read("$.properties['eop:orbitNumber']"));
        assertEquals("2018-01-01T00:00:00.000+0000", json.read("$.properties['timeStart']"));
        assertEquals("EPSG:32632", json.read("$.properties['crs']"));
        
        SimpleFeature sf = new FeatureJSON().readFeature(json.jsonString());
        ReferencedEnvelope bounds = ReferencedEnvelope.reference(sf.getBounds());
        assertTrue(new Envelope(-180,180,-90,90).equals(bounds));
    }

    @Test
    public void testUpdateProduct() throws Exception {
        // create the product
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products", getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                response.getHeader("location"));

        // grab the JSON to modify some bits
        JSONObject feature = (JSONObject) getAsJSON(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04");
        JSONObject properties = feature.getJSONObject("properties");
        properties.element("eop:orbitNumber", 66);
        properties.element("timeStart", "2017-01-01T00:00:00Z");

        // send it back
        response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                feature.toString(), "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json = getAsJSONPath(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                200);
        assertEquals(Integer.valueOf(66), json.read("$.properties['eop:orbitNumber']"));
        assertEquals("2017-01-01T00:00:00.000+0000", json.read("$.properties['timeStart']"));
    }

    @Test
    public void testDeleteProduct() throws Exception {
        // create the product
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products", getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                response.getHeader("location"));

        // it's there
        getAsJSONPath(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                200);

        // and now kill the poor beast
        response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04");
        assertEquals(200, response.getStatus());

        // no more there
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04");
        assertEquals(404, response.getStatus());
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
    public void testPutProductLinks() throws Exception {
        testCreateProduct();

        // create the links
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks",
                getTestData("/product-links.json"), MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        // check they are there
        assertProductLinks();
    }

    private void assertProductLinks() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks",
                200);
        assertEquals("http://www.opengis.net/spec/owc/1.0/req/atom/wms",
                json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals(
                "${BASE_URL}/SENTINEL2/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
    }

    @Test
    public void testDeleteProductLinks() throws Exception {
        testPutProductLinks();

        // delete the links
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks");
        assertEquals(200, response.getStatus());

        // check they are gone
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks");
        assertEquals(404, response.getStatus());
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
    public void testPutProductMetadata() throws Exception {
        testCreateProduct();

        // create the metadata
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/metadata",
                getTestData("/product-metadata.xml"), MediaType.TEXT_XML_VALUE);
        assertEquals(200, response.getStatus());

        // grab and check
        assertProductMetadata();
    }

    private void assertProductMetadata() throws Exception, UnsupportedEncodingException {
        MockHttpServletResponse response;
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/metadata");
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        assertThat(response.getContentAsString(), both(containsString("opt:EarthObservation")).and(
                containsString("S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04")));
    }

    @Test
    public void testDeleteProductMetadata() throws Exception {
        // creates the product and adds the metadata
        testPutProductMetadata();

        // now remove
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/metadata");
        assertEquals(200, response.getStatus());

        // check it's not there anymore
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/metadata");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetProductDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/description");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertThat(response.getContentAsString(), both(containsString("<table>"))
                .and(containsString("2016-01-17T10:10:30.743Z / 2016-01-17T10:10:30.743Z")));
    }

    @Test
    public void testPutProductDescription() throws Exception {
        testCreateProduct();

        // create the description
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/description",
                getTestData("/product-description.html"), MediaType.TEXT_HTML_VALUE);
        assertEquals(200, response.getStatus());

        // grab and check
        assertProductDescription();
    }

    private void assertProductDescription() throws Exception, UnsupportedEncodingException {
        MockHttpServletResponse response;
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/description");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertThat(response.getContentAsString(), both(containsString("<table")).and(
                containsString("S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04")));
    }

    @Test
    public void testDeleteCollectionDescription() throws Exception {
        // creates the collection and adds the metadata
        testPutProductDescription();

        // now remove
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/description");
        assertEquals(200, response.getStatus());

        // check it's not there anymore
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/description");
        assertEquals(404, response.getStatus());
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

    @Test
    public void testPutProductThumbnail() throws Exception {
        testCreateProduct();

        // create the image
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/thumbnail",
                getTestData("/product-thumb.jpeg"), MediaType.IMAGE_JPEG_VALUE);
        assertEquals(200, response.getStatus());

        // grab and check
        assertProductThumbnail();
    }

    private void assertProductThumbnail() throws Exception {
        getAsImage(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/thumbnail",
                "image/jpeg");
    }

    @Test
    public void testDeleteProductThumbnail() throws Exception {
        testPutProductThumbnail();

        // now delete it
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/thumbnail");
        assertEquals(200, response.getStatus());

        // no more there now
        response = getAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/thumbnail");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetProductGranules() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/granules",
                200);
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals(
                "/efs/geoserver_data/coverages/sentinel/california/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01.tif",
                json.read("$.features[0].properties.location"));
    }

    @Test
    public void testPutProductGranules() throws Exception {
        testCreateProduct();
        
        // add the granules
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                getTestData("/product-granules.json"), MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        assertProductGranules();
    }
    
    @Test
    public void testPutProductGranulesWithBands() throws Exception {
        testCreateProduct();
        
        // add the granules
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                getTestData("/product-granules-bands.json"), MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        assertProductGranules();
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                200);
        assertEquals("B1", json.read("$.features[0].properties.band"));
        assertEquals("B2", json.read("$.features[1].properties.band"));
    }

    private void assertProductGranules() throws Exception {
        // grab and check
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                200);
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(new Integer(2), json.read("$.features.length()"));
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals("/efs/geoserver_data/coverages/sentinel/california/R1C1.tif",
                json.read("$.features[0].properties.location"));
        assertEquals("Feature", json.read("$.features[1].type"));
        assertEquals("Polygon", json.read("$.features[1].geometry.type"));
        assertEquals("/efs/geoserver_data/coverages/sentinel/california/R1C2.tif",
                json.read("$.features[1].properties.location"));
        // parse the geojson, check the geometries have been parsed correctly
        SimpleFeatureCollection fc = (SimpleFeatureCollection) new FeatureJSON().readFeatureCollection(json.jsonString());
        assertEquals(2, fc.size());
        final SimpleFeatureIterator it = fc.features();
        SimpleFeature sf = it.next();
        assertTrue(new Envelope(10,12,40,42).contains(ReferencedEnvelope.reference(sf.getBounds())));
        sf = it.next();
        assertTrue(new Envelope(10,12,40,42).contains(ReferencedEnvelope.reference(sf.getBounds())));

        // check no other granule has been harmed
        json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPM_N02.01/granules",
                200);
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(new Integer(4), json.read("$.features.length()"));
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals(
                "/efs/geoserver_data/coverages/sentinel/california/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPM_N02.01.tif",
                json.read("$.features[0].properties.location"));
    }
    
    

    @Test
    public void testDeleteProductGranules() throws Exception {
        testPutProductDescription();
        
        // now delete it
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules");
        assertEquals(200, response.getStatus());

        // no more there now
        DocumentContext json = getAsJSONPath(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules", 200);
        assertEquals(200, response.getStatus());
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(new Integer(0), json.read("$.features.length()"));
    }
    
    
    @Test
    public void testCreateProductAsZip() throws Exception {
        // build all possible combinations of elements in the zip and check they all work
        Set<Set<ProductPart>> sets = Sets
                .powerSet(new HashSet<>(Arrays.asList(ProductPart.Product, ProductPart.Description,
                        ProductPart.Metadata, ProductPart.Thumbnail, ProductPart.OwsLinks, ProductPart.Granules)));

        for (Set<ProductPart> parts : sets) {
            if (parts.isEmpty()) {
                continue;
            }
            
            LOGGER.info("Testing zip product creation with parts:" + parts);
            cleanupTestProduct();
            testCreateProductAsZip(parts);
        }
    }
    
    private void testCreateProductAsZip(Set<ProductPart> parts) throws Exception {
        LOGGER.info("Testing: " + parts);
        byte[] zip = null;
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (ProductPart part : parts) {
                String resource, name;
                switch (part) {
                case Product:
                    resource = "/product.json";
                    name = "product.json";
                    break;
                case Description:
                    resource = "/product-description.html";
                    name = "description.html";
                    break;
                case Metadata:
                    resource = "/product-metadata.xml";
                    name = "metadata.xml";
                    break;
                case Thumbnail:
                    resource = "/product-thumb.jpeg";
                    name = "thumbnail.jpeg";
                    break;
                case OwsLinks:
                    resource = "/product-links.json";
                    name = "owsLinks.json";
                    break;
                case Granules:
                    resource = "/product-granules.json";
                    name = "granules.json";
                    break;
                default:
                    throw new RuntimeException("Unexpected part " + part);
                }

                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                IOUtils.copy(getClass().getResourceAsStream(resource), zos);
                zos.flush();
                zos.closeEntry();
            }
            zip = bos.toByteArray();
        }
        
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products", zip,
                MediaTypeExtensions.APPLICATION_ZIP_VALUE);
        if (parts.contains(ProductPart.Product)) {
            assertEquals(201, response.getStatus());
            assertEquals(
                    "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04",
                    response.getHeader("location"));

            assertProductCreated();            
        } else {
            assertEquals(400, response.getStatus());
            assertThat(response.getContentAsString(), containsString("product.json"));
            // failed, nothing else to check
            return;
        }

        if (parts.contains(ProductPart.Description)) {
            assertProductDescription();
        }
        if (parts.contains(ProductPart.Metadata)) {
            assertProductMetadata();
        }
        if (parts.contains(ProductPart.Thumbnail)) {
            assertProductThumbnail();
        }
        if (parts.contains(ProductPart.OwsLinks)) {
            assertProductLinks();
        }
        if (parts.contains(ProductPart.Granules)) {
            assertProductGranules();
        }
    }
    
}
