package org.geoserver.featurestemplating.writers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.xml.sax.helpers.NamespaceSupport;

public class JsonWriterTest {

    private SimpleFeature createSimpleFeature() throws URISyntaxException {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("geometry", Point.class);
        tb.add("string", String.class);
        tb.add("integer", Integer.class);
        tb.add("double", Double.class);
        tb.add("url", URI.class);
        tb.add("intArray", Integer[].class);
        tb.add("strArray", String[].class);
        tb.setName("schema");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(tb.buildFeatureType());
        GeometryFactory factory = new GeometryFactory();
        Point point = factory.createPoint(new Coordinate(1, 1));
        fb.set("geometry", point);
        fb.set("string", "stringValue");
        fb.set("integer", 1);
        fb.set("double", 0.0);
        fb.set("url", new URI("http://some/url/to.test"));
        fb.set(
                "intArray",
                new Integer[] {Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2)});
        fb.set("strArray", new String[] {"one", "two", "three"});
        return fb.buildFeature("1");
    }

    @Test
    public void testJsonLDWriterEncodesURL() throws URISyntaxException, IOException {
        // test that values of URL types are correctly encoded
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleFeature f = createSimpleFeature();
        JSONLDWriter writer =
                new JSONLDWriter(new JsonFactory().createGenerator(baos, JsonEncoding.UTF8));
        writer.writeStartObject();
        for (Property prop : f.getProperties()) {
            writer.writeElementName(prop.getName().toString(), null);
            writer.writeValue(prop.getValue());
        }
        writer.endObject(null, null);
        writer.close();
        String jsonString = new String(baos.toByteArray());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        assertEquals(json.getString("url"), "http://some/url/to.test");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonWriterEncodesArrays() throws URISyntaxException, IOException {
        // test that values of URL types are correctly encoded
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleFeature f = createSimpleFeature();
        GeoJSONWriter writer =
                new GeoJSONWriter(new JsonFactory().createGenerator(baos, JsonEncoding.UTF8));
        writer.writeStartObject();
        for (Property prop : f.getProperties()) {
            writer.writeElementName(prop.getName().toString(), null);
            writer.writeValue(prop.getValue());
        }
        writer.endObject(null, null);
        writer.close();
        String jsonString = new String(baos.toByteArray());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);

        List<Integer> intArray = json.getJSONArray("intArray");
        assertThat(
                intArray,
                Matchers.hasItems(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2)));

        List<String> strArray = json.getJSONArray("strArray");
        assertThat(strArray, Matchers.hasItems("one", "two", "three"));
    }

    @Test
    public void testArrayIntegration() throws URISyntaxException, IOException {
        // load the template
        NamespaceSupport namespaceSuport = new NamespaceSupport();
        namespaceSuport.declarePrefix("", "http://www.geoserver.org");
        InputStream is = getClass().getResource("arrayTemplate.json").openStream();
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JSONTemplateReader templateReader =
                new JSONTemplateReader(
                        mapper.readTree(is),
                        new TemplateReaderConfiguration(namespaceSuport),
                        Collections.emptyList());
        RootBuilder builder = templateReader.getRootBuilder();

        // write the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(new JsonFactory().createGenerator(baos, JsonEncoding.UTF8));
        SimpleFeature sf = createSimpleFeature();
        builder.evaluate(writer, new TemplateBuilderContext(sf));
        writer.close();

        String jsonString = new String(baos.toByteArray());
        System.out.println(jsonString);
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);

        // straight array expansion tests
        List<Integer> intArray = json.getJSONArray("intArray");
        assertThat(
                intArray,
                Matchers.hasItems(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2)));

        List<String> strArray = json.getJSONArray("strArray");
        assertThat(strArray, Matchers.hasItems("one", "two", "three"));

        // iterating over array elements and building objects around them
        List<JSONObject> intObjectArray = json.getJSONArray("intObjectArray");
        assertEquals(3, intObjectArray.size());
        for (int i = 0; i < 3; i++) {
            JSONObject jo = intObjectArray.get(i);
            assertEquals(i, jo.getInt("idx"));
            assertEquals("TheInteger" + i, jo.getString("name"));
        }
        List<JSONObject> strObjectArray = json.getJSONArray("strObjectArray");
        assertEquals(3, strObjectArray.size());
        String[] names = (String[]) sf.getAttribute("strArray");
        for (int i = 0; i < 3; i++) {
            JSONObject jo = strObjectArray.get(i);
            assertEquals(names[i], jo.getString("id"));
            assertEquals("TheString" + names[i], jo.getString("name"));
        }

        // extracting a single item out of the array
        assertEquals(2, json.getInt("singleIntItem"));
    }

    @Test
    public void testStaticArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);
        writer.startArray(null, null);
        writer.writeStaticContent(null, "abc", new EncodingHints());
        writer.writeStaticContent(null, 5, new EncodingHints());
        writer.endArray(null, null);
        writer.close();
        String jsonString = new String(baos.toByteArray());
        JSONArray json = (JSONArray) JSONSerializer.toJSON(jsonString);
        assertEquals("abc", json.get(0));
        assertEquals(Integer.valueOf(5), json.get(1));
    }

    @Test
    public void testJsonLDWriterEncodesActualTypesByDefault()
            throws URISyntaxException, IOException {
        // test that values of URL types are correctly encoded
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SimpleFeature f = createSimpleFeature();
        JSONLDWriter writer =
                new JSONLDWriter(new JsonFactory().createGenerator(baos, JsonEncoding.UTF8));
        writer.writeStartObject();
        for (Property prop : f.getProperties()) {
            writer.writeElementName(prop.getName().toString(), null);
            writer.writeValue(prop.getValue());
        }
        writer.endObject(null, null);
        writer.close();
        String jsonString = new String(baos.toByteArray());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        assertTrue(json.getString("string") instanceof String);
        assertTrue(json.get("integer") instanceof Integer);
        assertTrue(json.get("double") instanceof Double);
        JSONObject object = json.getJSONObject("geometry");
        JSONArray coordinates = object.getJSONArray("coordinates");
        for (int i = 0; i < coordinates.size(); i++) {
            assertTrue(coordinates.get(i) instanceof Number);
        }
    }
}
