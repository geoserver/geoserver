/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.kvp;

import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wps.GetExecutionsType;

/**
 * KVP reader for the GetExecutions request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetExecutionsKvpRequestReader extends KvpRequestReader {

    public GetExecutionsKvpRequestReader() {
        super(GetExecutionsType.class);
    }
}
