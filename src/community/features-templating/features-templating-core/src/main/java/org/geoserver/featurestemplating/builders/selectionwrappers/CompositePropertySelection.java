/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.selectionwrappers;

import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.PropertySelectionHandler;

/** PropertySelectionWrapper for a compositeBuilder. Used for dynamic keys selection. */
public class CompositePropertySelection extends PropertySelectionWrapper {

    public CompositePropertySelection(
            CompositeBuilder templateBuilder, PropertySelectionHandler propertySelectionHandler) {
        super(templateBuilder, propertySelectionHandler);
    }

    @Override
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {

        AbstractTemplateBuilder result =
                new CompositeBuilder((CompositeBuilder) templateBuilder, true) {
                    @Override
                    public boolean canWrite(TemplateBuilderContext context) {
                        return CompositePropertySelection.this.canWrite(context);
                    }
                };
        return result;
    }
}
