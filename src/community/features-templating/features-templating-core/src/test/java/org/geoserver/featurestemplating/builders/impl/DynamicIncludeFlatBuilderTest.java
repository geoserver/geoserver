package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataTestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicIncludeFlatBuilderTest extends DataTestCase {

    private static final String BASE = "{\"someStaticValue\":\"theValue\"}";
    private static final String DYNAMIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"dynamicValue\":\"${dynamicValue}\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";
    private static final String STATIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";

    private static final String ARRAY_METADATA =
            "{\"array\":[1,2,3,4], \"anotherArray\":[\"one\",\"two\",\"three\",\"four\"]}";

    private SimpleFeature jsonFieldSimpleFeature;

    @Before
    public void setup() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("dynamicMetadata", String.class);
        tb.add("staticMetadata", String.class);
        tb.add("dynamicValue", String.class);
        tb.add("arrayMetadata", String.class);
        tb.add("nullMetadata", String.class);
        tb.setName("jsonFieldSimpleType");
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("dynamicMetadata")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("staticMetadata")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("arrayMetadata")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(DYNAMIC_METADATA);
        fb.add(STATIC_METADATA);
        fb.add("dynamic value result");
        fb.add(ARRAY_METADATA);
        jsonFieldSimpleFeature = fb.buildFeature("jsonFieldSimpleType.1");
    }

    @Test
    public void testDynamicIncludeFlatDynamicResult() throws Exception {
        String message = null;
        try {
            encodeDynamicIncludeFlat("${dynamicMetadata}", jsonFieldSimpleFeature, BASE);
        } catch (UnsupportedOperationException e) {
            message = e.getMessage();
        }
        assertEquals(
                message,
                "A json attribute value cannot have a template directive among its fields.");
    }

    @Test
    public void testDynamicIncludeFlatStaticResult() throws Exception {
        JSONObject json =
                encodeDynamicIncludeFlat("${staticMetadata}", jsonFieldSimpleFeature, BASE);
        JSONObject metadata19139 =
                json.getJSONObject("metadata").getJSONObject("metadata_iso_19139");
        assertEquals("metadata_iso_19139", metadata19139.getString("title"));
        assertEquals("http://metadata_iso_19139.org", metadata19139.getString("href"));
        assertEquals("metadata", metadata19139.getString("type"));
    }

    @Test
    public void testDynamicIncludeFlatMultipleArrays() throws Exception {
        JSONObject json =
                encodeDynamicIncludeFlat("${arrayMetadata}", jsonFieldSimpleFeature, BASE);
        JSONArray array1 = json.getJSONArray("array");
        JSONArray array2 = json.getJSONArray("anotherArray");
        for (int i = 0; i < array1.size(); i++) {
            assertEquals(array1.getInt(i), i + 1);
        }

        assertEquals(array2.getString(0), "one");
        assertEquals(array2.getString(1), "two");
        assertEquals(array2.getString(2), "three");
        assertEquals(array2.getString(3), "four");
    }

    @Test
    public void testDynamicIncludeFlatOnNull() throws Exception {
        JSONObject json = encodeDynamicIncludeFlat("${nullMetadata}", jsonFieldSimpleFeature, BASE);
        assertEquals(1, json.size());
        assertEquals("theValue", json.getString("someStaticValue"));
    }

    private JSONObject encodeDynamicIncludeFlat(String expression, Feature feature, String baseNode)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode base = objectMapper.readTree(baseNode);
        DynamicIncludeFlatBuilder builder =
                new DynamicIncludeFlatBuilder(expression, new NamespaceSupport(), base);
        writer.writeStartObject();
        builder.evaluate(writer, new TemplateBuilderContext(feature));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }
}
