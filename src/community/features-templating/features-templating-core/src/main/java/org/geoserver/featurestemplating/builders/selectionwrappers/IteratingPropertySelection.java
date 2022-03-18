/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;

/** PropertySelectionWrapper for an Iterating builder. Using when dealing with dynamic keys. */
public class IteratingPropertySelection extends PropertySelectionWrapper {

    public IteratingPropertySelection(
            AbstractTemplateBuilder templateBuilder,
            PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {

        AbstractTemplateBuilder result =
                new IteratingBuilder((IteratingBuilder) templateBuilder, true) {
                    @Override
                    public boolean canWrite(TemplateBuilderContext context) {
                        return IteratingPropertySelection.this.canWrite(context);
                    }
                };
        return result;
    }
}
