/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.Arrays;
import java.util.List;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * This and wfs BBoxKvpParser share a lot, it's just they don't share the same output type. Find a
 * way to create one common superclass.
 *
 * @author Andrea Aime
 */
public class BoundingBoxKvpParser extends KvpParser {
    public BoundingBoxKvpParser() {
        super("BoundingBox", BoundingBoxType.class);
    }

    public Object parse(String value) throws Exception {
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() < 4) {
            throw new WcsException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates (should have at least 4): "
                            + unparsed.size(),
                    WcsExceptionCode.InvalidParameterValue,
                    "BoundingBox");
        }

        // if it does, store them in an array of doubles
        int size = unparsed.size();
        Double[] lower = new Double[(int) Math.floor(size / 2.0)];
        Double[] upper = new Double[lower.length];

        for (int i = 0; i < lower.length; i++) {
            try {
                lower[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate is not parsable:" + unparsed.get(i * 2),
                        WcsExceptionCode.InvalidParameterValue,
                        "BoundingBox");
            }

            try {
                upper[i] = Double.parseDouble((String) unparsed.get(lower.length + i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate is not parsable:" + unparsed.get(i * 2 + 1),
                        WcsExceptionCode.InvalidParameterValue,
                        "BoundingBox");
            }
        }

        // check for crs
        String crsName = null;
        CoordinateReferenceSystem crs = null;
        if (unparsed.size() % 2 == 1) {
            crsName = (String) unparsed.get(unparsed.size() - 1);
            try {
                if ("urn:ogc:def:crs:OGC:1.3:CRS84".equals(crsName)) {
                    crsName = "EPSG:4326";
                } else {
                    crs = CRS.decode(crsName);
                    if (crs.getCoordinateSystem().getDimension() != lower.length)
                        throw new WcsException(
                                "CRS specified has dimension "
                                        + crs.getCoordinateSystem().getDimension()
                                        + " but bbox specified has "
                                        + lower.length,
                                InvalidParameterValue,
                                "BoundingBox");
                }
            } catch (Exception e) {
                throw new WcsException(
                        "Could not recognize crs " + crsName, InvalidParameterValue, "BoundingBox");
            }
        }

        // we do not check that lower <= higher because in the case of geographic
        // bbox we have to accept the case where the lower coordinate is higher
        // than the high one and handle it as antimeridian crossing, better do that once
        // in the code that handles GetCoverage (the same check must be performed for xml requests)

        BoundingBoxType bbt = Ows11Factory.eINSTANCE.createBoundingBoxType();
        bbt.setCrs(crsName);
        bbt.setLowerCorner(Arrays.asList(lower));
        bbt.setUpperCorner(Arrays.asList(upper));
        return bbt;
    }
}
