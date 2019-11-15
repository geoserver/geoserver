package org.geoserver.jsonld.builders;

import java.util.LinkedList;
import java.util.List;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.xml.sax.helpers.NamespaceSupport;

/** Abstract class for builders that can set the context for their children through source xpath */
public abstract class SourceBuilder extends AbstractJsonBuilder {

    private AttributeExpressionImpl source;

    protected List<JsonBuilder> children;

    public SourceBuilder(String key) {
        super(key);
        this.children = new LinkedList<JsonBuilder>();
    }

    public JsonBuilderContext evaluateSource(JsonBuilderContext context) {

        if (source != null && !source.equals(context.getCurrentSource())) {
            Object o = evaluateSource(context.getCurrentObj());
            JsonBuilderContext newContext = new JsonBuilderContext(o, source.getPropertyName());
            newContext.setParent(context);
            return newContext;
        }
        return context;
    }

    public Object evaluateSource(Object o) {
        return source.evaluate(o);
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
        return source.getPropertyName();
    }

    public void setSource(String source, NamespaceSupport namespaces) {
        this.source = new AttributeExpressionImpl(source, namespaces);
    }
}
