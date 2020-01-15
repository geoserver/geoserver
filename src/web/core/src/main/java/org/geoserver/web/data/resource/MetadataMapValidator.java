/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.geoserver.catalog.MetadataMap;

/**
 * Allows object to validate a MetadataMap catalog instance.
 *
 * <p>Fernando Mino - Geosolutions
 */
public interface MetadataMapValidator {

    /** Validates a Metadata map, should throw an Exception in case of invalid state. */
    void validate(MetadataMap map);
}
