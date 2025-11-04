/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;

/** This class is responsible for serializing date into ISO format and writing other fields to the JSON */
@Component
public class UnknownFieldsSerializer extends JsonSerializer {

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                String key = entry.getKey();
                Object valuePair = entry.getValue();
                if (valuePair instanceof Date) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    gen.writeObjectField(key, dateFormat.format(valuePair));
                } else {
                    gen.writeObjectField(key, valuePair);
                }
            }
        }
    }
}
