/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Convert Map to/from JSON.
 *
 * @author jody
 */
@Component
public class MapJSONConverter extends BaseMessageConverter<Map<?, ?>> {

    public MapJSONConverter() {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    public Map<?, ?> readInternal(Class<? extends Map<?, ?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // TODO: character set

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line);
        }
        return JSONObject.fromObject(text.toString());
    }

    @Override
    public void writeInternal(Map<?, ?> map, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        // TODO: character set
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));

        // JD: why does this initial flush occur?
        outWriter.flush();

        JSON obj = (JSON) toJSONObject(map);

        obj.write(outWriter);
        outWriter.flush();
    }

    /**
     * Convert to JSON representation.
     *
     * @return json representation
     */
    public Object toJSONObject(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) obj;
            JSONObject json = new JSONObject();

            for (Entry<?, ?> entry : m.entrySet()) {
                String key = (String) entry.getKey();
                Object value = toJSONObject(entry.getValue());
                json.put(key, value);
            }
            return json;
        } else if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            JSONArray json = new JSONArray();
            for (Object object : collection) {
                Object value = toJSONObject(object);
                json.add(toJSONObject(value));
            }
            return json;
        } else if (obj instanceof Number) {
            return obj;
        } else if (obj instanceof Boolean) {
            return obj;
        } else if (obj == null) {
            return JSONNull.getInstance();
        } else {
            return obj.toString();
        }
    }
}
