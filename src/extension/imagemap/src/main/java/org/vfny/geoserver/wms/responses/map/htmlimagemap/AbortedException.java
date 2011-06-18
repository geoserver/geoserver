/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;


/**
 * DOCUMENT ME!
 *
 * @author Mauro Bartolomeoli 
 */
public class AbortedException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 225177065602384118L;

	public AbortedException(String msg) {
        super(msg);
    }
}
