/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.springframework.http.HttpMethod;

/** Serializer for HttpMethod enum-like class */
public class HttpMethodSerializer extends JsonSerializer<HttpMethod> {
    @Override
    public void serialize(HttpMethod value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.name());
    }
}
