/* (c) 2013 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterDataTest;
import org.geoserver.importer.ImporterInfoDAO;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.impl.GeoServerUser;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class ImporterIntegrationTest extends ImporterTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void createH2Store() throws Exception {
        // create the store for the indirect imports
        String wsName = getCatalog().getDefaultWorkspace().getName();
        createH2DataStore(wsName, "h2");
        // remove any callback set to check the request spring context
        RequestContextListener listener = applicationContext.getBean(RequestContextListener.class);
        listener.setCallBack(null);
    }

    @Test
    public void testDirectWrongFile() throws Exception {
        // set a callback to check that the request spring context is passed to the job thread
        RequestContextListener listener = applicationContext.getBean(RequestContextListener.class);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication());

        final boolean[] invoked = {false};
        listener.setCallBack(
                (request, user, resource) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(request, notNullValue());
                    assertThat(resource, notNullValue());
                    assertThat(auth, notNullValue());
                    invoked[0] = true;
                });
        File dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        File geotiffFile = new File(dir, "EmissiveCampania.tif");
        String wrongPath = jsonSafePath(geotiffFile).replace("/EmissiveCampania", "/foobar");

        String wsName = getCatalog().getDefaultWorkspace().getName();

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + wrongPath
                        + "\"\n"
                        + "      },"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        // initialize the import
        MockHttpServletResponse servletResponse =
                postAsServletResponse("/rest/imports", contextDefinition, "application/json");
        JSONObject json = (JSONObject) json(servletResponse);
        int importId = json.getJSONObject("import").getInt("id");
        json = (JSONObject) getAsJSON("/rest/imports/" + importId);

        // Expect an error
        assertEquals("INIT_ERROR", json.getJSONObject("import").getString("state"));
        assertEquals("input == null!", json.getJSONObject("import").getString("message"));
    }

    @Test
    public void testDirectWrongDir() throws Exception {
        // set a callback to check that the request spring context is passed to the job thread
        RequestContextListener listener = applicationContext.getBean(RequestContextListener.class);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication());

        final boolean[] invoked = {false};
        listener.setCallBack(
                (request, user, resource) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(request, notNullValue());
                    assertThat(resource, notNullValue());
                    assertThat(auth, notNullValue());
                    invoked[0] = true;
                });
        File dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        File geotiffFile = new File(dir, "EmissiveCampania.tif");
        String wrongPath =
                jsonSafePath(geotiffFile).replace("/EmissiveCampania", "bar/EmissiveCampania");

        String wsName = getCatalog().getDefaultWorkspace().getName();

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + wrongPath
                        + "\"\n"
                        + "      },"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        // initialize the import
        MockHttpServletResponse servletResponse =
                postAsServletResponse("/rest/imports", contextDefinition, "application/json");

        assertEquals(500, servletResponse.getStatus());
    }

    @Test
    public void testDefaultTransformationsInit() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "locations.csv");

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(locations)
                        + "\"\n"
                        + "      },\n"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      },\n"
                        + "      \"transforms\": [\n"
                        + "        {\n"
                        + "          \"type\": \"AttributesToPointGeometryTransform\",\n"
                        + "          \"latField\": \"LAT\","
                        + "          \"lngField\": \"LON\""
                        + "        }\n"
                        + "      ]"
                        + "   }\n"
                        + "}";
        // @formatter:on

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports", contextDefinition, "application/json"));
        // print(json);
        int importId = json.getJSONObject("import").getInt("id");

        checkLatLonTransformedImport(importId);
    }

    @Test
    public void testDefaultTransformationsUpload() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "locations.csv");

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      },\n"
                        + "      \"transforms\": [\n"
                        + "        {\n"
                        + "          \"type\": \"AttributesToPointGeometryTransform\",\n"
                        + "          \"latField\": \"LAT\","
                        + "          \"lngField\": \"LON\""
                        + "        }\n"
                        + "      ]"
                        + "   }\n"
                        + "}";
        // @formatter:on

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports", contextDefinition, "application/json"));
        // print(json);
        int importId = json.getJSONObject("import").getInt("id");

        // upload the data
        String body =
                "--AaB03x\r\nContent-Disposition: form-data; name=filedata; filename=data.csv\r\n"
                        + "Content-Type: text/plain\n"
                        + "\r\n\r\n"
                        + FileUtils.readFileToString(locations, "UTF-8")
                        + "\r\n\r\n--AaB03x--";

        post("/rest/imports/" + importId + "/tasks", body, "multipart/form-data; boundary=AaB03x");

        checkLatLonTransformedImport(importId);
    }

    private void checkLatLonTransformedImport(int importId) throws IOException {
        ImportContext context = importer.getContext(importId);
        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);

        TransformChain transformChain = task.getTransform();
        assertThat(
                transformChain.getTransforms().get(0),
                CoreMatchers.instanceOf(AttributesToPointGeometryTransform.class));
        assertEquals(ImportTask.State.NO_CRS, task.getState());

        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");

        importer.changed(task);
        assertEquals(ImportTask.State.READY, task.getState());

        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());
        FeatureTypeInfo fti = (FeatureTypeInfo) resource;
        SimpleFeatureType featureType = (SimpleFeatureType) fti.getFeatureType();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertNotNull("Expecting geometry", geometryDescriptor);
        assertEquals("Invalid geometry name", "location", geometryDescriptor.getLocalName());
        assertEquals(3, featureType.getAttributeCount());
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource =
                fti.getFeatureSource(null, null);
        FeatureCollection<? extends FeatureType, ? extends Feature> features =
                featureSource.getFeatures();
        assertEquals(9, features.size());
        FeatureIterator<? extends Feature> featureIterator = features.features();
        assertTrue("Expected features", featureIterator.hasNext());
        SimpleFeature feature = (SimpleFeature) featureIterator.next();
        assertNotNull(feature);
        assertEquals("Invalid city attribute", "Trento", feature.getAttribute("CITY"));
        assertEquals("Invalid number attribute", 140, feature.getAttribute("NUMBER"));
        Object geomAttribute = feature.getAttribute("location");
        assertNotNull("Expected geometry", geomAttribute);
        Point point = (Point) geomAttribute;
        Coordinate coordinate = point.getCoordinate();
        assertEquals("Invalid x coordinate", 11.12, coordinate.x, 0.1);
        assertEquals("Invalid y coordinate", 46.07, coordinate.y, 0.1);
        featureIterator.close();
    }

    @Test
    public void testDirectExecuteAsync() throws Exception {
        testDirectExecuteInternal(true);
    }

    @Test
    public void testDirectExecuteSync() throws Exception {
        testDirectExecuteInternal(false);
    }

    void testDirectExecuteInternal(boolean async) throws Exception {

        // set a callback to check that the request spring context is passed to the job thread
        RequestContextListener listener = applicationContext.getBean(RequestContextListener.class);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication());

        final boolean[] invoked = {false};
        listener.setCallBack(
                (request, user, resource) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(request, notNullValue());
                    assertThat(resource, notNullValue());
                    assertThat(auth, notNullValue());
                    invoked[0] = true;
                });

        File gmlFile = file("gml/poi.gml2.gml");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(gmlFile)
                        + "\"\n"
                        + "      },"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports?exec=true" + (async ? "&async=true" : ""),
                                        contextDefinition,
                                        "application/json"));
        // print(json);
        String state = null;
        int importId;
        if (async) {
            importId = json.getJSONObject("import").getInt("id");
            for (int i = 0; i < 60 * 2 * 2; i++) {
                json = (JSONObject) getAsJSON("/rest/imports/" + importId);
                // print(json);
                state = json.getJSONObject("import").getString("state");
                if ("INIT".equals(state) || "RUNNING".equals(state) || "PENDING".equals(state)) {
                    Thread.sleep(500);
                }
            }
        } else {
            state = json.getJSONObject("import").getString("state");
            importId = json.getJSONObject("import").getInt("id");
        }
        Thread.sleep(500);
        assertEquals("COMPLETE", state);
        assertThat(invoked[0], is(true));
        checkPoiImport();

        // Test delete
        MockHttpServletResponse resp = deleteAsServletResponse("/rest/imports/" + importId);
        assertEquals(204, resp.getStatus());

        // check it was actually deleted
        MockHttpServletResponse getAgain = getAsServletResponse("/rest/imports/" + importId);
        assertEquals(404, getAgain.getStatus());
    }

    @Test
    public void testDirectExecutePhasedAsync() throws Exception {
        testDirectExecutePhasedInternal(true);
    }

    @Test
    public void testDirectExecutePhasedSync() throws Exception {
        testDirectExecutePhasedInternal(false);
    }

    void testDirectExecutePhasedInternal(boolean async) throws Exception {

        // set a callback to check that the request spring context is passed to the job thread
        RequestContextListener listener = applicationContext.getBean(RequestContextListener.class);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication());

        final boolean[] invoked = {false};
        listener.setCallBack(
                (request, user, resource) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(request, notNullValue());
                    assertThat(resource, notNullValue());
                    assertThat(auth, notNullValue());
                    invoked[0] = true;
                });

        File gmlFile = file("gml/poi.gml2.gml");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(gmlFile)
                        + "\"\n"
                        + "      },"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        // initialize the import
        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports" + (async ? "?async=true" : ""),
                                        contextDefinition,
                                        "application/json"));
        // print(json);
        String state = null;
        int importId;
        importId = json.getJSONObject("import").getInt("id");

        // wait until PENDING:
        if (async) {
            for (int i = 0; i < 60 * 2 * 2; i++) {
                json = (JSONObject) getAsJSON("/rest/imports/" + importId);
                // print(json);
                state = json.getJSONObject("import").getString("state");
                if ("INIT".equals(state)) {
                    Thread.sleep(500);
                }
            }
        }

        assertThat(invoked[0], is(true));
        invoked[0] = false;

        // run the import
        postAsServletResponse(
                "/rest/imports/" + importId + (async ? "?async=true" : ""), "", "application/json");

        if (async) {
            for (int i = 0; i < 60 * 2 * 2; i++) {
                json = (JSONObject) getAsJSON("/rest/imports/" + importId);
                // print(json);
                state = json.getJSONObject("import").getString("state");
                if ("INIT".equals(state) || "RUNNING".equals(state) || "PENDING".equals(state)) {
                    Thread.sleep(500);
                }
            }
        } else {
            MockHttpServletResponse response = getAsServletResponse("/rest/imports/" + importId);
            assertEquals("application/json", response.getContentType());
            json = (JSONObject) json(response);
            state = json.getJSONObject("import").getString("state");
        }
        Thread.sleep(500);
        assertEquals("COMPLETE", state);
        assertThat(invoked[0], is(true));
        checkPoiImport();

        // test delete
        MockHttpServletResponse resp = deleteAsServletResponse("/rest/imports/" + importId);
        assertEquals(204, resp.getStatus());
    }

    protected Authentication createAuthentication() {
        GeoServerUser anonymous = GeoServerUser.createAnonymous();
        List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
        roles.addAll(anonymous.getAuthorities());
        AnonymousAuthenticationToken auth =
                new AnonymousAuthenticationToken("geoserver", anonymous.getUsername(), roles);
        return auth;
    }

    private String jsonSafePath(File gmlFile) throws IOException {
        return gmlFile.getCanonicalPath().replace('\\', '/');
    }

    private void checkPoiImport() throws Exception {
        FeatureTypeInfo fti = getCatalog().getResourceByName("poi", FeatureTypeInfo.class);
        assertNotNull(fti);
        SimpleFeatureType featureType = (SimpleFeatureType) fti.getFeatureType();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertEquals(
                "Expecting a point geometry",
                Point.class,
                geometryDescriptor.getType().getBinding());
        assertEquals(4, featureType.getAttributeCount());

        // read the features, check they are in the right order
        SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
        SimpleFeatureCollection fc = fs.getFeatures(CQL.toFilter("NAME = 'museam'"));
        assertEquals(1, fc.size());
        SimpleFeature sf = DataUtilities.first(fc);
        Point p = (Point) sf.getDefaultGeometry();
        assertEquals(-74.0104611, p.getX(), 1e-6);
        assertEquals(40.70758763, p.getY(), 1e-6);
    }

    @Test
    public void testImportGranuleInEmptyMosaic() throws Exception {
        Catalog catalog = getCatalog();

        // prepare an empty mosaic
        File root = getTestData().getDataDirectoryRoot();
        String mosaicName = "emptyMosaic";
        File mosaicRoot = new File(root, mosaicName);
        ensureClean(mosaicRoot);
        File granulesRoot = new File(root, mosaicName + "_granules");
        ensureClean(granulesRoot);
        Properties props = new Properties();
        props.put("SPI", "org.geotools.data.h2.H2DataStoreFactory");
        props.put("database", "empty");
        try (FileOutputStream fos =
                new FileOutputStream(new File(mosaicRoot, "datastore.properties"))) {
            props.store(fos, null);
        }
        props.clear();
        props.put("CanBeEmpty", "true");
        try (FileOutputStream fos =
                new FileOutputStream(new File(mosaicRoot, "indexer.properties"))) {
            props.store(fos, null);
        }
        CatalogBuilder cb = new CatalogBuilder(catalog);
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        cb.setWorkspace(ws);

        CoverageStoreInfo store = cb.buildCoverageStore(mosaicName);
        store.setURL("./" + mosaicName);
        store.setType("ImageMosaic");
        catalog.save(store);

        // put a granule in the mosaic
        unpack("geotiff/EmissiveCampania.tif.bz2", granulesRoot);
        File granule = new File(granulesRoot, "EmissiveCampania.tif");

        store = catalog.getCoverageStoreByName(mosaicName);

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(granule.getAbsoluteFile())
                        + "\"\n"
                        + "      },"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \""
                        + store.getName()
                        + "\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        // sync execution
        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports?exec=true",
                                        contextDefinition,
                                        "application/json"));
        // print(json);
        String state = json.getJSONObject("import").getString("state");
        assertEquals("COMPLETE", state);

        // check the import produced a granule
        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) store.getGridCoverageReader(null, null);
        GranuleSource granules = reader.getGranules(reader.getGridCoverageNames()[0], true);
        assertEquals(1, granules.getCount(Query.ALL));

        // check we now also have a layer
        LayerInfo layer = catalog.getLayerByName(mosaicName);
        assertNotNull(layer);
    }

    /** Attribute computation integration test */
    @Test
    public void testAttributeCompute() throws Exception {
        // create H2 store to act as a target
        DataStoreInfo h2Store =
                createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "computeDB");

        // create context with default name
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(0l);
        context.setTargetStore(h2Store);
        importer.changed(context);
        importer.update(context, new SpatialFile(new File(dir, "archsites.shp")));

        // add a transformation to compute a new attribute
        String json =
                "{\n"
                        + "  \"type\": \"AttributeComputeTransform\",\n"
                        + "  \"field\": \"label\",\n"
                        + "  \"fieldType\": \"java.lang.String\",\n"
                        + "  \"cql\": \"'Test string'\"\n"
                        + "}";

        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/imports/"
                                + context.getId()
                                + "/tasks/0/transforms",
                        json,
                        "application/json");
        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // run it
        context = importer.getContext(context.getId());
        importer.run(context);

        // check created type, layer and database table
        DataStore store = (DataStore) h2Store.getDataStore(null);
        SimpleFeatureSource fs = store.getFeatureSource("archsites");
        assertNotNull(fs.getSchema().getType("label"));
        SimpleFeature first = DataUtilities.first(fs.getFeatures());
        assertEquals("Test string", first.getAttribute("label"));
    }

    private void ensureClean(File mosaicRoot) throws IOException {
        if (mosaicRoot.exists()) {
            FileUtils.deleteDirectory(mosaicRoot);
        }
        mosaicRoot.mkdirs();
    }

    @Test
    public void testUploadRootExternal() throws Exception {
        File dirFromEnv = null;
        try {
            // Let's now override the external folder through the Environment variable. This takes
            // precedence on .properties
            System.setProperty(ImporterInfoDAO.UPLOAD_ROOT_KEY, "env_uploads");
            importer.reloadConfiguration();
            assertNotNull(importer.getUploadRoot());

            // the target layer is not there
            assertNull(getCatalog().getLayerByName("archsites"));

            // create context with default name
            File dir = unpack("shape/archsites_epsg_prj.zip");
            ImportContext context = importer.createContext(0l);
            importer.changed(context);
            importer.update(context, new SpatialFile(new File(dir, "archsites.shp")));

            // run it
            context = importer.getContext(context.getId());
            importer.run(context);

            // check the layer has been created
            assertNotNull(getCatalog().getLayerByName("archsites"));

            // verify the file has been placed under the uploaded root specified on Env vars
            dirFromEnv = Resources.directory(Resources.fromPath("env_uploads"));
            // ... and ensure it is the same as defined on the .properties file
            assertEquals(dirFromEnv, importer.getUploadRoot());

            // ... and that the "archsites_epsg_prj" data has been stored inside that folder
            for (String subFolder : dirFromEnv.list()) {
                File archsites = new File(subFolder, "archsites.shp");
                assertTrue(archsites.exists());
                break;
            }
        } finally {
            if (dirFromEnv != null && dirFromEnv.exists()) {
                FileUtils.deleteQuietly(dirFromEnv);
            }
            if (System.getProperty(ImporterInfoDAO.UPLOAD_ROOT_KEY) != null) {
                System.clearProperty(ImporterInfoDAO.UPLOAD_ROOT_KEY);
            }
        }
    }

    @Test
    public void testRunPostScript() throws Exception {
        // check if bash is there
        Assume.assumeTrue(
                "Could not find sh in path, skipping", ImporterDataTest.checkShellAvailable());
        // even with bash available, the test won't work on windows as it won't know
        // how to run the .sh out of the box
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        // the target layer is not there
        assertNull(getCatalog().getLayerByName("archsites"));

        // write out a simple shell script in the data dir and make it executable
        File scripts = getDataDirectory().findOrCreateDir("importer", "scripts");
        File script = new File(scripts, "test.sh");
        FileUtils.writeStringToFile(script, "touch test.properties\n", "UTF-8");
        script.setExecutable(true, true);

        // create context with default name
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(0l);
        importer.changed(context);
        importer.update(context, new SpatialFile(new File(dir, "archsites.shp")));

        // add a transformation to run post script
        String json =
                "{\n"
                        + "  \"type\": \"PostScriptTransform\",\n"
                        + "  \"name\": \"test.sh\"\n"
                        + "}";

        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/imports/"
                                + context.getId()
                                + "/tasks/0/transforms",
                        json,
                        "application/json");
        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // run it
        context = importer.getContext(context.getId());
        importer.run(context);

        // check the layer has been created
        assertNotNull(getCatalog().getLayerByName("archsites"));

        // verify the script also run
        File testFile = new File(scripts, "test.properties");
        assertTrue(testFile.exists());
    }

    @Test
    public void testRunPostScriptWithOptions() throws Exception {
        // check if bash is there
        Assume.assumeTrue(
                "Could not find sh in path, skipping", ImporterDataTest.checkShellAvailable());
        // even with bash available, the test won't work on windows as it won't know
        // how to run the .sh out of the box
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        // the target layer is not there
        assertNull(getCatalog().getLayerByName("archsites"));

        // write out a simple shell script in the data dir and make it executable
        File scripts = getDataDirectory().findOrCreateDir("importer", "scripts");
        File script = new File(scripts, "test.sh");
        FileUtils.writeStringToFile(script, "touch $1\n", "UTF-8");
        script.setExecutable(true, true);

        // create context with default name
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(0l);
        importer.changed(context);
        importer.update(context, new SpatialFile(new File(dir, "archsites.shp")));

        // add a transformation to run post script
        String json =
                "{\n"
                        + "  \"type\": \"PostScriptTransform\",\n"
                        + "  \"name\": \"test.sh\",\n"
                        + "  \"options\": [\"test.abc\"]"
                        + "}";

        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/imports/"
                                + context.getId()
                                + "/tasks/0/transforms",
                        json,
                        "application/json");
        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // run it
        context = importer.getContext(context.getId());
        importer.run(context);

        // check the layer has been created
        assertNotNull(getCatalog().getLayerByName("archsites"));

        // verify the script also run
        File testFile = new File(scripts, "test.abc");
        assertTrue(testFile.exists());
    }

    @Test
    public void testRunWithTimeDimension() throws Exception {
        Catalog cat = getCatalog();

        DataStoreInfo ds = createH2DataStore(cat.getDefaultWorkspace().getName(), "ming");

        // the target layer is not there
        assertNull(getCatalog().getLayerByName("ming_time"));

        // create context with default name
        File dir = unpack("shape/ming_time.zip");
        ImportContext context = importer.createContext(0l);
        importer.changed(context);
        importer.update(context, new SpatialFile(new File(dir, "ming_time.shp")));
        context.setTargetStore(ds);

        assertEquals(1, context.getTasks().size());

        context.getTasks().get(0).getData().setCharsetEncoding("UTF-8");

        // add a transformation to run post script
        String json =
                "{\n"
                        + "  \"type\": \"DateFormatTransform\",\n"
                        + "  \"field\": \"Year_Date\",\n"
                        + "  \"presentation\": \"DISCRETE_INTERVAL\""
                        + "}";

        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/imports/"
                                + context.getId()
                                + "/tasks/0/transforms",
                        json,
                        "application/json");
        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // run it
        context = importer.getContext(context.getId());
        ImportTask task = context.getTasks().get(0);
        task.setDirect(false);
        task.setStore(ds);
        importer.changed(task);
        assertEquals(ImportTask.State.READY, task.getState());

        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        // check the layer has been created
        LayerInfo layer = cat.getLayerByName("ming_time");
        assertNotNull(layer);

        ResourceInfo resource = layer.getResource();

        // verify the TIME dimension has benn defined
        MetadataMap md = resource.getMetadata();
        assertNotNull(md);
        assertTrue(md.containsKey("time"));

        DimensionInfo timeDimension = (DimensionInfo) md.get("time");
        assertNotNull(timeDimension);

        assertEquals(timeDimension.getAttribute(), "Year_Date");
        assertEquals(timeDimension.getPresentation(), DimensionPresentation.DISCRETE_INTERVAL);
    }

    @Test
    public void testIndirectImportTempCleanup() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "locations.csv");

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(locations)
                        + "\"\n"
                        + "      },\n"
                        + "      targetStore: {\n"
                        + "        dataStore: {\n"
                        + "        name: \"h2\",\n"
                        + "        }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports", contextDefinition, "application/json"));
        // print(json);
        int importId = json.getJSONObject("import").getInt("id");

        ImportContext context = importer.getContext(importId);
        assertEquals(ImportContext.State.PENDING, context.getState());

        assertTrue(new File(context.getUploadDirectory().getFile(), ".locking").exists());

        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        importer.changed(task);
        assertEquals(ImportTask.State.READY, task.getState());
        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());
        assertTrue(context.getState() == ImportContext.State.COMPLETE);

        assertFalse(new File(context.getUploadDirectory().getFile(), ".locking").exists());
        assertTrue(new File(context.getUploadDirectory().getFile(), ".clean-me").exists());
    }

    @Test
    public void testDirectImportTempCleanup() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "locations.csv");

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(locations)
                        + "\"\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        // @formatter:on

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports", contextDefinition, "application/json"));
        // print(json);
        int importId = json.getJSONObject("import").getInt("id");

        ImportContext context = importer.getContext(importId);
        assertEquals(ImportContext.State.PENDING, context.getState());

        assertTrue(new File(context.getUploadDirectory().getFile(), ".locking").exists());

        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        importer.changed(task);
        assertEquals(ImportTask.State.READY, task.getState());
        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());
        assertTrue(context.getState() == ImportContext.State.COMPLETE);

        assertTrue(new File(context.getUploadDirectory().getFile(), ".locking").exists());
    }

    // GEOS-8869 - Importer Converter can override normal layer converter
    @Test
    public void testLayerRest() throws Exception {
        Catalog catalog = getCatalog();
        getTestData().addVectorLayer(MockData.BASIC_POLYGONS, catalog);

        // Normal layer converter can handle styles, but the importer one ignores them,
        // so we can use this to determine which one is getting used.
        StyleInfo generic = catalog.getStyleByName("generic");
        StyleInfo polygon = catalog.getStyleByName("polygon");

        NameImpl basicPolygonsName =
                new NameImpl(
                        MockData.BASIC_POLYGONS.getPrefix(),
                        MockData.BASIC_POLYGONS.getLocalPart());
        LayerInfo basicPolygons = catalog.getLayerByName(basicPolygonsName);

        // initialize to a known default style
        basicPolygons.setDefaultStyle(generic);
        catalog.save(basicPolygons);

        assertEquals(
                generic.getId(),
                catalog.getLayerByName(basicPolygonsName).getDefaultStyle().getId());

        // Do a JSON PUT to change the default style
        String contextDefinition =
                "{\"layer\": {\n"
                        + "  \"name\": \"BasicPolygons\",\n"
                        + "  \"defaultStyle\":   {\n"
                        + "    \"name\": \"polygon\"\n"
                        + "  },\n"
                        + "}}";

        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/layers/" + basicPolygonsName.toString(),
                        contextDefinition,
                        "application/json");

        assertEquals(200, response.getStatus());
        assertEquals(
                polygon.getName(),
                catalog.getLayerByName(basicPolygonsName).getDefaultStyle().getName());
        assertEquals(
                polygon.getId(),
                catalog.getLayerByName(basicPolygonsName).getDefaultStyle().getId());
    }
    // GEOS-9073
    @Test
    public void testCharsetEncodingOption() throws Exception {
        File dir = unpack("shape/bad_char_shp.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "bad_char.shp");

        // @formatter:off
        String contextDefinition =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \""
                        + wsName
                        + "\"\n"
                        + "         }\n"
                        + "      },\n"
                        + "      \"data\": {\n"
                        + "        \"type\": \"file\",\n"
                        + "        \"file\": \""
                        + jsonSafePath(locations)
                        + "\",\n"
                        + "    \"charsetEncoding\":\"UTF-8\"\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";

        JSONObject json =
                (JSONObject)
                        json(
                                postAsServletResponse(
                                        "/rest/imports", contextDefinition, "application/json"));
        int importId = json.getJSONObject("import").getInt("id");

        ImportContext context = importer.getContext(importId);
        assertEquals(ImportContext.State.PENDING, context.getState());

        assertTrue(new File(context.getUploadDirectory().getFile(), ".locking").exists());

        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        importer.changed(task);
        assertEquals(ImportTask.State.READY, task.getState());
        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());
        assertTrue(context.getState() == ImportContext.State.COMPLETE);

        assertTrue(new File(context.getUploadDirectory().getFile(), ".locking").exists());
    }
}
