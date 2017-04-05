/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class GeoJSONFeatureCollectionConverter extends FeatureCollectionConverter {

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON,
                MediaType.valueOf(CatalogController.TEXT_JSON));
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        SimpleFeatureCollection features = getFeatures(t);
        final FeatureJSON json = new FeatureJSON();
        boolean geometryless = features.getSchema().getGeometryDescriptor() == null;
        json.setEncodeFeatureCollectionBounds(!geometryless);
        json.setEncodeFeatureCollectionCRS(!geometryless);
        json.writeFeatureCollection(features, outputMessage.getBody());
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return (SimpleFeatureCollection.class.isAssignableFrom(clazz) && getSupportedMediaTypes().contains(mediaType))
                || FormatCollectionWrapper.JSONCollectionWrapper.class.isAssignableFrom(clazz);
    }

}
