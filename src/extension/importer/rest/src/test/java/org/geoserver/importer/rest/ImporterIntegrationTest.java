package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import net.sf.json.JSONObject;

public class ImporterIntegrationTest extends ImporterTestSupport {

    @Before
    public void createH2Store() throws Exception {
        // create the store for the indirect imports
        String wsName = getCatalog().getDefaultWorkspace().getName();
        createH2DataStore(wsName, "h2");
    }

    @Test
    public void testDefaultTransformationsInit() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        File locations = new File(dir, "locations.csv");

        // @formatter:off 
        String contextDefinition = "{\n" + 
                "   \"import\": {\n" +
                "      \"targetWorkspace\": {\n" + 
                "         \"workspace\": {\n" + 
                "            \"name\": \"" + wsName + "\"\n" + 
                "         }\n" + 
                "      },\n" + 
                "      \"data\": {\n" + 
                "        \"type\": \"file\",\n" + 
                "        \"file\": \"" + jsonSafePath(locations) + "\"\n" + 
                "      },\n" + 
                "      targetStore: {\n" + 
                "        dataStore: {\n" + 
                "        name: \"h2\",\n" + 
                "        }\n" +
                "      },\n" +
                "      \"transforms\": [\n" +
                "        {\n" +
                "          \"type\": \"AttributesToPointGeometryTransform\",\n" +
                "          \"latField\": \"LAT\"," +
                "          \"lngField\": \"LON\"" +
                "        }\n" + "      ]" +
                "   }\n" + 
                "}";
        // @formatter:on 

        JSONObject json = (JSONObject) json(postAsServletResponse("/rest/imports",
                contextDefinition, "application/json"));
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
        String contextDefinition = "{\n" + 
                "   \"import\": {\n" +
                "      \"targetWorkspace\": {\n" + 
                "         \"workspace\": {\n" + 
                "            \"name\": \"" + wsName + "\"\n" + 
                "         }\n" + 
                "      },\n" + 
                "      targetStore: {\n" + 
                "        dataStore: {\n" + 
                "        name: \"h2\",\n" + 
                "        }\n" +
                "      },\n" +
                "      \"transforms\": [\n" +
                "        {\n" +
                "          \"type\": \"AttributesToPointGeometryTransform\",\n" +
                "          \"latField\": \"LAT\"," +
                "          \"lngField\": \"LON\"" +
                "        }\n" + "      ]" +
                "   }\n" + 
                "}";
        // @formatter:on 

        JSONObject json = (JSONObject) json(postAsServletResponse("/rest/imports",
                contextDefinition, "application/json"));
        // print(json);
        int importId = json.getJSONObject("import").getInt("id");

        // upload the data
        String body = "--AaB03x\r\nContent-Disposition: form-data; name=filedata; filename=data.csv\r\n"
                + "Content-Type: text/plain\n"
                + "\r\n\r\n"
                + FileUtils.readFileToString(locations)
                + "\r\n\r\n--AaB03x--";

        post("/rest/imports/" + importId + "/tasks", body, "multipart/form-data; boundary=AaB03x");

        checkLatLonTransformedImport(importId);
    }

    private void checkLatLonTransformedImport(int importId) throws IOException {
        ImportContext context = importer.getContext(importId);
        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);

        TransformChain transformChain = task.getTransform();
        assertThat(transformChain.getTransforms().get(0),
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
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = fti
                .getFeatureSource(null, null);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = featureSource
                .getFeatures();
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
        File gmlFile = file("gml/poi.gml2.gml");
        String wsName = getCatalog().getDefaultWorkspace().getName();

        // @formatter:off 
        String contextDefinition = "{\n" + 
                "   \"import\": {\n" +
                "      \"targetWorkspace\": {\n" + 
                "         \"workspace\": {\n" + 
                "            \"name\": \"" + wsName + "\"\n" + 
                "         }\n" + 
                "      },\n" + 
                "      \"data\": {\n" + 
                "        \"type\": \"file\",\n" + 
                "        \"file\": \"" + jsonSafePath(gmlFile) +  "\"\n" + 
                "      }," +
                "      targetStore: {\n" + 
                "        dataStore: {\n" + 
                "        name: \"h2\",\n" + 
                "        }\n" +
                "      }\n" +    
                "   }\n" + 
                "}";
        // @formatter:on 

        JSONObject json = (JSONObject) json(
                postAsServletResponse("/rest/imports?exec=true"
                + (async ? "&async=true" : ""), contextDefinition, "application/json"));
        // print(json);
        String state = null;
        if (async) {
            int importId = json.getJSONObject("import").getInt("id");
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
        }
        assertEquals("COMPLETE", state);
        checkPoiImport();
    }

    private String jsonSafePath(File gmlFile) throws IOException {
        return gmlFile.getCanonicalPath().replace('\\', '/');
    }

    private void checkPoiImport() throws Exception {
        FeatureTypeInfo fti = getCatalog().getResourceByName("poi", FeatureTypeInfo.class);
        assertNotNull(fti);
        SimpleFeatureType featureType = (SimpleFeatureType) fti.getFeatureType();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertEquals("Expecting a point geometry", Point.class, geometryDescriptor.getType()
                .getBinding());
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

}
