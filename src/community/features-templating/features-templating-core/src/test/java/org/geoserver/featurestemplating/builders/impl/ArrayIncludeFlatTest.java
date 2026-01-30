/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

public class ArrayIncludeFlatTest {

    private static final String BASE =
            "[\"someStaticValue\",\"$includeFlat{$${jsonPointer(jsonNode,'/array')}}\",\"$includeFlat{$${jsonPointer(jsonNode,'/anotherArray')}}\"]";

    private static final String ARRAY_METADATA =
            "{\"array\":[1,2,3,4], \"anotherArray\":[\"one\",\"two\",\"three\",\"four\"]}";

    private static final String BASE_WITH_NULL = "[\"someStaticValue\",\"$includeFlat{${nullNode}!}\"]";

    private SimpleFeature jsonFieldSimpleFeature;

    @Before
    public void setup() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("jsonNode", String.class);
        tb.add("nullNode", String.class);
        tb.setName("jsonFieldSimpleType");
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("jsonNode").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("nullNode").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(ARRAY_METADATA);
        jsonFieldSimpleFeature = fb.buildFeature("jsonFieldSimpleType.1");
    }

    @Test
    public void testArrayIncludeFlat() throws Exception {
        List<String> validArrayFields =
                Arrays.asList("one", "two", "three", "four", "1", "2", "3", "4", "someStaticValue");
        JsonNode node = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build()
                .readTree(BASE);
        TemplateReaderConfiguration configuration = new TemplateReaderConfiguration(new NamespaceSupport());
        JSONTemplateReader templateReader = templateReader(node, configuration);
        CompositeBuilder builder = new CompositeBuilder(null, new NamespaceSupport(), false);
        templateReader.getBuilderFromJson("array", node, builder, configuration.getBuilderMaker());
        JsonNode result = writeOutput(builder, jsonFieldSimpleFeature);
        ArrayNode array = (ArrayNode) result.get("array");
        assertEquals(9, array.size());
        for (JsonNode el : array.elements()) {
            assertTrue(validArrayFields.contains(el.asString()));
        }
    }

    @Test
    public void testArrayIncludeFlatWithNull() throws Exception {
        Set<String> validArrayFields = Set.of("null", "someStaticValue");
        JsonNode node = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build()
                .readTree(BASE_WITH_NULL);
        TemplateReaderConfiguration configuration = new TemplateReaderConfiguration(new NamespaceSupport());
        JSONTemplateReader templateReader = templateReader(node, configuration);
        CompositeBuilder builder = new CompositeBuilder(null, new NamespaceSupport(), false);
        templateReader.getBuilderFromJson("array", node, builder, configuration.getBuilderMaker());
        JsonNode result = writeOutput(builder, jsonFieldSimpleFeature);
        ArrayNode array = (ArrayNode) result.get("array");
        assertEquals(2, array.size());
        Set<String> actualValues =
                array.valueStream().map(n -> n.isNull() ? "null" : n.asString()).collect(Collectors.toSet());
        assertEquals(validArrayFields, actualValues);
    }

    private JsonNode writeOutput(TemplateBuilder builder, Feature f) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TemplateOutputWriter writer = writer(baos);
        builder.evaluate(writer, new TemplateBuilderContext(f));
        writer.close();
        return new ObjectMapper().readTree(new String(baos.toByteArray()));
    }

    private JSONTemplateReader templateReader(JsonNode node, TemplateReaderConfiguration configuration) {
        JSONTemplateReader templateReader = new JSONTemplateReader(node, configuration, Collections.emptyList());
        return templateReader;
    }

    private TemplateOutputWriter writer(OutputStream stream) throws IOException {
        return new GeoJSONWriter(
                JsonFactory.builder().build().createGenerator(ObjectWriteContext.empty(), stream, JsonEncoding.UTF8),
                TemplateIdentifier.JSON);
    }
}
