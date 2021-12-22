package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.data.DataTestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicIncludeFlatBuilderTest extends DataTestCase {
    private static final String DYNAMIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"dynamicValue\":\"${dynamicValue}\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";
    private static final String STATIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";

    private SimpleFeature jsonFieldSimpleFeature;

    @Before
    public void setup() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("dynamicMetadata", String.class);
        tb.add("staticMetadata", String.class);
        tb.add("dynamicValue", String.class);
        tb.add("arrayMetadata", String.class);
        tb.setName("jsonFieldSimpleType");
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("dynamicMetadata")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("staticMetadata")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(DYNAMIC_METADATA);
        fb.add(STATIC_METADATA);
        fb.add("dynamic value result");
        jsonFieldSimpleFeature = fb.buildFeature("jsonFieldSimpleType.1");
    }

    @Test
    public void testDynamicIncludeFlatDynamicResult() throws Exception {
        JSONObject json = encodeDynamicIncludeFlat("${dynamicMetadata}", jsonFieldSimpleFeature);
        JSONObject metadata19139 =
                json.getJSONObject("metadata").getJSONObject("metadata_iso_19139");
        assertEquals("metadata_iso_19139", metadata19139.getString("title"));
        assertEquals("http://metadata_iso_19139.org", metadata19139.getString("href"));
        assertEquals("metadata", metadata19139.getString("type"));
        assertEquals("dynamic value result", metadata19139.getString("dynamicValue"));
    }

    @Test
    public void testDynamicIncludeFlatStaticResult() throws Exception {
        JSONObject json = encodeDynamicIncludeFlat("${staticMetadata}", jsonFieldSimpleFeature);
        JSONObject metadata19139 =
                json.getJSONObject("metadata").getJSONObject("metadata_iso_19139");
        assertEquals("metadata_iso_19139", metadata19139.getString("title"));
        assertEquals("http://metadata_iso_19139.org", metadata19139.getString("href"));
        assertEquals("metadata", metadata19139.getString("type"));
    }

    @Test
    public void testNoDynamicIncludeFlatResult() throws Exception {
        JSONObject json = encodeDynamicIncludeFlat("${dynamicValue}", jsonFieldSimpleFeature);
        assertEquals("dynamic value result", json.get("dynamicIncludeFlatBuilder"));
    }

    private JSONObject encodeDynamicIncludeFlat(String expression, Feature feature)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);

        DynamicIncludeFlatBuilder builder =
                new DynamicIncludeFlatBuilder(
                        "dynamicIncludeFlatBuilder", expression, new NamespaceSupport());
        writer.writeStartObject();
        builder.evaluate(writer, new TemplateBuilderContext(feature));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }
}
