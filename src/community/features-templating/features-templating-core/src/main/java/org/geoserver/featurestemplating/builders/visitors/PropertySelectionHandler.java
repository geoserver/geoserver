/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import com.fasterxml.jackson.databind.JsonNode;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;

/**
 * Strategy used by {@link PropertySelectionVisitor} and by a {@link
 * org.geoserver.featurestemplating.builders.selectionwrappers.PropertySelectionWrapper} to
 * determine whether a template builder evaluation result is matching the selected attributes of a
 * feature. Subclasses should implement to provide a different matching logic of a template key.
 */
public interface PropertySelectionHandler {

    /**
     * Check if the templateBuilder evaluation result should be included in the final output based
     * on requested Feature's properties. Use this method if the builder belongs to a nested tree
     * where one or more keys are dynamic.
     *
     * @param templateBuilder the templateBuilder.
     * @param object a Feature Attribute or a Feature to be evaluated from the builder . Needed when
     *     performing the selection at Feature evaluation time.
     * @return true if the templateBuilder evaluation result should be included in the final output,
     *     false otherwise.
     */
    boolean isBuilderSelected(AbstractTemplateBuilder templateBuilder, Object object);

    /**
     * Check if the templateBuilder evaluation result should be included in the final output based
     * on requested Feature's properties. Use this method when the full key can be computed at
     * runtime: eg. no dynamic keys, merge, flat inclusion is at stake.
     *
     * @param templateBuilder the templateBuilder.
     * @param extradata PropertySelectionExtradata eventually holding the full key of the template
     *     being passed.
     * @return true if the templateBuilder evaluation result should be included in the final output,
     *     false otherwise.
     */
    boolean isBuilderSelected(
            AbstractTemplateBuilder templateBuilder, PropertySelectionContext extradata);

    /**
     * Prune the JsonNode node from the attributes that are not selected.
     *
     * @param node the JsonNode.
     * @param fullKey the full key of the builder producing the JsonNode.
     * @return the updated JsonNode.
     */
    JsonNode pruneJsonAttributes(JsonNode node, String fullKey);

    /**
     * Method to check if a Static or Dynamic builder with a JsonValue result must be wrapped or
     * not. This method should not perform any logic to assess if the passed builder has a JsonValue
     * as evaluation result. It is expected that the client code already did the check.
     *
     * @param builder the Dynamic or Static builder.
     * @return true if must be wrapped, false otherwise.
     */
    boolean hasSelectableJsonValue(AbstractTemplateBuilder builder);
}
