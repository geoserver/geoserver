/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;

import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Base class for converters handling (wrapped) feature collections
 */
public abstract class FeatureCollectionConverter extends BaseMessageConverter {

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    SimpleFeatureCollection getFeatures(Object o) {
        if(o instanceof FormatCollectionWrapper) {
            return ((FormatCollectionWrapper) o).getCollection();
        } else {
            return (SimpleFeatureCollection) o;
        }
    }
}
