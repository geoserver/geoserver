/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

public enum Format {
	
	KML("application/vnd.google-earth.kml"),
	X3D("model/x3d xml"),
	HTML("text/html"),
	OLE("application/ole");
	
	private final String mimeType;
	
	Format(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
	}
	
}
