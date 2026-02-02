/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/** This class is responsible for serializing date into ISO format and writing other fields to the JSON */
public class UnknownFieldsSerializer extends ValueSerializer<Object> {

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(Object value, JsonGenerator gen, SerializationContext serializers) {
        if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                String key = entry.getKey();
                Object valuePair = entry.getValue();
                gen.writeName(key);
                if (valuePair instanceof Date) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    gen.writeString(dateFormat.format(valuePair));
                } else {
                    gen.writePOJO(valuePair);
                }
            }
        }
    }
}
