package org.geoserver.kml.decorator;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.wms.GetMapRequest;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class PlacemarkGeometryDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(Class<? extends Feature> featureClass, KmlEncodingContext context) {
        if (Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkGeometryDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkGeometryDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            // encode the geometry
            Placemark pm = (Placemark) feature;
            SimpleFeature sf = context.getCurrentFeature();
            Geometry geometry = (Geometry) sf.getDefaultGeometry();

            if(geometry != null) {
                pm.setGeometry(encodeGeometry(geometry));
            }

            return feature;
        }

        private de.micromata.opengis.kml.v_2_2_0.Geometry encodeGeometry(Geometry geometry) {
            Coordinate firstPoint = geometry.getCoordinate();
            if (geometry instanceof Point || (geometry instanceof MultiPoint)
                    && ((MultiPoint) geometry).getNumPoints() == 1) {
                Coordinate c = firstPoint;
                return toKmlPoint(c);
            } else {
                MultiGeometry mg = new MultiGeometry();

                // centroid + full geometry
                Coordinate c = geometry.getCentroid().getCoordinate();
                if (!Double.isNaN(firstPoint.z)) {
                    c.setOrdinate(2, firstPoint.z);
                }
                mg.addToGeometry(toKmlPoint(c));

                // encode the full geometry
                encodeGeometry(mg, geometry);

                return mg;
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

        private void encodeGeometry(MultiGeometry mg, Geometry geometry) {
            if (geometry instanceof GeometryCollection) {
                GeometryCollection gc = (GeometryCollection) geometry;
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    Geometry child = gc.getGeometryN(i);
                    encodeGeometry(mg, child);
                }
            } else if (geometry instanceof Point) {
                de.micromata.opengis.kml.v_2_2_0.Point kmlPoint = toKmlPoint(geometry
                        .getCoordinate());
                mg.addToGeometry(kmlPoint);
            } else if (geometry instanceof LinearRing) {
                de.micromata.opengis.kml.v_2_2_0.LinearRing kmlLine = convertLinearRing((LinearRing) geometry);
                mg.addToGeometry(kmlLine);
            } else if (geometry instanceof LineString) {
                de.micromata.opengis.kml.v_2_2_0.LineString kmlLine = new de.micromata.opengis.kml.v_2_2_0.LineString();
                List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlCoordinates = dumpCoordinateSequence(((LineString) geometry)
                        .getCoordinateSequence());
                kmlLine.setCoordinates(kmlCoordinates);
                mg.addToGeometry(kmlLine);
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
                mg.addToGeometry(kmlPolygon);
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

    }

}
