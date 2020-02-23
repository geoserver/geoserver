/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.geoserver.data.test.MockData.SF_PREFIX;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.namespace.QName;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.data.DataAccess;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class FeatureTypeControllerTest extends CatalogRESTTestSupport {

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @Before
    public void removePropertyStores() {
        removeStore("gs", "pds");
        removeStore("gs", "ngpds");
    }

    @Before
    public void addPrimitiveGeoFeature() throws IOException {
        revertLayer(SystemTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/workspaces/sf/featuretypes.xml");
        assertEquals(
                catalog.getFeatureTypesByNamespace(catalog.getNamespaceByPrefix("sf")).size(),
                dom.getElementsByTagName("featureType").getLength());
    }

    @Test // GEOS-9190
    public void testCreateFeatureTypeSameStoreNameDifferentWorkspace() throws Exception {
        final boolean configureFeatureType = false;
        final String ws1 = "gs";
        final String ws2 = "sf";
        // create two stores named "pds" on different workspaces
        addPropertyDataStore(ws1, configureFeatureType);
        addPropertyDataStore(ws2, configureFeatureType);

        String xml = "<featureType><name>pdsa</name><store>pds</store></featureType>";

        MockHttpServletResponse response;
        String ws1FetureTypesPath =
                BASEPATH + "/workspaces/" + ws1 + "/datastores/pds/featuretypes";
        String ws2FeatureTypesPath =
                BASEPATH + "/workspaces/" + ws2 + "/datastores/pds/featuretypes";

        response = postAsServletResponse(ws1FetureTypesPath, xml, "text/xml");
        assertEquals(201, response.getStatus());

        response = postAsServletResponse(ws2FeatureTypesPath, xml, "text/xml");
        assertEquals(201, response.getStatus());
    }

    void addPropertyDataStore(boolean configureFeatureType) throws Exception {
        addPropertyDataStore("gs", configureFeatureType);
    }

    void addPropertyDataStore(String workspace, boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH
                                + "/workspaces/"
                                + workspace
                                + "/datastores/pds/file.properties?"
                                + q,
                        zbytes.toByteArray(),
                        "application/zip");
        assertEquals(201, response.getStatus());
    }

    void addGeomlessPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsa.0='zero'|0\n");
        writer.write("ngpdsa.1='one'|1\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("ngpdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,intProperty:Integer\n");
        writer.write("ngpdsb.0='two'|2\n");
        writer.write("ngpdsb.1='trhee'|3\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("ngpdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=" + (configureFeatureType ? "all" : "none");
        put(
                BASEPATH + "/workspaces/gs/datastores/ngpds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    /** Add a property data store with multiple feature types, but only configure the first. */
    void addPropertyDataStoreOnlyConfigureFirst() throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsa.0='zero'|POINT(0 0)\n");
        writer.write("pdsa.1='one'|POINT(1 1)\n");
        writer.flush();

        zout.putNextEntry(new ZipEntry("pdsa.properties"));
        zout.write(bytes.toByteArray());
        bytes.reset();

        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("pdsb.0='two'|POINT(2 2)\n");
        writer.write("pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry(new ZipEntry("pdsb.properties"));
        zout.write(bytes.toByteArray());

        zout.flush();
        zout.close();

        String q = "configure=first";
        put(
                BASEPATH + "/workspaces/gs/datastores/pds/file.properties?" + q,
                zbytes.toByteArray(),
                "application/zip");
    }

    @Test
    public void testGetAllByDataStore() throws Exception {
        // two stores to play with
        addPropertyDataStore(true);
        addGeomlessPropertyDataStore(true);

        Document dom = getAsDOM(BASEPATH + "/workspaces/gs/datastores/pds/featuretypes.xml");

        assertEquals(2, dom.getElementsByTagName("featureType").getLength());
        assertXpathEvaluatesTo("1", "count(//featureType/name[text()='pdsa'])", dom);
        assertXpathEvaluatesTo("1", "count(//featureType/name[text()='pdsb'])", dom);
    }

    @Test
    public void testGetAllAvailable() throws Exception {
        addPropertyDataStore(false);

        Document dom =
                getAsDOM(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes.xml?list=available");
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsa'])", dom);
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsb'])", dom);
    }

    @Test
    public void testGetAllAvailableWithGeometryOnly() throws Exception {
        addGeomlessPropertyDataStore(false);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "/workspaces/gs/datastores/ngpds/featuretypes.xml?list=available");
        assertXpathEvaluatesTo("2", "count(//featureTypeName)", dom);

        dom =
                getAsDOM(
                        BASEPATH
                                + "/workspaces/gs/datastores/ngpds/featuretypes.xml?list=available_with_geom");
        assertXpathEvaluatesTo("0", "count(//featureTypeName)", dom);
    }

    /**
     * Test that a list of all feature types for a data source are returned when "list=all",
     * including both configured and unconfigured ones.
     */
    @Test
    public void testGetAllByDataStoreWithListAll() throws Exception {
        // Create a data store with only the first feature type configured.
        addPropertyDataStoreOnlyConfigureFirst();
        Document dom =
                getAsDOM(BASEPATH + "/workspaces/gs/datastores/pds/featuretypes.xml?list=all");
        assertEquals(2, dom.getElementsByTagName("featureTypeName").getLength());
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsa'])", dom);
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsb'])", dom);
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(BASEPATH + "/workspaces/sf/datastores/sf/featuretypes")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(BASEPATH + "/workspaces/sf/datastores/sf/featuretypes")
                        .getStatus());
    }

    @Test
    public void testPostToResource() throws Exception {
        addPropertyDataStore(true);
        String xml = "<featureType>" + "<name>pdsa</name>" + "</featureType>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/pds/featuretypes/pdsa",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/workspaces/sf/featuretypes/PrimitiveGeoFeature.xml");

        assertEquals("featureType", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("PrimitiveGeoFeature", "/featureType/name", dom);
        assertXpathEvaluatesTo("EPSG:4326", "/featureType/srs", dom);
        assertEquals(CRS.decode("EPSG:4326").toWKT(), xp.evaluate("/featureType/nativeCRS", dom));

        FeatureTypeInfo ft = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");

        /*
         * ReferencedEnvelope re = ft.getNativeBoundingBox(); assertXpathEvaluatesTo( re.getMinX()+"" , "/featureType/nativeBoundingBox/minx", dom );
         * assertXpathEvaluatesTo( re.getMaxX()+"" , "/featureType/nativeBoundingBox/maxx", dom ); assertXpathEvaluatesTo( re.getMinY()+"" ,
         * "/featureType/nativeBoundingBox/miny", dom ); assertXpathEvaluatesTo( re.getMaxY()+"" , "/featureType/nativeBoundingBox/maxy", dom );
         */
        ReferencedEnvelope re = ft.getLatLonBoundingBox();
        assertXpathEvaluatesTo(re.getMinX() + "", "/featureType/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo(re.getMaxX() + "", "/featureType/latLonBoundingBox/maxx", dom);
        assertXpathEvaluatesTo(re.getMinY() + "", "/featureType/latLonBoundingBox/miny", dom);
        assertXpathEvaluatesTo(re.getMaxY() + "", "/featureType/latLonBoundingBox/maxy", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/workspaces/sf/featuretypes/PrimitiveGeoFeature.json");
        JSONObject featureType = ((JSONObject) json).getJSONObject("featureType");
        assertNotNull(featureType);

        assertEquals("PrimitiveGeoFeature", featureType.get("name"));
        assertEquals(CRS.decode("EPSG:4326").toWKT(), featureType.get("nativeCRS"));
        assertEquals("EPSG:4326", featureType.get("srs"));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.html");
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        addPropertyDataStore(true);

        String dom = getAsString(BASEPATH + "/workspaces/gs/datastores/pds/featuretypes.xml");

        // System.out.println(dom);
    }

    @Test
    public void testGetWrongFeatureType() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String ds = "sf";
        String ft = "PrimitiveGeoFeaturessss";
        // Request path
        String requestPath = BASEPATH + "/workspaces/" + ws + "/featuretypes/" + ft + ".html";
        String requestPath2 =
                BASEPATH
                        + "/workspaces/"
                        + ws
                        + "/datastores/"
                        + ds
                        + "/featuretypes/"
                        + ft
                        + ".html";
        // Exception path
        String exception = "No such feature type: " + ws + "," + ft;
        String exception2 = "No such feature type: " + ws + "," + ds + "," + ft;

        // CASE 1: No datastore set

        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertContains(response.getContentAsString(), exception);

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());

        // CASE 2: datastore set

        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertEquals(404, response.getStatus());
        assertContains(response.getContentAsString(), exception2);

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testPut() throws Exception {
        String xml = "<featureType>" + "<title>new title</title>" + "</featureType>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.xml");
        assertXpathEvaluatesTo("new title", "/featureType/title", dom);

        FeatureTypeInfo ft = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertEquals("new title", ft.getTitle());
    }

    @Test
    public void testPutWithoutStore() throws Exception {
        String xml = "<featureType>" + "<title>new title</title>" + "</featureType>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/featuretypes/PrimitiveGeoFeature",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(BASEPATH + "/workspaces/sf/featuretypes/PrimitiveGeoFeature.xml");
        assertXpathEvaluatesTo("new title", "/featureType/title", dom);

        FeatureTypeInfo ft = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertEquals("new title", ft.getTitle());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertTrue(ft.isEnabled());
        boolean isAdvertised = ft.isAdvertised();
        int maxFeatures = ft.getMaxFeatures();
        int numDecimals = ft.getNumDecimals();
        boolean isOverridingServiceSRS = ft.isOverridingServiceSRS();
        boolean getSkipNumberMatched = ft.getSkipNumberMatched();
        boolean isCircularArcPresent = ft.isCircularArcPresent();

        String xml = "<featureType>" + "<title>new title</title>" + "</featureType>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        ft = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertTrue(ft.isEnabled());
        assertEquals(isAdvertised, ft.isAdvertised());
        assertEquals(maxFeatures, ft.getMaxFeatures());
        assertEquals(numDecimals, ft.getNumDecimals());
        assertEquals(isOverridingServiceSRS, ft.isOverridingServiceSRS());
        assertEquals(getSkipNumberMatched, ft.getSkipNumberMatched());
        assertEquals(isCircularArcPresent, ft.isCircularArcPresent());
    }

    /**
     * Check feature type modification involving calculation of bounds.
     *
     * <p>Update: Ensure feature type modification does not reset ResourcePool DataStoreCache
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testPutWithCalculation() throws Exception {
        DataStoreInfo dataStoreInfo = getCatalog().getDataStoreByName("sf", "sf");
        String dataStoreId = dataStoreInfo.getId();
        DataAccess dataAccessBefore = dataStoreInfo.getDataStore(null);
        assertSame(
                "ResourcePool DataStoreCache",
                dataAccessBefore,
                getCatalog().getResourcePool().getDataStoreCache().get(dataStoreId));

        String clearLatLonBoundingBox =
                "<featureType>"
                        + "<nativeBoundingBox>"
                        + "<minx>-180.0</minx>"
                        + "<maxx>180.0</maxx>"
                        + "<miny>-90.0</miny>"
                        + "<maxy>90.0</maxy>"
                        + "<crs>EPSG:4326</crs>"
                        + "</nativeBoundingBox>"
                        + "<latLonBoundingBox/>"
                        + "</featureType>";

        String path = BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature";
        MockHttpServletResponse response =
                putAsServletResponse(path, clearLatLonBoundingBox, "text/xml");
        assertEquals(
                "Couldn't remove lat/lon bounding box:\n" + response.getContentAsString(),
                200,
                response.getStatus());

        Document dom = getAsDOM(path + ".xml");
        assertXpathEvaluatesTo("0.0", "/featureType/latLonBoundingBox/minx", dom);

        // confirm ResourcePoool cache of DataStore is unchanged
        DataAccess dataAccessAfter = getCatalog().getDataStoreByName("sf", "sf").getDataStore(null);
        assertSame("ResourcePool DataStoreCache check 1", dataAccessBefore, dataAccessAfter);
        assertSame(
                "ResourcePool DataStoreCache",
                dataAccessBefore,
                getCatalog().getResourcePool().getDataStoreCache().get(dataStoreId));

        String updateNativeBounds =
                "<featureType>"
                        + "<srs>EPSG:3785</srs>"
                        + "<nativeBoundingBox>"
                        + "<minx>-20037508.34</minx>"
                        + "<maxx>20037508.34</maxx>"
                        + "<miny>-20037508.34</miny>"
                        + "<maxy>20037508.34</maxy>"
                        + "<crs>EPSG:3785</crs>"
                        + "</nativeBoundingBox>"
                        + "</featureType>";

        response = putAsServletResponse(path + ".xml", updateNativeBounds, "text/xml");
        assertEquals(
                "Couldn't update native bounding box: \n" + response.getContentAsString(),
                200,
                response.getStatus());
        dom = getAsDOM(path + ".xml");
        print(dom);
        assertXpathExists("/featureType/latLonBoundingBox/minx[text()!='0.0']", dom);

        dataAccessAfter = getCatalog().getDataStoreByName("sf", "sf").getDataStore(null);
        assertSame("ResourcePool DataStoreCache check 2", dataAccessBefore, dataAccessAfter);
        assertSame(
                "ResourcePool DataStoreCache",
                dataAccessBefore,
                getCatalog().getResourcePool().getDataStoreCache().get(dataStoreId));
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<featureType>" + "<title>new title</title>" + "</featureType>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/NonExistant",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        String featureTypeId = featureType.getId();
        String dataStoreId = featureType.getStore().getId();
        Name name = featureType.getFeatureType().getName();

        assertNotNull("PrmitiveGeoFeature available", featureType);
        for (LayerInfo l : catalog.getLayers(featureType)) {
            catalog.remove(l);
        }
        assertEquals(
                200,
                deleteAsServletResponse(
                                BASEPATH
                                        + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature")
                        .getStatus());
        assertNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));

        if (catalog.getResourcePool().getFeatureTypeAttributeCache().containsKey(featureTypeId)) {
            List<AttributeTypeInfo> attributesList =
                    catalog.getResourcePool().getFeatureTypeAttributeCache().get(featureTypeId);
            assertNull("attributes cleared", attributesList);
        }
        if (catalog.getResourcePool().getDataStoreCache().containsKey(dataStoreId)) {
            DataAccess dataStore = catalog.getResourcePool().getDataStoreCache().get(dataStoreId);
            List<Name> names = dataStore.getNames();
            assertTrue(names.contains(name));
        }
    }

    @Test
    public void testDeleteWithoutStore() throws Exception {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        String featureTypeId = featureType.getId();
        String dataStoreId = featureType.getStore().getId();
        Name name = featureType.getFeatureType().getName();

        assertNotNull("PrmitiveGeoFeature available", featureType);
        for (LayerInfo l : catalog.getLayers(featureType)) {
            catalog.remove(l);
        }
        assertEquals(
                200,
                deleteAsServletResponse(
                                BASEPATH + "/workspaces/sf/featuretypes/PrimitiveGeoFeature")
                        .getStatus());
        assertNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));

        if (catalog.getResourcePool().getFeatureTypeAttributeCache().containsKey(featureTypeId)) {
            List<AttributeTypeInfo> attributesList =
                    catalog.getResourcePool().getFeatureTypeAttributeCache().get(featureTypeId);
            assertNull("attributes cleared", attributesList);
        }
        if (catalog.getResourcePool().getDataStoreCache().containsKey(dataStoreId)) {
            DataAccess dataStore = catalog.getResourcePool().getDataStoreCache().get(dataStoreId);
            List<Name> names = dataStore.getNames();
            assertTrue(names.contains(name));
        }
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/NonExistant")
                        .getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        assertNotNull(catalog.getLayerByName("sf:PrimitiveGeoFeature"));

        assertEquals(
                403,
                deleteAsServletResponse(
                                BASEPATH
                                        + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature")
                        .getStatus());
        assertEquals(
                200,
                deleteAsServletResponse(
                                BASEPATH
                                        + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature?recurse=true")
                        .getStatus());

        assertNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        assertNull(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
    }

    @Test
    public void testPostGeometrylessFeatureType() throws Exception {
        addGeomlessPropertyDataStore(false);

        String xml = "<featureType>" + "<name>ngpdsa</name>" + "</featureType>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/gs/datastores/ngpds/featuretypes", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/datastores/ngpds/featuretypes/ngpdsa"));
    }

    @Test
    public void testDeleteWsNotSameAsStoreName() throws Exception {
        // create a feature type whose store name is not same as workspace name
        testPostGeometrylessFeatureType();

        assertEquals(
                200,
                deleteAsServletResponse(
                                BASEPATH
                                        + "/workspaces/gs/datastores/ngpds/featuretypes/ngpdsa?recurse=true")
                        .getStatus());
        assertNull(catalog.getFeatureTypeByName("gs", "ngpdsa"));
    }

    @Test
    public void testCreateFeatureType() throws Exception {
        String xml =
                "<featureType>\n"
                        + "  <name>states</name>\n"
                        + "  <nativeName>states</nativeName>\n"
                        + "  <namespace>\n"
                        + "    <name>cite</name>\n"
                        + "  </namespace>\n"
                        + "  <title>USA Population</title>\n"
                        + "  <srs>EPSG:4326</srs>\n"
                        + "  <attributes>\n"
                        + "    <attribute>\n"
                        + "      <name>the_geom</name>\n"
                        + "      <binding>org.locationtech.jts.geom.MultiPolygon</binding>\n"
                        + "    </attribute>\n"
                        + "    <attribute>\n"
                        + "      <name>STATE_NAME</name>\n"
                        + "      <binding>java.lang.String</binding>\n"
                        + "      <length>25</length>\n"
                        + "    </attribute>\n"
                        + "    <attribute>\n"
                        + "      <name>LAND_KM</name>\n"
                        + "      <binding>java.lang.Double</binding>\n"
                        + "    </attribute>\n"
                        + "  </attributes>\n"
                        + "</featureType>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        BASEPATH + "/workspaces/cite/datastores/default/featuretypes",
                        xml,
                        "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/cite/datastores/default/featuretypes/states"));

        FeatureTypeInfo ft = catalog.getFeatureTypeByName("cite", "states");
        assertNotNull(ft);
        FeatureType schema = ft.getFeatureType();
        assertEquals("states", schema.getName().getLocalPart());
        assertEquals(
                catalog.getNamespaceByPrefix("cite").getURI(), schema.getName().getNamespaceURI());
        assertEquals(3, schema.getDescriptors().size());
        assertNotNull(schema.getDescriptor("the_geom"));
        assertEquals(MultiPolygon.class, schema.getDescriptor("the_geom").getType().getBinding());
        assertNotNull(schema.getDescriptor("LAND_KM"));
        assertEquals(Double.class, schema.getDescriptor("LAND_KM").getType().getBinding());
    }

    @Test
    public void testRoundTripFeatureTypeXML() throws Exception {
        // Fetch the feature directly from the catalog
        FeatureTypeInfo before = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");

        // Do a round-trip GET and PUT of the resource
        String xml =
                getAsString(
                        BASEPATH
                                + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        // Fetch the feature from the catalog again, and ensure nothing changed.
        FeatureTypeInfo after = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertEquals(before, after);
    }

    @Test
    public void testRoundTripFeatureTypJSONL() throws Exception {
        // Fetch the feature directly from the catalog
        FeatureTypeInfo before = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");

        // Do a round-trip GET and PUT of the resource
        String json =
                getAsString(
                        BASEPATH
                                + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.json");
        // System.out.println(json);
        MockHttpServletResponse response =
                putAsServletResponse(
                        BASEPATH + "/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());

        // Fetch the feature from the catalog again, and ensure nothing changed.
        FeatureTypeInfo after = catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        assertEquals(before, after);
    }

    @Test
    public void testGetWithMultipleStore() throws Exception {
        QName geometryless = CiteTestData.GEOMETRYLESS;
        QName qName =
                new QName(geometryless.getNamespaceURI(), geometryless.getLocalPart(), SF_PREFIX);
        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();
        props.put(SystemTestData.LayerProperty.STORE, "tempStore");
        getTestData().addVectorLayer(qName, props, catalog);

        MockHttpServletResponse layerStore1 =
                getAsServletResponse(ROOT_PATH + "/workspaces/sf/featuretypes/GenericEntity.json");
        MockHttpServletResponse layerStore2 =
                getAsServletResponse(ROOT_PATH + "/workspaces/sf/featuretypes/Geometryless.json");

        assertEquals(layerStore1.getStatus(), 200);
        assertEquals(layerStore2.getStatus(), 200);
    }

    public static void assertContains(String message, String contains) {
        assertTrue(
                "Expected \"" + message + "\" to contain \"" + contains + "\"",
                message.contains(contains));
    }

    /** Tests services disabled on layer-resource */
    @Test
    public void testEnabledServicesOnLayer() throws Exception {
        disableServicesOnBuildings();
        Document dom =
                getAsDOM(
                        ROOT_PATH + "/workspaces/cite/datastores/cite/featuretypes/Buildings.xml",
                        200);
        assertXpathEvaluatesTo("true", "//serviceConfiguration", dom);
        assertXpathExists("//disabledServices/string[.='WFS']", dom);
        assertXpathExists("//disabledServices/string[.='CSW']", dom);
        enableServicesOnBuildings();
    }

    private void disableServicesOnBuildings() {
        LayerInfo linfo = getCatalog().getLayerByName("Buildings");
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(true);
        ri.setDisabledServices(new ArrayList<>(Arrays.asList("WFS", "CSW")));
        getCatalog().save(ri);
        getCatalog().save(linfo);
    }

    private void enableServicesOnBuildings() {
        LayerInfo linfo = getCatalog().getLayerByName("Buildings");
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(false);
        ri.setDisabledServices(new ArrayList<>());
        getCatalog().save(ri);
        getCatalog().save(linfo);
    }
}
