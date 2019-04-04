/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

/** @author Mauro Bartolomeoli */
public class AbortedException extends Exception {
    /** */
    private static final long serialVersionUID = 225177065602384118L;

    public AbortedException(String msg) {
        super(msg);
    }
}
