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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.geoserver.platform.resource.Resource;

/** Parses a JSON structure, processing eventual includes and expanding them */
public class RecursiveJSONParser extends RecursiveTemplateResourceParser {

    private static final String INCLUDE_KEY = "$include";
    private static final String MERGE_KEY = "$merge";
    private static final String INCLUDE_FLAT_KEY = "$includeFlat";

    private final ObjectMapper mapper;
    private final String rootCollectionName;

    public RecursiveJSONParser(Resource resource) {
        super(resource);
        this.mapper = new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        this.rootCollectionName = "features";
    }

    public RecursiveJSONParser(Resource resource, String rootCollectionName) {
        super(resource);
        this.mapper = new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        this.rootCollectionName = rootCollectionName;
    }

    private RecursiveJSONParser(RecursiveJSONParser parent, Resource resource) {
        super(resource, parent);
        this.mapper = parent.mapper;
        this.rootCollectionName = parent.rootCollectionName;
    }

    public JsonNode parse() throws IOException {
        // read and close before doing recursion, avoids keeping severa files open in parallel
        JsonNode root = readResource();
        JsonNode result = expandIncludes(root);
        return result;
    }

    private ObjectNode processMergeDirective(ObjectNode root) throws IOException {
        // verify key structure and target existence
        JsonNode mergeValue = root.remove(MERGE_KEY);
        if (!(mergeValue.getNodeType() == JsonNodeType.STRING))
            throw new IllegalArgumentException(
                    MERGE_KEY + " property must have a string value, pointing to a base template");
        Resource mergeResource = getResource(this.resource, mergeValue.textValue());
        if (mergeResource.getType() != Resource.Type.RESOURCE)
            throw new IllegalArgumentException(
                    MERGE_KEY + " resource " + mergeResource.path() + " could not be found");

        // load the other template, using the recursive loading
        RecursiveJSONParser delegate = new RecursiveJSONParser(this, mergeResource);
        JsonNode base = delegate.parse();
        return mergeTrees(base, root);
    }

    private ObjectNode mergeTrees(JsonNode base, JsonNode overlay) {
        // first validate they are both objects
        if (base.getNodeType() != JsonNodeType.OBJECT
                || overlay.getNodeType() != JsonNodeType.OBJECT)
            throw new IllegalArgumentException(
                    "Trying to merge but either source or target are not objects:\n"
                            + base.toPrettyString()
                            + "\n"
                            + overlay.toPrettyString());

        return mergeTrees((ObjectNode) base, (ObjectNode) overlay);
    }

    private ObjectNode mergeTrees(ObjectNode base, ObjectNode overlay) {
        Set<String> baseNames = new LinkedHashSet<>(IteratorUtils.toList(base.fieldNames()));

        // add/override missing
        ObjectNode merged = JsonNodeFactory.instance.objectNode();
        for (String name : baseNames) {
            JsonNode bv = base.get(name);
            JsonNode ov = overlay.get(name);

            if (ov == null) {
                // keep original
                merged.set(name, bv);
            } else if (ov instanceof ObjectNode && bv instanceof ObjectNode) {
                // recurse merge
                JsonNode mergedChild = mergeTrees((ObjectNode) bv, (ObjectNode) ov);
                merged.set(name, mergedChild);
            } else if (isRootCollectionArray(name, bv, ov)) {
                // special case for the features array, drill down
                merged.set(name, bv);
                JsonNode mergedChild = mergeTrees(bv.get(0), ov.get(0));
                ((ArrayNode) merged.get(name)).set(0, mergedChild);
            } else if (ov.getNodeType() != JsonNodeType.NULL) {
                merged.set(name, ov);
            }
        }

        // add the extra bits
        Set<String> overlayNames = new LinkedHashSet<>(IteratorUtils.toList(overlay.fieldNames()));
        overlayNames.removeAll(baseNames);
        for (String name : overlayNames) {
            JsonNode ov = overlay.get(name);
            merged.set(name, ov);
        }

        return merged;
    }

    private boolean isRootCollectionArray(String name, JsonNode bv, JsonNode ov) {
        return rootCollectionName.equals(name)
                && bv instanceof ArrayNode
                && ov instanceof ArrayNode
                && bv.size() == 1
                && ov.size() == 1
                && bv.get(0) instanceof ObjectNode
                && ov.get(0) instanceof ObjectNode;
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
        // process eventual merge directive as well
        if (result.has(MERGE_KEY)) {
            result = processMergeDirective(result);
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
