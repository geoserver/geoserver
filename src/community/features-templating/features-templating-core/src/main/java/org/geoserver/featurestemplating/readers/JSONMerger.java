package org.geoserver.featurestemplating.readers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;

public class JSONMerger {

    public static final String DYNAMIC_MERGE_KEY = "$dynamicMerge";
    public static final String NODE1 = "node1";
    public static final String NODE2 = "node2";
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
                boolean check = false;
                if (ov.asText().startsWith("${") || ov.asText().startsWith("$${")) {
                    ObjectNode emptyNode = JsonNodeFactory.instance.objectNode();
                    ObjectNode emptyNode2 = JsonNodeFactory.instance.objectNode();

                    // set empty node to create DYNAMIC_MERGE_KEY as parent
                    merged.set(DYNAMIC_MERGE_KEY, emptyNode);
                    merged.with(DYNAMIC_MERGE_KEY).set(name, emptyNode2);
                    merged.with(DYNAMIC_MERGE_KEY).with(name).set(NODE1, ov);
                    merged.with(DYNAMIC_MERGE_KEY).with(name).set(NODE2, bv);
                    check = true;
                }
                if (!check) merged.set(name, ov);
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
}
