/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.IOUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class OseoGeoJSONFeatureConverter extends BaseMessageConverter<Object> {

    static final Logger LOGGER = Logging.getLogger(OseoGeoJSONFeatureConverter.class);

    public OseoGeoJSONFeatureConverter() {
        super(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return SimpleFeature.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        SimpleFeature f = (SimpleFeature) t;
        try (GeoJSONWriter writer = new GeoJSONWriter(outputMessage.getBody())) {
            writer.setPrettyPrinting(true);
            writer.setSingleFeature(true);
            writer.write(f);
        }
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try {
            return GeoJSONReader.parseFeature(
                    new String(
                            IOUtils.toByteArray(inputMessage.getBody()), StandardCharsets.UTF_8));
            // parser throws RuntimeException, however this changes the Spring response from
            // 400 (user's fault) to 500 (internal server error). Catching and re-wrapping instead.
        } catch (RuntimeException e) {
            // spring does not seem too keen on logging these exceptions, doing it here
            LOGGER.log(Level.FINE, "Failed to parse GeoJSON", e);
            throw new HttpMessageNotReadableException("Failed to read GeoJSON", e, inputMessage);
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
