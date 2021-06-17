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
import java.util.Iterator;
import org.geoserver.platform.resource.Resource;

/** Parses a JSON structure, processing eventual includes and expanding them */
public class RecursiveJSONParser extends RecursiveTemplateResourceParser {

    private static final String INCLUDE_KEY = "$include";
    private static final String INCLUDE_FLAT_KEY = "$includeFlat";

    private final ObjectMapper mapper;

    public RecursiveJSONParser(Resource resource) {
        super(resource);
        this.mapper = new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
    }

    private RecursiveJSONParser(RecursiveJSONParser parent, Resource resource) {
        super(resource, parent);
        this.mapper = parent.mapper;
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
                Resource resource = getResource(this.resource, node.asText());
                JsonNode processed = new RecursiveJSONParser(this, resource).parse();
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
            Resource resource = getResource(this.resource, path);
            return new RecursiveJSONParser(this, resource).parse();
        }
        return null;
    }

    private JsonNode readResource() throws IOException {
        try (InputStream is = resource.in()) {
            return mapper.readTree(is);
        }
    }
}
