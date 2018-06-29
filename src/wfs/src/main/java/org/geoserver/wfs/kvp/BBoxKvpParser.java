/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** @author Niels Charlier : added 3D BBOX support */
public class BBoxKvpParser extends KvpParser {
    public BBoxKvpParser() {
        super("bbox", Envelope.class);
    }

    public Object parse(String value) throws Exception {
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

        // check for srs
        String srs = null;
        if (unparsed.size() > countco) {
            // merge back the CRS definition, in case it is an AUTO one
            StringBuilder sb = new StringBuilder();
            for (int i = countco; i < unparsed.size(); i++) {
                sb.append(unparsed.get(i));
                if (i < (unparsed.size() - 1)) {
                    sb.append(",");
                }
            }
            srs = sb.toString();
        }

        return buildEnvelope(countco, minx, miny, minz, maxx, maxy, maxz, srs);
    }

    protected Object buildEnvelope(
            int countco,
            double minx,
            double miny,
            double minz,
            double maxx,
            double maxy,
            double maxz,
            String srs)
            throws NoSuchAuthorityCodeException, FactoryException {
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
            CoordinateReferenceSystem crs = srs != null ? CRS.decode(srs) : null;
            return new ReferencedEnvelope3D(minx, maxx, miny, maxy, minz, maxz, crs);
        } else {
            CoordinateReferenceSystem crs = srs != null ? CRS.decode(srs) : null;
            if (crs == null || crs.getCoordinateSystem().getDimension() == 2) {
                return new SRSEnvelope(minx, maxx, miny, maxy, srs);
            } else if (crs.getCoordinateSystem().getDimension() == 3) {
                return new ReferencedEnvelope3D(
                        minx, maxx, miny, maxy, -Double.MAX_VALUE, Double.MAX_VALUE, crs);
            } else {
                throw new WFSException(
                        "Unexpected BBOX, can only handle 2D or 3D ones",
                        "bbox",
                        "InvalidParameterValue");
            }
        }
    }
}
