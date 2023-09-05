/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.dggs.store;

import static org.geotools.dggs.gstore.DGGSGeometryStore.GEOMETRY;
import static org.geotools.dggs.gstore.DGGSGeometryStore.RESOLUTION;
import static org.geotools.dggs.gstore.DGGSGeometryStore.VP_RESOLUTION;
import static org.geotools.dggs.gstore.DGGSGeometryStore.VP_RESOLUTION_DELTA;
import static org.geotools.dggs.gstore.DGGSGeometryStore.ZONE_ID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.dggs.gstore.DGGSGeometryStore;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.dggs.h3.H3DGGSFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class DGGSGeometryStoreTest {

    private static final String NAMESPACE = "http://this.is/my/test/namespace";
    private final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    @Before
    public void ensureAvailable() {
        Assume.assumeTrue(new H3DGGSFactory().isAvailable());
    }

    @Test
    public void testH3StoreCreation() throws IOException {
        DataStore store = getH3Store();

        assertThat(store, CoreMatchers.instanceOf(DGGSStore.class));
        assertThat(store, CoreMatchers.instanceOf(DGGSGeometryStore.class));
        assertEquals(1, store.getTypeNames().length);
        assertEquals("H3", store.getTypeNames()[0]);

        SimpleFeatureType schema = store.getSchema("H3");
        assertEquals(NAMESPACE, schema.getName().getNamespaceURI());
        assertNotNull(schema.getDescriptor(ZONE_ID));
        assertNotNull(schema.getDescriptor(RESOLUTION));
        assertNotNull(schema.getDescriptor(GEOMETRY));
    }

    public DataStore getH3Store() throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put(DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key, "H3");
        params.put(DGGSGeometryStoreFactory.NAMESPACE.key, NAMESPACE);
        DataStore store = DataStoreFinder.getDataStore(params);
        return store;
    }

    @Test
    public void testDistanceHint() throws IOException {
        DataStore store = getH3Store();
        SimpleFeatureSource fs = store.getFeatureSource("H3");
        Query q = new Query();

        // 1 degree
        q.getHints().put(Hints.GEOMETRY_DISTANCE, 1d);
        SimpleFeature f1deg = DataUtilities.first(fs.getFeatures(q));
        assertEquals(0, f1deg.getAttribute(RESOLUTION));

        // 1 km (give or take)
        q.getHints().put(Hints.GEOMETRY_DISTANCE, 0.01);
        SimpleFeature f1km = DataUtilities.first(fs.getFeatures(q));
        assertEquals(3, f1km.getAttribute(RESOLUTION));
    }

    @Test
    public void testResolutionHint() throws IOException {
        DataStore store = getH3Store();
        SimpleFeatureSource fs = store.getFeatureSource("H3");
        Query q = new Query();

        // explicitly ask for resolution 13
        q.getHints()
                .put(Hints.VIRTUAL_TABLE_PARAMETERS, Collections.singletonMap(VP_RESOLUTION, 13));
        SimpleFeature f13 = DataUtilities.first(fs.getFeatures(q));
        assertEquals(13, f13.getAttribute(RESOLUTION));

        // invalid, negative
        try {
            q.getHints()
                    .put(
                            Hints.VIRTUAL_TABLE_PARAMETERS,
                            Collections.singletonMap(VP_RESOLUTION, -1));
            DataUtilities.first(fs.getFeatures(q));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("-1"));
        }

        // invalid, too large
        try {
            q.getHints()
                    .put(
                            Hints.VIRTUAL_TABLE_PARAMETERS,
                            Collections.singletonMap(VP_RESOLUTION, 50));
            DataUtilities.first(fs.getFeatures(q));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("50"));
        }
    }

    @Test
    public void testResolutionOffsetHint() throws IOException {
        DataStore store = getH3Store();
        SimpleFeatureSource fs = store.getFeatureSource("H3");
        Query q = new Query();

        // 1 m (give or take)
        q.getHints().put(Hints.GEOMETRY_DISTANCE, 0.00001);
        SimpleFeature f13 = DataUtilities.first(fs.getFeatures(q));
        assertEquals(10, f13.getAttribute(RESOLUTION));

        // now add a negative offset
        q.getHints()
                .put(
                        Hints.VIRTUAL_TABLE_PARAMETERS,
                        Collections.singletonMap(VP_RESOLUTION_DELTA, -3));
        SimpleFeature f13m3 = DataUtilities.first(fs.getFeatures(q));
        assertEquals(7, f13m3.getAttribute(RESOLUTION));

        // now add a positive offset
        q.getHints()
                .put(
                        Hints.VIRTUAL_TABLE_PARAMETERS,
                        Collections.singletonMap(VP_RESOLUTION_DELTA, 3));
        SimpleFeature f13p3 = DataUtilities.first(fs.getFeatures(q));
        assertEquals(13, f13p3.getAttribute(RESOLUTION));

        // below lower bound
        q.getHints()
                .put(
                        Hints.VIRTUAL_TABLE_PARAMETERS,
                        Collections.singletonMap(VP_RESOLUTION_DELTA, -100));
        assertEquals(0, DataUtilities.count(fs.getFeatures(q)));

        // above higher bound
        q.getHints()
                .put(
                        Hints.VIRTUAL_TABLE_PARAMETERS,
                        Collections.singletonMap(VP_RESOLUTION_DELTA, 100));
        assertEquals(0, DataUtilities.count(fs.getFeatures(q)));
    }

    @Test
    public void testNeighbor() throws IOException, CQLException {
        DataStore store = getH3Store();
        SimpleFeatureSource fs = store.getFeatureSource("H3");
        Query q = new Query();
        q.setFilter(ECQL.toFilter("neighbor(zoneId, '807ffffffffffff', 1) = true"));
        SimpleFeatureCollection features = fs.getFeatures(q);

        // was a pentagon, only 5 zones
        assertEquals(5, features.size());
        Set<String> identifiers = new HashSet<>();
        features.accepts(
                feature -> identifiers.add((String) feature.getProperty("zoneId").getValue()),
                null);
        assertThat(
                identifiers,
                CoreMatchers.hasItems(
                        "805bfffffffffff",
                        "8077fffffffffff",
                        "809bfffffffffff",
                        "8071fffffffffff",
                        "809ffffffffffff"));
    }

    @Test
    public void testAttributeManagement() throws CQLException, IOException {
        Query q = new Query();
        // filter uses the geometry
        q.setFilter(FF.bbox("", -20, -20, 20, 20, "EPSG:4326"));
        // sorting uses the shape type (pentagons first) and by zoneId after
        q.setSortBy(
                new SortBy[] {
                    FF.sort("shape", SortOrder.DESCENDING), FF.sort("zoneId", SortOrder.ASCENDING)
                });
        // only output is the zoneId
        q.setPropertyNames(new String[] {"zoneId"});

        DataStore store = getH3Store();
        SimpleFeatureCollection features = store.getFeatureSource("H3").getFeatures(q);
        assertEquals(1, features.getSchema().getAttributeCount());
        List<String> identifiers = new ArrayList<>();
        features.accepts(
                feature -> identifiers.add((String) feature.getProperty("zoneId").getValue()),
                null);
        // hand verified list, with a map and checking sorting, 8075fffffffffff is the pentagon
        List<String> expected =
                Arrays.asList(
                        "8075fffffffffff",
                        "803ffffffffffff",
                        "8055fffffffffff",
                        "8059fffffffffff",
                        "806bfffffffffff",
                        "807dfffffffffff",
                        "8083fffffffffff",
                        "8097fffffffffff",
                        "8099fffffffffff",
                        "80a5fffffffffff",
                        "80adfffffffffff");
        assertEquals(expected, identifiers);
    }
}
