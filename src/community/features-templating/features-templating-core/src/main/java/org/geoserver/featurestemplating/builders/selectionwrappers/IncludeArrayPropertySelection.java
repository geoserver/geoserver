/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.ArrayIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionVisitor;
import org.opengis.feature.Property;
import org.opengis.feature.type.PropertyType;

/** A PropertySelectionWrapper meant to wrap an {@link ArrayIncludeFlatBuilder}. */
public class IncludeArrayPropertySelection extends PropertySelectionWrapper {

    public IncludeArrayPropertySelection(
            ArrayIncludeFlatBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        return new ArrayIncludeFlatBuilder((ArrayIncludeFlatBuilder) templateBuilder, true) {
            @Override
            protected ArrayNode getFinalJSON(TemplateBuilderContext context) {
                ArrayNode node = super.getFinalJSON(context);
                return (ArrayNode) pruneJsonNodeIfNeeded(context, node);
            }

            @Override
            public TemplateBuilder getNestedTree(JsonNode node, TemplateBuilderContext context) {
                TemplateBuilder builder = super.getNestedTree(node, context);
                Object object = context != null ? context.getCurrentObj() : null;
                if (object != null) {
                    Property prop = (Property) object;
                    PropertyType type = prop.getType();
                    PropertySelectionVisitor propertySelectionVisitor =
                            new PropertySelectionVisitor(strategy, type);
                    PropertySelectionContext selContext =
                            new PropertySelectionContext(getFullKey(context), false, false);
                    builder =
                            (TemplateBuilder) builder.accept(propertySelectionVisitor, selContext);
                }
                return builder;
            }

            @Override
            public boolean canWrite(TemplateBuilderContext context) {
                return IncludeArrayPropertySelection.this.canWrite(context)
                        && super.canWrite(context);
            }
        };
    }
}
