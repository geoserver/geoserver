/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */
package org.geoserver.w3ds.x3d;

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class X3DLines extends X3DGeometry {

	public X3DLines(X3DGeometryType type, X3DAttribute geoSystem) {
		super(type, geoSystem);
	}

	public X3DLines(X3DGeometryType type, X3DAttribute geoSystem,
			int coordinatesType) {
		super(type, geoSystem, coordinatesType);
	}

	@Override
	public X3DNode getX3D() {
		if (coordinatesType == X3DDefinitions.GEOGRAPHIC_DEGREES.getCode()) {
			return getX3D_degree();
		} else {
			return getX3D_metric();
		}
	}

	private X3DNode getX3D_degree() {
		X3DNode shape = new X3DNode("Shape");
		if(this.appearance.isValid()) {
			shape.addX3DNode(this.appearance);
		}
		X3DNode indexedLineSet = new X3DNode("IndexedLineSet");
		X3DNode geoOrigin = new X3DNode("geoOrigin");
		geoOrigin.addX3DAttribute("USE", X3DDefinitions.GEO_ORIGIN.getDefinition());
		geoOrigin.setExpand(true);
		X3DNode coordinate = new X3DNode("GeoCoordinate");
		coordinate.addX3DAttribute("point", this.getPoints());
		coordinate.addX3DAttribute(this.geoSystem);
		coordinate.addX3DNode(geoOrigin);
		indexedLineSet.addX3DNode(coordinate);
		indexedLineSet.addX3DAttribute(new X3DAttribute("coordIndex", this.getIndexis()));
		
		shape.addX3DNode(indexedLineSet);
		return shape;
	}

	private X3DNode getX3D_metric() {
		X3DNode shape = new X3DNode("Shape");
		if(this.appearance.isValid()) {
			/*X3DNode aux = this.appearance.clone();
		    X3DNode line_propreties = new X3DNode("LineProperties");
			line_propreties.addX3DAttribute("linewidthScaleFactor", "6");
			aux.addX3DNode(line_propreties);
			shape.addX3DNode(aux);*/
			shape.addX3DNode(this.appearance);
		}
		X3DNode indexedLineSet = new X3DNode("IndexedLineSet");
		X3DNode coordinate = new X3DNode("Coordinate");
		coordinate.addX3DAttribute("point", this.getPoints());
		indexedLineSet.addX3DNode(coordinate);
		indexedLineSet.addX3DAttribute(new X3DAttribute("coordIndex", this
				.getIndexis()));
		shape.addX3DNode(indexedLineSet);
		return shape;
	}

	public void addLineString(LineString geometry) throws IOException {
		Coordinate[] c = geometry.getCoordinates();
		add(geometry.getCoordinates());
	}

}
