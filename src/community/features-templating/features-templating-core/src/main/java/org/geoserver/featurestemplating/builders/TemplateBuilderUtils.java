/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;

/** Class that provides some utility method for TemplateBuilders. */
public class TemplateBuilderUtils {

    /**
     * Check if the key of the builder is selectable. By selectable it is meant a key that is not
     * out of the scope of a feature properties.
     *
     * @param builder the builder whose key needs to be checked.
     * @return true if the key is selectable false otherwise.
     */
    public static boolean hasSelectableKey(TemplateBuilder builder) {
        boolean validKey = true;
        if (builder instanceof SourceBuilder) {
            SourceBuilder sourceBuilder = (SourceBuilder) builder;
            validKey = !sourceBuilder.isTopLevelFeature() && sourceBuilder.hasOwnOutput();
        }
        return validKey;
    }

    public static TemplateBuilder getBuilderFor(TemplateBuilder builder, String... pathComponents) {
        for (String path : pathComponents) {
            builder = lookupBuilder(builder, path);
            if (builder == null) return null;
        }

        return builder;
    }

    private static AbstractTemplateBuilder lookupBuilder(TemplateBuilder parent, String key) {
        for (TemplateBuilder child : parent.getChildren()) {
            if (child instanceof AbstractTemplateBuilder) {
                AbstractTemplateBuilder atb = (AbstractTemplateBuilder) child;
                if (key.equals(atb.getKey(null))) {
                    return atb;
                } else if (atb instanceof CompositeBuilder && atb.getKey(null) == null) {
                    return lookupBuilder(atb, key);
                }
            }
        }
        return null;
    }
}
