/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.restlet.data.MediaType;

/**
 * Writes out the feature collection as a GeoJSON document
 * TODO: make sure we respect the timestamp encoding
 * TODO: either optimize FeatureJSON or rewrite this not to use it, the top output speed is disappointing,
 * just 17MBs off the FOSS4G 2010 building layer when WFS GML2 can go up to 37MBs
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class FeaturesJSONFormat extends StreamDataFormat {
    protected FeaturesJSONFormat() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException("Can't read capabilities documents with this class");
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        SimpleFeatureCollection features = (SimpleFeatureCollection) object;
        final FeatureJSON json = new FeatureJSON();
        boolean geometryless = features.getSchema().getGeometryDescriptor() == null;
        json.setEncodeFeatureCollectionBounds(!geometryless);
        json.setEncodeFeatureCollectionCRS(!geometryless);
        json.writeFeatureCollection(features, out);
    }

}
