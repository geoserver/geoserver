/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static java.util.Arrays.asList;
import static org.geoserver.opensearch.eo.ProductClass.GENERIC;
import static org.geoserver.opensearch.rest.ProductsController.ProductPart.Granules;
import static org.geoserver.opensearch.rest.ProductsController.ProductPart.OwsLinks;
import static org.geoserver.opensearch.rest.ProductsController.ProductPart.Product;
import static org.geoserver.opensearch.rest.ProductsController.ProductPart.Thumbnail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.minidev.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.rest.ProductsController.ProductPart;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class ProductsControllerTest extends OSEORestTestSupport {

    public static final String PRODUCT_CREATE_UPDATE_ID =
            "S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04";
    public static final String PRODUCT_ATM_CREATE_UPDATE_ID = "SAS1_20180101T000000.01";

    @Override
    protected boolean populateGranulesTable() {
        return true;
    }

    @Before
    public void cleanupTestProduct() throws IOException {
        DataStoreInfo ds = getCatalog().getDataStoreByName("oseo");
        OpenSearchAccess access = (OpenSearchAccess) ds.getDataStore(null);
        FeatureStore store = (FeatureStore) access.getProductSource();
        store.removeFeatures(FF.and(
                FF.equal(
                        FF.property(new NameImpl(GENERIC.getPrefix(), "parentIdentifier")),
                        FF.literal("SENTINEL2"),
                        true),
                FF.equal(
                        FF.property(new NameImpl(GENERIC.getPrefix(), "identifier")),
                        FF.literal(PRODUCT_CREATE_UPDATE_ID),
                        true)));
        store.removeFeatures(FF.and(
                FF.equal(FF.property(new NameImpl(GENERIC.getPrefix(), "parentIdentifier")), FF.literal("SAS1"), true),
                FF.equal(
                        FF.property(new NameImpl(GENERIC.getPrefix(), "identifier")),
                        FF.literal(PRODUCT_ATM_CREATE_UPDATE_ID),
                        true)));
    }

    @Test
    public void testGetProductsForNonExistingCollection() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/oseo/collections/fooBar/products");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("fooBar"));
    }

    @Test
    public void testGetProducts() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/products", 200);
        assertEquals(19, json.read("$.products.*", List.class).size());
        // check the first (sorted alphabetically, it should be stable)
        assertEquals("S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04", json.read("$.products[0].id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                json.read("$.products[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?uid=S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                json.read("$.products[0].rss"));
    }

    @Test
    public void testGetProductsPaging() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/products?offset=1&limit=1", 200);
        assertEquals(1, json.read("$.products.*", List.class).size());
        // check the first (sorted alphabetically, it should be stable)
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01", json.read("$.products[0].id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.products[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?uid=S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                json.read("$.products[0].rss"));
    }

    @Test
    public void testGetProductsPagingValidation() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/oseo/collections/SENTINEL2/products?offset=-1");
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
        MockHttpServletResponse response = getAsServletResponse("/rest/oseo/collections/SENTINEL2/products/foobar");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("foobar"));
    }

    @Test
    public void testGetProduct() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
                200);
        assertEquals("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals(
                "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01",
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
    public void testGetAtmosphericProduct() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SAS1/products/SAS1_20180226102021.01", 200);
        assertEquals("SAS1_20180226102021.01", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("SAS1_20180226102021.01", json.read("$.properties['eop:identifier']"));
        assertEquals("SAS1", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals(jsonArray("O3", "O3", "CO2"), json.read("$.properties['atm:species']"));
        assertEquals(jsonArray(1000.0, 2000.0, 0.0), json.read("$.properties['atm:verticalRange']"));
    }

    private JSONArray jsonArray(Object... values) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < values.length; i++) {
            array.add(values[i]);
        }
        return array;
    }

    @Test
    public void testCreateProductPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products",
                getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // check it's really there
        assertProduct("2018-01-01T00:00:00.000Z", "2018-01-01T00:00:00.000Z");
    }

    /**
     * Creates a product whose id looks like a timestamp. Used to fail as the GeoJSON reader was parsing that id as a
     * Date
     */
    @Test
    public void testCreateProductPostDateId() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/gsTestCollection/products",
                getTestData("/productDate.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        String productId = "2009-08-29T223949_RE4_1B-NAC_1678843_48007";
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/gsTestCollection/products/" + productId,
                response.getHeader("location"));

        // check it's really there
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/gsTestCollection/products/" + productId, 200);
        assertEquals(productId, json.read("$.id"));
    }

    @Test
    public void testCreateProductPut() throws Exception {
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // check it's really there
        assertProduct("2018-01-01T00:00:00.000Z", "2018-01-01T00:00:00.000Z");
    }

    @Test
    public void testCreateProductPutIdentifierMismatch() throws Exception {
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/foobar",
                getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertEquals(
                "Product id in URL not consistent with eop:identifier in JSON, refusing creation",
                response.getContentAsString());
    }

    @Test
    public void testCreateProductNoIdentifierPut() throws Exception {
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                getTestData("/product-no-identifier.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // check it's really there
        assertProduct("2018-01-01T00:00:00.000Z", "2018-01-01T00:00:00.000Z");
    }

    @Test
    public void testCreateAtmosphericProduct() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SAS1/products",
                getTestData("/product-atm.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // check it's really there
        DocumentContext json =
                getAsJSONPath("/rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID, 200);
        assertEquals(PRODUCT_ATM_CREATE_UPDATE_ID, json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("SAS1", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals("NOMINAL", json.read("$.properties['eop:acquisitionType']"));
        assertEquals(Integer.valueOf(65), json.read("$.properties['eop:orbitNumber']"));
        assertEquals("2018-01-01T00:00:00.000Z", json.read("$.properties['timeStart']"));
        assertEquals("2018-01-01T00:00:00.000Z", json.read("$.properties['timeEnd']"));
        assertEquals("EPSG:32632", json.read("$.properties['crs']"));
        assertEquals(jsonArray("O2", "O2", "NO3", "NO3"), json.read("$.properties['atm:species']"));
        assertEquals(jsonArray(250d, 500d, 250d, 500d), json.read("$.properties['atm:verticalRange']"));

        SimpleFeature sf = GeoJSONReader.parseFeature(json.jsonString());
        ReferencedEnvelope bounds = ReferencedEnvelope.reference(sf.getBounds());
        assertEquals(new Envelope(-180, 180, -90, 90), bounds);
    }

    @Test
    public void testCreateProductInCustomCollection() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/gsTestCollection/products",
                getTestData("/product-custom-class.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/gsTestCollection/products/GS_TEST_PRODUCT.02",
                response.getHeader("location"));

        // check it's really there
        DocumentContext json = getAsJSONPath("rest/oseo/collections/gsTestCollection/products/GS_TEST_PRODUCT.02", 200);
        assertEquals("GS_TEST_PRODUCT.02", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("gsTestCollection", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals("NOMINAL", json.read("$.properties['eop:acquisitionType']"));
        assertEquals(Integer.valueOf(65), json.read("$.properties['eop:orbitNumber']"));
        assertEquals("2018-01-01T00:00:00.000Z", json.read("$.properties['timeStart']"));
        assertEquals("2018-01-01T00:00:00.000Z", json.read("$.properties['timeEnd']"));
        assertEquals("123456", json.read("$.properties['gs:test']"));

        SimpleFeature sf = GeoJSONReader.parseFeature(json.jsonString());
        ReferencedEnvelope bounds = ReferencedEnvelope.reference(sf.getBounds());
        assertEquals(new Envelope(-180, 180, -90, 90), bounds);
    }

    private void assertProduct(String timeStart, String timeEnd) throws Exception {
        DocumentContext json =
                getAsJSONPath("/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID, 200);
        assertEquals(PRODUCT_CREATE_UPDATE_ID, json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("SENTINEL2", json.read("$.properties['eop:parentIdentifier']"));
        assertEquals("NOMINAL", json.read("$.properties['eop:acquisitionType']"));
        assertEquals(Integer.valueOf(65), json.read("$.properties['eop:orbitNumber']"));
        assertEquals(timeStart, json.read("$.properties['timeStart']"));
        assertEquals(timeEnd, json.read("$.properties['timeEnd']"));
        assertEquals("EPSG:32632", json.read("$.properties['crs']"));
        // check some properties of the assets, a JSON property
        assertEquals("Band 1 (coastal)", json.read("$.properties.assets.B01.title"));
        assertEquals(Arrays.asList(3, 2, 1), json.read("$.properties.assets.tki.eo:bands"));

        SimpleFeature sf = GeoJSONReader.parseFeature(json.jsonString());
        ReferencedEnvelope bounds = ReferencedEnvelope.reference(sf.getBounds());
        assertEquals(new Envelope(-180, 180, -90, 90), bounds);
    }

    @Test
    public void testUpdateProduct() throws Exception {
        // create the product
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products",
                getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // grab the JSON to modify some bits
        JSONObject feature =
                (JSONObject) getAsJSON("rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID);
        JSONObject properties = feature.getJSONObject("properties");
        properties.element("eop:orbitNumber", 66);
        properties.element("timeStart", "2017-01-01T00:00:00Z");

        // send it back
        response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                feature.toString(),
                "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json =
                getAsJSONPath("rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID, 200);
        assertEquals(Integer.valueOf(66), json.read("$.properties['eop:orbitNumber']"));
        assertEquals("2017-01-01T00:00:00.000Z", json.read("$.properties['timeStart']"));
    }

    @Test
    public void testUpdateDisabledProduct() throws Exception {
        // grab the JSON to modify some bits
        JSONObject feature = (JSONObject) getAsJSON("rest/oseo/collections/LANDSAT8/products/LS8_TEST.DISABLED");
        JSONObject properties = feature.getJSONObject("properties");
        properties.element("enabled", true);

        // send it back
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/LANDSAT8/products/LS8_TEST.DISABLED", feature.toString(), "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json = getAsJSONPath("rest/oseo/collections/LANDSAT8/products/LS8_TEST.DISABLED", 200);
        assertEquals(Boolean.TRUE, json.read("$.properties['enabled']"));
    }

    @Test
    public void testUpdateAtmosphericProduct() throws Exception {
        // create the product
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SAS1/products",
                getTestData("/product-atm.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // grab the JSON to modify some bits
        JSONObject feature =
                (JSONObject) getAsJSON("rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID);
        JSONObject properties = feature.getJSONObject("properties");
        properties.element("atm:species", Arrays.asList("A", "B", "C", "D"));

        // send it back
        response = putAsServletResponse(
                "rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID,
                feature.toString(),
                "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json =
                getAsJSONPath("rest/oseo/collections/SAS1/products/" + PRODUCT_ATM_CREATE_UPDATE_ID, 200);
        assertEquals(jsonArray("A", "B", "C", "D"), json.read("$.properties['atm:species']"));
    }

    @Test
    public void testDeleteProduct() throws Exception {
        // create the product
        MockHttpServletResponse response = postAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products",
                getTestData("/product.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                response.getHeader("location"));

        // it's there
        getAsJSONPath("rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID, 200);

        // and now kill the poor beast
        response = deleteAsServletResponse("rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID);
        assertEquals(200, response.getStatus());

        // no more there
        response = getAsServletResponse("rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetProductLinks() throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01/ogcLinks",
                200);
        assertEquals("http://www.opengis.net/spec/owc/1.0/req/atom/wms", json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals(
                "${BASE_URL}/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
        assertEquals("6881", json.read("$.links[0].intTest").toString());
        assertEquals("6881.1", json.read("$.links[0].floatTest").toString());
        assertEquals("true", json.read("$.links[0].booleanTest").toString());
        assertEquals(
                "2017-01-01T00:00:00.000Z", json.read("$.links[0].dateTest").toString());
        assertEquals("6881text", json.read("$.links[0].varcharTest").toString());
    }

    @Test
    public void testPutProductLinks() throws Exception {
        testCreateProductPost();

        // create the links
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks",
                getTestData("/product-links.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        // check they are there
        assertProductLinks("application/xml");
    }

    private void assertProductLinks(String firstLinkType) throws Exception {
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ogcLinks",
                200);
        assertEquals("http://www.opengis.net/spec/owc/1.0/req/atom/wms", json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals(firstLinkType, json.read("$.links[0].type"));
        assertEquals(
                "${BASE_URL}/SENTINEL2/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
        assertEquals("1", json.read("$.links[0].intTest").toString());
        assertEquals("1.1", json.read("$.links[0].floatTest").toString());
        assertEquals("false", json.read("$.links[0].booleanTest").toString());
        assertEquals(
                "2015-07-01T00:00:00.000Z", json.read("$.links[0].dateTest").toString());
        assertEquals("text1", json.read("$.links[0].varcharTest").toString());
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
        assertThat(
                response.getContentAsString(),
                containsString("S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPL_N02.01"));
    }

    @Test
    public void testPutProductThumbnail() throws Exception {
        testCreateProductPost();

        // create the image
        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/thumbnail",
                getTestData("/product-thumb.jpeg"),
                MediaType.IMAGE_JPEG_VALUE);
        assertEquals(200, response.getStatus());

        // grab and check
        assertProductThumbnail("./src/test/resources/product-thumb.jpeg");
    }

    private void assertProductThumbnail(String expectedImagePath) throws Exception {
        BufferedImage actual = getAsImage(
                "rest/oseo/collections/SENTINEL2/products"
                        + "/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04"
                        + "/thumbnail",
                "image/jpeg");
        BufferedImage expected = ImageIO.read(new File(expectedImagePath));
        ImageAssert.assertEquals(actual, expected, 0);
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
        testCreateProductPost();

        // add the granules
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                getTestData("/product-granules.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        assertProductGranules("/efs/geoserver_data/coverages/sentinel/california/R1C1.tif");
    }

    @Test
    public void testPutProductGranulesWithBands() throws Exception {
        testCreateProductPost();

        // add the granules
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                getTestData("/product-granules-bands.json"),
                MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        assertProductGranules("/efs/geoserver_data/coverages/sentinel/california/R1C1.tif");
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                200);
        assertEquals("B1", json.read("$.features[0].properties.band"));
        assertEquals("B2", json.read("$.features[1].properties.band"));
    }

    private void assertProductGranules(String firstGranulePath) throws Exception {
        // grab and check
        DocumentContext json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                200);
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(Integer.valueOf(2), json.read("$.features.length()"));
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals(firstGranulePath, json.read("$.features[0].properties.location"));
        assertEquals("Feature", json.read("$.features[1].type"));
        assertEquals("Polygon", json.read("$.features[1].geometry.type"));
        assertEquals(
                "/efs/geoserver_data/coverages/sentinel/california/R1C2.tif",
                json.read("$.features[1].properties.location"));
        // parse the geojson, check the geometries have been parsed correctly
        SimpleFeatureCollection fc = GeoJSONReader.parseFeatureCollection(json.jsonString());
        assertEquals(2, fc.size());
        final SimpleFeatureIterator it = fc.features();
        SimpleFeature sf = it.next();
        assertTrue(new Envelope(10, 12, 40, 42).contains(ReferencedEnvelope.reference(sf.getBounds())));
        sf = it.next();
        assertTrue(new Envelope(10, 12, 40, 42).contains(ReferencedEnvelope.reference(sf.getBounds())));

        // check no other granule has been harmed
        json = getAsJSONPath(
                "/rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPM_N02.01/granules",
                200);
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(Integer.valueOf(4), json.read("$.features.length()"));
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals(
                "/efs/geoserver_data/coverages/sentinel/california/S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T32TPM_N02.01.tif",
                json.read("$.features[0].properties.location"));
    }

    @Test
    public void testDeleteProductGranules() throws Exception {
        testCreateProductPost();

        // now delete it
        MockHttpServletResponse response = deleteAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules");
        assertEquals(200, response.getStatus());

        // no more there now
        DocumentContext json = getAsJSONPath(
                "rest/oseo/collections/SENTINEL2/products/S2A_OPER_MSI_L1C_TL_SGS__20180101T000000_A006640_T32TPP_N02.04/granules",
                200);
        assertEquals(200, response.getStatus());
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals(Integer.valueOf(0), json.read("$.features.length()"));
    }

    @Test
    public void testCreateProductAsZip() throws Exception {
        // build all possible combinations of elements in the zip and check they all work
        HashSet<ProductPart> allProducts = new HashSet<>(asList(Product, Thumbnail, OwsLinks, Granules));
        Set<Set<ProductPart>> sets = Sets.powerSet(allProducts);

        List<HttpMethod> methods = Arrays.asList(HttpMethod.POST, HttpMethod.PUT);
        for (Set<ProductPart> parts : sets) {
            if (parts.isEmpty()) {
                continue;
            }

            LOGGER.info("Testing zip product creation with parts:" + parts);
            for (HttpMethod method : methods) {
                cleanupTestProduct();
                testCreateProductAsZip(parts, method);
            }
        }
    }

    private void testCreateProductAsZip(Set<ProductPart> parts, HttpMethod method) throws Exception {
        LOGGER.info("Testing: " + parts);
        MockHttpServletResponse response = createProductAsZip(parts, method);
        if (parts.contains(Product)) {
            assertEquals(201, response.getStatus());
            assertEquals(
                    "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/products/"
                            + PRODUCT_CREATE_UPDATE_ID,
                    response.getHeader("location"));

            assertProduct("2018-01-01T00:00:00.000Z", "2018-01-01T00:00:00.000Z");
        } else {
            assertEquals(400, response.getStatus());
            assertThat(response.getContentAsString(), containsString("product.json"));
            // failed, nothing else to check
            return;
        }

        if (parts.contains(Thumbnail)) {
            assertProductThumbnail("./src/test/resources/product-thumb.jpeg");
        }
        if (parts.contains(OwsLinks)) {
            assertProductLinks("application/xml");
        }
        if (parts.contains(Granules)) {
            assertProductGranules("/efs/geoserver_data/coverages/sentinel/california/R1C1.tif");
        }
    }

    private MockHttpServletResponse createProductAsZip(Set<ProductPart> parts, HttpMethod method) throws Exception {
        byte[] zip = buildZip(parts);

        if (method == HttpMethod.POST)
            return postAsServletResponse(
                    "rest/oseo/collections/SENTINEL2/products", zip, MediaTypeExtensions.APPLICATION_ZIP_VALUE);
        else if (method == HttpMethod.PUT)
            return putAsServletResponse(
                    "rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                    zip,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
        else throw new RuntimeException("Unsupported method: " + method);
    }

    private byte[] buildZip(Set<ProductPart> parts) throws IOException {
        byte[] zip;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (ProductPart part : parts) {
                String resource, name;
                switch (part) {
                    case Product:
                        resource = "/product.json";
                        name = "product.json";
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
                try (InputStream is = getClass().getResourceAsStream(resource)) {
                    int copy = IOUtils.copy(is, zos);
                    assertThat(copy, Matchers.greaterThan(0));
                }
                zos.closeEntry();
            }
        }
        zip = bos.toByteArray();
        return zip;
    }

    private MockHttpServletResponse putProductAsZip(Collection<ProductPart> parts) throws Exception {
        byte[] zip = buildZip(parts);

        MockHttpServletResponse response = putAsServletResponse(
                "rest/oseo/collections/SENTINEL2/products/" + PRODUCT_CREATE_UPDATE_ID,
                zip,
                MediaTypeExtensions.APPLICATION_ZIP_VALUE);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        return response;
    }

    private byte[] buildZip(Collection<ProductPart> parts) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (ProductPart part : parts) {
                String resource, name;
                switch (part) {
                    case Product:
                        resource = "/product-updated.json";
                        name = "product.json";
                        break;
                    case Thumbnail:
                        resource = "/product-thumb-updated.jpeg";
                        name = "thumbnail.jpeg";
                        break;
                    case OwsLinks:
                        resource = "/product-links-updated.json";
                        name = "owsLinks.json";
                        break;
                    case Granules:
                        resource = "/product-granules-updated.json";
                        name = "granules.json";
                        break;
                    default:
                        throw new RuntimeException("Unexpected part " + part);
                }

                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                InputStream stream = getClass().getResourceAsStream(resource);
                assertNotNull("Could not find " + resource, stream);
                int copied = IOUtils.copy(stream, zos);
                assertThat(copied, Matchers.greaterThan(0));
                zos.closeEntry();
                zos.flush();
            }
        }
        return bos.toByteArray();
    }

    private void testUpdateProductAsZip(Collection<ProductPart> parts) throws Exception {
        LOGGER.info("Testing: " + parts);
        MockHttpServletResponse response = putProductAsZip(parts);
        assertEquals(200, response.getStatus());

        if (parts.contains(Product)) {
            assertProduct("2018-05-01T00:00:00.000Z", "2018-05-01T00:00:00.000Z");
        }
        if (parts.contains(Thumbnail)) {
            assertProductThumbnail("./src/test/resources/product-thumb-updated.jpeg");
        }
        if (parts.contains(OwsLinks)) {
            assertProductLinks("text/xml");
        }
        if (parts.contains(Granules)) {
            assertProductGranules("/var/geoserver_data/coverages/sentinel/california/R1C1.tif");
        }
    }

    @Test
    public void testUpdateProductAsZipFromFullProduct() throws Exception {
        // prepare a full initial product
        Set<ProductPart> allProductParts = new LinkedHashSet<>(asList(Product, Thumbnail, OwsLinks, Granules));
        cleanupTestProduct();
        assertEquals(
                HttpStatus.CREATED.value(),
                createProductAsZip(allProductParts, HttpMethod.POST).getStatus());

        // update one items at a time
        for (ProductPart part : allProductParts) {
            testUpdateProductAsZip(asList(part));
        }
    }

    @Test
    public void testUpdateAllProductPartsAsZipFromFullProduct() throws Exception {
        // prepare a full initial product
        Set<ProductPart> allProductParts = new LinkedHashSet<>(asList(Product, Thumbnail, OwsLinks, Granules));
        cleanupTestProduct();
        assertEquals(
                HttpStatus.CREATED.value(),
                createProductAsZip(allProductParts, HttpMethod.POST).getStatus());

        // update all in one shot
        testUpdateProductAsZip(allProductParts);
    }

    @Test
    public void testUpdateProductAsZipFromBasicProduct() throws Exception {
        // prepare a basic initial product
        Set<ProductPart> initialProduct = new HashSet<>(asList(Product));
        cleanupTestProduct();
        assertEquals(
                HttpStatus.CREATED.value(),
                createProductAsZip(initialProduct, HttpMethod.POST).getStatus());

        // update/add one item at a time
        Set<ProductPart> allProductParts = new LinkedHashSet<>(asList(Product, Thumbnail, OwsLinks, Granules));
        for (ProductPart part : allProductParts) {
            testUpdateProductAsZip(asList(part));
        }
    }
}
