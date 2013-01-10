/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */
package org.geoserver.w3ds.x3d;

public enum X3DGeometryType {
	
	POINTS("points", 0), LINES("lines", 1), POLYGONS("polygons", 2), TIN("tin", 3), TRIANGLES(
			"triangles", 4);

	private final String geometryType;
	private final int code;

	X3DGeometryType(String geometryType, int code) {
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
