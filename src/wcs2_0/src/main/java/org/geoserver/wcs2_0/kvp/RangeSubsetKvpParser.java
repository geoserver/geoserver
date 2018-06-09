/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.RangeIntervalType;
import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * KVP parser for the WCS 2.0 {@link RangeSubsetType}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RangeSubsetKvpParser extends KvpParser {

    public RangeSubsetKvpParser() {
        super("rangesubset", RangeSubsetType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        RangeSubsetType result = Wcs20Factory.eINSTANCE.createRangeSubsetType();

        // remove space
        value = value.trim();

        // minimal validation
        if (value.matches(".*,\\s*,.*")) {
            // two consequent commas
            throwInvalidSyntaxException();
        } else if (value.startsWith(",") || value.endsWith(",")) {
            throwInvalidSyntaxException();
        }

        String[] components = value.split("\\s*,\\s*");
        for (String component : components) {
            if (component.contains(":")) {
                String[] lowHigh = component.split(":");
                if (lowHigh.length != 2) {
                    throwInvalidSyntaxException();
                }
                RangeIntervalType ri = Wcs20Factory.eINSTANCE.createRangeIntervalType();
                ri.setStartComponent(lowHigh[0]);
                ri.setEndComponent(lowHigh[1]);
                RangeItemType item = Wcs20Factory.eINSTANCE.createRangeItemType();
                item.setRangeInterval(ri);

                result.getRangeItems().add(item);
            } else {
                RangeItemType item = Wcs20Factory.eINSTANCE.createRangeItemType();
                item.setRangeComponent(component);

                result.getRangeItems().add(item);
            }
        }

        return result;
    }

    protected void throwInvalidSyntaxException() {
        throw new WCS20Exception(
                "Invalid RangeSubset syntax, expecting a list of band names or band ranges (b1:b2)",
                WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                "rangeSubset");
    }
}
