/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import java.io.IOException;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Plugs into the GetCoverage request cycle and transforms a request for a single EO granule to one
 * against the coverage, but with the filter to limit it to the specified granule
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetCoverageEOCallback extends AbstractDispatcherCallback {

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory2();

    private EOCoverageResourceCodec codec;

    public GetCoverageEOCallback(EOCoverageResourceCodec codec) {
        this.codec = codec;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Object[] parameters = operation.getParameters();
        if (parameters != null
                && parameters.length > 0
                && parameters[0] instanceof GetCoverageType) {
            // check we are going against a granule
            GetCoverageType gc = (GetCoverageType) parameters[0];
            String coverageId = gc.getCoverageId();
            if (coverageId == null) {
                throw new WCS20Exception(
                        "Required parameter coverageId is missing",
                        WCS20Exception.WCS20ExceptionCode.MissingParameterValue,
                        "coverageId");
            }
            CoverageInfo coverage = codec.getGranuleCoverage(coverageId);
            if (coverage != null) {
                // set the actual coverage name
                String actualCoverageId = NCNameResourceCodec.encode(coverage);
                gc.setCoverageId(actualCoverageId);

                // extract the granule filter
                Filter granuleFilter = codec.getGranuleFilter(coverageId);

                // check the filter actually matches one granule
                if (!readerHasGranule(coverage, granuleFilter)) {
                    throw new WCS20Exception(
                            "Could not locate coverage " + coverageId,
                            WCS20ExceptionCode.NoSuchCoverage,
                            "coverageId");
                }

                // set and/or merge with the previous filter
                Filter previous = gc.getFilter();
                if (previous == null || previous == Filter.INCLUDE) {
                    gc.setFilter(granuleFilter);
                } else {
                    gc.setFilter(FF.and(previous, granuleFilter));
                }
            }
        }

        return operation;
    }

    private boolean readerHasGranule(CoverageInfo ci, Filter granuleFilter) {
        try {
            StructuredGridCoverage2DReader reader =
                    (StructuredGridCoverage2DReader) ci.getGridCoverageReader(null, null);
            String coverageName = codec.getCoverageName(ci);
            GranuleSource source = reader.getGranules(coverageName, true);
            return source.getCount(new Query(coverageName, granuleFilter)) > 0;
        } catch (IOException e) {
            throw new WCS20Exception(
                    "Could not determine if the coverage has the specified granule", e);
        }
    }
}
