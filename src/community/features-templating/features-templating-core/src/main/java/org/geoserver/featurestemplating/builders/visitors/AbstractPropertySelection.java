package org.geoserver.featurestemplating.builders.visitors;

import static org.geoserver.featurestemplating.builders.TemplateBuilderUtils.hasSelectableKey;
import static org.geoserver.featurestemplating.readers.JSONMerger.DYNAMIC_MERGE_KEY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.selectionwrappers.PropertySelectionWrapper;

/** Abstract implementation of a {@link PropertySelectionHandler}. */
public abstract class AbstractPropertySelection implements PropertySelectionHandler {

    protected Set<String> includedFields;

    @Override
    public boolean isBuilderSelected(AbstractTemplateBuilder templateBuilder, Object context) {

        TemplateBuilderContext builderContext = null;
        if (context != null) builderContext = new TemplateBuilderContext(context);
        boolean result;
        if (templateBuilder instanceof PropertySelectionWrapper) {
            PropertySelectionWrapper selectionWrapper = (PropertySelectionWrapper) templateBuilder;
            String fullPath = selectionWrapper.getFullKey(builderContext);
            fullPath = removeDynamicMergeKey(fullPath);
            result = isKeySelected(fullPath);
        } else {
            result = isBuilderSelected(templateBuilder, null);
        }
        return result;
    }

    @Override
    public boolean isBuilderSelected(
            AbstractTemplateBuilder templateBuilder, PropertySelectionContext extradata) {

        String key;
        if (extradata != null) {
            key = extradata.getStaticFullKey();
        } else {
            key = templateBuilder.getKey(null);
        }
        key = removeDynamicMergeKey(key);
        return isKeySelected(templateBuilder, key);
    }

    /**
     * Check if the template builder has been selected.
     *
     * @param abstractTb the templateBuilder.
     * @param key the full key of the builder.
     * @return true if it is selected, false otherwise.
     */
    protected boolean isKeySelected(AbstractTemplateBuilder abstractTb, String key) {
        return (hasSelectableKey(abstractTb) && isKeySelected(key))
                || key == null
                || !hasSelectableKey(abstractTb);
    }

    /**
     * Check if the key is selected.
     *
     * @param key the full key of the attribute that needs to be checked.
     * @return true if the attribute was selected, false otherwise.
     */
    protected abstract boolean isKeySelected(String key);

    @Override
    public JsonNode pruneJsonAttributes(JsonNode node, String fullKey) {
        if (node.isObject()) pruneObjectNode((ObjectNode) node, fullKey);
        else if (node.isArray()) pruneArrayNode((ArrayNode) node, fullKey);
        return node;
    }

    private void pruneArrayNode(ArrayNode arrayNode, String parentPath) {
        int length = arrayNode.size();
        for (int i = 0; i < length; i++) {
            JsonNode node = arrayNode.get(i);
            if (node.isObject()) pruneObjectNode((ObjectNode) node, parentPath);
        }
    }

    private void pruneObjectNode(ObjectNode objectNode, String parentPath) {
        Iterator<String> names = objectNode.fieldNames();
        List<String> excluded = new ArrayList<>();
        while (names.hasNext()) {
            String name = names.next();
            String fullPath = updatedFullKey(parentPath, name);
            fullPath = removeDynamicMergeKey(fullPath);
            if (!isKeySelected(fullPath)) {
                excluded.add(name);
            } else {
                JsonNode node = objectNode.get(name);
                if (emptyArrayOrObject(node)) {
                    excluded.add(name);
                } else if (node.isObject()) {
                    pruneObjectNode((ObjectNode) node, fullPath);
                } else if (node.isArray()) {
                    pruneArrayNode((ArrayNode) node, fullPath);
                }
            }
        }
        objectNode.remove(excluded);
    }

    private boolean emptyArrayOrObject(JsonNode node) {
        return (node.isArray() || node.isObject()) && node.isEmpty();
    }

    private String updatedFullKey(String currentPath, String attribute) {
        if (currentPath == null && attribute != null) currentPath = attribute;
        else if (attribute != null) currentPath = currentPath.concat(".").concat(attribute);
        return currentPath;
    }

    private String removeDynamicMergeKey(String key) {
        if (key != null) {
            int index = key.indexOf(DYNAMIC_MERGE_KEY);
            while (index != -1) {
                String subStr = key.substring(index + DYNAMIC_MERGE_KEY.length());
                int sepIndex = subStr.indexOf(".");
                if (sepIndex == -1) {
                    key = null;
                    break;
                }
                String suffix = subStr.substring(0, sepIndex);
                key = key.replace(DYNAMIC_MERGE_KEY + suffix + ".", "");
                index = key.indexOf(DYNAMIC_MERGE_KEY);
            }
        }
        return key;
    }
}
