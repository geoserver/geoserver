package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.xml.sax.helpers.NamespaceSupport;

public class PropertyPathFunction extends FunctionExpressionImpl implements PropertyName {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "propertyPath",
                    parameter("result", Object.class),
                    parameter("domainProperty", String.class));

    protected String propertyPath;

    protected NamespaceSupport namespaceSupport;

    public PropertyPathFunction(String propertyPath) {
        super(NAME);
        this.propertyPath = propertyPath;
    }

    public PropertyPathFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        String strPropertyPath = (String) getParameters().get(0).evaluate(object);
        AttributeExpressionImpl attributeExpression =
                new AttributeExpressionImpl(strPropertyPath, namespaceSupport);
        return attributeExpression.evaluate(object);
    }

    @Override
    public String getPropertyName() {
        if (getParameters() != null && getParameters().size() > 0)
            return getParameters().get(0).evaluate(null, String.class);
        else return propertyPath;
    }

    public void setPropertyName(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    @Override
    public NamespaceSupport getNamespaceContext() {
        return namespaceSupport;
    }

    public void setNamespaceContext(NamespaceSupport namespaceContext) {
        this.namespaceSupport = namespaceContext;
    }

    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        // we explicitly handle the attribute extractor filter
        return visitor.visit((PropertyName) this, extraData);
    }
}
