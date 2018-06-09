/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wps.GetExecutionResultType;

/**
 * KVP reader for the ExecutionStatus request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetExecutionStatusKvpRequestReader extends KvpRequestReader {

    public GetExecutionStatusKvpRequestReader() {
        super(GetExecutionResultType.class);
    }
}
