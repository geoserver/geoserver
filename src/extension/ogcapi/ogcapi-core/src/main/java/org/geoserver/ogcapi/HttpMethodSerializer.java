/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.springframework.http.HttpMethod;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/** Serializer for HttpMethod enum-like class */
public class HttpMethodSerializer extends ValueSerializer<HttpMethod> {
    @Override
    public void serialize(HttpMethod value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeString(value.name());
    }
}
