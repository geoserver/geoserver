/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wms.dimension.AbstractDefaultValueSelectionStrategy;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Default implementation for selecting the default values for dimensions of coverage (raster)
 * resources using the minimum domain value strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class CoverageMinimumValueSelectionStrategyImpl
        extends AbstractDefaultValueSelectionStrategy {

    private static Logger LOGGER =
            Logging.getLogger(CoverageMinimumValueSelectionStrategyImpl.class);
    /** Default constructor. */
    public CoverageMinimumValueSelectionStrategyImpl() {}

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        Object retval = null;
        try {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader)
                            ((CoverageInfo) resource).getGridCoverageReader(null, null);
            ReaderDimensionsAccessor dimAccessor = new ReaderDimensionsAccessor(reader);

            if (dimensionName.equals(ResourceInfo.TIME)) {
                retval = dimAccessor.getMinTime();
            } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
                retval = dimAccessor.getMinElevation();
            } else if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                String custDimName =
                        dimensionName.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                // see if we have an optimize way to get the minimum
                String min = reader.getMetadataValue(custDimName.toUpperCase() + "_DOMAIN_MINIMUM");
                if (min != null) {
                    retval = min;
                } else {

                    // ok, get the full domain then
                    List<String> domain = dimAccessor.getDomain(custDimName);

                    if (domain.isEmpty()) {
                        retval = null;
                    } else {
                        // Just a lexical (string) sort.
                        // Should we be prepared for numeric and date values?
                        Collections.sort(domain);
                        retval = domain.get(0);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return Converters.convert(retval, clz);
    }
}
