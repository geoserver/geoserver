/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.List;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.opengis.geometry.DirectPosition;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * Parses the grid origin into a double[]
 * TODO: consider use a {@link DirectPosition} instead?
 * @author Andrea Aime
 */
public class GridOriginKvpParser extends KvpParser {

    public GridOriginKvpParser() {
        super("GridOrigin", double[].class);
    }

    @Override
    public Object parse(String value) throws Exception {
        List values = KvpUtils.readFlat(value);

        if (values.size() < 2)
            throw new WcsException("Invalid grid origin, should have at least two values",
                    WcsExceptionCode.InvalidParameterValue, "GridOrigin");

        Double[] origins = new Double[values.size()];
        for (int i = 0; i < origins.length; i++) {
            try {
                origins[i] = Double.parseDouble((String) values.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException("Invalid ordinate " + origins[i],
                        WcsExceptionCode.InvalidParameterValue, "GridOrigin");
            }
        }
        
        return origins;
    }
}
