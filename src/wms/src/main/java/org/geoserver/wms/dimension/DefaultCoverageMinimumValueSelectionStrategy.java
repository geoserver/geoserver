/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;

class DefaultCoverageMinimumValueSelectionStrategy extends AbstractCapabilitiesDefaultValueSelectionStrategy {
    /** serialVersionUID */
    private static final long serialVersionUID = -6119387494519619670L;

    /**
     * Default constructor.
     */
    public DefaultCoverageMinimumValueSelectionStrategy() {
    }

    @Override
    protected Object doGetDefaultValue(ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        Object retval = null;
        try {
            GridCoverage2DReader reader = (GridCoverage2DReader) ((CoverageInfo) resource)
                    .getGridCoverageReader(null, null);
            ReaderDimensionsAccessor dimAccessor = new ReaderDimensionsAccessor(reader);

            if (dimensionName.equals(ResourceInfo.TIME)) {
                retval = dimAccessor.getMinTime();
            } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
                retval = dimAccessor.getMinElevation();           
            } else if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)){
                String custDimName = dimensionName.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                // see if we have an optimize way to get the minimum
                String min = reader.getMetadataValue(custDimName.toUpperCase()
                        + "_DOMAIN_MINIMUM");
                if (min != null) {
                    retval = min;
                }
                else {                        

                // ok, get the full domain then
                List<String> domain = dimAccessor.getDomain(custDimName);

                if (domain.isEmpty()) {
                    retval = null;
                } else {
                    //Just a lexical (string) sort. 
                    //Should we be prepared for numeric and date values?
                    Collections.sort(domain);
                    retval = domain.get(0);
                }
                }
            }
        } catch (IOException e) {
            DimensionDefaultValueStrategyFactoryImpl.LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return retval;
    }        
    
}