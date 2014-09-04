/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

/**
 * A provider that can parse an input in a lazy way, to allow the input parsing time (sometimes
 * significant) to be included as part of the overall execution time (and associated progress)
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
interface InputProvider {

    /**
     * Returns the value associated with this provider
     * @return
     * @throws Exception
     */
    public Object getValue() throws Exception;

    /**
     * Returns the input id for this value
     * @return
     */
    public String getInputId();

    /**
     * Returns true if the value has already been parsed 
     * @return
     */
    public boolean resolved();

    /**
     * Returns true if the parse can be a long operation
     * @return
     */
    boolean longParse();
}
