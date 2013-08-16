/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;

public class W3DSLayerInfo {
	private LayerInfo layerInfo;
	private String requestName;
	private StyleInfo requestStyle;
	
	public W3DSLayerInfo(LayerInfo layerInfo, String requestName) {
		super();
		this.layerInfo = layerInfo;
		this.requestName = requestName;
		this.requestStyle = null;
	}
	
	public boolean haveRequestStyle() {
		return requestStyle != null;
	}

	public StyleInfo getRequestStyle() {
		return requestStyle;
	}

	public void setRequestStyle(StyleInfo requestStyle) {
		this.requestStyle = requestStyle;
	}

	public LayerInfo getLayerInfo() {
		return layerInfo;
	}

	public void setLayerInfo(LayerInfo layerInfo) {
		this.layerInfo = layerInfo;
	}

	public String getRequestName() {
		return requestName;
	}

	public void setRequestName(String requestName) {
		this.requestName = requestName;
	}
	
}
