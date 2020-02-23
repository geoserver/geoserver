/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders;

import java.io.IOException;
import java.util.List;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;

/** Basic interface for all the builders */
public interface JsonBuilder {

    /**
     * Writes a piece of json-ld output evaluating builder's corresponding portion of json-ld
     * template against current object passed inside ${@link JsonBuilderContext}
     */
    void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException;

    default void addChild(JsonBuilder children) {}

    default List<JsonBuilder> getChildren() {
        return null;
    }
}
