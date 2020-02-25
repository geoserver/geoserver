/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoundsAndCRS {

    static final Logger LOGGER = Logging.getLogger(BoundsAndCRS.class);

    ReferencedEnvelope envelope;

    public BoundsAndCRS(ReferencedEnvelope envelope) {
        this.envelope = envelope;
    }

    public double[] getBbox() {
        return new double[] {
            envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()
        };
    }

    public String getCrs() {
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            return "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        } else {
            try {
                return "urn:ogc:def:crs:EPSG::" + CRS.lookupEpsgCode(crs, true);
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, "Failed to lookup EPSG code for " + crs, e);
                return null;
            }
        }
    }

    @Override
    public String toString() {
        return "BoundsAndCRS{" + "envelope=" + envelope + '}';
    }
}
