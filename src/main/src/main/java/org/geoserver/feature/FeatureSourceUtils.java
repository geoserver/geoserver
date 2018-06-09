/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Set of utility methods for {@link org.geotools.data.FeatureSource}.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class FeatureSourceUtils {
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.feature");

    /**
     * Retreives the bounds for a feature source.
     *
     * <p>If the feautre source can calculate the bounds directly, those bounds are returned.
     * Otherwise, the underlying feature collection is retreived and asked to calculate bounds. If
     * that fails, an empty envelope is returned.
     *
     * @param fs The feature source.
     * @return The bounds.
     * @throws IOException Execption calculating bounds on feature source.
     */
    public static ReferencedEnvelope getBoundingBoxEnvelope(
            FeatureSource<? extends FeatureType, ? extends Feature> fs) throws IOException {
        ReferencedEnvelope ev = fs.getBounds();

        if ((ev == null) || ev.isNull()) {
            try {
                ev = fs.getFeatures().getBounds();
            } catch (Throwable t) {
                LOGGER.log(
                        Level.FINE,
                        "Could not compute the data bounding box. Returning an empty envelope",
                        t);
                ev = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
            }
        }

        return ev;
    }
}
