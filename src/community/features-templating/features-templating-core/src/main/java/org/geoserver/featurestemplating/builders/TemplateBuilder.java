/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
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
        return Collections.emptyList();
    }

    /**
     * Get the encoding hints held by this TemplateBuilder.
     *
     * @return
     */
    Map<String, Object> getEncodingHints();

    /**
     * Add an encoding hint to the ones held by this builder.
     *
     * @param key the hint key.
     * @param value the hint value.
     */
    void addEncodingHint(String key, Object value);

    /**
     * Accept method for a TemplateVisitor.
     *
     * @param visitor the {@link TemplateVisitor} to accept.
     * @param value extra data, can be null.
     * @return the result of the visiting process if any.
     */
    Object accept(TemplateVisitor visitor, Object value);

    /**
     * Get the parent of this TemplateBuilder.
     *
     * @return
     */
    default TemplateBuilder getParent() {
        return null;
    }

    /**
     * Set the parent of this TemplateBuilder.
     *
     * @param builder
     */
    default void setParent(TemplateBuilder builder) {
        // does nothing
    }
}
