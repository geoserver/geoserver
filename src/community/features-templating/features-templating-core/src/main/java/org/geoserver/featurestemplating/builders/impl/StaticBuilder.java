/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** This class provides functionality to write content from Json-ld template file as it is */
public class StaticBuilder extends AbstractTemplateBuilder {

    protected JsonNode staticValue;
    protected String strValue;

    public StaticBuilder(String key, JsonNode value, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.staticValue = value;
    }

    public StaticBuilder(String key, String strValue, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.strValue = strValue;
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (evaluateFilter(context)) {
            evaluateInternal(writer, context);
        }
    }

    protected void evaluateInternal(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        addChildrenEvaluationToEncodingHints(writer, context);
        String key = getKey(context);
        if (strValue != null) writer.writeStaticContent(key, strValue, getEncodingHints());
        else writer.writeStaticContent(key, staticValue, getEncodingHints());
    }

    /**
     * Get the static value as a JsonNode
     *
     * @return the static value
     */
    public JsonNode getStaticValue() {
        return staticValue;
    }

    public String getStrValue() {
        return strValue;
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return visitor.visit(this, value);
    }
}
