/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import org.geoserver.ows.KvpParser;
import org.geotools.referencing.CRS;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Does not really parse the srs, it just makes sure it's a valid one
 *
 * @author Andrea Aime
 */
public class GridBaseCRSKvpParser extends KvpParser {

    public GridBaseCRSKvpParser() {
        super("GridBaseCRS", String.class);
    }

    @Override
    public Object parse(String epsgCode) throws Exception {
        if (epsgCode != null) {
            try {
                CRS.decode(epsgCode);
            } catch (Exception e) {
                throw new WcsException(
                        "Invalid SRS code " + epsgCode, InvalidParameterValue, "GridBaseCRS");
            }
        }

        return epsgCode;
    }
}
