/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class OseoGeoJSONFeatureConverter extends BaseMessageConverter<Object> {

    public OseoGeoJSONFeatureConverter() {
        super(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8);
    }

    @Override
    protected boolean supports(Class clazz) {
        return SimpleFeature.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        SimpleFeature f = (SimpleFeature) t;
        FeatureJSON json = new FeatureJSON();
        json.writeFeature(f, outputMessage.getBody());
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return new FeatureJSON().readFeature(inputMessage.getBody());
    }

    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
