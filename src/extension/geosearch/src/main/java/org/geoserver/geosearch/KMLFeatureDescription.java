package org.geoserver.geosearch;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import org.geoserver.ows.URLMangler.URLType;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class KMLFeatureDescription extends AbstractFeatureDescription {
    private Namespace KML = 
        Namespace.getNamespace("http://www.opengis.net/kml/2.2");

    private String GEOSERVER_URL;

    public void handle(Request req, Response resp) {
        GEOSERVER_URL = getBaseURL(req);

        if (req.getMethod().equals(Method.GET)) {
            doGet(req, resp);
        } else {
            resp.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        }
    }

    public void doGet(Request req, Response resp) {
        SimpleFeature f = findFeature(req);
        String prefix = (String)req.getAttributes().get("namespace");
        String type = (String)req.getAttributes().get("layer");
        Document kml = buildKMLDoc(prefix + ":" + type, f);
        resp.setEntity(new JDOMRepresentation(kml, new MediaType("application/vnd.google-earth.kml+xml")));
    }

    private Document buildKMLDoc(String typeName, SimpleFeature f) {
        Document kml = new Document();
        Element root = new Element("kml", KML);
        Element doc = new Element("Document");
        doc.addContent(buildLookAt(f));
        doc.addContent(buildNetworkLink(typeName));
        root.addContent(doc);
        kml.setRootElement(root);
        return kml;
    }

    private Element buildLookAt(SimpleFeature f) {
        Element lookat = new Element("LookAt");
        Coordinate p = getLatLonCentroid(f);
        lookat.addContent(new Element("longitude").addContent("" + p.x));
        lookat.addContent(new Element("latitude").addContent("" + p.y));
        lookat.addContent(new Element("altitude").addContent("0"));
        lookat.addContent(new Element("range").addContent("700"));
        lookat.addContent(new Element("tilt").addContent("0"));
        lookat.addContent(new Element("heading").addContent("0"));
        lookat.addContent(
                new Element("altitudeMode").addContent("clampToGround")
                );
        return lookat;
    }

    private Element buildNetworkLink(String typeName) {
        Element networklink = new Element("NetworkLink");
        networklink.addContent(new Element("name").addContent(
                "Complete " + typeName + " hierarchy"));
        networklink.addContent(new Element("visibility").addContent("1"));

        Element link = new Element("Link");
        link.addContent(new Element("href").addContent(
                
                buildURL(GEOSERVER_URL, "wms/kml?layers=" + typeName, null, URLType.SERVICE)));

        networklink.addContent(link);
        return networklink;
    }

    private Coordinate getLatLonCentroid(SimpleFeature f) {
        Coordinate c = geometryCentroid((Geometry)f.getDefaultGeometry());

        try {
            CoordinateReferenceSystem nativeCRS = 
                f.getType().getCoordinateReferenceSystem();
            CoordinateReferenceSystem latLon = CRS.decode("EPSG:4326");

            if (!CRS.equalsIgnoreMetadata(nativeCRS, latLon)) {
                MathTransform xform = 
                    CRS.findMathTransform(nativeCRS, latLon, true);
                //convert data bbox to lat/long
                c = JTS.transform(c, null, xform); 
            } 
        } catch (Exception e) {
            // TODO: Log error
        }

        return c;
    }

    /**
     * Returns the centroid of the geometry, handling a geometry collection.
     * <p>
     * In the case of a collection a multi point containing the centroid of
     * each geometry in the collection is calculated. The first point in the
     * multi point is returned as the cetnroid.
     * </p>
     */
    Coordinate geometryCentroid(Geometry g) {
        // TODO: should the collection case return the centroid of the
        // multi point?
        if (g instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) g;

            // check for case of single geometry
            if (gc.getNumGeometries() == 1) {
                g = gc.getGeometryN(0);
            } else {
                double maxAreaSoFar = gc.getGeometryN(0).getArea();
                Coordinate centroidToReturn =
                    gc.getGeometryN(0).getCentroid().getCoordinate();

                for (int t = 0; t < gc.getNumGeometries(); t++) {
                    double area = gc.getGeometryN(t).getArea();
                    if (area > maxAreaSoFar) {
                        maxAreaSoFar = area;
                        centroidToReturn =
                            gc.getGeometryN(t).getCentroid().getCoordinate();
                    }
                }

                return centroidToReturn;
            }
        }

        if (g instanceof Point) {
            // thats easy
            return g.getCoordinate();
        } else if (g instanceof LineString) {
            // make sure the point we return is actually on the line
            double tol = 1E-6;
            double mid = g.getLength() / 2d;

            Coordinate[] coords = g.getCoordinates();

            // walk along the linestring until we get to a point where we
            // have two coordinates that straddle the midpoint
            double len = 0d;
            for (int i = 1; i < coords.length; i++) {
                LineSegment line = new LineSegment(coords[i - 1], coords[i]);
                len += line.getLength();

                if (Math.abs(len - mid) < tol) {
                    // close enough
                    return line.getCoordinate(1);
                }

                if (len > mid) {
                    // we have gone past midpoint
                    return line.pointAlong(1 - ((len - mid) / line
                                .getLength()));
                }
            }

            // should never get there
            return g.getCentroid().getCoordinate();
        } else {
            // return the actual centroid
            return g.getCentroid().getCoordinate();
        }
    }
}
