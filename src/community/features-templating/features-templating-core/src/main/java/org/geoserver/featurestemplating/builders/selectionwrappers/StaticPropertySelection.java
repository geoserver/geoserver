/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

/** A PropertySelectionWrapper meant to wrap a StaticBuilder. */
public class StaticPropertySelection extends PropertySelectionWrapper {

    public StaticPropertySelection(
            StaticBuilder templateBuilder, PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        StaticBuilder staticBuilder =
                new StaticBuilder((StaticBuilder) templateBuilder, true) {
                    @Override
                    protected void evaluateInternal(
                            TemplateOutputWriter writer, TemplateBuilderContext context)
                            throws IOException {
                        staticValue =
                                (JsonNode) pruneJsonNodeIfNeeded(context, staticValue.deepCopy());
                        super.evaluateInternal(writer, context);
                    }

                    @Override
                    public boolean canWrite(TemplateBuilderContext context) {
                        return StaticPropertySelection.this.canWrite(context)
                                && super.canWrite(context);
                    }
                };
        return staticBuilder;
    }
}
