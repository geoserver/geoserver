/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.AbstractJsonBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/** This class provides functionality to write content from Json-ld template file as it is */
public class StaticBuilder extends AbstractJsonBuilder {

    private JsonNode staticValue;
    private String strValue;

    public StaticBuilder(String key, JsonNode value, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.staticValue = value;
    }

    public StaticBuilder(String key, String strValue, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.strValue = strValue;
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        if (evaluateFilter(context)) {
            if (strValue != null) {
                writer.writeString(key, strValue);
            } else {
                if (staticValue.isObject()) {
                    writer.writeObjectNode(key, staticValue);
                } else if (staticValue.isArray()) {
                    writer.writeArrayNode(key, staticValue);
                } else {
                    writer.writeValueNode(key, staticValue);
                }
            }
        }
    }

    public JsonNode getStaticValue() {
        return staticValue;
    }
}
