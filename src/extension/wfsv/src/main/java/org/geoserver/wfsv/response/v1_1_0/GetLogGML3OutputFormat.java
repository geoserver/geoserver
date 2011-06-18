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
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;


/**
 * Variation on the GML3 output format that allows for handling outputs
 * coming from
 *
 */
public class GetLogGML3OutputFormat extends GML3OutputFormat {
    public GetLogGML3OutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        super(geoServer, configuration);
    }

    /**
     * Ensures that the operation being executed is a GetFeature operation.
     * <p>
     * Subclasses may implement
     * </p>
     */
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
