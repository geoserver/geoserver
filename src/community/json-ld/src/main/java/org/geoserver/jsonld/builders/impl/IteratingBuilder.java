/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import java.util.List;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This builder handle the writing of a Json array by invoking its children builders and setting the
 * context according to the $source specified in the template file.
 */
public class IteratingBuilder extends SourceBuilder {

    private boolean isFeaturesField;

    public IteratingBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.isFeaturesField = key != null && key.equalsIgnoreCase("features");
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        if (!isFeaturesField) {
            context = evaluateSource(context);
            if (context.getCurrentObj() != null) {
                writeKey(writer);
                writer.writeStartArray();
                if (context.getCurrentObj() instanceof List) evaluateCollection(writer, context);
                else evaluateInternal(writer, context);
                writer.writeEndArray();
            }
        } else {
            evaluateInternal(writer, context);
        }
    }

    public void evaluateCollection(JsonLdGenerator writer, JsonBuilderContext context)
            throws IOException {

        List elements = (List) context.getCurrentObj();
        for (Object o : elements) {
            JsonBuilderContext childContext = new JsonBuilderContext(o);
            childContext.setParent(context.getParent());
            evaluateInternal(writer, childContext);
        }
    }

    private void evaluateInternal(JsonLdGenerator writer, JsonBuilderContext context)
            throws IOException {
        if (evaluateFilter(context)) {
            for (JsonBuilder child : children) {
                child.evaluate(writer, context);
            }
        }
    }
}
