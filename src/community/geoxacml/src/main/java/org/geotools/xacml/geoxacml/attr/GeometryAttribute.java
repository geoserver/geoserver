/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.attr;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.attr.AttributeValue;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * This class is the implemtation of a XACML geometry attribute as described in the GeoXACML
 * specification
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryAttribute extends AttributeValue {

    // GeoXACML type
    public static final String identifier = "urn:ogc:def:dataType:geoxacml:1.0:geometry";

    private Geometry geometry;

    private String srsName, gid, gmlType;

    private GMLVersion gmlVersion;

    private int srsDimension = 2; // default Value

    /**
     * Constructor
     * 
     * @param geometry
     *            the jts geometry
     * @param srsName
     *            the srsName, derived from the GML srsName attribute, may be null
     * @param gid
     *            the gid (GML global identifier, my be null)
     * @param gmlVersion
     *            gmlVersion (Version2 or Version3)
     * @param gmlType
     *            the gml type encoded as xml type
     * @throws URISyntaxException
     */
    public GeometryAttribute(Geometry geometry, String srsName, String gid, GMLVersion gmlVersion,
            String gmlType) throws URISyntaxException {
        super(new URI(identifier));
        this.geometry = geometry;
        this.srsName = srsName;
        this.gid = gid;
        this.gmlVersion = gmlVersion;
        this.gmlType = gmlType;
    }

    /*
     * @see com.sun.xacml.attr.AttributeValue#encode()
     * 
     * encodes the attribue as XML Fragment, dependent on the gmlVersion inst var
     */
    @Override
    public String encode() {

        GMLSupport gmlSupport = GMLSupportFactory.getGMLSupport(this);
        StringBuffer buff = new StringBuffer();
        gmlSupport.encodeASGML(this, buff);
        return buff.toString();
    }

    /**
     * @param root
     *            GML node from an GML DOM
     * @return instance of this class or null if there is no Node in the GML namespace
     * @throws Exception
     *             Thrown if GML tree could not be interpreted
     */
    public static GeometryAttribute getInstance(Node root) throws Exception {

        Node gmlNode = GMLSupport.GMLNS.equals(root.getNamespaceURI()) ? root : null;

        if (gmlNode == null) {
            NodeList nodelist = root.getChildNodes();
            for (int i = 0; i < nodelist.getLength(); i++) {
                Node n = nodelist.item(i);
                if (GMLSupport.GMLNS.equals(n.getNamespaceURI())) {
                    gmlNode = n;
                    break;
                }
            }
        }

        if (gmlNode == null) {
            Logger.getLogger(GeometryAttribute.class.getName()).severe("No GML node found");
            return null;
        }

        GMLSupport gmlSupport = GMLSupportFactory.getGMLSupport(gmlNode);
        GeometryAttribute retval = gmlSupport.buildFromGML(gmlNode);
        // System.out.println(retval.getGeometry().getClass()+"\t"+retval.getSrsName()+"\t"+retval.
        // hashCode()+"\t"+retval.getGeometry());
        return retval;
    }

    /**
     * @param value
     *            The gml string of a geometry
     * @return instance of this class or null if there is no Node in the GML namespace
     * @throws Exception
     *             Thrown if GML String is not valid or GML tree could not be interpreted
     */
    public static GeometryAttribute getInstance(String value) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        // factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);

        // create a builder based on the factory & try to parse the GML String
        DocumentBuilder db = factory.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(value.getBytes()));

        // handle the policy, if it's a known type
        Element root = doc.getDocumentElement();

        return getInstance(root);

    }

    public String getSrsName() {
        return srsName;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public String getGid() {
        return gid;
    }

    public GMLVersion getGmlVersion() {
        return gmlVersion;
    }

    public String getGmlType() {
        return gmlType;
    }

    public int getSrsDimension() {
        return srsDimension;
    }

    public void setSrsDimension(int crsDimension) {
        this.srsDimension = crsDimension;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object) Must behave like the equals function in the
     * GeoXACML specification. Check for SRS and check the coordinates
     */
    public boolean equals(Object o) {
        if (!(o instanceof GeometryAttribute))
            return false;
        GeometryAttribute other = (GeometryAttribute) o;

        if (srsName == null && other.srsName != null)
            return false;
        if (srsName != null && srsName.equals(other.srsName) == false)
            return false;
        return geometry.equalsExact(other.geometry);
    }

    private int hashCodeForDouble(double value) {
        long v = Double.doubleToLongBits(value);
        return (int) (v ^ (v >>> 32));

    }

    /*
     * @see java.lang.Object#hashCode()
     * 
     * procues an hash value, the following rule is important if 2 instances of this class are
     * compared with equal and the result is true, the hasCodes must be identical
     */
    public int hashCode() {
        int hashValue = 1;
        if (srsName != null)
            hashValue = srsName.hashCode();

        Envelope env = geometry.getEnvelopeInternal();
        if (env.isNull())
            return hashValue;

        hashValue = hashValue * 31 + hashCodeForDouble(env.getMinX());
        hashValue = hashValue * 31 + hashCodeForDouble(env.getMinY());
        hashValue = hashValue * 31 + hashCodeForDouble(env.getMaxX());
        hashValue = hashValue * 31 + hashCodeForDouble(env.getMaxY());

        return hashValue;

    }

}
