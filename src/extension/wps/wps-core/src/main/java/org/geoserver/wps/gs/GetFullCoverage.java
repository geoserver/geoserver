/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * A process that returns a coverage fully (something which is un-necessarily hard in WCS)
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
@DescribeProcess(
    title = "GetFullCoverage",
    description = "Returns a raster from the catalog, with optional filtering"
)
public class GetFullCoverage implements GeoServerProcess {

    private Catalog catalog;

    public GetFullCoverage(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "result", description = "Output raster", type = GridCoverage2D.class)
    public GridCoverage2D execute(
            @DescribeParameter(
                        name = "name",
                        description = "Name of raster, optionally fully qualified (workspace:name)"
                    )
                    String name,
            @DescribeParameter(
                        name = "filter",
                        description = "Filter to use on the raster data",
                        min = 0
                    )
                    Filter filter)
            throws IOException {
        CoverageInfo ci = catalog.getCoverageByName(name);
        if (ci == null) {
            throw new WPSException("Could not find coverage " + name);
        }

        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        final List<GeneralParameterDescriptor> parameterDescriptors =
                readParametersDescriptor.getDescriptor().descriptors();
        GeneralParameterValue[] params = new GeneralParameterValue[0];
        if (filter != null) {
            params =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors, params, filter, "FILTER", "Filter");
        }

        return (GridCoverage2D) reader.read(params);
    }
}
