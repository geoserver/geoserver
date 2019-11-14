/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;

/**
 * Groups {@link StaticBuilder} and {@link DynamicValueBuilder}, invoke them and set, if found, the
 * context from them, according to $source value in template file.
 */
public class CompositeBuilder extends SourceBuilder {

    private List<JsonBuilder> children;

    public CompositeBuilder(String key) {
        super(key);
        this.children = new LinkedList<>();
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        context = evaluateSource(context);
        if (context.getCurrentObj() != null) {
            writeKey(writer);
            writer.writeStartObject();
            for (JsonBuilder jb : children) {
                jb.evaluate(writer, context);
            }
            writer.writeEndObject();
        }
    }

    @Override
    public void addChild(JsonBuilder children) {
        this.children.add(children);
    }

    @Override
    public List<JsonBuilder> getChildren() {
        return children;
    }
}
