/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import org.geoserver.opensearch.eo.store.CollectionLayer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * This one exists solely to have a converter that will care for the collection objects without
 * hitting GeoServer own xstream converter, which is generating an ugly output
 *
 * @author Andrea Aime - GeoSolutions
 */
@Component
public class OseoJSONConverter extends BaseMessageConverter<Object> {

    MappingJackson2HttpMessageConverter delegate = new MappingJackson2HttpMessageConverter();

    public OseoJSONConverter() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected boolean supports(Class clazz) {
        return (OgcLinks.class.isAssignableFrom(clazz)
                || (ProductReferences.class.isAssignableFrom(clazz))
                || (CollectionLayer.class.isAssignableFrom(clazz)));
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    @Override
    protected Object readInternal(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return delegate.read(clazz, inputMessage);
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        delegate.write(t, MediaType.APPLICATION_JSON, outputMessage);
    }
}
