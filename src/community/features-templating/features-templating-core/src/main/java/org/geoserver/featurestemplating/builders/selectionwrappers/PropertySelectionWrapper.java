/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import static org.geoserver.featurestemplating.builders.TemplateBuilderUtils.hasSelectableKey;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedList;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderWrapper;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;

/**
 * A generic PropertySelectionWrapper suitable for usage when a selectable TemplateBuilder has a
 * dynamic key. It uses a {@link PropertySelectionHandler} to determine if the builder should
 * participate in the output encoding or not.
 */
public class PropertySelectionWrapper extends TemplateBuilderWrapper {

    protected PropertySelectionHandler strategy;

    private String fullKey;

    public PropertySelectionWrapper(
            AbstractTemplateBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder);
        this.strategy = propertySelectionHandler;
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        return strategy.isBuilderSelected(this, context.getCurrentObj());
    }

    /**
     * Get the full key/path by concatenating the key of the wrapped builder with the one of the
     * parent.
     *
     * @param context the TemplateBuilder context used for dynamic keys.
     * @return the full key/path
     */
    public String getFullKey(TemplateBuilderContext context) {
        String key = getKey(context);
        // if no parent of this builder has a dynamic key
        // the property selection visitor should have set it.
        if (fullKey != null) {
            String result = fullKey;
            if (key != null && !fullKey.endsWith(key)) result = result.concat(".").concat(key);
            return result;
        }

        LinkedList<String> linkedList = new LinkedList<>();
        if (key != null) {
            linkedList.add(key);
        }
        TemplateBuilder builder = this;
        TemplateBuilder currParent = builder.getParent();
        while (currParent != null) {
            if (currParent instanceof AbstractTemplateBuilder && hasSelectableKey(currParent)) {
                AbstractTemplateBuilder parent = (AbstractTemplateBuilder) currParent;
                String pKey = parent.getKey(context);
                if (pKey != null) linkedList.addFirst(pKey);
            }
            currParent = currParent.getParent();
        }
        if (!linkedList.isEmpty()) key = linkedList.stream().collect(Collectors.joining("."));
        return key;
    }

    /**
     * Method to prune a value before encoding it if it has JsonNode type.
     *
     * @param context the TemplateBuilderContext.
     * @param value the value.
     * @return the value. If the value is of type JsonNode the result will be a JsonNode pruned
     *     accordingly to the chosen handler.
     */
    protected Object pruneJsonNodeIfNeeded(TemplateBuilderContext context, Object value) {
        if (value instanceof JsonNode) {
            value = strategy.pruneJsonAttributes((JsonNode) value, getFullKey(context));
        }
        return value;
    }

    /**
     * Set the full key.
     *
     * @param fullKey the full key.
     */
    public void setFullKey(String fullKey) {
        this.fullKey = fullKey;
    }
}
