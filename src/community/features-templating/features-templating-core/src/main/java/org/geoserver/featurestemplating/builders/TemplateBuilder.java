/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;

/** Basic interface for all the builders */
public interface TemplateBuilder {

    /**
     * Writes a piece of json-ld output evaluating builder's corresponding portion of json-ld
     * template against current object passed inside ${@link TemplateBuilderContext}
     */
    void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context) throws IOException;

    /**
     * Add a template builder a child of this builder
     *
     * @param children
     */
    default void addChild(TemplateBuilder children) {}

    /**
     * Get all the children of this builder
     *
     * @return the builder's children list
     */
    default List<TemplateBuilder> getChildren() {
        return null;
    }
}
