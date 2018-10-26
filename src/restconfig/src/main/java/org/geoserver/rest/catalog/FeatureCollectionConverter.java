/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import org.geoserver.rest.catalog.FormatCollectionWrapper.JSONCollectionWrapper;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.wfs.GML;
import org.geotools.wfs.GML.Version;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Base class for converters handling (wrapped) feature collections */
public abstract class FeatureCollectionConverter<T> extends BaseMessageConverter<T> {

    public FeatureCollectionConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return SimpleFeatureCollection.class.isAssignableFrom(clazz)
                || JSONCollectionWrapper.class.isAssignableFrom(clazz);
    }
    //
    // reading
    //
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }
    //
    // writing
    //
    protected void writeGeoJsonl(SimpleFeatureCollection features, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        final FeatureJSON json = new FeatureJSON();
        boolean geometryless = features.getSchema().getGeometryDescriptor() == null;
        json.setEncodeFeatureCollectionBounds(!geometryless);
        json.setEncodeFeatureCollectionCRS(!geometryless);
        json.writeFeatureCollection(features, outputMessage.getBody());
    }

    protected void writeGML(SimpleFeatureCollection features, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        GML gml = new GML(Version.WFS1_0);
        gml.setNamespace("gf", features.getSchema().getName().getNamespaceURI());
        // gml.setFeatureBounding(false);
        gml.encode(outputMessage.getBody(), features);
    }
}
