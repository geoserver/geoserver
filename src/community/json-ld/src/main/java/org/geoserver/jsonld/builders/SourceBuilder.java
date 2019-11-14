package org.geoserver.jsonld.builders;

import java.util.LinkedList;
import java.util.List;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.filter.expression.Expression;

/** Abstract class for builders that can set the context for their children through source xpath */
public abstract class SourceBuilder extends AbstractJsonBuilder {

    private String source;

    protected List<JsonBuilder> children;

    public SourceBuilder(String key) {
        super(key);
        this.children = new LinkedList<JsonBuilder>();
    }

    public JsonBuilderContext evaluateSource(JsonBuilderContext context) {

        if (source != null && !source.equals(context.getCurrentSource())) {
            Object o = evaluateSource(context.getCurrentObj());
            JsonBuilderContext newContext = new JsonBuilderContext(o, source);
            newContext.setCurrentSource(source);
            newContext.setParent(context);
            return newContext;
        }
        return context;
    }

    public Object evaluateSource(Object o) {
        Expression expression = new AttributeExpressionImpl(source, RootBuilder.namespaces);
        return expression.evaluate(o);
    }

    @Override
    public void addChild(JsonBuilder builder) {
        this.children.add(builder);
    }

    @Override
    public List<JsonBuilder> getChildren() {
        return children;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
