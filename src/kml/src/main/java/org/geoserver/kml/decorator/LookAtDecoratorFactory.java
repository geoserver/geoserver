/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.utils.LookAtOptions;
import org.geoserver.wms.WMSInfo;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Adds LookAt elements on Document, Folder and Placemark
 *
 * @author Andrea Aime - GeoSolutions
 */
public class LookAtDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator makes sense only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        if (Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkLookAtDecorator();
        } else if (Folder.class.isAssignableFrom(featureClass)
                || NetworkLink.class.isAssignableFrom(featureClass)) {
            return new LayerLookAtDecorator();
        } else if (Document.class.isAssignableFrom(featureClass)) {
            return new DocumentLookAtDecorator();
        } else {
            return null;
        }
    }

    class DocumentLookAtDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Document document = (Document) feature;
            Envelope bounds = context.getMapContent().getRenderingArea();
            LookAt lookAt = buildLookAt(bounds, context.getLookAtOptions(), false);
            document.setAbstractView(lookAt);

            return document;
        }
    }

    class LayerLookAtDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Envelope bounds = context.getCurrentLayer().getBounds();
            LookAt lookAt = buildLookAt(bounds, context.getLookAtOptions(), false);
            feature.setAbstractView(lookAt);

            return feature;
        }
    }

    class PlacemarkLookAtDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Placemark pm = (Placemark) feature;
            Geometry geometry = (Geometry) context.getCurrentFeature().getDefaultGeometry();
            Envelope bounds = null;
            if (geometry != null) {
                bounds = geometry.getEnvelopeInternal();
            }
            LookAt lookAt = buildLookAt(bounds, context.getLookAtOptions(), true);
            pm.setAbstractView(lookAt);

            return pm;
        }
    }

    public LookAt buildLookAt(Envelope bounds, LookAtOptions options, boolean forceBounds) {
        // get/build the target envelope
        Envelope lookAtEnvelope = bounds;
        Geometry lookAtGeometry = options.getLookAt();
        if (!forceBounds && lookAtGeometry != null) {
            lookAtEnvelope = lookAtGeometry.getEnvelopeInternal();
        }

        if (lookAtEnvelope == null || lookAtEnvelope.isNull()) {
            return null;
        }

        // compute the lookAt details
        double lon1 = lookAtEnvelope.getMinX();
        double lat1 = lookAtEnvelope.getMinY();
        double lon2 = lookAtEnvelope.getMaxX();
        double lat2 = lookAtEnvelope.getMaxY();

        double R_EARTH = 6.371 * 1000000; // meters
        double VIEWER_WIDTH = 22 * Math.PI / 180; // The field of view of the google maps
        // camera, in radians
        double[] p1 = getRect(lon1, lat1, R_EARTH);
        double[] p2 = getRect(lon2, lat2, R_EARTH);
        double[] midpoint =
                new double[] {(p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2, (p1[2] + p2[2]) / 2};

        midpoint = getGeographic(midpoint[0], midpoint[1], midpoint[2]);

        Double distance = options.getRange();
        if (null == distance) {
            distance = distance(p1, p2);
        }
        double height = distance / (2 * Math.tan(VIEWER_WIDTH));

        final Double tilt = options.getTilt() == null ? Double.valueOf(0) : options.getTilt();
        final Double heading =
                options.getHeading() == null ? Double.valueOf(0) : options.getHeading();
        final Double altitude =
                options.getAltitude() == null ? Double.valueOf(height) : options.getAltitude();

        // build the lookat
        LookAt lookAt = new LookAt();
        lookAt.setLongitude(midpoint[0]);
        lookAt.setLatitude(midpoint[1]);
        lookAt.setAltitude(altitude);
        lookAt.setRange(distance);
        lookAt.setTilt(tilt);
        lookAt.setHeading(heading);
        lookAt.setAltitudeMode(options.getAltitudeMode());

        return lookAt;
    }

    private double[] getRect(double lat, double lon, double radius) {
        double theta = (90 - lat) * Math.PI / 180;
        double phi = (90 - lon) * Math.PI / 180;

        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.sin(phi) * Math.sin(theta);
        double z = radius * Math.cos(phi);
        return new double[] {x, y, z};
    }

    private double[] getGeographic(double x, double y, double z) {
        double theta, phi, radius;
        radius = distance(new double[] {x, y, z}, new double[] {0, 0, 0});
        theta = Math.atan2(Math.sqrt(x * x + y * y), z);
        phi = Math.atan2(y, x);

        double lat = 90 - (theta * 180 / Math.PI);
        double lon = 90 - (phi * 180 / Math.PI);

        return new double[] {(lon > 180 ? lon - 360 : lon), lat, radius};
    }

    private double distance(double[] p1, double[] p2) {
        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        double dz = p1[2] - p2[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
