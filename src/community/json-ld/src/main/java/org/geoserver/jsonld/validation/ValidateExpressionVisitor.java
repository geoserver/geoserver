/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.validation;

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

    private JsonBuilderContext context;

    public ValidateExpressionVisitor(JsonBuilderContext context) {
        super();
        this.context = context;
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        Object result = null;
        // attribute selector @ will not evaluate against featureType
        if (!expression.getPropertyName().contains("@")) {
            String xpathPath = expression.getPropertyName();
            PropertyName pn =
                    new AttributeExpressionImpl(xpathPath, expression.getNamespaceContext());
            result = pn.evaluate(context.getCurrentObj());
        } else {
            result = context;
        }
        return result;
    }

    public JsonBuilderContext getContext() {
        return context;
    }

    public void setContext(JsonBuilderContext context) {
        this.context = context;
    }
}
