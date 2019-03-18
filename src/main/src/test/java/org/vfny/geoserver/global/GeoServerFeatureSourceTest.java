/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.data.Query;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class GeoServerFeatureSourceTest {

    private PropertyDataStore pds;

    @Before
    public void setup() {
        pds = new PropertyDataStore(new File("src/test/java/org/geoserver/data/test"));
    }

    @After
    public void cleanup() {
        EnvFunction.clearLocalValues();
    }

    @Test
    public void testEnvVarExpansionDefaultInclude() throws Exception {
        checkRoadSegmentsDefinitionQuery(
                ECQL.toFilter("FID > env('threshold', 100)"),
                ECQL.toFilter("FID > 100"),
                Filter.INCLUDE);
    }

    @Test
    public void testEnvVarExpansionDefault() throws Exception {
        checkRoadSegmentsDefinitionQuery(
                ECQL.toFilter("FID > env('threshold', 100)"),
                ECQL.toFilter("NAME='Main Street' AND FID > 100"),
                ECQL.toFilter("NAME='Main Street'"));
    }

    @Test
    public void testEnvVarExpansionInclude() throws Exception {
        EnvFunction.setLocalValue("threshold", 20);
        checkRoadSegmentsDefinitionQuery(
                ECQL.toFilter("FID > env('threshold', 100)"),
                ECQL.toFilter("FID > 20"),
                Filter.INCLUDE);
    }

    @Test
    public void testEnvVarExpansion() throws Exception {
        EnvFunction.setLocalValue("threshold", 30);
        checkRoadSegmentsDefinitionQuery(
                ECQL.toFilter("FID > env('threshold', 100)"),
                ECQL.toFilter("NAME='Main Street' AND FID > 30"),
                ECQL.toFilter("NAME='Main Street'"));
    }

    private void checkRoadSegmentsDefinitionQuery(
            Filter definitionFilter, Filter expected, Filter requestFilter)
            throws IOException, CQLException {
        ContentFeatureSource basicRoads = pds.getFeatureSource("RoadSegments");

        // wrap with a filter capturing reader
        AtomicReference<Filter> lastFilter = new AtomicReference<>();
        DecoratingSimpleFeatureSource roads =
                new DecoratingSimpleFeatureSource(basicRoads) {
                    @Override
                    public int getCount(Query query) throws IOException {
                        lastFilter.set(query.getFilter());
                        return super.getCount(query);
                    }

                    @Override
                    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
                        lastFilter.set(query.getFilter());
                        return super.getFeatures(query);
                    }

                    @Override
                    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
                        lastFilter.set(filter);
                        return super.getFeatures(filter);
                    }
                };
        GeoServerFeatureSource fs =
                new GeoServerFeatureSource(
                        roads,
                        roads.getSchema(),
                        definitionFilter,
                        roads.getSchema().getCoordinateReferenceSystem(),
                        ProjectionPolicy.FORCE_DECLARED.getCode(),
                        null,
                        new MetadataMap());
        fs.getCount(new Query(null, requestFilter));
        assertEquals(expected, lastFilter.get());

        lastFilter.set(null);
        fs.getFeatures(requestFilter);
        assertEquals(expected, lastFilter.get());

        lastFilter.set(null);
        fs.getFeatures(new Query(null, requestFilter));
        assertEquals(expected, lastFilter.get());
    }
}
