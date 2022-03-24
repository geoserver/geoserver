package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
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

public class DynamicMergeBuilderTest extends DataTestCase {

    private static final String BASE =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"basetext\",\"href\":\"basehref\",\"type\":\"basetype\"}}}";
    private static final String DYNAMIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"dynamicValue\":\"${dynamicValue}\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";
    private static final String STATIC_METADATA =
            "{\"metadata\":{\"metadata_iso_19139\":{\"title\":\"metadata_iso_19139\",\"href\":\"http://metadata_iso_19139.org\",\"type\":\"metadata\"}}}";

    private JsonNode node;
    private SimpleFeature jsonFieldSimpleFeature;

    @Before
    public void setup() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        node = mapper.readTree(BASE);

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
    public void testObtainDynamicMergeBuilder() throws JsonProcessingException {
        JsonNode overlay = new ObjectMapper().readTree("{\"metadata\":\"${staticMetadata}\"}");
        ObjectNode mergedNode = new JSONMerger().mergeTrees(node, overlay);
        TemplateBuilderMaker builderMaker = new TemplateBuilderMaker();
        TemplateBuilder build = builderMaker.build();
        TemplateReaderConfiguration configuration = new TemplateReaderConfiguration(null);
        new JSONTemplateReader(mergedNode, configuration, new ArrayList<>())
                .getBuilderFromJson(null, mergedNode, build, builderMaker);
        assertTrue(build.getChildren().get(0) instanceof DynamicMergeBuilder);
    }

    @Test
    public void testMergeStaticResultOverlayExpression() throws Exception {
        JSONObject json = encodeDynamicMerge("${staticMetadata}", jsonFieldSimpleFeature, true);
        JSONObject metadata19139 =
                json.getJSONObject("dynamicMergeBuilder")
                        .getJSONObject("metadata")
                        .getJSONObject("metadata_iso_19139");
        assertEquals("metadata_iso_19139", metadata19139.getString("title"));
        assertEquals("http://metadata_iso_19139.org", metadata19139.getString("href"));
        assertEquals("metadata", metadata19139.getString("type"));
    }

    @Test
    public void testMergeStaticResultBaseExpression() throws Exception {
        JSONObject json = encodeDynamicMerge("${staticMetadata}", jsonFieldSimpleFeature, false);
        JSONObject metadata19139 =
                json.getJSONObject("dynamicMergeBuilder")
                        .getJSONObject("metadata")
                        .getJSONObject("metadata_iso_19139");
        assertEquals("basetext", metadata19139.getString("title"));
        assertEquals("basehref", metadata19139.getString("href"));
        assertEquals("basetype", metadata19139.getString("type"));
    }

    @Test
    public void testErrorWhenJsonAttributeWithDirectives() throws IOException {
        String message = null;
        try {
            encodeDynamicMerge("${dynamicMetadata}", jsonFieldSimpleFeature, false);
        } catch (UnsupportedOperationException e) {
            message = e.getMessage();
        }
        assertEquals(
                message,
                "A json attribute value cannot have a template directive among its fields.");
    }

    private JSONObject encodeDynamicMerge(
            String expression, Feature feature, boolean overlayExpression) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);

        DynamicMergeBuilder builder =
                new DynamicMergeBuilder(
                        "dynamicMergeBuilder",
                        expression,
                        new NamespaceSupport(),
                        node,
                        overlayExpression);
        writer.writeStartObject();
        builder.evaluate(writer, new TemplateBuilderContext(feature));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }
}
