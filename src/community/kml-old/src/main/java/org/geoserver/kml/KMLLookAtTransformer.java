/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.geoserver.kml.KMLLookAt.AltitudeMode;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Transformer for a {@link KMLLookAt}
 * 
 * @author Gabriel Roldan
 */
class KMLLookAtTransformer extends TransformerBase {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    private Envelope targetBounds;

    /**
     * @param targetBounds
     *            the bounds of the region to look at, overridden by the
     *            {@link KMLLookAt#getLookAt()} geometry if present.
     * 
     */
    public KMLLookAtTransformer(final Envelope targetBounds, final int indentation, Charset charset) {
        this.targetBounds = targetBounds;
        setIndentation(indentation);
        setEncoding(charset);
    }

    @Override
    public Translator createTranslator(final ContentHandler handler) {
        return new LookAtTranslator(handler);
    }

    private class LookAtTranslator extends TranslatorSupport {

        public LookAtTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {

            final KMLLookAt lookAt = (KMLLookAt) o;

            Envelope lookAtEnvelope = targetBounds;
            if (lookAt.getLookAt() != null) {
                Geometry geometry = lookAt.getLookAt();
                lookAtEnvelope = geometry.getEnvelopeInternal();
            }

            if (lookAtEnvelope.isNull()) {
                return;
            }

            double lon1 = lookAtEnvelope.getMinX();
            double lat1 = lookAtEnvelope.getMinY();
            double lon2 = lookAtEnvelope.getMaxX();
            double lat2 = lookAtEnvelope.getMaxY();

            double R_EARTH = 6.371 * 1000000; // meters
            double VIEWER_WIDTH = 22 * Math.PI / 180; // The field of view of the google maps
                                                      // camera, in radians
            double[] p1 = getRect(lon1, lat1, R_EARTH);
            double[] p2 = getRect(lon2, lat2, R_EARTH);
            double[] midpoint = new double[] { (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2,
                    (p1[2] + p2[2]) / 2 };

            midpoint = getGeographic(midpoint[0], midpoint[1], midpoint[2]);

            Double distance = lookAt.getRange();
            if (null == distance) {
                distance = distance(p1, p2);
            }

            double height = distance / (2 * Math.tan(VIEWER_WIDTH));

            LOGGER.fine("lat1: " + lat1 + "; lon1: " + lon1);
            LOGGER.fine("lat2: " + lat2 + "; lon2: " + lon2);
            LOGGER.fine("latmid: " + midpoint[1] + "; lonmid: " + midpoint[0]);

            final Double tilt = lookAt.getTilt() == null ? Double.valueOf(0) : lookAt.getTilt();
            final Double heading = lookAt.getHeading() == null ? Double.valueOf(0) : lookAt
                    .getHeading();
            final Double altitude = lookAt.getAltitude() == null ? Double.valueOf(height) : lookAt
                    .getAltitude();
            final KMLLookAt.AltitudeMode altMode = lookAt.getAltitudeMode() == null ? AltitudeMode.clampToGround
                    : lookAt.getAltitudeMode();

            start("LookAt");
            element("longitude", String.valueOf(midpoint[0]));
            element("latitude", String.valueOf(midpoint[1]));
            element("altitude", String.valueOf(altitude));
            element("range", String.valueOf(distance));
            element("tilt", String.valueOf(tilt));
            element("heading", String.valueOf(heading));
            element("altitudeMode", String.valueOf(altMode));
            end("LookAt");
        }

        private double[] getRect(double lat, double lon, double radius) {
            double theta = (90 - lat) * Math.PI / 180;
            double phi = (90 - lon) * Math.PI / 180;

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            return new double[] { x, y, z };
        }

        private double[] getGeographic(double x, double y, double z) {
            double theta, phi, radius;
            radius = distance(new double[] { x, y, z }, new double[] { 0, 0, 0 });
            theta = Math.atan2(Math.sqrt(x * x + y * y), z);
            phi = Math.atan2(y, x);

            double lat = 90 - (theta * 180 / Math.PI);
            double lon = 90 - (phi * 180 / Math.PI);

            return new double[] { (lon > 180 ? lon - 360 : lon), lat, radius };
        }

        private double distance(double[] p1, double[] p2) {
            double dx = p1[0] - p2[0];
            double dy = p1[1] - p2[1];
            double dz = p1[2] - p2[2];
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

}
