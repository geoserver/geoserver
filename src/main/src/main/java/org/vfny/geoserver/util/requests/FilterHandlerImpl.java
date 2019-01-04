/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests;

import java.util.logging.Logger;
import org.geotools.filter.FilterHandler;
import org.opengis.filter.Filter;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Minimal class to implement the FilterHandler interface.
 *
 * @author Rob Hranac, TOPP
 * @version $Id$
 */
public class FilterHandlerImpl extends XMLFilterImpl implements ContentHandler, FilterHandler {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests");

    /** Tracks current filter */
    private Filter currentFilter = null;

    /** Empty constructor. */
    public FilterHandlerImpl() {
        super();
    }

    /**
     * Recieves the filter from the filter parsing children.
     *
     * @param filter (OGC WFS) Filter from (SAX) filter..
     */
    public void filter(Filter filter) {
        LOGGER.finest("found filter: " + filter.toString());
        currentFilter = filter;
    }

    /**
     * Gives filter to whoever wants it.
     *
     * @return (OGC WFS) Filter from (SAX) filter..
     */
    public Filter getFilter() {
        return currentFilter;
    }
}
