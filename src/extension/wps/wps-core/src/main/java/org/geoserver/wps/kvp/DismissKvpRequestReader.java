/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wps.DismissType;

/**
 * KVP reader for the Dismiss request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DismissKvpRequestReader extends KvpRequestReader {

    public DismissKvpRequestReader() {
        super(DismissType.class);
    }
}
