/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * Parses the grid offsets into a double[]
 *
 * @author Andrea Aime
 */
public class GridOffsetsKvpParser extends KvpParser {

    public GridOffsetsKvpParser() {
        super("GridOffsets", double[].class);
    }

    @Override
    public Object parse(String value) throws Exception {
        List values = KvpUtils.readFlat(value);

        if (values.size() < 2)
            throw new WcsException(
                    "Invalid grid offset, should have at least two values",
                    WcsExceptionCode.InvalidParameterValue,
                    "GridOffsets");

        Double[] offsets = new Double[values.size()];
        for (int i = 0; i < offsets.length; i++) {
            try {
                offsets[i] = Double.valueOf((String) values.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Invalid offset " + offsets[i],
                        WcsExceptionCode.InvalidParameterValue,
                        "GridOffsets");
            }
        }

        return offsets;
    }
}
