/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import org.opengis.util.ProgressListener;

/**
 * A provider that can parse an input in a lazy way, to allow the input parsing time (sometimes
 * significant) to be included as part of the overall execution time (and associated progress)
 *
 * @author Andrea Aime - GeoSolutions
 */
interface InputProvider {

    /** Returns the value associated with this provider */
    public Object getValue(ProgressListener subListener) throws Exception;

    /** Returns the input id for this value */
    public String getInputId();

    /** Returns true if the value has already been parsed */
    public boolean resolved();

    /**
     * Returns the number of "long" steps to be carried out in order to get this input. A long step
     * is either executing a sub-process, or having to fetch a remote data set
     */
    int longStepCount();
}
