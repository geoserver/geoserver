/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geoserver.platform.resource.Resource;

/** Parses a JSON structure, processing eventual includes and expanding them */
public class RecursiveJSONParser {

    private static final String INCLUDE_KEY = "$include";
    private static final String INCLUDE_FLAT_KEY = "$includeFlat";

    public static final int MAX_RECURSION_DEPTH =
            Integer.parseInt(System.getProperty("GEOSERVER_FT_MAX_DEPTH", "50"));

    private final Resource resource;
    private final ObjectMapper mapper;
    private final RecursiveJSONParser parent;

    public RecursiveJSONParser(Resource resource) {
        this.resource = resource;
        validateResource(resource);
        this.mapper = new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        this.parent = null;
    }

    private RecursiveJSONParser(RecursiveJSONParser parent, String path) {
        this.resource = getResource(parent.resource, path);
        validateResource(resource);
        this.mapper = parent.mapper;
        this.parent = parent;
        int depth = getDepth();
        if (depth > MAX_RECURSION_DEPTH)
            throw new RuntimeException(
                    "Went beyond maximum nested inclusion depth ("
                            + depth
                            + "), inclusion chain is: "
                            + getInclusionChain());
    }

    private void validateResource(Resource resource) {
        if (!resource.getType().equals(Resource.Type.RESOURCE))
            throw new IllegalArgumentException("Path " + resource.path() + " does not exist");
    }

    /**
     * Returns the list of inclusions, starting from the top-most parent and walking down to the
     * current reader
     */
    private List<String> getInclusionChain() {
        List<String> resources = new ArrayList<>();
        RecursiveJSONParser curr = this;
        while (curr != null) {
            resources.add(curr.resource.path());
            curr = curr.parent;
        }
        Collections.reverse(resources);
        return resources;
    }

    private int getDepth() {
        int depth = 0;
        RecursiveJSONParser curr = this.parent;
        while (curr != null) {
            curr = curr.parent;
            depth++;
        }
        return depth;
    }

    public JsonNode parse() throws IOException {
        // read and close before doing recursion, avoids keeping severa files open in parallel
        JsonNode root = readResource();
        return expandIncludes(root);
    }

    private JsonNode expandIncludes(JsonNode node) throws IOException {
        if (node.isArray()) {
            return expandIncludesInArray((ArrayNode) node);
        } else if (node.isObject()) {
            return expandIncludesInObject((ObjectNode) node);
        }

        return node;
    }

    private ObjectNode expandIncludesInObject(ObjectNode input) throws IOException {
        // this is done to allow insertion of flat values in-place, without changing
        // the expected order
        ObjectNode result = mapper.getNodeFactory().objectNode();
        Iterator<String> names = input.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode node = input.get(name);
            // each if here handles a case and "exits" to the next loop iteration
            if (name.equals(INCLUDE_KEY)) {
                throw new IllegalArgumentException(
                        "Cannot have an include directive as the key in an object, "
                                + "only $includeFlat can be used here");
            }
            if (name.equals(INCLUDE_FLAT_KEY)) {
                if (!node.isTextual()) {
                    throw new IllegalArgumentException(
                            "The value of a "
                                    + INCLUDE_FLAT_KEY
                                    + " key must be the path of the file being included");
                }
                JsonNode processed = new RecursiveJSONParser(this, node.asText()).parse();
                Iterator<String> fields = processed.fieldNames();
                while (fields.hasNext()) {
                    String field = fields.next();
                    result.set(field, processed.get(field));
                }
                continue;
            }
            // inclusion in value?
            if (node.isTextual()) {
                String txt = node.asText();
                JsonNode processed = processInlineDirective(txt, INCLUDE_KEY);
                if (processed != null) {
                    result.set(name, processed);
                    continue;
                }
            }
            //  otherwise recurse and add
            result.set(name, expandIncludes(node));
        }

        return result;
    }

    private ArrayNode expandIncludesInArray(ArrayNode array) throws IOException {
        ArrayNode result = mapper.getNodeFactory().arrayNode();
        for (int i = 0; i < array.size(); i++) {
            JsonNode node = array.get(i);
            if (node.isTextual()) {
                String txt = node.asText();

                // simple inclusion
                JsonNode processed = processInlineDirective(txt, INCLUDE_KEY);
                if (processed != null) {
                    result.add(processed);
                    continue;
                }

                // flat inclusion
                processed = processInlineDirective(txt, INCLUDE_FLAT_KEY);
                if (processed != null) {
                    if (!processed.isArray())
                        throw new IllegalArgumentException(
                                "This include flat is in an array, "
                                        + "the included object can only be another array, but it's not: "
                                        + txt);
                    for (JsonNode child : processed) {
                        result.add(child);
                    }
                    continue;
                }
            }

            // if we got here, no expansion happened, recurse
            result.add(expandIncludes(node));
        }
        return result;
    }

    /**
     * Check is the provided text is an inclusion directive if so, extracts the inclusion, parses
     * it, and provides the result to the consumer
     *
     * @param value The text value that might contain the directive
     * @param directive The directive (INCLUDE_KEY or INCLUDE_FLAT_KEY)
     * @return
     * @throws java.io.IOException
     */
    private JsonNode processInlineDirective(String value, String directive) throws IOException {
        if (value.startsWith(directive + "{") && value.endsWith("}")) {
            String path = value.substring(directive.length() + 1, value.length() - 1);
            return new RecursiveJSONParser(this, path).parse();
        }
        return null;
    }

    private JsonNode readResource() throws IOException {
        try (InputStream is = resource.in()) {
            return mapper.readTree(is);
        }
    }

    private Resource getResource(Resource resource, String path) {
        // relative paths are
        if (path.startsWith("./")) path = path.substring(2);
        if (path.startsWith("/")) return getRoot(resource).get(path);
        return resource.parent().get(path);
    }

    /** API is not 100% clear, but going up should lead us to the root of the virtual file system */
    private Resource getRoot(Resource resource) {
        Resource r = resource;
        Resource parent = r.parent();
        while (parent != null && !parent.equals(r)) {
            r = parent;
            parent = r.parent();
        }

        return r;
    }
}
