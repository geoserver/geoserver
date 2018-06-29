/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import java.util.Date;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Estimates the size of a feature collection by guessing how bit an average feature will be by
 * checking its feature type
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureSizeEstimator implements ObjectSizeEstimator {

    @Override
    public long getSizeOf(Object object) {
        if (object instanceof SimpleFeature) {
            SimpleFeatureType ft = ((SimpleFeature) object).getFeatureType();
            return estimateSizeByFeatureType(ft);
        } else if (object instanceof SimpleFeatureCollection) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) object;
            int count = fc.size();
            if (count > 0) {
                SimpleFeatureType ft = fc.getSchema();
                return count + estimateSizeByFeatureType(ft);
            }
        }

        return UNKNOWN_SIZE;
    }

    private int estimateSizeByFeatureType(SimpleFeatureType ft) {
        int bytes = 0;
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            // all of these are object, account for the reference and the object header
            bytes += 4 + 12;
            Class<?> type = ad.getType().getBinding();
            if (Point.class.isAssignableFrom(type)) {
                bytes += 12 + 16 + 16; // assuming a packed coordinate sequence
            } else if (Geometry.class.isAssignableFrom(type)) {
                bytes += 12 + 16 + 16 * 64; // assuming 64 coordinates per geometry
            } else if (Number.class.isAssignableFrom(type)) {
                if (Double.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
                    bytes += 8;
                } else if (Float.class.isAssignableFrom(type)
                        || Integer.class.isAssignableFrom(type)) {
                    bytes += 4;
                } else if (Short.class.isAssignableFrom(type)) {
                    bytes += 2;
                } else if (Byte.class.isAssignableFrom(type)) {
                    bytes += 1;
                }
            } else if (Character.class.isAssignableFrom(type)
                    || Boolean.class.isAssignableFrom(type)) {
                bytes += 1;
            } else if (Date.class.isAssignableFrom(type)) {
                bytes += 8;
            } else {
                // blind assumption for anything else, similar to an array of 64 char entries
                bytes += 16 + 64;
            }
        }

        return bytes;
    }
}
