/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

public enum Operation {
	
	GETCAPABILITIES("GetCapabilities"),
	GETSCENE("GetScene"),
	GETFEATUREINFO("GetFeatureInfo"),
	GETILE("GetTile");
	
	private final String name;
	
	Operation(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
