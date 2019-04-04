package org.geoserver.mapml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.mapml.xml.ObjectFactory;
import org.geoserver.mapml.xml.PropertyContent;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;

public class MapMLGenerator {
    
    static ObjectFactory factory = new ObjectFactory();
    
    public static Feature buildFeature(SimpleFeature sf) throws IOException {
        
        Feature f = new Feature();
        f.setId(sf.getID());
        f.setClazz(sf.getFeatureType().getTypeName());
        PropertyContent pc = new PropertyContent();
        f.setProperties(pc);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr>");
        sb.append("<th role=\"columnheader\" scope=\"col\">Property name</th>");
        sb.append("<th role=\"columnheader\" scope=\"col\">Property value</th>");
        sb.append("</tr></thead><tbody>");
        
        Geometry g = null;
        for(AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors()) {
            if(attr.getType() instanceof GeometryType) {
                g = (Geometry)(sf.getAttribute(attr.getName()));
            } else {
                sb.append("<tr><th scope=\"row\">" + attr.getLocalName() + "</th>");
                sb.append("<td itemprop=\"" + attr.getName() + "\">" + sf.getAttribute(attr.getName()).toString() + "</td></tr>");
            }
        }
        
        sb.append("</tbody></table>");
        pc.setAnyElement(sb.toString());
        f.setGeometry(buildGeometry(g));
        return f;
    }

    @SuppressWarnings("unchecked")
    public static GeometryContent buildGeometry(Geometry g) throws IOException {
        GeometryContent geom = new GeometryContent();
        switch(g.getGeometryType()) {
            case "Point":
                break;
            case "MultiPoint":
                break;
            case "LineString":
                break;
            case "LinearRing":
                break;
            case "MultiLineString":
                break;
            case "Polygon":
                geom.setGeometryContent(factory.createPolygon(buildPolygon((Polygon)g)));
                break;
            case "MultiPolygon":
                org.geoserver.mapml.xml.MultiPolygon multiPoly = new org.geoserver.mapml.xml.MultiPolygon();
                List<org.geoserver.mapml.xml.Polygon> polys = multiPoly.getPolygon();
                MultiPolygon mp = (MultiPolygon)g;
                for(int i = 0; i < mp.getNumGeometries(); i++) {
                    polys.add(buildPolygon((Polygon)mp.getGeometryN(i)));
                }
                geom.setGeometryContent(factory.createMultiPolygon(multiPoly));
                break;
            case "GeometryCollection":
                break;
            default:
                throw new IOException("Unknown geometry type: " + g.getGeometryType());
        }

        return geom;
    }
    
    private static org.geoserver.mapml.xml.Polygon buildPolygon(Polygon p) {
        org.geoserver.mapml.xml.Polygon poly = new org.geoserver.mapml.xml.Polygon();
        List<JAXBElement<List<String>>> ringList = poly.getThreeOrMoreCoordinatePairs();
        List<String> coordList = buildCoordinates(p.getExteriorRing().getCoordinateSequence());
        // TODO inner rings
        ringList.add(factory.createPolygonCoordinates(coordList));
        return poly;
    }

    private static List<String> buildCoordinates(CoordinateSequence cs) {
        List<String> coordList = new ArrayList<String>(cs.size());
        for(int i = 0; i < cs.size(); i++) {
            coordList.add(cs.getX(i) + " " + cs.getY(i));
        }
        return coordList;
    }
}
