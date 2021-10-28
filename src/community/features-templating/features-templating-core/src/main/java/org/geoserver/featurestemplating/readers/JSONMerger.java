package org.geoserver.featurestemplating.readers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.IteratorUtils;

/**
 * This class is responsible for merging 2 JsonNodes, base and overlay. JsonNode overlay contains
 * keyword $merge. If overlay attributes exists on base node, base node's same attributes will be
 * replaced with overlay's and others remain.
 */
public class JSONMerger {

    public static final String DYNAMIC_MERGE_KEY = "$dynamicMerge_";
    public static final String DYNAMIC_MERGE_OVERLAY = "overlay";
    public static final String DYNAMIC_MERGE_BASE = "base";
    private String rootCollectionName = "features";

    public JSONMerger() {}

    public JSONMerger(String rootCollectionName) {
        this.rootCollectionName = rootCollectionName;
    }

    public ObjectNode mergeTrees(JsonNode base, JsonNode overlay) {
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

    /**
     * When the overlay as a property interpolation directive or expression (${} or $${}), it puts
     * "$dynamicMerge" to the root of the merge result. Then assign "node1" to the overlay and
     * "node2" to the base.
     *
     * @param base
     * @param overlay
     * @return merge result of base and overlay
     */
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
                if (isDynamicMerge(ov, bv)) dynamicMergeDirective(merged, name, bv, ov);
                else merged.set(name, ov);
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

    private boolean isDynamicMerge(JsonNode ov, JsonNode bv) {
        Predicate<JsonNode> isDynamic =
                node ->
                        node.isTextual()
                                && (node.asText().startsWith("${")
                                        || node.asText().startsWith("$${"));
        Predicate<JsonNode> isObject = node -> node.getNodeType() == JsonNodeType.OBJECT;
        return (isDynamic.test(ov) && isObject.test(bv))
                || (isDynamic.test(bv) && isObject.test(ov));
    }

    private void dynamicMergeDirective(ObjectNode merged, String name, JsonNode bv, JsonNode ov) {
        ObjectNode emptyNode = JsonNodeFactory.instance.objectNode();
        ObjectNode emptyNode2 = JsonNodeFactory.instance.objectNode();
        String key = DYNAMIC_MERGE_KEY.concat(name);
        // set empty node to create DYNAMIC_MERGE_KEY as parent
        merged.set(key, emptyNode);
        merged.with(key).set(name, emptyNode2);
        merged.with(key).with(name).set(DYNAMIC_MERGE_OVERLAY, ov);
        merged.with(key).with(name).set(DYNAMIC_MERGE_BASE, bv);
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
}
