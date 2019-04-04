/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.List;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Utilities;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to retrieve a grid coverage band min/max value
 *
 * @author Andrea Aime, GeoSolutions SAS
 */
public class FilterFunction_bandStats extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "bandStats",
                    parameter("value", Number.class),
                    parameter("bandIndex", Number.class),
                    parameter("property", String.class));

    public FilterFunction_bandStats() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        try {
            Integer bandIndex = (getExpression(0).evaluate(feature, Integer.class));
            String propertyName = (getExpression(1).evaluate(feature, String.class));
            Object val = null;
            if (feature instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) feature;
                val = evaluate(coverage, bandIndex, propertyName);
            }
            if (val != null) {
                return val;
            }
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats: Unable to find the stat "
                            + propertyName
                            + " from the input object of type "
                            + feature.getClass());
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats", e);
        }
    }

    Object evaluate(final GridCoverage2D coverage, final int bandIndex, final String statName) {
        Utilities.ensureNonNull("coverage", coverage);
        GridSampleDimension sd = coverage.getSampleDimension(bandIndex);
        if ("minimum".equalsIgnoreCase(statName)) {
            return ensureNotNull(sd, bandIndex, statName, getMinimum(sd));
        } else if ("maximum".equalsIgnoreCase(statName)) {
            return ensureNotNull(sd, bandIndex, statName, getMaximum(sd));
        } else {
            throw new IllegalArgumentException(
                    "Invalid property "
                            + statName
                            + ", supported values are 'minimum' and 'maximum'");
        }
    }

    private double ensureNotNull(
            GridSampleDimension sd, int bandIndex, String statName, Double value) {
        if (value != null) {
            return value;
        } else {
            throw new RuntimeException(
                    "Could not find the " + statName + " from " + sd + " of band " + bandIndex);
        }
    }

    private Double getMaximum(GridSampleDimension sd) {
        for (Category cat : sd.getCategories()) {
            final double result = cat.getRange().getMaximum();
            if (!Category.NODATA.getName().equals(cat.getName()) && !Double.isNaN(result)) {
                return result;
            }
        }

        return null;
    }

    private Double getMinimum(GridSampleDimension sd) {
        final List<Category> categories = sd.getCategories();
        for (int i = categories.size() - 1; i >= 0; i--) {
            Category cat = categories.get(i);
            final double result = cat.getRange().getMinimum();
            if (!Category.NODATA.getName().equals(cat.getName()) && !Double.isNaN(result)) {
                return result;
            }
        }

        return null;
    }
}
