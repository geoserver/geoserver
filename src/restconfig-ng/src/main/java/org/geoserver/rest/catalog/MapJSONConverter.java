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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

@Component
public class MapJSONConverter extends BaseMessageConverter {

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return Map.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return Map.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON,
                MediaType.valueOf(CatalogController.TEXT_JSON));
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // TODO: character set
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));
        StringBuilder text = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            text.append(line);
        }
        return JSONObject.fromObject(text.toString());
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        // TODO: character set
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));

        // JD: why does this initial flush occur?
        outWriter.flush();

        JSON obj = (JSON) toJSONObject(t);

        obj.write(outWriter);
        outWriter.flush();
    }

    public Object toJSONObject(Object obj) {
        if (obj instanceof Map) {
            Map m = (Map) obj;
            JSONObject json = new JSONObject();
            Iterator it = m.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                json.put((String) entry.getKey(), toJSONObject(entry.getValue()));
            }

            return json;
        } else if (obj instanceof Collection) {
            Collection col = (Collection) obj;
            JSONArray json = new JSONArray();
            Iterator it = col.iterator();

            while (it.hasNext()) {
                json.add(toJSONObject(it.next()));
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
