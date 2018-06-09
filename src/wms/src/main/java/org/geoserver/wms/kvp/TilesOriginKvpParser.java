/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.awt.geom.Point2D;
import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;

public class TilesOriginKvpParser extends KvpParser {
    public TilesOriginKvpParser() {
        super("tilesorigin", Point2D.Double.class);
    }

    public Object parse(String value) throws Exception {
        List coordValues = KvpUtils.readFlat(value);

        if (coordValues.size() != 2) {
            throw new ServiceException(value + " is not a valid coordinate", getClass().getName());
        }

        try {
            double minx = Double.parseDouble(coordValues.get(0).toString());
            double miny = Double.parseDouble(coordValues.get(1).toString());

            return new Point2D.Double(minx, miny);
        } catch (NumberFormatException ex) {
            throw new ServiceException(
                    ex,
                    "Illegal value for TILESORIGIN parameter: " + value,
                    getClass().getName() + "::parseTilesOrigin()");
        }
    }
}
