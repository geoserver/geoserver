/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import org.geoserver.opensearch.eo.OSEODescriptionRequest;
import org.geoserver.ows.KvpRequestReader;

/**
 * Reads a "description" request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescriptionRequestKvpReader extends KvpRequestReader {

    public DescriptionRequestKvpReader() {
        super(OSEODescriptionRequest.class);
    }
}
