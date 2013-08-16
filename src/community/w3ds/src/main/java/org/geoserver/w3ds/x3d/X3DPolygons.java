/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;


public class X3DPolygons extends X3DGeometry {

	public X3DPolygons(X3DGeometryType type, X3DAttribute geoSystem) {
		super(type, geoSystem);
	}
	
	public X3DPolygons(X3DGeometryType type, X3DAttribute geoSystem, int coordinatesType) {
		super(type, geoSystem, coordinatesType);
	}

	@Override
	public X3DNode getX3D() {
		if(coordinatesType == X3DDefinitions.GEOGRAPHIC_DEGREES.getCode()) {
			return getX3D_degree();
		}
		else {
			return getX3D_metric();
		}
	}
	
	private X3DNode getX3D_degree() {
		X3DNode shape = new X3DNode("Shape");
		// DEBUG
		
		if(this.appearance.isValid()) {
			shape.addX3DNode(this.appearance);
		}
		
		// DEBUG
		X3DNode indexedFaceSet = new X3DNode("IndexedFaceSet");
		X3DNode geoOrigin = new X3DNode("geoOrigin");
		geoOrigin.addX3DAttribute("USE", X3DDefinitions.GEO_ORIGIN.getDefinition());
		//geoOrigin.addX3DAttribute(new X3DAttribute("geoSystem", "\"GD\" \"WE\" \"longitude_first\""));
		//geoOrigin.addX3DAttribute("geoCoords", "-8.6079026220914 41.533268476858 5.0");
		geoOrigin.setExpand(true);
		X3DNode coordinate = new X3DNode("GeoCoordinate");
		coordinate.addX3DAttribute("point", this.getPoints());
		coordinate.addX3DAttribute(this.geoSystem);
		coordinate.addX3DNode(geoOrigin);
		indexedFaceSet.addX3DNode(coordinate);
		indexedFaceSet.addX3DAttribute(new X3DAttribute("coordIndex", this.getIndexis()));
		indexedFaceSet.addX3DAttribute(new X3DAttribute("solid", "false"));
		indexedFaceSet.addX3DAttribute(new X3DAttribute("convex", "false"));
		//indexedFaceSet.addX3DAttribute("creaseAngle", "1");
		
		if(this.appearance.isValid()) {
			if(this.appearance.haveChild("ImageTexture")) {
				indexedFaceSet.addX3DNode(calculateTextureCoordinates());
				indexedFaceSet.addX3DAttribute("texCoordIndex", this.getIndexis());
			}
		}
		
		//indexedFaceSet.addX3DAttribute("normalPerVertex", "true");
		
		shape.addX3DNode(indexedFaceSet);
		return shape;
	}
	
	private X3DNode getX3D_metric() {
		X3DNode shape = new X3DNode("Shape"); 
		// DEBUG
		
		if(this.appearance.isValid()) {
			shape.addX3DNode(this.appearance);
		}
		
		// DEBUG
		X3DNode indexedFaceSet = new X3DNode("IndexedFaceSet");
		X3DNode coordinate = new X3DNode("Coordinate");
		coordinate.addX3DAttribute("point", this.getPoints());
		indexedFaceSet.addX3DNode(coordinate);
		indexedFaceSet.addX3DAttribute(new X3DAttribute("coordIndex", this.getIndexis()));
		indexedFaceSet.addX3DAttribute(new X3DAttribute("solid", "false"));
		indexedFaceSet.addX3DAttribute(new X3DAttribute("convex", "false"));
		indexedFaceSet.addX3DAttribute("creaseAngle", "0.3");
		
		if(this.appearance.isValid()) {
			if(this.appearance.haveChild("ImageTexture")) {
				indexedFaceSet.addX3DNode(calculateTextureCoordinates());
				indexedFaceSet.addX3DAttribute("texCoordIndex", this.getIndexis());
			}
		}
		
		//indexedFaceSet.addX3DAttribute("normalPerVertex", "true"); 
		
		shape.addX3DNode(indexedFaceSet);
		return shape;
	}
	
	// Very stupid method to calculate the textures coodinates
	private X3DNode calculateTextureCoordinates() {
		double bbox[] = new double[4];
		bbox = calculateBbox(this.coordinates);
		double lx = bbox[2] - bbox[0];
		double ly = bbox[3] - bbox[1];
		StringBuilder strb = new StringBuilder();
		for(Coordinate c : this.coordinates) {
			double s; 
			if(c.x == bbox[0]) {
				s = 0;
			}
			else {
				s = (c.x - bbox[0]) / lx;
			}
			double t; 
			if(c.y == bbox[1]) {
				t = 0;
			}
			else {
				t = (c.y - bbox[1]) / ly;
			}
			strb.append(s + " " + t + " ");
		}
		X3DNode textureCoordinate = new X3DNode("TextureCoordinate");
		textureCoordinate.addX3DAttribute("point", strb.toString());
		return textureCoordinate;
	}

	public void addPolygon(Polygon geometry) throws IOException {
		Coordinate[] c = geometry.getCoordinates();
		add(geometry.getCoordinates());
	}
}
