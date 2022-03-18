/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.opengis.feature.Property;
import org.opengis.feature.type.PropertyType;

/** A PropertySelectionWrapper meant to wrap a DynamicMergeBuilder. */
public class MergePropertySelection extends PropertySelectionWrapper {

    public MergePropertySelection(
            DynamicMergeBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        DynamicMergeBuilder dynamicMergeBuilder =
                new DynamicMergeBuilder((DynamicMergeBuilder) templateBuilder, true) {
                    @Override
                    protected void writeValue(
                            String name,
                            TemplateOutputWriter writer,
                            Object value,
                            TemplateBuilderContext context)
                            throws IOException {
                        value = pruneJsonNodeIfNeeded(context, value);
                        super.writeValue(name, writer, value, context);
                    }

                    @Override
                    public TemplateBuilder getNestedTree(
                            JsonNode node, TemplateBuilderContext context) {
                        TemplateBuilder result = super.getNestedTree(node, context);
                        Object object = context != null ? context.getCurrentObj() : null;
                        if (object != null) {
                            Property prop = (Property) object;
                            PropertyType type = prop.getType();
                            PropertySelectionVisitor propertySelectionVisitor =
                                    new PropertySelectionVisitor(strategy, type);
                            PropertySelectionContext selContext =
                                    new PropertySelectionContext(getFullKey(context), false, false);
                            result =
                                    (TemplateBuilder)
                                            result.accept(propertySelectionVisitor, selContext);
                        }
                        return result;
                    }

                    @Override
                    public boolean canWrite(TemplateBuilderContext context) {
                        return MergePropertySelection.this.canWrite(context)
                                && super.canWrite(context);
                    }
                };
        return dynamicMergeBuilder;
    }
}
