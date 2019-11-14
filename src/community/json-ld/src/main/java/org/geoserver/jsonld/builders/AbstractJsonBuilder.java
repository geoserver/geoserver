/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders;

import java.io.IOException;
import org.geoserver.jsonld.JsonLdGenerator;

/** Abstract implementation of {@link JsonBuilder} who groups some common attributes and methods. */
public abstract class AbstractJsonBuilder implements JsonBuilder {

    protected String key;

    protected boolean isFeaturesField;

    public AbstractJsonBuilder(String key) {
        this.isFeaturesField = key != null && key.equalsIgnoreCase("features");
        this.key = key;
    }

    public AbstractJsonBuilder() {}

    protected void writeKey(JsonLdGenerator writer) throws IOException {
        if (key != null && !key.equals("") && !isFeaturesField) writer.writeFieldName(key);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
