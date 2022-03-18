/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

/** A property selection wrapper meant to wrap a DynamicValueBuilder. */
public class DynamicPropertySelection extends PropertySelectionWrapper {

    public DynamicPropertySelection(
            DynamicValueBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        DynamicValueBuilder dynamic =
                new DynamicValueBuilder((DynamicValueBuilder) templateBuilder, true) {
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
                    public boolean canWrite(TemplateBuilderContext context) {
                        return DynamicPropertySelection.this.canWrite(context)
                                && super.canWrite(context);
                    }
                };
        return dynamic;
    }
}
