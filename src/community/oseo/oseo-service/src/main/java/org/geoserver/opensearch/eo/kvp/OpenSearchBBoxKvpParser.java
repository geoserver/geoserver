/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import org.geoserver.platform.OWS20Exception;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/**
 * A parser that allows dateline crossing envelopes
 *
 * @author Andrea Aime
 */
public class OpenSearchBBoxKvpParser extends BBoxKvpParser {

    @Override
    protected Object buildEnvelope(
            int countco, double minx, double miny, double minz, double maxx, double maxy, double maxz, String srs)
            throws NoSuchAuthorityCodeException, FactoryException {
        if (countco > 4) {
            throw new IllegalArgumentException("Too many coordinates, openSearch cannot handle non flat envelopes yet");
        }

        CoordinateReferenceSystem crs = srs == null ? null : CRS.decode(srs, true);
        if (crs != null && !CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            throw new OWS20Exception(
                    "OpenSearch for EO requests only support boundig boxes in WGS84",
                    OWS20Exception.OWSExceptionCode.InvalidParameterValue,
                    "box");
        }
        minx = rollLongitude(minx);
        maxx = rollLongitude(maxx);

        if (minx > maxx) {
            // dateline crossing case
            return new ReferencedEnvelope[] {
                new ReferencedEnvelope(minx, 180, miny, maxy, crs), new ReferencedEnvelope(-180, maxx, miny, maxy, crs),
            };
        } else {
            return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        }
    }

    protected static double rollLongitude(final double x) {
        double mod = (x + 180) % 360;
        if (mod == 0) {
            return x > 0 ? 180 : -180;
        } else {
            return mod - 180;
        }
    }
}
