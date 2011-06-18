/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event.impl;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;

public class CatalogEventImpl implements CatalogEvent {

	CatalogInfo source;
	
	
	public CatalogInfo getSource() {
		return source;
	}

	public void setSource(CatalogInfo source) {
		this.source = source;
	}
}
