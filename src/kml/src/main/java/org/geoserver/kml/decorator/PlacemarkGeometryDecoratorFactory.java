/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.utils.KmlCentroidBuilder;
import org.geoserver.kml.utils.KmlCentroidOptions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Encodes the geometry element for Placemark elements
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkGeometryDecoratorFactory implements KmlDecoratorFactory {

    static final KmlCentroidBuilder CENTROIDS = new KmlCentroidBuilder();

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        if (Placemark.class.isAssignableFrom(featureClass)) {
            boolean hasHeightTemplate = hasHeightTemplate(context);
            boolean isExtrudeEnabled = isExtrudeEnabled(context);
            KmlCentroidOptions centroidOpts = KmlCentroidOptions.create(context);

            return new PlacemarkGeometryDecorator(
                    hasHeightTemplate, isExtrudeEnabled, centroidOpts);
        } else {
            return null;
        }
    }

    private boolean hasHeightTemplate(KmlEncodingContext context) {
        // we apply the height template only on wms outputs
        if (!(context.getService() instanceof WMSInfo)) {
            return false;
        }

        try {
            SimpleFeatureType schema = context.getCurrentFeatureCollection().getSchema();
            return !context.getTemplate()
                    .isTemplateEmpty(schema, "height.ftl", FeatureTemplate.class, "0\n");
        } catch (IOException e) {
            throw new ServiceException("Failed to apply height template during kml generation", e);
        }
    }

    private boolean isExtrudeEnabled(KmlEncodingContext context) {
        // extrusion applies only to WMS mode
        if (!(context.getService() instanceof WMSInfo)) {
            return false;
        }

        // were we asked not to perform extrusion?
        Object foExtrude = context.getRequest().getFormatOptions().get("extrude");
        if (foExtrude == null) {
            // true by default
            return true;
        }
        // be careful, in case someone set extrude to some funny value
        Boolean extrude = Converters.convert(foExtrude, Boolean.class);
        return extrude == Boolean.TRUE;
    }

    static class PlacemarkGeometryDecorator implements KmlDecorator {

        static final Logger LOGGER = Logging.getLogger(PlacemarkGeometryDecorator.class);
        private boolean hasHeightTemplate;
        private boolean extrudeEnabled;
        private KmlCentroidOptions centroidOpts;

        public PlacemarkGeometryDecorator(
                boolean hasHeightTemplate,
                boolean extrudeEnabled,
                KmlCentroidOptions centroidOpts) {
            this.hasHeightTemplate = hasHeightTemplate;
            this.extrudeEnabled = extrudeEnabled;
            this.centroidOpts = centroidOpts;
        }

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            // encode the geometry
            Placemark pm = (Placemark) feature;
            SimpleFeature sf = context.getCurrentFeature();

            double height = Double.NaN;
            if (hasHeightTemplate) {
                try {
                    String output =
                            context.getTemplate().template(sf, "height.ftl", FeatureTemplate.class);
                    height = Double.valueOf(output);
                } catch (IOException ioe) {
                    LOGGER.log(
                            Level.WARNING,
                            "Couldn't render height template for " + sf.getID(),
                            ioe);
                }
            }

            Geometry geometry = getFeatureGeometry(sf, height);
            if (geometry != null) {
                pm.setGeometry(encodeGeometry(geometry, context, height));
            }

            return feature;
        }

        /** Extracts the */
        private Geometry getFeatureGeometry(SimpleFeature sf, final double height) {
            Geometry geom = (Geometry) sf.getDefaultGeometry();

            if (!Double.isNaN(height) && height != 0) {
                geom.apply(
                        new CoordinateFilter() {
                            public void filter(Coordinate c) {
                                c.setCoordinate(new Coordinate(c.x, c.y, height));
                            }
                        });
                geom.geometryChanged();
            }

            return geom;
        }

        private de.micromata.opengis.kml.v_2_2_0.Geometry encodeGeometry(
                Geometry geometry, KmlEncodingContext context, double height) {
            de.micromata.opengis.kml.v_2_2_0.Geometry kmlGeometry = toKmlGeometry(geometry);
            boolean isSinglePoint =
                    geometry instanceof Point
                            || (geometry instanceof MultiPoint)
                                    && ((MultiPoint) geometry).getNumPoints() == 1;

            // if is not a single point and is description enabled, we
            // add and extrude a centroid together with the geometry
            if (!isSinglePoint && context.isDescriptionEnabled()) {
                MultiGeometry mg = new MultiGeometry();

                // centroid + full geometry
                Coordinate c =
                        CENTROIDS.geometryCentroid(
                                geometry, context.getRequest().getBbox(), centroidOpts);
                if (!Double.isNaN(height)) {
                    c.setOrdinate(2, height);
                }
                de.micromata.opengis.kml.v_2_2_0.Point kmlPoint = toKmlPoint(c);
                mg.addToGeometry(kmlPoint);

                // encode the full geometry
                mg.addToGeometry(kmlGeometry);

                kmlGeometry = mg;
            }

            if (hasHeightTemplate) {
                applyExtrusion(kmlGeometry);
            }
            return kmlGeometry;
        }

        private de.micromata.opengis.kml.v_2_2_0.Point toKmlPoint(Coordinate c) {
            de.micromata.opengis.kml.v_2_2_0.Point result =
                    new de.micromata.opengis.kml.v_2_2_0.Point();
            if (Double.isNaN(c.getZ())) {
                result.addToCoordinates(c.x, c.y);
            } else {
                result.addToCoordinates(c.x, c.y, c.getZ());
            }

            return result;
        }

        private de.micromata.opengis.kml.v_2_2_0.Geometry toKmlGeometry(Geometry geometry) {
            if (geometry == null) {
                return null;
            }

            if (geometry instanceof GeometryCollection) {
                MultiGeometry mg = new MultiGeometry();
                GeometryCollection gc = (GeometryCollection) geometry;
                if (gc.getNumGeometries() == 1) {
                    return toKmlGeometry(gc.getGeometryN(0));
                } else {
                    for (int i = 0; i < gc.getNumGeometries(); i++) {
                        Geometry child = gc.getGeometryN(i);
                        mg.addToGeometry(toKmlGeometry(child));
                    }
                }
                return mg;
            } else if (geometry instanceof Point) {
                return toKmlPoint(geometry.getCoordinate());
            } else if (geometry instanceof LinearRing) {
                return convertLinearRing((LinearRing) geometry);
            } else if (geometry instanceof LineString) {
                de.micromata.opengis.kml.v_2_2_0.LineString kmlLine =
                        new de.micromata.opengis.kml.v_2_2_0.LineString();
                List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlCoordinates =
                        dumpCoordinateSequence(((LineString) geometry).getCoordinateSequence());
                kmlLine.setCoordinates(kmlCoordinates);
                return kmlLine;
            } else if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon) geometry;
                de.micromata.opengis.kml.v_2_2_0.Polygon kmlPolygon =
                        new de.micromata.opengis.kml.v_2_2_0.Polygon();
                de.micromata.opengis.kml.v_2_2_0.LinearRing kmlOuterRing =
                        convertLinearRing((LinearRing) polygon.getExteriorRing());
                kmlPolygon.createAndSetOuterBoundaryIs().setLinearRing(kmlOuterRing);
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    LinearRing interior = (LinearRing) polygon.getInteriorRingN(i);
                    de.micromata.opengis.kml.v_2_2_0.LinearRing kmlInterior =
                            convertLinearRing(interior);
                    kmlPolygon.createAndAddInnerBoundaryIs().setLinearRing(kmlInterior);
                }
                return kmlPolygon;
            } else {
                throw new IllegalArgumentException("Unrecognized geometry type: " + geometry);
            }
        }

        private de.micromata.opengis.kml.v_2_2_0.LinearRing convertLinearRing(LinearRing geometry) {
            de.micromata.opengis.kml.v_2_2_0.LinearRing kmlLine =
                    new de.micromata.opengis.kml.v_2_2_0.LinearRing();
            List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlCoordinates =
                    dumpCoordinateSequence(((LineString) geometry).getCoordinateSequence());
            kmlLine.setCoordinates(kmlCoordinates);
            if (!hasHeightTemplate) {
                // allow the polygon to follow the ground, otherwise some polygons with long
                // edges will disappear in mountain areas
                kmlLine.setTessellate(true);
            }
            return kmlLine;
        }

        private List<de.micromata.opengis.kml.v_2_2_0.Coordinate> dumpCoordinateSequence(
                CoordinateSequence cs) {
            List<de.micromata.opengis.kml.v_2_2_0.Coordinate> result =
                    new ArrayList<de.micromata.opengis.kml.v_2_2_0.Coordinate>(cs.size());
            for (int i = 0; i < cs.size(); i++) {
                double x = cs.getOrdinate(i, 0);
                double y = cs.getOrdinate(i, 1);
                double z = Double.NaN;
                if (cs.getDimension() >= 3 || hasHeightTemplate) {
                    z = cs.getOrdinate(i, 2);
                }
                de.micromata.opengis.kml.v_2_2_0.Coordinate c;
                if (Double.isNaN(z)) {
                    c = new de.micromata.opengis.kml.v_2_2_0.Coordinate(x, y);
                } else {
                    c = new de.micromata.opengis.kml.v_2_2_0.Coordinate(x, y, z);
                }
                result.add(c);
            }

            return result;
        }

        public void applyExtrusion(de.micromata.opengis.kml.v_2_2_0.Geometry kmlGeometry) {
            if (kmlGeometry instanceof de.micromata.opengis.kml.v_2_2_0.Polygon) {
                de.micromata.opengis.kml.v_2_2_0.Polygon polygon =
                        (de.micromata.opengis.kml.v_2_2_0.Polygon) kmlGeometry;
                polygon.setExtrude(extrudeEnabled);
                polygon.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
            } else if (kmlGeometry instanceof de.micromata.opengis.kml.v_2_2_0.LinearRing) {
                de.micromata.opengis.kml.v_2_2_0.LinearRing ring =
                        (de.micromata.opengis.kml.v_2_2_0.LinearRing) kmlGeometry;
                ring.setExtrude(extrudeEnabled);
                ring.setTessellate(true);
                ring.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
            } else if (kmlGeometry instanceof de.micromata.opengis.kml.v_2_2_0.LineString) {
                de.micromata.opengis.kml.v_2_2_0.LineString ls =
                        (de.micromata.opengis.kml.v_2_2_0.LineString) kmlGeometry;
                ls.setExtrude(extrudeEnabled);
                ls.setTessellate(true);
                ls.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
            } else if (kmlGeometry instanceof de.micromata.opengis.kml.v_2_2_0.Point) {
                de.micromata.opengis.kml.v_2_2_0.Point point =
                        (de.micromata.opengis.kml.v_2_2_0.Point) kmlGeometry;
                point.setExtrude(extrudeEnabled);
                point.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
            } else if (kmlGeometry instanceof MultiGeometry) {
                de.micromata.opengis.kml.v_2_2_0.MultiGeometry mg =
                        (de.micromata.opengis.kml.v_2_2_0.MultiGeometry) kmlGeometry;
                for (de.micromata.opengis.kml.v_2_2_0.Geometry g : mg.getGeometry()) {
                    applyExtrusion(g);
                }
            }
        }
    }
}
