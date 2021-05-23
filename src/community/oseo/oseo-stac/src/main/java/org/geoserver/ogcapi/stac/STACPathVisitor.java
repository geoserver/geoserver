/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.ArrayList;
import java.util.List;
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

    public STACPathVisitor(FeatureType type) {
        super(type);
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        String propertyName = expression.getPropertyName();
        if (extraData instanceof TemplateBuilder) {
            TemplateBuilder builder = (TemplateBuilder) extraData;
            Object newExpression = mapPropertyThroughBuilder(propertyName, builder);
            // stricted behavior than base class, if property is not found then it's always null
            if (newExpression != null) {
                return newExpression;
            } else return ff.literal(null);
        }
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
