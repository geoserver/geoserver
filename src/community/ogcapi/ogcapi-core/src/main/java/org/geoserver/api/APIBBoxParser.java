/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** A BBOX parser specialized for API bounding boxes (up to 6 coordinates, default CRS is CRS84) */
public class APIBBoxParser {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /**
     * Turns a bbox specification into a OGC filter, assuming it's expressed in {@link
     * DefaultGeographicCRS#WGS84}. Will return {@link Filter#INCLUDE} if the bbox * spec itself is
     * null or empty.
     */
    public static Filter toFilter(String bbox) throws FactoryException {
        return toFilter(bbox, null);
    }

    /**
     * Turns a bbox specification into a OGC filter, using the given CRS, or {@link
     * DefaultGeographicCRS#WGS84} if null is passed. Will return {@link Filter#INCLUDE} if the bbox
     * spec itself is null or empty.
     */
    public static Filter toFilter(String bbox, CoordinateReferenceSystem crs)
            throws FactoryException {
        if (bbox == null || bbox.trim().isEmpty()) {
            return Filter.INCLUDE;
        }

        // to envelopes first, build filter around them then
        ReferencedEnvelope[] parsed = parse(bbox, crs);
        if (parsed.length == 1) {
            return FF.bbox(FF.property(""), parsed[0]);
        } else if (parsed instanceof ReferencedEnvelope[]) {
            List<Filter> filters =
                    Stream.of((ReferencedEnvelope[]) parsed)
                            .map(e -> FF.bbox(FF.property(""), e))
                            .collect(Collectors.toList());
            return FF.or(filters);
        } else {
            throw new IllegalArgumentException("Could not understand parsed bbox " + parsed);
        }
    }

    /** Parses a BBOX assuming the default {@link DefaultGeographicCRS#WGS84} as the CRS */
    public static ReferencedEnvelope[] parse() throws FactoryException {
        return parse();
    }

    /** Parses a BBOX assuming the default {@link DefaultGeographicCRS#WGS84} as the CRS */
    public static ReferencedEnvelope[] parse(String value) throws FactoryException {
        return parse(value, null);
    }

    /** Parses a BBOX with the given CRS, if null {@link DefaultGeographicCRS#WGS84} will be used */
    public static ReferencedEnvelope[] parse(String value, CoordinateReferenceSystem crs)
            throws FactoryException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() < 4) {
            throw new IllegalArgumentException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates (should have "
                            + "4): "
                            + unparsed.size());
        }

        int countco = 4;
        if (unparsed.size() == 6 || unparsed.size() == 7) { // 3d-coordinates
            countco = 6;
        }

        // if it does, store them in an array of doubles
        double[] bbox = new double[countco];

        for (int i = 0; i < countco; i++) {
            try {
                bbox[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Bounding box coordinate " + i + " is not parsable:" + unparsed.get(i));
            }
        }

        // ensure the values are sane
        double minx = bbox[0];
        double miny = bbox[1];
        double minz = 0, maxx = 0, maxy = 0, maxz = 0;
        if (countco == 6) {
            minz = bbox[2];
            maxx = bbox[3];
            maxy = bbox[4];
            maxz = bbox[5];
        } else {
            maxx = bbox[2];
            maxy = bbox[3];
        }

        return buildEnvelope(countco, minx, miny, minz, maxx, maxy, maxz, crs);
    }

    private static ReferencedEnvelope[] buildEnvelope(
            int countco,
            double minx,
            double miny,
            double minz,
            double maxx,
            double maxy,
            double maxz,
            CoordinateReferenceSystem crs)
            throws NoSuchAuthorityCodeException, FactoryException {
        if (crs == null) {
            if (countco == 4) {
                crs = DefaultGeographicCRS.WGS84;
            } else {
                crs = DefaultGeographicCRS.WGS84_3D;
            }
        }

        if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)
                || CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84_3D)) {
            minx = rollLongitude(minx);
            maxx = rollLongitude(maxx);

            if (minx > maxx) {
                // dateline crossing case
                return new ReferencedEnvelope[] {
                    buildSingleEnvelope(countco, minx, miny, minz, 180, maxy, maxz, crs),
                    buildSingleEnvelope(countco, -180, miny, minz, maxx, maxy, maxz, crs),
                };
            }
        }
        // fallback, single envelope
        return new ReferencedEnvelope[] {
            buildSingleEnvelope(countco, minx, miny, minz, maxx, maxy, maxz, crs)
        };
    }

    private static ReferencedEnvelope buildSingleEnvelope(
            int countco,
            double minx,
            double miny,
            double minz,
            double maxx,
            double maxy,
            double maxz,
            CoordinateReferenceSystem crs) {
        if (minx > maxx) {
            throw new ServiceException(
                    "illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx);
        }

        if (miny > maxy) {
            throw new ServiceException(
                    "illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy);
        }

        if (minz > maxz) {
            throw new ServiceException(
                    "illegal bbox, minZ: " + minz + " is " + "greater than maxZ: " + maxz);
        }

        if (countco == 6) {
            return new ReferencedEnvelope3D(minx, maxx, miny, maxy, minz, maxz, crs);
        } else {
            if (crs == null || crs.getCoordinateSystem().getDimension() == 2) {
                return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
            } else if (crs.getCoordinateSystem().getDimension() == 3) {
                return new ReferencedEnvelope3D(
                        minx, maxx, miny, maxy, -Double.MAX_VALUE, Double.MAX_VALUE, crs);
            } else {
                throw new InvalidParameterValueException(
                        "Unexpected BBOX, can only handle 2D or 3D ones");
            }
        }
    }

    private static double rollLongitude(final double x) {
        double mod = (x + 180) % 360;
        if (mod == 0) {
            return x > 0 ? 180 : -180;
        } else {
            return mod - 180;
        }
    }
}
