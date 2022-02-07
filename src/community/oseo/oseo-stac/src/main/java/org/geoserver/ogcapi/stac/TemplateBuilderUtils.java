/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;

class TemplateBuilderUtils {

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
