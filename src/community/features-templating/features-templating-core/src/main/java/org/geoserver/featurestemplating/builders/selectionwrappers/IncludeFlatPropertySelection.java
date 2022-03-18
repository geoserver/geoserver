/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionVisitor;
import org.opengis.feature.Property;
import org.opengis.feature.type.PropertyType;

/** A PropertySelectionWrapper meant to wrap a DynamicIncludeFlatBuilder. */
public class IncludeFlatPropertySelection extends PropertySelectionWrapper {

    public IncludeFlatPropertySelection(
            DynamicIncludeFlatBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        DynamicIncludeFlatBuilder includeFlat =
                new DynamicIncludeFlatBuilder((DynamicIncludeFlatBuilder) templateBuilder, true) {
                    @Override
                    protected ObjectNode getFinalJSON(TemplateBuilderContext context) {
                        ObjectNode node = super.getFinalJSON(context);
                        return (ObjectNode) pruneJsonNodeIfNeeded(context, node);
                    }

                    @Override
                    public TemplateBuilder getNestedTree(
                            JsonNode node, TemplateBuilderContext context) {
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
                                    (TemplateBuilder)
                                            builder.accept(propertySelectionVisitor, selContext);
                        }
                        return builder;
                    }

                    @Override
                    public boolean canWrite(TemplateBuilderContext context) {
                        return IncludeFlatPropertySelection.this.canWrite(context)
                                && super.canWrite(context);
                    }
                };
        return includeFlat;
    }
}
