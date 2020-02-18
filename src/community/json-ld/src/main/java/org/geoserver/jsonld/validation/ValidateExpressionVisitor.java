/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.validation;

import static org.geoserver.jsonld.expressions.ExpressionsUtils.determineContextPos;
import static org.geoserver.jsonld.expressions.ExpressionsUtils.removeBackDots;

import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.PropertyName;

/**
 * Visitor to perform a validation of json-ld template based on evaluation of xpath against the
 * {@link FeatureType} and on the checking of param count for CQL expression
 */
public class ValidateExpressionVisitor extends DefaultFilterVisitor {

    private int contextPos = 0;

    public ValidateExpressionVisitor() {
        super();
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        if (expression.getPropertyName().indexOf("@") == -1) {
            String xpathPath = expression.getPropertyName();
            this.contextPos = determineContextPos(xpathPath);
            xpathPath = removeBackDots(xpathPath);
            PropertyName pn =
                    new AttributeExpressionImpl(xpathPath, expression.getNamespaceContext());
            JsonBuilderContext context = (JsonBuilderContext) data;
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            return pn.evaluate(context.getCurrentObj(), Object.class);
        } else return expression.getPropertyName();
    }
}
