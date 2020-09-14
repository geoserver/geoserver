package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class XpathFunction extends FunctionExpressionImpl implements PropertyName {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "xpath",
                    parameter("result", Object.class),
                    parameter("property", String.class));

    protected String propertyName;

    protected NamespaceSupport namespaceSupport;

    public XpathFunction(String propertyName) {
        super(NAME);
        this.propertyName = propertyName;
    }

    public XpathFunction() {
        super(NAME);
    }

    @Override
    public Object evaluate(Object object) {
        String strXpath = (String) getParameters().get(0).evaluate(object);
        AttributeExpressionImpl attributeExpression =
                new AttributeExpressionImpl(strXpath, namespaceSupport);
        return attributeExpression.evaluate(object);
    }

    @Override
    public String getPropertyName() {
        if (getParameters() != null && getParameters().size() > 0)
            return getParameters().get(0).evaluate(null).toString();
        else return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
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
