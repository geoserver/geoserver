/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Juha Hyvärinen / Cyberlightning Ltd
 */

package org.geoserver.w3ds.xml3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.Envelope;
import org.geoserver.w3ds.x3d.GeometryType;
import org.geoserver.w3ds.xml3d.XML3DNode;
import org.geoserver.w3ds.utilities.Format;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class XML3DBuilder {
private final static Logger LOGGER = Logging.getLogger(XML3DBuilder.class);

private BufferedWriter writer = null;

private OutputStream outputStream = null;

private Envelope boundingBox = null;

private XML3DNode xml3dRootNode = null;

private XML3DNode points = null;

private Format requestFormat = null;

private GeometryType geometryType = null;

private XML3DGeometry outputObject = null;

private Long startTime;

public XML3DBuilder(Envelope bbox, OutputStream output, Format format) {
    boundingBox = bbox;
    outputStream = output;
    requestFormat = format;

    writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    xml3dRootNode = new XML3DNode();
}

public boolean setLayerAttribute(String name, String value) {
    if (!value.isEmpty()) {
        xml3dRootNode.addXML3DAttribute(new XML3DAttribute(name, value));
        return true;
    }

    return false;
}

private static String getObjectID(W3DSLayer layer, Feature feature) {
    if (!layer.getHasObjectID()) {
        return "unknown";
    }
    String id = layer.getObjectID();
    Object o = feature.getProperty(id).getValue();
    if (o != null) {
        return feature.getProperty(id).getValue().toString();
    }
    return "unknown";
}

private static String getObjectClass(W3DSLayer layer, Feature feature) {
    if (!layer.getHasObjectClass()) {
        return "";
    }
    StringBuilder strb = new StringBuilder();
    for (String cn : layer.getObjectClass()) {
        Object o = feature.getProperty(cn).getValue();
        if (o != null) {
            strb.append(feature.getProperty(cn).getValue().toString() + " ");
        }
    }
    return strb.toString();
}

private XML3DNode newObject(String id, String className) {
    XML3DNode node = new XML3DNode("group");
    xml3dRootNode.addXML3DNode(node);

    node.addXML3DAttribute(new XML3DAttribute("id", id));
    node.addXML3DAttribute(new XML3DAttribute("class", className));

    return node;
}

public void addGeometry(Geometry geometry, String id, String mesh_ref, String className)
        throws IOException {

    if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
        // Initialize output geometry object.
        if (outputObject == null) {
            geometryType = GeometryType.POLYGON;
            outputObject = new XML3DGeometry(boundingBox, geometryType, requestFormat);
        }
        outputObject.addGeometry(geometry);

    } else if (geometry instanceof LineString || geometry instanceof MultiLineString) {
        if (outputObject == null) {
            geometryType = GeometryType.LINESTRING;
            outputObject = new XML3DGeometry(boundingBox, geometryType, requestFormat);
        }
        outputObject.addGeometry(geometry);

    } else if (geometry instanceof Point) {
        geometryType = GeometryType.POINT;
        if (points == null) {
            points = new XML3DNode("objects");
            xml3dRootNode.addXML3DNode(points);
        }
        if (mesh_ref != null) {
            // Create new XML3DNode and add it to the XML3D points node
            XML3DNode point = new XML3DNode("group");
            points.addXML3DNode(point);

            point.addXML3DAttribute(new XML3DAttribute("id", id));
            Coordinate coordinate = geometry.getCoordinate();

            // Transform coordinates to relative coordinates which are calculated from request
            // bounding box.
            double minX = boundingBox.getLowerCorner().getCoordinate()[0]; // Min X
            double minY = boundingBox.getLowerCorner().getCoordinate()[1]; // Min Y

            point.addXML3DAttribute(new XML3DAttribute("translation", String.valueOf(coordinate.x
                    - minX)
                    + " "
                    + String.valueOf(coordinate.z)
                    + " "
                    + String.valueOf(coordinate.y - minY)));

            XML3DNode node = new XML3DNode("mesh");
            XML3DAttribute mesh_src = new XML3DAttribute("src", mesh_ref);
            node.addXML3DAttribute(mesh_src);

            point.addXML3DNode(node);
        } else {
            // XML3D doesn't support drawing of points and therefore point geometry should be used
            // only for external mesh references
            // TODO: implement this case when support is added to XML3D
        }
    } else {
        throw new IllegalArgumentException("Unable to determine geometry type "
                + geometry.getClass());
    }

}

public void addW3DSLayer(W3DSLayer layer) {
    startTime = System.currentTimeMillis();
    FeatureCollection<?, ?> collection = layer.getFeatures();

    FeatureIterator<?> iterator = collection.features();

    try {
        SimpleFeature feature;
        SimpleFeatureType fType;
        List<AttributeDescriptor> types;
        while (iterator.hasNext()) {
            feature = (SimpleFeature) iterator.next();
            fType = feature.getFeatureType();
            types = fType.getAttributeDescriptors();

            Geometry geometry = null;
            String id = null;
            String classname = null;
            String mesh_ref = null;

            for (int j = 0; j < types.size(); j++) {
                Object value = feature.getAttribute(j);

                if (value != null) {
                    if (types.get(j).getLocalName().equalsIgnoreCase("mesh_ref")) {
                        mesh_ref = (String) value;
                    }

                    if (value instanceof Geometry) {
                        id = layer.getLayerInfo().getRequestName() + ":"
                                + getObjectID(layer, feature);
                        classname = getObjectClass(layer, feature);
                        geometry = (Geometry) value;
                    }
                }
            }
            addGeometry(geometry, id, mesh_ref, classname);
        }
        iterator.close();
        iterator = null;

    } catch (Exception exception) {
        ServiceException serviceException = new ServiceException("Error: " + exception.getMessage());
        serviceException.initCause(exception);
        throw serviceException;
    } finally {
        if (iterator != null) {
            iterator.close();
            iterator = null;
        }
    }

}

private void finalizeGeometry() {
    // Shader should be added by client software,
    // since server can't know what user wants to do with generated object.

    if (outputObject != null) {
        // Create new XML3DNode and add it to the XML3D root node
        XML3DNode activeObject = newObject(geometryType.toString(), "NodeClassName");
        activeObject.addXML3DNode(outputObject.toXML3DNode());
    }
}

private void writeXML3D() {
    finalizeGeometry();
    try {
        writer.write(xml3dRootNode.toString());
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void writeHTML() {
    finalizeGeometry();
    try {
        writer.write(xml3dRootNode.toHtml());
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/*
 * Write byte array with size information of planar component, resolution
 * and elevation data for each point in that planar.
 */
private void writeOctetStream() {
    try {
        if (outputObject != null) {
            outputStream.write(outputObject.toByteArray());
        }
        outputStream.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public void writeOutput() {
    if (requestFormat == Format.XML3D) {
        writeXML3D();
    } else if (requestFormat == Format.HTML_XML3D) {
        writeHTML();
    } else if (requestFormat == Format.OCTET_STREAM) {
        writeOctetStream();
    }

    if (LOGGER.isLoggable(Level.INFO))
        LOGGER.info("XML3D request handling took: " + (System.currentTimeMillis() - startTime)
                + "ms");
}

public void close() throws IOException {
    writer.flush();
    writer.close();
}

}
