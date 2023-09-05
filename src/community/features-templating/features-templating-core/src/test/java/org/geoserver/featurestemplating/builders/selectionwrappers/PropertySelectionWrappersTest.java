/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.AbstractPropertySelection;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataTestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class PropertySelectionWrappersTest extends DataTestCase {

    private static final String BASE_JSON_NODE =
            "{\n"
                    + "  \"attr1\":\"${attr1}\",\n"
                    + "  \"attr2\":\"${attr2}\",\n"
                    + "  \"attrE\":\"${attrE}\",\n"
                    + "  \"attr3\":{\n"
                    + "     \"attr4\":\"${attr4}\"\n"
                    + "  }\n"
                    + "   }";

    private static final String JSON_ATTRIBUTE =
            "{\n"
                    + "  \"attrA\":\"a\",\n"
                    + "  \"attrB\": {\n"
                    + "     \"attrC\":\"c\",\n"
                    + "     \"attrD\":\"d\"\n"
                    + "  },\n"
                    + "  \"attrE\":\"e\"\n"
                    + "}";

    private String TYPE_NAME = "propertySel";

    private SimpleFeature feature;

    @Before
    public void setup() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("attr1", String.class);
        tb.add("attr2", Double.class);
        tb.add("attrE", Integer.class);
        tb.add("attr4", String.class);
        tb.add("jsonField", String.class);
        tb.setName(TYPE_NAME);
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("jsonField")
                .getUserData()
                .put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add("attr1Value");
        fb.add(0.2);
        fb.add(10);
        fb.add("attr4 value");
        fb.add(JSON_ATTRIBUTE);
        feature = fb.buildFeature(TYPE_NAME.concat(".1"));
    }

    @Test
    public void testIncludeFlatSelection() throws IOException {
        DynamicIncludeFlatBuilder builder =
                new DynamicIncludeFlatBuilder(
                        "${jsonField}", new NamespaceSupport(), readJsonString(BASE_JSON_NODE));

        AbstractPropertySelection propertySelection =
                new AbstractPropertySelection() {
                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null && key.endsWith("attrC")) return false;
                        return true;
                    }

                    @Override
                    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
                        return true;
                    }
                };
        IncludeFlatPropertySelection wrapper =
                new IncludeFlatPropertySelection(builder, propertySelection);

        String result = encodeTemplateToString(wrapper);
        JsonNode node = readJsonString(result);
        ObjectNode object = (ObjectNode) node;
        assertEquals("attr1Value", object.get("attr1").asText());
        assertEquals(0.2, object.get("attr2").asDouble(), 0d);
        // overrided attr
        assertEquals("e", object.get("attrE").asText());
        assertEquals("attr4 value", object.get("attr3").get("attr4").asText());
        ObjectNode childB = (ObjectNode) object.get("attrB");
        // this was not selected.
        assertFalse(childB.has("attrC"));
        assertEquals("d", childB.get("attrD").asText());
    }

    @Test
    public void testMergeSelection() throws IOException {
        DynamicMergeBuilder builder =
                new DynamicMergeBuilder(
                        "nestedAttr",
                        "${jsonField}",
                        new NamespaceSupport(),
                        readJsonString(BASE_JSON_NODE),
                        true);

        AbstractPropertySelection propertySelection =
                new AbstractPropertySelection() {
                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null && key.endsWith("attrD")) return false;
                        return true;
                    }

                    @Override
                    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
                        return true;
                    }
                };
        MergePropertySelection wrapper = new MergePropertySelection(builder, propertySelection);

        String result = encodeTemplateToString(wrapper);
        JsonNode node = readJsonString(result);
        ObjectNode object = (ObjectNode) node.get("nestedAttr");
        assertEquals("attr1Value", object.get("attr1").asText());
        assertEquals(0.2, object.get("attr2").asDouble(), 0d);
        // overrided attr
        assertEquals("e", object.get("attrE").asText());
        assertEquals("attr4 value", object.get("attr3").get("attr4").asText());
        ObjectNode childB = (ObjectNode) object.get("attrB");
        assertFalse(childB.has("attrD"));
        assertEquals("c", childB.get("attrC").asText());
    }

    @Test
    public void testStaticSelection() throws IOException {
        StaticBuilder builder =
                new StaticBuilder(
                        "staticBuilder", readJsonString(JSON_ATTRIBUTE), new NamespaceSupport());

        AbstractPropertySelection propertySelection =
                new AbstractPropertySelection() {
                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null && key.endsWith("attrB")) return false;
                        return true;
                    }

                    @Override
                    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
                        return true;
                    }
                };
        StaticPropertySelection wrapper = new StaticPropertySelection(builder, propertySelection);

        String result = encodeTemplateToString(wrapper);
        JsonNode node = readJsonString(result);
        ObjectNode object = (ObjectNode) node.get("staticBuilder");
        assertEquals(2, object.size());
        assertEquals("a", object.get("attrA").asText());
        assertEquals("e", object.get("attrE").asText());
    }

    @Test
    public void testCompositeWithDynamicKey() throws IOException {
        AbstractPropertySelection propertySelection =
                new AbstractPropertySelection() {
                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null && key.endsWith("attr1Value")) return false;
                        return true;
                    }

                    @Override
                    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
                        return true;
                    }
                };
        CompositeBuilder builder = new CompositeBuilder("${attr1}", new NamespaceSupport(), false);
        CompositeBuilder builder2 = new CompositeBuilder("${attr2}", new NamespaceSupport(), false);
        DynamicValueBuilder dynamicValueBuilder =
                new DynamicValueBuilder("nested", "${jsonField}", new NamespaceSupport());
        builder.addChild(new DynamicPropertySelection(dynamicValueBuilder, propertySelection));
        StaticBuilder staticBuilder =
                new StaticBuilder(
                        "staticBuilder", readJsonString(JSON_ATTRIBUTE), new NamespaceSupport());
        builder2.addChild(new StaticPropertySelection(staticBuilder, propertySelection));
        PropertySelectionWrapper one = new CompositePropertySelection(builder, propertySelection);
        PropertySelectionWrapper two = new CompositePropertySelection(builder2, propertySelection);
        CompositeBuilder container =
                new CompositeBuilder("container", new NamespaceSupport(), false);
        container.addChild(one);
        container.addChild(two);

        String result =
                encodeTemplateToString(new PropertySelectionWrapper(container, propertySelection));
        JsonNode node = readJsonString(result);
        ObjectNode object = (ObjectNode) node.get("container");
        ObjectNode dynamicKeySelected = (ObjectNode) object.get("0.2");
        assertFalse(object.has("attr1Value"));
    }

    private JsonNode readJsonString(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    private String encodeTemplateToString(TemplateBuilder builder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);
        writer.writeStartObject();
        builder.evaluate(writer, new TemplateBuilderContext(feature));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        return new String(baos.toByteArray());
    }
}
