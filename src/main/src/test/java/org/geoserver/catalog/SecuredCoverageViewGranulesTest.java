/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.StructuredCoverageViewReader.GranuleStoreView;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredStructuredGridCoverage2DReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Checks the granule read filter survives the trip through a coverage view, whose granule source renames the feature
 * type and the feature ids but not the attributes.
 */
public class SecuredCoverageViewGranulesTest {

    private static final String VIEW = "view";

    private static final String BAND_COVERAGE = "gray";

    private static final String TIME_FILTER = "time AFTER 2020-01-01T00:00:00Z";

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private GranuleSource bandGranules;

    private Filter readFilter;

    /** Secured reader over a delegate handing back the real coverage view granule source. */
    private SecuredStructuredGridCoverage2DReader secured;

    @Before
    public void setUp() throws Exception {
        readFilter = ECQL.toFilter("elevation < 100");

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(BAND_COVERAGE);
        tb.add("location", String.class);
        SimpleFeatureType granuleSchema = tb.buildFeatureType();

        bandGranules = mock(GranuleSource.class);
        when(bandGranules.getGranules(any())).thenReturn(new ListFeatureCollection(granuleSchema));

        StructuredGridCoverage2DReader bandReader = mock(StructuredGridCoverage2DReader.class);
        when(bandReader.getGranules(any(), anyBoolean())).thenReturn(bandGranules);

        CoverageBand band = new CoverageBand(List.of(new InputCoverageBand(BAND_COVERAGE, "0")), BAND_COVERAGE, 0);
        CoverageView coverageView = new CoverageView(VIEW, List.of(band));

        StructuredGridCoverage2DReader viewReader = mock(StructuredGridCoverage2DReader.class);
        when(viewReader.getGranules(any(), anyBoolean()))
                .thenReturn(new GranuleStoreView(bandReader, VIEW, coverageView, true));

        CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.HIDE, readFilter, null, null);
        secured = new SecuredStructuredGridCoverage2DReader(viewReader, WrapperPolicy.readOnlyHide(limits));
    }

    /** Attributes are not renamed by the view, so a filter on the granule attributes reaches the band untouched. */
    @Test
    public void testReadFilterReachesTheBandSource() throws Exception {
        secured.getGranules(VIEW, true).getGranules(new Query(VIEW, ECQL.toFilter(TIME_FILTER)));

        Query pushedDown = capturedBandQuery();
        assertEquals(ECQL.toFilter(TIME_FILTER + " AND elevation < 100"), pushedDown.getFilter());
        assertEquals(BAND_COVERAGE, pushedDown.getTypeName());
    }

    /** The view prefixes granule ids with its own name, so the security conjunction has to survive the unmapping. */
    @Test
    public void testViewPrefixedIdsUnmappedAroundTheReadFilter() throws Exception {
        Filter byId = FF.id(FF.featureId(VIEW + "." + BAND_COVERAGE + ".1"));
        secured.getGranules(VIEW, true).getGranules(new Query(VIEW, byId));

        Filter expected = FF.and(FF.id(FF.featureId(BAND_COVERAGE + ".1")), readFilter);
        assertEquals(expected, capturedBandQuery().getFilter());
    }

    private Query capturedBandQuery() throws Exception {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(bandGranules).getGranules(captor.capture());
        return captor.getValue();
    }
}
