package org.geoserver.wfstemplating.request;

import java.util.logging.Level;
import org.geoserver.wfstemplating.builders.AbstractTemplateBuilder;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.expression.AbstractExpressionVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

public class EnvJsonPathVisitor extends JsonPathVisitor {

    public EnvJsonPathVisitor(FeatureType type) {
        super(type);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Expression param1 = filter.getExpression1();
        AbstractTemplateBuilder result =
                (AbstractTemplateBuilder)
                        getTemplateBuilderFromProperty((PropertyName) param1, extraData);
        if (result != null) {
            Expression envExpr = filter.getExpression2();
            // if we are dealing with complex features set the namespaces
            // to the PropertyName
            if (result.getNamespaces() != null) {
                AbstractExpressionVisitor visitor =
                        new AbstractExpressionVisitor() {
                            @Override
                            public Object visit(PropertyName expr, Object extraData) {
                                if (expr instanceof AttributeExpressionImpl) {
                                    return new AttributeExpressionImpl(
                                            expr.getPropertyName(), result.getNamespaces());
                                }
                                return super.visit(expr, extraData);
                            }
                        };
                envExpr = (Expression) envExpr.accept(visitor, null);
            }
            result.setEnvExpr(envExpr);
        }
        return super.visit(filter, extraData);
    }

    private TemplateBuilder getTemplateBuilderFromProperty(
            PropertyName expression, Object extraData) {
        String propertyValue = expression.getPropertyName();
        if (extraData instanceof TemplateBuilder) {
            String[] elements;
            if (propertyValue.indexOf(".") != -1) {
                elements = propertyValue.split("\\.");
            } else {
                elements = propertyValue.split("/");
            }
            TemplateBuilder builder = (TemplateBuilder) extraData;
            try {
                currentSource = null;
                currentEl = 0;
                return findBuilder(builder.getChildren(), elements);
            } catch (Exception ex) {
                LOGGER.log(
                        Level.INFO,
                        "Unable to evaluate the json-ld path against"
                                + "the json-ld template. Cause: {0}",
                        ex.getMessage());
            }
        }
        return null;
    }
}
