/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Juha Hyvärinen / Cyberlightning Ltd
 * 
 */
package org.geoserver.w3ds.xml3d;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.geoserver.w3ds.types.Vector3;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.x3d.GeometryType;
import org.opengis.geometry.Envelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class XML3DGeometry {
private final static Logger LOGGER = Logging.getLogger(XML3DGeometry.class);

private List<Integer> indices = null;

private List<Vector3> vertices = null;

private List<XML3DNode> nodeList = null;

private HashMap<String, Integer> verticesHashMap;

private GeometryType geometryType;

private Format requestFormat;

private Double bbox[] = null;

public XML3DGeometry(Envelope bBox, GeometryType type, Format format) {
    verticesHashMap = new HashMap<String, Integer>(200000);
    vertices = new ArrayList<Vector3>();
    requestFormat = format;
    geometryType = type;

    bbox = new Double[4];
    bbox[0] = bBox.getLowerCorner().getCoordinate()[0]; // Min X
    bbox[1] = bBox.getLowerCorner().getCoordinate()[1]; // Min Z

    bbox[2] = bBox.getUpperCorner().getCoordinate()[0]; // Max X
    bbox[3] = bBox.getUpperCorner().getCoordinate()[1]; // Max Z
}

public void addGeometry(Geometry geometry) {
    if (requestFormat == Format.OCTET_STREAM) {
        List<Vector3> vertexList = filterCoordinates(geometry.getCoordinates());
        for (int i = 0; i < vertexList.size(); i++) {
            Vector3 vertex = vertexList.get(i);
            if (!vertices.contains(vertex)) {
                vertices.add(vertex);
            }
        }
    } else {
        if (geometry instanceof Polygon) {
            triangulateFromCoordinates(geometry.getCoordinates());
        } else if (geometry instanceof LineString || geometry instanceof MultiLineString) {
            lineFromCoordinates(geometry.getCoordinates());
        } else {
            int geometries = geometry.getNumGeometries();
    
            for (int i = 0; i < geometries; i++) {
                triangulateFromCoordinates(geometry.getGeometryN(i).getCoordinates());
            }
        }
    }
}

private void lineFromCoordinates(Coordinate[] coordinates) {
    int len = coordinates.length;
    for (int i = 0; i < len; i++) {
        if (coordinates[i].z == Double.NaN) {
            vertices.add(new Vector3(coordinates[i].x, 0.0, coordinates[i].y));
        } else {
            vertices.add(new Vector3(coordinates[i].x, coordinates[i].z, coordinates[i].y));
        }
    }
    // Create new group for each line
    if (nodeList == null) {
        nodeList = new ArrayList<XML3DNode>();
    }
    nodeList.add(createXML3DNode());
}

private void triangulateFromCoordinates(Coordinate[] coordinates) {
    if (indices == null) {
        indices = new ArrayList<Integer>();
    }

    // Filter out vertices which are outside of requested bounding box.
    List<Vector3> filteredArray = filterCoordinates(coordinates);

    if (filteredArray.size() < 3) {
        // Not enough valid vertices to generate triangles
        return;
    }
    // Split the geometry
    triangulateWithSplitting(filteredArray);
}

private void triangulateWithSplitting(List<Vector3> vertexList) {
    if (vertices.size() < 63000) {
        triangulate(vertexList);
    } else {
        if (nodeList == null) {
            nodeList = new ArrayList<XML3DNode>();
        }
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Add new node to splitted geometry: " + nodeList.size());

        nodeList.add(createXML3DNode());
        triangulate(vertexList);
    }
}

private void triangulate(List<Vector3> vertexList) {
    int counter = 0;
    int prevIndex = 0;
    
    int firstIndex;
    String vertexKey = vertexList.get(0).toString();
    if (verticesHashMap.containsKey(vertexKey)) {
        firstIndex = verticesHashMap.get(vertexKey);
    } else {
        firstIndex = vertices.size();
    }

    for (int i = 0; i < vertexList.size(); i++) {
        Vector3 vertex = vertexList.get(i);
        String vertexString = vertex.toString();

        if (counter > 2) {
            indices.add(prevIndex);
            counter = 1;
        }

        if (verticesHashMap.containsKey(vertexString)) {
            prevIndex = verticesHashMap.get(vertexString);
            indices.add(prevIndex);

        } else {
            prevIndex = vertices.size();
            indices.add(prevIndex);
            vertices.add(vertex);

            verticesHashMap.put(vertexString, prevIndex);
        }
        counter++;
    }
    
    if (counter < 3) {
        indices.add(firstIndex);
    }
}

private List<Double> createNormalsArray() {
    List<Double> vertexNormals = new ArrayList<Double>();

    for (int i = 0; i < vertices.size(); i++) {
        Vector3 normal = Vector3.Normalize(vertices.get(i).getNormal());

        vertexNormals.add(normal.x);
        vertexNormals.add(normal.y);
        vertexNormals.add(normal.z);
    }
    return vertexNormals;
}

private List<Vector3> filterCoordinates(Coordinate[] coordinates) {
    List<Vector3> filtered = new ArrayList<Vector3>();

    // First and last one are the same coordinates in the coordinates list
    for (int i = 0; i < coordinates.length; i++) {
        // Offset is needed because in the client side it is impossible to seamlessly clue
        // requested terrain planes together if returned plane doesn't match bounding box
        // or plane is larger than bounding box.
        int offset = 5;
        if (coordinates[i].x < bbox[0] - offset || coordinates[i].x > bbox[2] + offset) {
            continue;
        }
        if (coordinates[i].y < bbox[1] - offset || coordinates[i].y > bbox[3] + offset) {
            continue;
        }
        
        Vector3 vertex = new Vector3(coordinates[i].x, coordinates[i].z, coordinates[i].y);
        if (filtered.contains(vertex)) {
            continue;
        }
        filtered.add(vertex);
    }
    return filtered;
}

private Vector3 computeTriangleNormal(Vector3 v0, Vector3 v1, Vector3 v2) {
    Vector3 normal = Vector3.Cross(v2.Minus(v0), v1.Minus(v0));

    return normal;
}

private List<Double> computeVertexNormals() {
    Vector3 v0 = null;
    Vector3 v1 = null;
    Vector3 v2 = null;

    int len = indices.size();
    for (int i = 0; i < len; i += 3) {
        v0 = vertices.get(indices.get(i));
        v1 = vertices.get(indices.get(i + 1));
        v2 = vertices.get(indices.get(i + 2));

        Vector3 normal = computeTriangleNormal(v0, v1, v2);

        v0.addNormal(normal);
        v1.addNormal(normal);
        v2.addNormal(normal);
    }

    return createNormalsArray();
}

// Simple method to calculate the textures coordinates
private List<Double> computeTextureCoordinates() {
    List<Double> textureCoordinates = new ArrayList<Double>();

    double ls = bbox[2] - bbox[0];
    double lt = bbox[3] - bbox[1];

    // Compute texture coordinates per vertex and add value to the texture coordinate array.
    for (Vector3 vertex : this.vertices) {
        double s, t;
        s = (vertex.x - bbox[0]) / ls;
        t = (vertex.z - bbox[1]) / lt;

        textureCoordinates.add(s);
        textureCoordinates.add(t);

        // In XML3D -Z axis points forward, X points to right and Y is up vector.
        // -> transform Z after texture coordinates have been computed.

        // Move all vertices to a relative position starting from origo (0,0,0).
        // End point is (x, y, -z)
        vertex.x -= bbox[0];
        vertex.z = bbox[3] - (vertex.z - bbox[1]);
        vertex.z -= bbox[1];
    }
    return textureCoordinates;
}

private XML3DNode createXML3DNode() {
    XML3DNode rootNode = null;

    List<Double> vertexNormals = null;
    List<Double> textureCoordinates = null;
    if (geometryType == GeometryType.POLYGON) {
        vertexNormals = computeVertexNormals();
        textureCoordinates = computeTextureCoordinates();
    }

    // Formatter for double values, since we don't want more than 6 decimals
    DecimalFormat decimalFormat = new DecimalFormat("0.0#####");
    rootNode = new XML3DNode("mesh");

    // TODO: Change XML3DNode name and type once XML3D supports drawing of simple lines!
    // NOTE: At the moment XML3D doesn't support line drawing!
    if (geometryType == GeometryType.LINESTRING) {
        rootNode.addXML3DAttribute(new XML3DAttribute("type", "line"));
    } else {
        rootNode.addXML3DAttribute(new XML3DAttribute("type", "triangles"));
    }

    if (indices != null) {
        StringBuilder strBuilder = new StringBuilder();

        XML3DNode indicesNode = new XML3DNode("int");
        indicesNode.addXML3DAttribute(new XML3DAttribute("name", "index"));

        for (int i = 0; i < indices.size(); i++) {
            strBuilder.append(indices.get(i) + " ");
        }
        indicesNode.addNodeValues(strBuilder.toString());
        rootNode.addXML3DNode(indicesNode);

        indices.clear();
    }

    if (vertices != null) {
        StringBuilder strBuilder = new StringBuilder();

        XML3DNode verticesNode = new XML3DNode("float3");
        verticesNode.addXML3DAttribute(new XML3DAttribute("name", "position"));

        for (int i = 0; i < vertices.size(); i++) {
            strBuilder.append(vertices.get(i).toString() + " ");
        }
        verticesNode.addNodeValues(strBuilder.toString());
        rootNode.addXML3DNode(verticesNode);

        vertices.clear();
    }

    // Append normals to mesh node
    if (vertexNormals != null) {
        StringBuilder strBuilder = new StringBuilder();

        XML3DNode normalsNode = new XML3DNode("float3");
        normalsNode.addXML3DAttribute(new XML3DAttribute("name", "normal"));

        for (int i = 0; i < vertexNormals.size(); i++) {
            strBuilder.append(decimalFormat.format(vertexNormals.get(i)) + " ");
        }

        normalsNode.addNodeValues(strBuilder.toString());

        rootNode.addXML3DNode(normalsNode);
    }

    if (textureCoordinates != null) {
        StringBuilder strBuilder = new StringBuilder();

        XML3DNode texCoordsNode = new XML3DNode("float2");
        texCoordsNode.addXML3DAttribute(new XML3DAttribute("name", "texcoord"));

        for (int i = 0; i < textureCoordinates.size(); i++) {
            strBuilder.append(decimalFormat.format(textureCoordinates.get(i)) + " ");
        }

        texCoordsNode.addNodeValues(strBuilder.toString());
        rootNode.addXML3DNode(texCoordsNode);
    }

    verticesHashMap.clear();
    return rootNode;
}

private List<List<Vector3> > getVertexGrid() {
    // At the moment this works only for points that form uniform grid
    List<List<Vector3> > vertexGrid = new ArrayList<List<Vector3> >();

    for (int i = 0; i < vertices.size(); i++) {
        Vector3 vertex = vertices.get(i);
        
        // Find correct row for vertex and add it there.
        boolean found = false;
        for (int j = 0; j < vertexGrid.size(); j++) {
            // Add first vertex to empty row
            if (vertexGrid.get(j).size() == 0) {
                vertexGrid.get(j).add(vertex);
                found = true;
                break;
            } 
            Vector3 temp =vertexGrid.get(j).get(0); 
            if (temp.z - 0.01 < vertex.z && temp.z + 0.01 > vertex.z) {
                vertexGrid.get(j).add(vertex);
                found = true;
                break;
            }
        }
        
        // Row was not yet created
        if (!found) {
            vertexGrid.add(new ArrayList<Vector3>());
            vertexGrid.get(vertexGrid.size()-1).add(vertex);
        }
    }
    
    return sortVertexGrid(vertexGrid);
}

private List<List<Vector3> > sortVertexGrid(List<List<Vector3> > vertexGrid) {
    // Sort columns with insertion sort
    int size = vertexGrid.size();
    for (int i = 0; i < size; i++) {
        // Get row for sorting
        List<Vector3> row = vertexGrid.get(i);
        
        // Sort vertices in that row
        for (int j = 0; j < row.size(); j++) {
            Vector3 vertex = row.get(j);
            int k = j - 1;
            while (k >= 0 && row.get(k).x < vertex.x) {
                row.set(k+1, row.get(k));
                k = k-1;
            }
            row.set(k + 1, vertex);
        }
    }
    
    // Sort rows with insertion sort
    for (int i = 0; i < size; i++) {
        List<Vector3> row = vertexGrid.get(i);
        int j = i - 1;
        while (j >= 0 && vertexGrid.get(j).get(0).z < row.get(0).z) {
            vertexGrid.set(j+1, vertexGrid.get(j));
            j = j-1;
        }
        vertexGrid.set(j + 1, row);
    }
    return vertexGrid;
}

public byte[] toByteArray() {
    List<List<Vector3> > grid = getVertexGrid();
    
    int sizeX = grid.get(0).size();
    int sizeZ = grid.size();
    
    // Compute array resolution        
    float resolutionX = (float)(bbox[2] - bbox[0]) / sizeX;
    float resolutionZ = (float)(bbox[3] - bbox[1]) / sizeZ;       

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream out = null;
    byte outputByteArray[] = null;
    try {
        out = new DataOutputStream(bos);
        
        out.writeInt(sizeX);
        out.writeInt(sizeZ);
        
        out.writeFloat(resolutionX);
        out.writeFloat(resolutionZ);

        for (int i = 0; i < sizeZ; i++) {
            int len = grid.get(i).size()-1;
            for (int j = len; j >= 0; j--) {
                out.writeFloat((float)grid.get(i).get(j).y);
            }
        }
        out.flush();


        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Points in first row: " + sizeX + " Rows: " + sizeZ);
            LOGGER.fine("ByteArray resolution in x: " + resolutionX + " in y: " + resolutionZ + "\n");
        }

        outputByteArray = bos.toByteArray();

    } catch (IOException e) {
        // Ignore I/O error exception while writing stream header
    } finally {
        try {
            if (out != null) {
                out.close();
            }
            bos.close();
        } catch (IOException ex) {
            // Ignore close exception
        }
    }
    
    return outputByteArray;
}

public XML3DNode toXML3DNode() {
    XML3DNode outputNode;
    if (nodeList != null) {
        outputNode = new XML3DNode();
        for (int i = 0; i < nodeList.size(); i++) {
            outputNode.addXML3DNode(nodeList.get(i));
        }
        outputNode.addXML3DNode(createXML3DNode());
        return outputNode;
    }

    return createXML3DNode();
}

@Override
public String toString() {
    return toXML3DNode().toString();
}
}
