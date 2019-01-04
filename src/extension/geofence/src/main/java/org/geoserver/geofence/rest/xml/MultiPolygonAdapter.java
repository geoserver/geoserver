/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.xsd.DOMParser;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Translates between GML3.2 MultiGeometry and MultiPolygon object for JAXB.
 *
 * @author Niels Charlier
 */
public class MultiPolygonAdapter extends XmlAdapter<Object, MultiPolygon> {

    @Override
    public Element marshal(MultiPolygon geometry) throws Exception {
        if (geometry == null) {
            return null;
        }
        try {
            Encoder encoder = new Encoder(new GMLConfiguration());
            return encoder.encodeAsDOM(geometry, org.geotools.gml3.v3_2.GML.MultiGeometry)
                    .getDocumentElement();
        } catch (Exception e) {
            throw new Exception("Cannot transform the specified geometry in GML", e);
        }
    }

    @Override
    public MultiPolygon unmarshal(Object o) throws Exception {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(doc.importNode(((Element) o).getFirstChild(), true));
            DOMParser parser = new DOMParser(new GMLConfiguration(), doc);
            Geometry geom = (Geometry) parser.parse();
            if (geom instanceof Polygon) {
                return new MultiPolygon(new Polygon[] {(Polygon) geom}, geom.getFactory());
            } else if (geom instanceof GeometryCollection) {
                Polygon[] pols = new Polygon[((GeometryCollection) geom).getNumGeometries()];
                for (int i = 0; i < pols.length; i++) {
                    pols[i] = (Polygon) ((GeometryCollection) geom).getGeometryN(i);
                }
                return new MultiPolygon(pols, geom.getFactory());
            }
            return (MultiPolygon) geom;
        } catch (Exception e) {
            throw new Exception("Cannot parse specified XML as GML Polygon", e);
        }
    }
}
