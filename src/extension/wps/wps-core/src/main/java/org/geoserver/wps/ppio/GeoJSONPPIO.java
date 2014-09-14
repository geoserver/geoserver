/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;

/**
 * Inputs and outputs feature collections in GeoJSON format using gt-geojson
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeoJSONPPIO extends CDataPPIO {

    protected GeoJSONPPIO() {
        super(FeatureCollection.class, FeatureCollection.class, "application/json");
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        FeatureJSON json = new FeatureJSON();
        // commented out due to GEOT-3209
        // json.setEncodeFeatureCRS(true);
        // json.setEncodeFeatureCollectionCRS(true);
        json.writeFeatureCollection((FeatureCollection) value, os);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return new FeatureJSON().readFeatureCollection(input);
    }

    @Override
    public Object decode(String input) throws Exception {
        return new FeatureJSON().readFeatureCollection(input);
    }
    
    @Override
    public String getFileExtension() {
        return "json";
    }

}
