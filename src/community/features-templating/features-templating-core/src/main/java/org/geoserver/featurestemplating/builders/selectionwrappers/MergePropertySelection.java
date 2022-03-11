/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

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
                    protected void writeFromNestedTree(
                            TemplateBuilderContext context,
                            TemplateOutputWriter writer,
                            JsonNode node)
                            throws IOException {
                        node = (JsonNode) pruneJsonNodeIfNeeded(context, node);
                        super.writeFromNestedTree(context, writer, node);
                    }
                };
        return dynamicMergeBuilder;
    }
}
