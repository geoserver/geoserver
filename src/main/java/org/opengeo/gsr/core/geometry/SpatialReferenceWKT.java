/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * @author Brett Antonides - LMN Solutions
 * 
 */
public class SpatialReferenceWKT implements SpatialReference {
	private String wkt;

	public String getWkt() {
		return wkt;
	}

	public void setWkt(String wkt) {
		this.wkt = wkt;
	}
	
	public SpatialReferenceWKT(String wkt) {
		this.wkt = wkt;
	}
	
}
