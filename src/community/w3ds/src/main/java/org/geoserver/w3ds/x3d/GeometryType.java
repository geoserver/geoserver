/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

public enum GeometryType {
	
	POINT("Point", 0), 
	LINESTRING("LineString", 1),
	POLYGON("Polygon", 2),
	MULTIPOINT("MultiPoint", 3),
	MULTILINESTRING("MultiLineString", 4),
	MULTIPOLYGON("MultiPolygon", 5),
	MULTIGEOMETRY("MultiGeometry", 6);

	private final String geometryType;
	private final int code;

	GeometryType(String geometryType, int code) {
		this.geometryType = geometryType;
		this.code = code;
	}

	public String getGeometryType() {
		return geometryType;
	}
	
	public int getCode() {
		return code;
	}
}
