/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;

/**
 * This builder handle the writing of a Json array by invoking its children builders and setting the
 * context according to the $source specified in the template file.
 */
public class IteratingBuilder extends SourceBuilder {

    public IteratingBuilder(String key) {
        super(key);
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        context = evaluateSource(context);
        if (context.getCurrentObj() != null) {
            writeKey(writer);
            if (!isFeaturesField) writer.writeStartArray();
            for (JsonBuilder child : children) {
                child.evaluate(writer, context);
            }
            if (!isFeaturesField) writer.writeEndArray();
        }
    }
}
