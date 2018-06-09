/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import org.geoserver.wps.process.ByteArrayRawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.process.StringRawData;

/**
 * Estimates non stream oriented raw data implementations
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RawDataSizeEstimator implements ObjectSizeEstimator {

    @Override
    public long getSizeOf(Object object) {
        // we can estimate selected types of raw
        if (object instanceof ByteArrayRawData) {
            ByteArrayRawData raw = (ByteArrayRawData) object;
            return raw.getData().length;
        } else if (object instanceof StringRawData) {
            StringRawData raw = (StringRawData) object;
            return raw.getData().length() * 2;
        } else if (object instanceof ResourceRawData) {
            ResourceRawData raw = (ResourceRawData) object;
            return raw.getResource().file().length();
        }

        return ObjectSizeEstimator.UNKNOWN_SIZE;
    }
}
