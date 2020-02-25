/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.flatgeobuf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wololo.flatgeobuf.geotools.FeatureCollectionConversions;

/**
 * A GetFeatureInfo response handler specialized in producing FlatGeobuf data for a GetFeatureInfo
 * request.
 *
 * @author Björn Harrtell
 */
public class FlatGeobufOutputFormat extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    public FlatGeobufOutputFormat(GeoServer gs) {
        super(gs, "application/flatgeobuf");
    }

    /** capabilities output format string. */
    public String getCapabilitiesElementName() {
        return "FlatGeobuf";
    }

    /** Returns the mime type */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/flatgeobuf";
    }

    /** Helper method that checks if the results feature collections contain complex features. */
    protected static boolean isComplexFeature(FeatureCollectionResponse results) {
        for (FeatureCollection featureCollection : results.getFeatures()) {
            if (!(featureCollection.getSchema() instanceof SimpleFeatureType)) {
                // this feature collection contains complex features
                return true;
            }
        }
        // all features collections contain only simple features
        return false;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation operation)
            throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) LOGGER.info("about to encode FlatGeobuf");

        if (isComplexFeature(featureCollection))
            throw new RuntimeException("FlatGeobuf does not support complex features");

        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) featureCollection.getFeature().get(0);
        FeatureCollectionConversions.serialize(fc, 0, output);
    }

    /** Is WFS configured to return feature and collection bounds? */
    protected boolean isFeatureBounding() {
        WFSInfo wfs = getInfo();
        return wfs.isFeatureBounding();
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return "fgb";
    }
}
