/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1;

import org.geotools.xml.XSDParserDelegate;

public class WCSParserDelegate extends XSDParserDelegate {

	public WCSParserDelegate() {
		super(new WCSConfiguration());
	}

}
