/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.io.Writer;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

@Component
public class OseoJSONConverter extends BaseMessageConverter<Object> {

    private XStream xs;

    public OseoJSONConverter() {
        super(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8);

        xs = new SecureXStream(new JsonHierarchicalStreamDriver() {
            public HierarchicalStreamWriter createWriter(Writer writer) {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE | JsonWriter.STRICT_MODE);
            }
        });

    }

    @Override
    protected boolean supports(Class clazz) {
        return CollectionReferences.class.isAssignableFrom(clazz)
                || ProductReferences.class.isAssignableFrom(clazz)
                || OgcLinks.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        xs.toXML(t, outputMessage.getBody());
    }

}
