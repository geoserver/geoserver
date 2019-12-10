/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.filter.expression.PropertyName;

/**
 * This builder handle the writing of a Json array by invoking its children builders and setting the
 * context according to the $source specified in the template file.
 */
public class IteratingBuilder extends SourceBuilder {

    private boolean isFeaturesField;

    public IteratingBuilder(String key) {
        super(key);
        this.isFeaturesField = key != null && key.equalsIgnoreCase("features");
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        if (context.getCurrentObj() != null) {
            if (!isFeaturesField) {
                writeKey(writer);
                writer.writeStartArray();
                evaluateCollection(writer, context);
                writer.writeEndArray();
            } else {
                evaluateInternal(writer, context);
            }
        }
    }

    public void evaluateCollection(JsonLdGenerator writer, JsonBuilderContext context)
            throws IOException {

        int i = 1;
        Object srcObject = iterateSource(i, context);
        if (srcObject != null) {
            while (srcObject != null) {
                JsonBuilderContext newContext = new JsonBuilderContext(srcObject);
                newContext.setParent(context);
                evaluateInternal(writer, newContext);
                i++;
                srcObject = iterateSource(i, context);
            }
        } else {
            context = evaluateSource(context);
            evaluateInternal(writer, context);
        }
    }

    private Object iterateSource(int i, JsonBuilderContext context) {
        String source = getStrSource() + "[" + i + "]";
        PropertyName pn = (PropertyName) getSource();
        AttributeExpressionImpl sourceXpath =
                new AttributeExpressionImpl(source, pn.getNamespaceContext());
        return sourceXpath.evaluate(context.getCurrentObj());
    }

    private void evaluateInternal(JsonLdGenerator writer, JsonBuilderContext context)
            throws IOException {
        for (JsonBuilder child : children) {
            child.evaluate(writer, context);
        }
    }
}
