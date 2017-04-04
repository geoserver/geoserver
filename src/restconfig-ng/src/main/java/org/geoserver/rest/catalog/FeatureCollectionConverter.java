/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;

import org.geoserver.catalog.rest.FormatCollectionWrapper.JSONCollectionWrapper;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Base class for converters handling (wrapped) feature collections
 */
public abstract class FeatureCollectionConverter extends BaseMessageConverter<SimpleFeatureCollection> {

    public FeatureCollectionConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }
    
    @Override
    protected boolean supports(Class<?> clazz) {
        return SimpleFeatureCollection.class.isAssignableFrom(clazz)
                || JSONCollectionWrapper.class.isAssignableFrom(clazz);
    }
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }
    
    @Override
    protected SimpleFeatureCollection readInternal(Class<? extends SimpleFeatureCollection> clazz,
            HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                getClass().getName() + " does not support deserialization");
    }
    
    /**
     * Access features, unwrapping if necessary.
     * @param o
     * @return features
     */
    SimpleFeatureCollection getFeatures(Object content) {
        if(content instanceof FormatCollectionWrapper) {
            return ((FormatCollectionWrapper) content).getCollection();
        } else {
            return (SimpleFeatureCollection) content;
        }
    }
}
