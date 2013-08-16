/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;


public class LOD {
	
	private String title;
	private String abstractTxt;
	private String identifier;
	private int lodValue;
	private float defaultRange;
	
	public LOD() {
		title = "";
		abstractTxt = "";
		identifier = "";
		lodValue = -1;
		defaultRange = -1;
	}
	
	public LOD(String title, String abstractTxt, String identifier,
			int lodValue, float defaultRange) {
		this.title = title;
		this.abstractTxt = abstractTxt;
		this.identifier = identifier;
		this.lodValue = lodValue;
		this.defaultRange = defaultRange;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAbstractTxt() {
		return abstractTxt;
	}
	public void setAbstractTxt(String abstractTxt) {
		this.abstractTxt = abstractTxt;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public int getLodValue() {
		return lodValue;
	}
	public void setLodValue(int lodValue) {
		this.lodValue = lodValue;
	}
	public float getDefaultRange() {
		return defaultRange;
	}
	public void setDefaultRange(float defaultRange) {
		this.defaultRange = defaultRange;
	}
	
	
	
}
