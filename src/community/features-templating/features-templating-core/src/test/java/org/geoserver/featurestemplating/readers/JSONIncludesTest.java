/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class JSONIncludesTest {

    FileSystemResourceStore store;

    @Before
    public void setupStore() {
        store = new FileSystemResourceStore(new File("src/test/resources/jsonIncludes"));
    }

    @Test
    public void testArrayInclusion() throws Exception {
        RecursiveJSONParser parser = new RecursiveJSONParser(store.get("includeInArray.json"));
        JsonNode parsed = parser.parse();

        checkArrayInclusion(parsed);
    }

    private void checkArrayInclusion(JsonNode parsed) {
        // basics, the static part was not touched
        assertEquals("test for array inclusion", parsed.get("name").textValue());

        // the array with inclusions
        JsonNode array = parsed.get("arrayProperty");
        assertEquals(JsonNodeType.ARRAY, array.getNodeType());
        assertEquals("first", array.get(0).textValue());
        // object expansion
        JsonNode object = array.get(1);
        assertEquals(JsonNodeType.OBJECT, object.getNodeType());
        assertEquals(10, object.get("int").intValue());
        assertEquals("abc", object.get("text").textValue());
        assertEquals(1, object.get("object").get("a").intValue());
        // array expansion (as a value)
        JsonNode na = array.get(2);
        assertEquals(JsonNodeType.ARRAY, na.getNodeType());
        assertEquals("one", na.get(0).textValue());
        assertEquals("two", na.get(1).get("name").textValue());
        assertEquals(3, na.get(2).intValue());
        // flat array expansion (not container, just adding entries)
        assertEquals("one", array.get(3).textValue());
        assertEquals("two", array.get(4).get("name").textValue());
        assertEquals(3, array.get(5).intValue());
        // last element is there
        assertEquals("last", array.get(6).textValue());

        // the last top level element was preserved as well
        assertEquals("endMarker", parsed.get("end").textValue());
    }

    @Test
    public void checkObjectInclusion() throws Exception {
        RecursiveJSONParser parser = new RecursiveJSONParser(store.get("includeInObject.json"));
        JsonNode parsed = parser.parse();

        checkObjectInclusion(parsed);
    }

    @Test
    public void checkObjectInclusionSubdirDownwards() throws Exception {
        RecursiveJSONParser parser =
                new RecursiveJSONParser(store.get("includeInObjectSubdir.json"));
        JsonNode parsed = parser.parse();

        checkObjectInclusion(parsed);
    }

    @Test
    public void checkObjectInclusionSubdirRelativeUp() {
        RuntimeException ex = checkThrowingTemplate("subdir/includeInObjectRelativeUp.json");
        assertEquals("Contains invalid '..' path: subdir/../object.json", ex.getMessage());
    }

    @Test
    public void checkObjectInclusionSubdirAbsoluteUp() throws IOException {
        RecursiveJSONParser parser =
                new RecursiveJSONParser(store.get("subdir/includeInObjectAbsoluteUp.json"));
        JsonNode parsed = parser.parse();

        checkObjectInclusion(parsed);
    }

    @Test
    public void checkObjectInclusionRelativeDot() throws IOException {
        RecursiveJSONParser parser =
                new RecursiveJSONParser(store.get("includeInObjectRelativeDot.json"));
        JsonNode parsed = parser.parse();

        checkObjectInclusion(parsed);
    }

    private void checkObjectInclusion(JsonNode parsed) {
        // basics, the static part was not touched
        assertEquals("test for object inclusion", parsed.get("name").textValue());

        // including a sub-object
        JsonNode object = parsed.get("myObjectProperty");
        assertEquals(JsonNodeType.OBJECT, object.getNodeType());
        assertEquals(10, object.get("int").intValue());
        assertEquals("abc", object.get("text").textValue());
        assertEquals(1, object.get("object").get("a").intValue());

        // including as a sub-array
        JsonNode array = parsed.get("myArrayProperty");
        assertEquals(JsonNodeType.ARRAY, array.getNodeType());
        assertEquals("one", array.get(0).textValue());
        assertEquals("two", array.get(1).get("name").textValue());
        assertEquals(3, array.get(2).intValue());

        // flat inclusion
        assertEquals(10, parsed.get("int").intValue());
        assertEquals("abc", parsed.get("text").textValue());
        assertEquals(1, object.get("object").get("a").intValue());

        // the last top level element was preserved as well
        assertEquals("endMarker", parsed.get("end").textValue());
    }

    @Test
    public void testNestedInclusion() throws Exception {
        RecursiveJSONParser parser = new RecursiveJSONParser(store.get("nestedInclusion.json"));
        JsonNode parsed = parser.parse();

        // basics, the static part was not touched
        assertEquals("test for nested inclusion", parsed.get("name").textValue());

        // nested objects
        checkObjectInclusion(parsed.get("obj1"));
        checkArrayInclusion(parsed.get("obj2"));
        JsonNode flat = parsed.get("obj3");
        checkObjectInclusion(flat);
        assertEquals("first", flat.get("o3First").asText());
        assertEquals("last", flat.get("o3Last").asText());

        // the last top level element was preserved as well
        assertEquals("endMarker", parsed.get("topLevelEnd").textValue());
    }

    @Test
    public void testRecursionLimited() {
        RuntimeException ex = checkThrowingTemplate("recurse.json");
        assertThat(
                ex.getMessage(),
                containsString(
                        "Went beyond maximum expansion depth (51), chain is: [recurse.json"));
    }

    @Test
    public void testDanglingInclude() {
        RuntimeException ex = checkThrowingTemplate("dangling.json");
        assertEquals("Path notThere.json does not exist", ex.getMessage());
    }

    @Test
    public void testRecursionPingPong() {
        // ping and pong import each other in an infinite recursion
        RuntimeException ex = checkThrowingTemplate("ping.json");
        assertThat(
                ex.getMessage(),
                containsString("Went beyond maximum expansion depth (51), chain is: [ping.json"));
        assertThat(ex.getMessage(), containsString("pong.json"));
    }

    @Test
    public void testIncludedModificationAreDetected() throws IOException, InterruptedException {
        Resource resource = store.get("includeInObject.json");
        RecursiveJSONParser parser = new RecursiveJSONParser(resource);
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(new NamespaceSupport());
        JSONTemplateReader reader =
                new JSONTemplateReader(parser.parse(), configuration, parser.getWatchers());
        RootBuilder rootBuilder = reader.getRootBuilder();
        assertFalse(rootBuilder.needsReload());
        Resource included = store.get("object.json");
        File file = included.file();
        file.setLastModified(new Date().getTime());

        for (int i = 0; i < 600; i++) {
            if (rootBuilder.needsReload()) return; // ok worked
            Thread.sleep(100);
        }
        fail("Should have found a reload 60 seconds, but did not");
    }

    @Test
    public void testObtainDynamicIncludeFlatKeyword() throws IOException {
        RecursiveJSONParser parser = new RecursiveJSONParser(store.get("dynamicIncludeFlat.json"));
        JsonNode parse = parser.parse();
        assertTrue(parse.toString().contains("$includeFlat"));
    }

    private RuntimeException checkThrowingTemplate(String s) {
        return assertThrows(
                RuntimeException.class, () -> new RecursiveJSONParser(store.get(s)).parse());
    }
}
