/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.HttpHeaders;
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
        String out = GeoJSONWriter.toGeoJSON(f);
        PrintWriter pw = new PrintWriter(outputMessage.getBody());
        pw.write(out);
        pw.close();
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        InputStream body = inputMessage.getBody();
        StringWriter writer = new StringWriter();
        HttpHeaders headers = inputMessage.getHeaders();
        String contentEncoding = headers.getFirst("content-encoding");
        IOUtils.copy(body, writer,contentEncoding);
        String theString = writer.toString();
        return GeoJSONReader.parseFeature(theString);
    }

    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
