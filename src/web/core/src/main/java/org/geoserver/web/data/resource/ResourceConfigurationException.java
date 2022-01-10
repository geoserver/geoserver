/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.data.resource;

import org.geoserver.platform.exception.GeoServerException;

/** Base class for exceptions used for validation errors in resource configuration */
public class ResourceConfigurationException extends GeoServerException {

    public static final String CQL_ATTRIBUTE_NAME_NOT_FOUND_$1 = "CQL_ATTRIBUTE_NAME_NOT_FOUND";

    public ResourceConfigurationException(String id, Object[] args) {
        super(id);
        setId(id);
        setArgs(args);
    }
}
