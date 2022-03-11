/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import static org.geoserver.featurestemplating.builders.TemplateBuildersUtils.hasSelectableKey;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedList;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderWrapper;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;

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
        boolean isSelected = strategy.isBuilderSelected(this, context.getCurrentObj());
        return isSelected && delegate.canWrite(context);
    }

    public String getFullKey(TemplateBuilderContext context) {
        if (fullKey != null) return fullKey;
        String key = getKey(context);
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

    protected Object pruneJsonNodeIfNeeded(TemplateBuilderContext context, Object value) {
        if (value instanceof JsonNode) {
            value = strategy.pruneJsonAttributes((JsonNode) value, getFullKey(context));
        }
        return value;
    }

    public void setFullKey(String fullKey) {
        this.fullKey = fullKey;
    }
}
