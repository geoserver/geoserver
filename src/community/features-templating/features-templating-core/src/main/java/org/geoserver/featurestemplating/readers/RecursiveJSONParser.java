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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.geoserver.platform.resource.Resource;

/** Parses a JSON structure, processing eventual includes and expanding them */
public class RecursiveJSONParser extends RecursiveTemplateResourceParser {

    private static final String INCLUDE_KEY = "$include";
    private static final String MERGE_KEY = "$merge";
    public static final String INCLUDE_FLAT_KEY = "$includeFlat";
    public static final String INCLUDE_FLAT_EXPR = "$includeExpression";
    public static final String INCLUDING_NODE = "$includingNode";

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
        // read and close before doing recursion, avoids keeping several files open in parallel
        JsonNode root = readResource();
        JsonNode result = expandIncludes(root);

        // we need to do this here to properly handle dynamic inclusion
        // regardless the presence of the $includeFlat directive
        // in a base or overlay template in case merge directive is present.
        result = processDynamicIncludeFlat(result);
        return result;
    }

    // Parse the JsonNode if no other inclusion/merge is pending
    // and set the keys for the dynamic includeFlat
    private JsonNode processDynamicIncludeFlat(JsonNode result) {
        if (parent == null) {
            List<JsonNode> parents = new LinkedList<>(result.findParents(INCLUDE_FLAT_KEY));
            List<JsonNode> replacements = new LinkedList<>();
            if (parents != null && !parents.isEmpty()) {
                parents.forEach(p -> replacements.add(buildContainer((ObjectNode) p)));
                int matched = -1;
                for (int i = 0; i < parents.size(); i++) {
                    if (parents.get(i).equals(result)) {
                        matched = i;
                        break;
                    }
                }
                if (matched != -1) {
                    result = replacements.get(matched);
                    parents.remove(matched);
                    replacements.remove(matched);
                }
                findAndReplace(result, parents, replacements);
            }
        }
        return result;
    }

    private void findAndReplace(
            JsonNode result, List<JsonNode> replaced, List<JsonNode> replacements) {
        if (result.isObject()) {
            ObjectNode objectNode = (ObjectNode) result;
            findAndReplaceInObj(objectNode, replaced, replacements);
        } else if (result.isArray()) {
            ArrayNode arrayNode = (ArrayNode) result;
            findAndReplaceInArray(arrayNode, replaced, replacements);
        }
    }

    private void findAndReplaceInObj(
            ObjectNode currNode, List<JsonNode> replaced, List<JsonNode> replacements) {
        Iterator<String> names = currNode.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode node = currNode.get(name);
            int index = replaced.indexOf(node);
            if (index != -1) {
                currNode.set(name, replacements.get(index));
                replaced.remove(index);
                replacements.remove(index);
            } else {
                findAndReplace(node, replaced, replacements);
            }
        }
    }

    private void findAndReplaceInArray(
            ArrayNode currNode, List<JsonNode> replaced, List<JsonNode> replacements) {
        for (int i = 0; i < currNode.size(); i++) {
            JsonNode node = currNode.get(i);
            int index = replaced.indexOf(node);
            if (index != -1) {
                currNode.set(i, replacements.get(index));
                replaced.remove(index);
                replacements.remove(index);
            } else {
                findAndReplace(node, replaced, replacements);
            }
        }
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

        JSONMerger jsonMerger = new JSONMerger(rootCollectionName);
        return jsonMerger.mergeTrees(base, root);
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
                if (!isDynamicIncludeFlat(node)) {
                    // ok we need to include a json file
                    Resource resource = getResource(this.resource, node.asText());
                    JsonNode processed = new RecursiveJSONParser(this, resource).parse();
                    Iterator<String> fields = processed.fieldNames();
                    while (fields.hasNext()) {
                        String field = fields.next();
                        result.set(field, processed.get(field));
                    }
                    continue;
                }
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

    private ObjectNode buildContainer(ObjectNode result) {
        JsonNode dynamicIncludeFlat = result.remove(INCLUDE_FLAT_KEY);
        ObjectNode objectNode = mapper.getNodeFactory().objectNode();
        objectNode.set(INCLUDING_NODE, result);
        objectNode.set(INCLUDE_FLAT_EXPR, dynamicIncludeFlat);
        ObjectNode container = mapper.getNodeFactory().objectNode();
        container.set(INCLUDE_FLAT_KEY, objectNode);
        return container;
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

    private boolean isDynamicIncludeFlat(JsonNode node) {
        return (node.asText().startsWith("${") || node.asText().startsWith("$${"));
    }
}
