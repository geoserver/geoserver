/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.validation;

import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

/**
 * Visitor to perform a validation of json-ld template based on evaluation of xpath against the
 * {@link FeatureType} and on the checking of param count for CQL expression
 */
public class ValidateExpressionVisitor extends DefaultFilterVisitor {

    private int contextPos;

    public ValidateExpressionVisitor() {
        super();
    }

    public Object visit(PropertyName expression, Object data) {
        if (expression.getPropertyName().indexOf("@") == -1) {
            String xpathPath = expression.getPropertyName();
            PropertyName pn =
                    new AttributeExpressionImpl(
                            determineContextPos(xpathPath), expression.getNamespaceContext());
            JsonBuilderContext context = (JsonBuilderContext) data;
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            return pn.evaluate(context.getCurrentObj(), Object.class);
        } else return expression.getPropertyName();
    }

    public Object visit(Function expression, Object data) {
        FunctionName fName = expression.getFunctionName();
        if (expression instanceof FunctionExpressionImpl) {
            if (expression.getParameters().size() < fName.getArgumentCount()) return false;
        }
        return true;
    }

    private String determineContextPos(String xpath) {
        contextPos = 0;
        while (xpath.contains("../")) {
            contextPos++;
            xpath = xpath.replaceFirst("\\.\\./", "");
        }
        return xpath;
    }
}
