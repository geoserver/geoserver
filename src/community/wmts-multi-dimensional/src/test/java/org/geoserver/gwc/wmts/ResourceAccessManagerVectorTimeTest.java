/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceAccessManagerVectorTimeTest extends VectorTimeTestSupport {

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_high", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_low", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // ------

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo vector = catalog.getFeatureTypeByName(getLayerId(VECTOR_ELEVATION));

        // Give cite_high only records with higher elevation
        Filter elevationHigh = ff.greater(ff.property("startElevation"), ff.literal(2));
        tam.putLimits(
                "cite_high",
                vector,
                new VectorAccessLimits(CatalogMode.HIDE, null, elevationHigh, null, null));

        // And cite_low the opposite
        Filter elevationLow = ff.lessOrEqual(ff.property("startElevation"), ff.literal(2));
        tam.putLimits(
                "cite_low",
                vector,
                new VectorAccessLimits(CatalogMode.HIDE, null, elevationLow, null, null));
    }

    @Test
    public void testGetDomainsValuesCiteHigh() throws Exception {
        login("cite_high", "cite_high");
        // high elevations are associated only to the 11th (start date)
        testDomainsValuesRepresentation(DimensionsUtils.NO_LIMIT, "2012-02-11T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-11T00:00:00.000Z");
    }

    @Test
    public void testGetDomainsValuesCiteLow() throws Exception {
        login("cite_low", "cite_low");
        // high elevations are associated only to both start dates
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, "2012-02-11T00:00:00.000Z", "2012-02-12T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z");
    }

    @Test
    public void testGetHistogramHigh() {
        login("cite_high", "cite_high");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1D"));
        // both in the same day
        assertThat(histogram.second, equalTo(Arrays.asList(2)));
    }

    @Test
    public void testGetHistogramLow() {
        login("cite_low", "cite_low");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z/P1D"));
        // one per day
        assertThat(histogram.second, equalTo(Arrays.asList(1, 1)));
    }
}
