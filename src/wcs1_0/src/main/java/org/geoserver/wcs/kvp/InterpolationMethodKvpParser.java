/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import net.opengis.wcs10.InterpolationMethodType;
import org.geoserver.ows.KvpParser;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parses the "sections" GetCapabilities kvp argument
 *
 * @author Andrea Aime - TOPP
 * @author Alessio Fabiani, GeoSolutions
 */
public class InterpolationMethodKvpParser extends KvpParser {

    public InterpolationMethodKvpParser() {
        super("interpolation", InterpolationMethodType.class);
    }

    @Override
    public Object parse(String interpolation) throws Exception {

        if (interpolation.startsWith("nearest")) {
            interpolation = "nearest neighbor";
        }
        if (InterpolationMethodType.get(interpolation) == null)
            throw new WcsException(
                    "Could not find interpolationMethod '" + interpolation + "'",
                    InvalidParameterValue,
                    "interpolationMethod");

        return InterpolationMethodType.get(interpolation);
    }
}
