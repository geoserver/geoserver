/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geotools.GML;
import org.geotools.GML.Version;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class GMLFeatureCollectionConverter extends FeatureCollectionConverter {

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        SimpleFeatureCollection features = getFeatures(t);
        GML gml = new GML(Version.WFS1_0);
        gml.setNamespace("gf", features.getSchema().getName().getNamespaceURI());
        // gml.setFeatureBounding(false);
        gml.encode(outputMessage.getBody(), features);
    }
    
    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return (SimpleFeatureCollection.class.isAssignableFrom(clazz)
                | FormatCollectionWrapper.XMLCollectionWrapper.class.isAssignableFrom(clazz))
                && getSupportedMediaTypes().contains(mediaType);
    }

}
