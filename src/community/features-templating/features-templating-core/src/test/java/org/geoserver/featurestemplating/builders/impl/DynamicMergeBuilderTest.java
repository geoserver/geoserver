package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.readers.JSONTemplateReaderUtil;
import org.geotools.data.DataTestCase;
import org.junit.Before;
import org.junit.Test;

public class DynamicMergeBuilderTest extends DataTestCase {

    private static final String BASE =
            "{\"properties\":{\"assets\":{\"metadata_iso_19139\":{\"randomAttribute\":\"basetext\",\"roles\":[\"metadata\",\"iso-23\"],\"href\":\"basehref\",\"title\":\"basetitle\",\"type\":\"basetype\"}}}}";
    private static final String OVERLAY = "{\"properties\":{\"assets\":\"${assets}\"}}";
    private JsonNode baseNode;
    private JsonNode overlayNode;

    @Before
    public void setup() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        baseNode = mapper.readTree(BASE);
        overlayNode = mapper.readTree(OVERLAY);
    }

    @Test
    public void testEncodeDynamicMergeKeys() {
        JSONMerger jsonMerger = new JSONMerger();
        ObjectNode mergedNode = jsonMerger.mergeTrees(baseNode, overlayNode);
        JsonNode mergeNode = mergedNode.get("properties").get("$dynamicMerge");
        assertEquals(
                "{\"assets\":{\"node1\":\"${assets}\",\"node2\":{\"metadata_iso_19139\":{\"randomAttribute\":\"basetext\",\"roles\":[\"metadata\",\"iso-23\"],\"href\":\"basehref\",\"title\":\"basetitle\",\"type\":\"basetype\"}}}}",
                mergeNode.toString());
        assertEquals("\"${assets}\"", mergeNode.get("assets").get("node1").toString());
        assertEquals(
                "{\"metadata_iso_19139\":{\"randomAttribute\":\"basetext\",\"roles\":[\"metadata\",\"iso-23\"],\"href\":\"basehref\",\"title\":\"basetitle\",\"type\":\"basetype\"}}",
                mergeNode.get("assets").get("node2").toString());
    }

    @Test
    public void testObtainDynamicMergeBuilder() {
        ObjectNode mergedNode = new JSONMerger().mergeTrees(baseNode, overlayNode);
        TemplateBuilderMaker builderMaker = new TemplateBuilderMaker();
        TemplateBuilder build = builderMaker.build();
        new JSONTemplateReaderUtil().getBuilderFromJson(null, mergedNode, build, builderMaker);
        assertTrue(build.getChildren().get(0).getChildren().get(0) instanceof DynamicMergeBuilder);
    }
}
