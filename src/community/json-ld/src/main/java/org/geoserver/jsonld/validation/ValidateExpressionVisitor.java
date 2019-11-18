package org.geoserver.jsonld.validation;

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

    private FeatureType featureType;

    public ValidateExpressionVisitor(FeatureType type) {
        this.featureType = type;
    }

    public ValidateExpressionVisitor() {
        super();
    }

    public Object visit(PropertyName expression, Object data) {
        if (!expression.getPropertyName().startsWith("@"))
            return expression.evaluate(featureType, Object.class);
        else return expression.getPropertyName();
    }

    public Object visit(Function expression, Object data) {
        FunctionName fName = expression.getFunctionName();
        if (expression instanceof FunctionExpressionImpl) {
            if (expression.getParameters().size() < fName.getArgumentCount()) return false;
        }
        return true;
    }
}
