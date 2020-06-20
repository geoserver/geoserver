/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders;

import java.util.LinkedList;
import java.util.List;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/** Abstract class for builders that can set the context for their children through source xpath */
public abstract class SourceBuilder extends AbstractJsonBuilder {

    private AttributeExpressionImpl source;

    protected List<JsonBuilder> children;

    public SourceBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.children = new LinkedList<JsonBuilder>();
    }

    public JsonBuilderContext evaluateSource(JsonBuilderContext context) {

        if (source != null && !source.getPropertyName().equals(context.getCurrentSource())) {
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

    public Expression getSource() {
        return source;
    }

    public String getStrSource() {
        return source != null ? source.getPropertyName() : null;
    }

    public void setSource(String source) {
        this.source = new AttributeExpressionImpl(source, namespaces);
    }
}
