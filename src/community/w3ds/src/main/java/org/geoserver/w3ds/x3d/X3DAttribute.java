/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.x3d;

public class X3DAttribute {

	private String attribute;
	private String value;
	
	private boolean valid;

	public X3DAttribute(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
		this.valid = true;
	}
	
	public X3DAttribute(X3DAttribute x3dAttribute) {
		this.attribute = x3dAttribute.getAttribute();
		this.value = x3dAttribute.getValue();
		this.valid = x3dAttribute.isValid();
	}
	
	public boolean isValid() {
		return this.valid;
	}
	
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if (!this.valid) {
			return "";
		}
		return attribute + "='" + value + "'";
		//return attribute + "=\"" + value + "\"";
	}
	
	public X3DAttribute clone() {
		return new X3DAttribute(this);
	}
}
