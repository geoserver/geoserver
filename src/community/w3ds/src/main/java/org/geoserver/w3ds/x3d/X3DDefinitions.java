/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

public enum X3DDefinitions {
	
	DEFAULT_STYLE("DEFAULT_STYLE", 0),
	DEFAULT_MATERIAL("DEFAULT_MATERIAL", 1), 
	GEO_ORIGIN("GEO_ORIGIN", 2),
	GEOGRAPHIC_DEGREES("DEGREES", 3),
	GEOGRAPHIC_METRIC("METRIC", 4);

	private final String definition;
	private final int code;

	X3DDefinitions(String definition, int code) {
		this.definition = definition;
		this.code = code;
	}

	public String getDefinition() {
		return definition;
	}
	
	public int getCode() {
		return code;
	}
}
