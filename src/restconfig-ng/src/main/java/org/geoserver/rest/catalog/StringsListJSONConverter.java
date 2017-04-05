/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class StringsListJSONConverter extends BaseMessageConverter {

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return StringsList.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON, MediaType.valueOf(CatalogController.TEXT_JSON));
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        StringsList stringsList = (StringsList) t;
        Map<String, Object> values = Collections.singletonMap("list",
                Collections.singletonMap("string", stringsList.getValues()));
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
        JSONObject.fromObject(values).write(outWriter);
        outWriter.flush();
        outWriter.close();
    }
}
