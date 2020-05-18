/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Groups {@link StaticBuilder} and {@link DynamicValueBuilder}, invoke them and set, if found, the
 * context from them, according to $source value in template file.
 */
public class CompositeBuilder extends SourceBuilder {

    private List<JsonBuilder> children;

    public CompositeBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.children = new LinkedList<>();
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        context = evaluateSource(context);
        Object o = context.getCurrentObj();
        if (o != null && evaluateFilter(context) && canWrite(context)) {
            writeKey(writer);
            writer.writeStartObject();
            for (JsonBuilder jb : children) {
                jb.evaluate(writer, context);
            }
            writer.writeEndObject();
        }
    }

    public boolean canWrite(JsonBuilderContext context) {
        List<JsonBuilder> filtered =
                children.stream()
                        .filter(
                                b ->
                                        b instanceof DynamicValueBuilder
                                                || b instanceof CompositeBuilder)
                        .collect(Collectors.toList());
        if (filtered.size() == children.size()) {
            int falseCounter = 0;
            for (JsonBuilder b : filtered) {
                if (b instanceof CompositeBuilder) {
                    if (!((CompositeBuilder) b).canWrite(context)) falseCounter++;
                } else {
                    if (!((DynamicValueBuilder) b).checkNotNullValue(context)) falseCounter++;
                }
            }
            if (falseCounter == filtered.size()) return false;
        }
        return true;
    }

    @Override
    public void addChild(JsonBuilder children) {
        this.children.add(children);
    }

    @Override
    public List<JsonBuilder> getChildren() {
        return children;
    }

    @Override
    protected void writeKey(JsonLdGenerator writer) throws IOException {
        if (key != null && !key.equals("")) writer.writeFieldName(key);
    }
}
