/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

public interface RegionatingStrategyFactory {
    /**
     * Based on a string identifying a strategy, can this factory produce a suitable strategy
     * implementation?
     *
     * @param strategyName the name of the desired strategy, such as 'geo' or 'sld'
     * @return true iff this factory can produce a suitable strategy implementation
     */
    public boolean canHandle(String strategyName);

    /** Get a string for which this.canHandle will return true. */
    public String getName();

    /**
     * Create a strategy to handle the request.
     *
     * @return the RegionatingStrategy to handle the request.
     */
    public RegionatingStrategy createStrategy();
}
