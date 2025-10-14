/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;

/** A BBOX parser specialized for API bounding boxes (up to 6 coordinates, default CRS is CRS84) */
public class APIBBoxParser {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /**
     * Turns a bbox specification into a OGC filter, assuming it's expressed in {@link DefaultGeographicCRS#WGS84}. Will
     * return {@link Filter#INCLUDE} if the bbox * spec itself is null or empty.
     */
    public static Filter toFilter(String bbox) throws FactoryException {
        return toFilter(bbox, (CoordinateReferenceSystem) null);
    }

    /**
     * Turns a bbox specification into a OGC filter, using the given CRS, or {@link DefaultGeographicCRS#WGS84} if null
     * is passed. Will return {@link Filter#INCLUDE} if the bbox spec itself is null or empty.
     */
    public static Filter toFilter(double[] bbox, CoordinateReferenceSystem crs) throws FactoryException {
        if (bbox == null) {
            return Filter.INCLUDE;
        }

        // to envelopes first, build filter around them then
        ReferencedEnvelope[] parsed;
        if (bbox.length == 4) {
            parsed = buildEnvelopes(bbox.length, bbox[0], bbox[1], 0, bbox[2], bbox[3], 0, crs);
        } else if (bbox.length == 6) {
            parsed = buildEnvelopes(bbox.length, bbox[0], bbox[1], bbox[2], bbox[3], bbox[4], bbox[5], crs);
        } else {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Bounding box array must have either 4 or 6 ordinates",
                    HttpStatus.BAD_REQUEST);
        }
        return toFilter(parsed);
    }

    /**
     * Turns a bbox specification into a OGC filter, using the given CRS, or {@link DefaultGeographicCRS#WGS84} if null
     * is passed. Will return {@link Filter#INCLUDE} if the bbox spec itself is null or empty.
     */
    public static Filter toFilter(String bbox, String crs) throws FactoryException {
        if (bbox == null || bbox.trim().isEmpty()) {
            return Filter.INCLUDE;
        }

        // to envelopes first, build filter around them then
        ReferencedEnvelope[] parsed = parse(bbox, crs);
        return toFilter(parsed);
    }

    /**
     * Turns a bbox specification into a OGC filter, using the given CRS, or {@link DefaultGeographicCRS#WGS84} if null
     * is passed. Will return {@link Filter#INCLUDE} if the bbox spec itself is null or empty.
     */
    public static Filter toFilter(String bbox, CoordinateReferenceSystem crs) throws FactoryException {
        if (bbox == null || bbox.trim().isEmpty()) {
            return Filter.INCLUDE;
        }

        // to envelopes first, build filter around them then
        ReferencedEnvelope[] parsed = parse(bbox, crs);
        return toFilter(parsed);
    }

    private static Filter toFilter(ReferencedEnvelope[] bboxes) {
        if (bboxes.length == 1) {
            return FF.bbox(FF.property(""), bboxes[0]);
        } else if (bboxes instanceof ReferencedEnvelope[]) {
            List<Filter> filters =
                    Stream.of(bboxes).map(e -> FF.bbox(FF.property(""), e)).collect(Collectors.toList());
            return FF.or(filters);
        } else {
            throw new IllegalArgumentException("Could not understand parsed bbox " + Arrays.toString(bboxes));
        }
    }

    /** Parses a BBOX assuming the default {@link DefaultGeographicCRS#WGS84} as the CRS */
    public static ReferencedEnvelope[] parse(String value) throws FactoryException {
        return parse(value, (CoordinateReferenceSystem) null);
    }

    /** Parses a BBOX with the given CRS, if null {@link DefaultGeographicCRS#WGS84} will be used */
    public static ReferencedEnvelope[] parse(String value, String crs) throws FactoryException {
        return parse(value, parseCRS(crs));
    }

    private static CoordinateReferenceSystem parseCRS(String crs) throws FactoryException {
        try {
            return crs != null ? CRS.decode(crs, true) : null;
        } catch (NoSuchAuthorityCodeException e) {
            throw new APIException(INVALID_PARAMETER_VALUE, "Invalid CRS: " + crs, HttpStatus.BAD_REQUEST);
        }
    }

    /** Parses a BBOX with the given CRS, if null {@link DefaultGeographicCRS#WGS84} will be used */
    public static ReferencedEnvelope[] parse(String value, CoordinateReferenceSystem crs) throws FactoryException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() < 4) {
            throw new IllegalArgumentException(
                    "Requested bounding box contains wrong number of coordinates (should have 4): " + unparsed.size());
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

        return buildEnvelopes(countco, minx, miny, minz, maxx, maxy, maxz, crs);
    }

    private static ReferencedEnvelope[] buildEnvelopes(
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
            } else if (countco == 6) {
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
        return new ReferencedEnvelope[] {buildSingleEnvelope(countco, minx, miny, minz, maxx, maxy, maxz, crs)};
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
            throw new ServiceException("illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx);
        }

        if (miny > maxy) {
            throw new ServiceException("illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy);
        }

        if (minz > maxz) {
            throw new ServiceException("illegal bbox, minZ: " + minz + " is " + "greater than maxZ: " + maxz);
        }

        if (countco == 6) {
            return new ReferencedEnvelope3D(minx, maxx, miny, maxy, minz, maxz, crs);
        } else {
            if (crs == null || crs.getCoordinateSystem().getDimension() == 2) {
                return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
            } else if (crs.getCoordinateSystem().getDimension() == 3) {
                return new ReferencedEnvelope3D(minx, maxx, miny, maxy, -Double.MAX_VALUE, Double.MAX_VALUE, crs);
            } else {
                throw new InvalidParameterValueException("Unexpected BBOX, can only handle 2D or 3D ones");
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

    public static Geometry toGeometry(String spec) throws FactoryException {
        ReferencedEnvelope[] parse = parse(spec);
        List<Polygon> polygons =
                Arrays.stream(parse).map(bbox -> JTS.toGeometry(bbox)).collect(Collectors.toList());
        if (polygons.size() == 1) {
            return polygons.get(0);
        } else {
            return polygons.get(0).getFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        }
    }
}
