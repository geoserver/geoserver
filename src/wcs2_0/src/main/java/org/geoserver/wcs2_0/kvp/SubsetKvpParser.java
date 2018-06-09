/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;

/**
 * Parses the WCS 2.0 subset key
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SubsetKvpParser extends KvpParser {

    public SubsetKvpParser() {
        super("subset", DimensionSubsetType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        // SubsetSpec: dimension [ , crs ] ( intervalOrPoint )
        // dimension: NCName
        // crs: anyURI
        // intervalOrPoint: interval | point
        // interval: low , high
        // low: point | *
        // high: point | *
        // point: number | " token " // " = ASCII 0x42

        // trim just in case
        value = value.trim();

        // first, locate the intervalOrPoint part
        int openIdx = value.indexOf("(");
        int closeIdx = value.indexOf(")");

        if (openIdx == -1 || closeIdx == -1 || closeIdx < value.length() - 1) {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) is expected",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }

        // parse the first part, dimension[,crs]
        String dimension = null;
        String crs = null;
        String dimensionCrs = value.substring(0, openIdx);
        String[] dcElements = dimensionCrs.split("\\s*,\\s*");
        if (dcElements.length == 1) {
            dimension = dcElements[0];
            crs = null;
        } else if (dcElements.length == 2) {
            dimension = dcElements[0];
            crs = dcElements[1];
        } else {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) is expected",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }

        // parse the second part, intervalOrPoint
        String valuePoint = value.substring(openIdx + 1, closeIdx);
        // split on all commas not contained in quotes
        String[] vpElements = valuePoint.split(",\\s*(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        if (vpElements.length == 1) {
            // point
            String point = parsePoint(vpElements[0], false);
            DimensionSliceType slice = Wcs20Factory.eINSTANCE.createDimensionSliceType();
            slice.setDimension(dimension);
            slice.setCRS(crs);
            slice.setSlicePoint(point);
            return slice;
        } else if (vpElements.length == 2) {
            String low = parsePoint(vpElements[0], true);
            String high = parsePoint(vpElements[1], true);

            DimensionTrimType trim = Wcs20Factory.eINSTANCE.createDimensionTrimType();
            trim.setDimension(dimension);
            trim.setCRS(crs);
            trim.setTrimLow(low);
            trim.setTrimHigh(high);
            return trim;
        } else {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) "
                            + "where interval or point has either 1 or two elements",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }
    }

    private String parsePoint(String point, boolean allowStar) {
        point = point.trim();
        if ("*".equals(point)) {
            if (allowStar) {
                // "no" limit
                return null;
            } else {
                throw new OWS20Exception(
                        "Invalid usage of *, it can be used only when specifying an interval",
                        WCS20ExceptionCode.InvalidEncodingSyntax,
                        "subset");
            }
        } else if (point.startsWith("\"") && point.endsWith("\"")) {
            point = point.substring(1, point.length() - 1);
        } else {
            try {
                // check it is a number
                Double.parseDouble(point);
            } catch (NumberFormatException e) {
                throw new OWS20Exception(
                        "Invalid point value "
                                + point
                                + ", it is not a number and it's not between double quotes",
                        WCS20ExceptionCode.InvalidEncodingSyntax,
                        "subset");
            }
        }

        return point;
    }
}
