/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand;
import it.geosolutions.imageio.pam.PAMParser;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.util.Utilities;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to retrieve a grid coverage Stat value from the underlying GridCoverage2D
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class FilterFunction_gridCoverageStats extends FunctionExpressionImpl {

    PAMParser pamParser = PAMParser.getInstance();

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "gridCoverageStats",
                    parameter("value", Number.class),
                    parameter("property", String.class));

    public FilterFunction_gridCoverageStats() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String arg0;

        try { // attempt to get value and perform conversion
            arg0 = (getExpression(0).evaluate(feature, String.class));
            Object val = null;
            if (feature instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) feature;
                val = evaluate(coverage, arg0);
            }
            if (val != null) {
                return val;
            }
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats: Unable to find the stat "
                            + arg0
                            + " from the input object of type "
                            + feature.getClass());
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats", e);
        }
    }

    /**
     * Evaluating the filter function based on the provided coverage and the requested statName
     * (minimum, maximum, ...)
     */
    public Object evaluate(final GridCoverage2D coverage, final String statName) {
        Utilities.ensureNonNull("coverage", coverage);
        final Object prop = coverage.getProperty(Utils.PAM_DATASET);
        if (prop != null && prop instanceof PAMDataset) {
            final PAMDataset dataset = (PAMDataset) prop;
            // Need to play with channel selection to deal with different raster bands
            final PAMRasterBand band = dataset.getPAMRasterBand().get(0);
            if (band != null) {
                final String value =
                        pamParser.getMetadataValue(band, "STATISTICS_" + statName.toUpperCase());
                return Double.parseDouble(value);
            }
        }
        return null;
    }
}
