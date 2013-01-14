/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.util.ArrayList;
import java.util.List;

public class LODSet {

	private String identifier;
	private List<LOD> lodSet;
	
	public LODSet() {
		identifier = "";
		lodSet = new ArrayList<LOD>();
	}
	
	public LODSet(String identifier) {
		this.identifier = identifier;
		this.lodSet = new ArrayList<LOD>();
	}

	public String getIdentifier() {
		return identifier;
	}

	public List<LOD> getLodSet() {
		return lodSet;
	}
	
	public void addLOD(LOD lod) {
		lodSet.add(lod);
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
