/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import org.geoserver.ows.KvpParser;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Not really a parser, but a validity checker instead (ensures the specified type is among the
 * values foreseen by the standard and supported by GeoServer)
 *
 * @author Andrea Aime
 */
public class GridTypeKvpParser extends KvpParser {
    public GridTypeKvpParser() {
        super("GridType", String.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        GridType type = null;
        for (GridType currType : GridType.values()) {
            if (currType.getXmlConstant().equalsIgnoreCase(value)) {
                type = currType;
                break;
            }
        }

        if (type == null)
            throw new WcsException(
                    "Could not understand grid type '" + value + "'",
                    InvalidParameterValue,
                    "GridType");

        if (type == GridType.GT2dGridIn3dCrs)
            throw new WcsException(
                    "GeoServer does not support type " + type.name(),
                    InvalidParameterValue,
                    "GridType");

        return type.getXmlConstant();
    }
}
