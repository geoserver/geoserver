/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.request.TemplatePathVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * STAC specific mapper that:
 *
 * <ul>
 *   <li>Assumes the filter is referencing properties inside "features.properties" directly.
 *   <li>Handles the case where multiple templates are used against the same template
 * </ul>
 */
public class STACPathVisitor extends TemplatePathVisitor {

    static final Map<String, String> WELL_KNOWN_PROPERTIES;

    static {
        WELL_KNOWN_PROPERTIES = new HashMap<>();
        WELL_KNOWN_PROPERTIES.put("geometry", "footprint");
        WELL_KNOWN_PROPERTIES.put("id", "identifier");
        WELL_KNOWN_PROPERTIES.put("collection", "parentIdentifier");
    }

    public STACPathVisitor(FeatureType type) {
        super(type);
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        String propertyName = expression.getPropertyName();

        // well known queriable?
        if (WELL_KNOWN_PROPERTIES.containsKey(propertyName)) {
            return getFactory(extraData)
                    .property(
                            WELL_KNOWN_PROPERTIES.get(propertyName),
                            expression.getNamespaceContext());
        }

        // pick from template
        if (extraData instanceof TemplateBuilder) {
            TemplateBuilder builder = (TemplateBuilder) extraData;
            Object newExpression = mapPropertyThroughBuilder(propertyName, builder);
            // stricteR behavior than base class, if property is not found then it's always null
            if (newExpression != null) {
                return newExpression;
            } else return ff.literal(null);
        }

        // fallback
        return getFactory(extraData)
                .property(expression.getPropertyName(), expression.getNamespaceContext());
    }

    @Override
    public Expression findFunction(TemplateBuilder builder, List<String> pathElements) {
        List<String> fullPath = new ArrayList<>();
        fullPath.add("features");
        fullPath.add("properties");
        fullPath.addAll(pathElements);
        return super.findFunction(builder, fullPath);
    }
}
