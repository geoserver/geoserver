/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import org.geoserver.ows.KvpParser;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class GridCSKvpParser extends KvpParser {

    public GridCSKvpParser() {
        super("GridCS", String.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        if (!GridCS.GCSGrid2dSquare.getXmlConstant().equalsIgnoreCase(value))
            throw new WcsException(
                    "Unrecognized GridCS " + value,
                    WcsExceptionCode.InvalidParameterValue,
                    "GridCS");

        return GridCS.GCSGrid2dSquare.getXmlConstant();
    }
}
