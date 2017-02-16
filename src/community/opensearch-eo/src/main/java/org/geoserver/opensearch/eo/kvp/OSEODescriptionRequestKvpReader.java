/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import org.geoserver.opensearch.eo.OSEODescriptionRequest;
import org.geoserver.ows.KvpRequestReader;

public class OSEODescriptionRequestKvpReader extends KvpRequestReader {

    public OSEODescriptionRequestKvpReader() {
        super(OSEODescriptionRequest.class);
    }

}
