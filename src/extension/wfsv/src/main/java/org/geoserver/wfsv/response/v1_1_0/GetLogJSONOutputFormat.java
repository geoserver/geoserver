/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.wfsv.response.v1_1_0;

import net.opengis.wfs.ResultTypeType;
import net.opengis.wfsv.GetLogType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.response.GeoJSONOutputFormat;

public class GetLogJSONOutputFormat extends GeoJSONOutputFormat{
	
	public GetLogJSONOutputFormat(GeoServer gs){
	    super(gs);
	}

	public boolean canHandle(Operation operation) {
	       //GetFeature operation?
        if ("GetLog".equalsIgnoreCase(operation.getId())) {
            //also check that the resultType is "results"
            GetLogType request = (GetLogType) OwsUtils.parameter(operation.getParameters(),
                    GetLogType.class);

            return request.getResultType() == ResultTypeType.RESULTS_LITERAL;
        }

        return false;
	}
	
}
