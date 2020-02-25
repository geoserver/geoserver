/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

/**
 * Implementations return instances of the DimensionDefaultValueStrategy selected based on given
 * reference value and optionally with a fixed value used in the capabilities documents.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public interface NearestValueStrategyFactory {

    /**
     * Creates a strategy selecting the nearest domain value compared to the given object toMatch.
     */
    public DimensionDefaultValueSelectionStrategy createNearestValueStrategy(Object toMatch);

    /**
     * Creates a strategy selecting the nearest domain value compared to the given object toMatch.
     * Uses the given fixedCapabilitiesValue for presenting the default value in capabilities
     * documents.
     *
     * @param fixedCapabilitiesValue for example "current"
     */
    public DimensionDefaultValueSelectionStrategy createNearestValueStrategy(
            Object toMatch, String fixedCapabilitiesValue);
}
