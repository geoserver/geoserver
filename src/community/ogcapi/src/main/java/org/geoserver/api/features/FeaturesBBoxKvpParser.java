/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api.features;

import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A parser that allows dateline crossing envelopes. TODO: Same as the OpenSearch one, the class
 * will eventually have to be either moved to core or merged with its based class with some option
 * to allow dateline crossing envelopes, or not, depending on the context.
 *
 * @author Andrea Aime
 */
class FeaturesBBoxKvpParser extends BBoxKvpParser {

    @Override
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
        if (countco > 4) {
            throw new IllegalArgumentException(
                    "Too many coordinates, openSearch cannot handle non flat envelopes yet");
        }

        CoordinateReferenceSystem crs = srs == null ? null : CRS.decode(srs, true);
        if (crs == null) {
            crs = DefaultGeographicCRS.WGS84;
        }
        minx = rollLongitude(minx);
        maxx = rollLongitude(maxx);

        if (minx > maxx) {
            // dateline crossing case
            return new ReferencedEnvelope[] {
                new ReferencedEnvelope(minx, 180, miny, maxy, crs),
                new ReferencedEnvelope(-180, maxx, miny, maxy, crs),
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
