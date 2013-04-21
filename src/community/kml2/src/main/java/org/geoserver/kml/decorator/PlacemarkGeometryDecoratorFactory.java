/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

/**
 * Encodes the geometry element for Placemark elements
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class PlacemarkGeometryDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(Class<? extends Feature> featureClass,
            KmlEncodingContext context) {
        if (Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkGeometryDecorator(hasHeightTemplate(context));
        } else {
            return null;
        }
    }

    private boolean hasHeightTemplate(KmlEncodingContext context) {
        try {
            SimpleFeatureType schema = context.getCurrentFeatureCollection().getSchema();
            return !context.getTemplate().isTemplateEmpty(schema, "height.ftl", FeatureTemplate.class, "0\n");
        } catch(IOException e) {
            throw new ServiceException("Failed to apply height template during kml generation", e);
        }
    }

    static class PlacemarkGeometryDecorator implements KmlDecorator {
        
        static final Logger LOGGER = Logging.getLogger(PlacemarkGeometryDecorator.class);
        private boolean hasHeightTemplate;

        public PlacemarkGeometryDecorator(boolean hasHeightTemplate) {
            this.hasHeightTemplate = hasHeightTemplate;
        }

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            // encode the geometry
            Placemark pm = (Placemark) feature;
            SimpleFeature sf = context.getCurrentFeature();
            Geometry geometry = getFeatureGeometry(sf, context);

            if (geometry != null) {
                pm.setGeometry(encodeGeometry(geometry, context));
            }

            return feature;
        }

        /**
         * Extracts the 
         * @param sf
         * @param context
         * @return
         */
        private Geometry getFeatureGeometry(SimpleFeature sf, KmlEncodingContext context) {
            Geometry geom = (Geometry) sf.getDefaultGeometry();
            if(hasHeightTemplate) {
                try {
                    String output =  context.getTemplate().template(sf, "height.ftl", FeatureTemplate.class);
                    final double height = Double.valueOf(output);
    
                    if (!Double.isNaN(height) && height != 0) {
                        geom.apply(new CoordinateFilter() {
                            public void filter(Coordinate c) {
                                c.setCoordinate(new Coordinate(c.x, c.y, height));
                            }
                        });
                        geom.geometryChanged();
                    }
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Couldn't render height template for " + sf.getID(), ioe);
                }
            }
            
            return geom;
        }

        private de.micromata.opengis.kml.v_2_2_0.Geometry encodeGeometry(Geometry geometry, KmlEncodingContext context) {
            if (geometry instanceof Point || (geometry instanceof MultiPoint)
                    && ((MultiPoint) geometry).getNumPoints() == 1) {
                Coordinate c = geometry.getCoordinate();
                return toKmlPoint(c);
            } else if(context.isDescriptionEnabled()) {
                MultiGeometry mg = new MultiGeometry();

                // centroid + full geometry
                Coordinate c = geometryCentroid(geometry);
                if (!Double.isNaN(c.z)) {
                    c.setOrdinate(2, c.z);
                }
                mg.addToGeometry(toKmlPoint(c));

                // encode the full geometry
                mg.addToGeometry(toKmlGeometry(geometry));

                return mg;
            } else {
                return toKmlGeometry(geometry);
            }
        }

        private de.micromata.opengis.kml.v_2_2_0.Point toKmlPoint(Coordinate c) {
            de.micromata.opengis.kml.v_2_2_0.Point result = new de.micromata.opengis.kml.v_2_2_0.Point();
            if (Double.isNaN(c.z)) {
                result.addToCoordinates(c.x, c.y);
            } else {
                result.addToCoordinates(c.x, c.y, c.z);
            }

            return result;
        }

        private de.micromata.opengis.kml.v_2_2_0.Geometry toKmlGeometry(Geometry geometry) {
            if(geometry == null) {
                return null;
            }
            
            if (geometry instanceof GeometryCollection) {
                MultiGeometry mg = new MultiGeometry();
                GeometryCollection gc = (GeometryCollection) geometry;
                if(gc.getNumGeometries() == 1) {
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
                de.micromata.opengis.kml.v_2_2_0.LineString kmlLine = new de.micromata.opengis.kml.v_2_2_0.LineString();
                List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlCoordinates = dumpCoordinateSequence(((LineString) geometry)
                        .getCoordinateSequence());
                kmlLine.setCoordinates(kmlCoordinates);
                return kmlLine;
            } else if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon) geometry;
                de.micromata.opengis.kml.v_2_2_0.Polygon kmlPolygon = new de.micromata.opengis.kml.v_2_2_0.Polygon();
                de.micromata.opengis.kml.v_2_2_0.LinearRing kmlOuterRing = convertLinearRing((LinearRing) polygon
                        .getExteriorRing());
                kmlPolygon.createAndSetOuterBoundaryIs().setLinearRing(kmlOuterRing);
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    LinearRing interior = (LinearRing) polygon.getInteriorRingN(i);
                    de.micromata.opengis.kml.v_2_2_0.LinearRing kmlInterior = convertLinearRing(interior);
                    kmlPolygon.createAndAddInnerBoundaryIs().setLinearRing(kmlInterior);
                }
                return kmlPolygon;
            } else {
                throw new IllegalArgumentException("Unrecognized geometry type: " + geometry);
            }
        }

        private de.micromata.opengis.kml.v_2_2_0.LinearRing convertLinearRing(LinearRing geometry) {
            de.micromata.opengis.kml.v_2_2_0.LinearRing kmlLine = new de.micromata.opengis.kml.v_2_2_0.LinearRing();
            List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlCoordinates = dumpCoordinateSequence(((LineString) geometry)
                    .getCoordinateSequence());
            kmlLine.setCoordinates(kmlCoordinates);
            return kmlLine;
        }

        private List<de.micromata.opengis.kml.v_2_2_0.Coordinate> dumpCoordinateSequence(
                CoordinateSequence cs) {
            List<de.micromata.opengis.kml.v_2_2_0.Coordinate> result = new ArrayList<de.micromata.opengis.kml.v_2_2_0.Coordinate>(
                    cs.size());
            for (int i = 0; i < cs.size(); i++) {
                double x = cs.getOrdinate(i, 0);
                double y = cs.getOrdinate(i, 1);
                double z = Double.NaN;
                if (cs.getDimension() >= 3) {
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

        /**
         * Returns the centroid of the geometry, handling a geometry collection.
         * <p>
         * In the case of a collection a multi point containing the centroid of each geometry in the
         * collection is calculated. The first point in the multi point is returned as the controid.
         * </p>
         */
        Coordinate geometryCentroid(Geometry g) {
            if (g instanceof GeometryCollection) {
                g = selectRepresentativeGeometry((GeometryCollection) g);
            }

            if (g == null) {
                return null;
            } else if (g instanceof Point) {
                // simple case
                return g.getCoordinate();
            } else if (g instanceof LineString) {
                // make sure the point we return is actually on the line
                LineString line = (LineString) g;
                LengthIndexedLine lil = new LengthIndexedLine(line);
                return lil.extractPoint(line.getLength() / 2.0);
            } else {
                // return the actual centroid
                return g.getCentroid().getCoordinate();
            }
        }

        /**
         * Selects a representative geometry from the collection (the one covering the biggest area)
         * 
         * @param g
         * @return
         */
        private Geometry selectRepresentativeGeometry(GeometryCollection g) {
            GeometryCollection gc = (GeometryCollection) g;

            if (gc.isEmpty()) {
                return null;
            }

            // check for case of single geometry or multipoint
            Geometry first = gc.getGeometryN(0);
            if (gc.getNumGeometries() == 1 || g instanceof MultiPoint) {
                return first;
            } else {
                // get the geometry with the largest bbox
                double maxAreaSoFar = first.getEnvelope().getArea();
                Geometry geometryToReturn = first;

                for (int t = 0; t < gc.getNumGeometries(); t++) {
                    Geometry curr = gc.getGeometryN(t);
                    double area = curr.getEnvelope().getArea();
                    if (area > maxAreaSoFar) {
                        maxAreaSoFar = area;
                        geometryToReturn = curr;
                    }
                }

                return geometryToReturn;
            }
        }

    }

}
