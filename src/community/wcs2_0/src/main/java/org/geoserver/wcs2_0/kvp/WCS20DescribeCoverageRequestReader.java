/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.Map;
import net.opengis.wcs20.DescribeCoverageType;

import net.opengis.wcs20.Wcs20Factory;

import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.platform.OWS20Exception;

/**
 * Parses a DescribeCoverage request for WCS into the correspondent model object
 * 
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * 
 */
public class WCS20DescribeCoverageRequestReader extends EMFKvpRequestReader {

    public WCS20DescribeCoverageRequestReader() {
        super(DescribeCoverageType.class, Wcs20Factory.eINSTANCE);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        DescribeCoverageType describeCoverage = (DescribeCoverageType) request;

        if(describeCoverage.getCoverageId() == null) {
            throw new OWS20Exception("Required parameter coverageId missing", OWS20Exception.OWSExceptionCode.MissingParameterValue, "coverageId");
        }

        return request;
    }

}
