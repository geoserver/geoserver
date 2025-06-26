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
package org.geotools.dggs.clickhouse;

import static org.hamcrest.MatcherAssert.assertThat;

import com.clickhouse.data.value.UnsignedInteger;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.h3.H3DGGSFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.GroupByVisitor;
import org.geotools.jdbc.JDBCDataStore;
import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Polygon;

@SuppressWarnings("PMD.UnitTestShouldUseTestAnnotation") // JUnit 3 tests here
public class ClickHouseH3OnlineTest extends ClickHouseOnlineTestCase {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    @Override
    protected String getDGGSId() {
        return new H3DGGSFactory().getId();
    }

    @Override
    protected void setupTestData(ClickHouseDGGSDataStore dataStore) throws Exception {
        try (Connection cx = ((JDBCDataStore) dataStore.getDelegate()).getConnection(Transaction.AUTO_COMMIT);
                Statement st = cx.createStatement()) {
            // cleanup
            st.execute("DROP TABLE IF EXISTS zoneAttributes");

            st.execute("CREATE TABLE zoneAttributes (\n"
                    + "    zoneId String,\n"
                    + "    ts UInt32,\n"
                    + "    value UInt32,\n"
                    + "    resolution UInt8\n"
                    + ") ENGINE = MergeTree()\n"
                    + "ORDER BY (zoneId, ts);");

            st.execute("INSERT INTO zoneAttributes VALUES\n"
                    + "('8009fffffffffff', 1, 100, 0),\n"
                    + "('8009fffffffffff', 2, 50, 0),\n"
                    + "('8009fffffffffff', 3, 150, 0),\n"
                    + "('8011fffffffffff', 1, 75, 0),\n"
                    + "('8011fffffffffff', 2, 25, 0),\n"
                    + "('8011fffffffffff', 3, 0, 0);");
        }
    }

    public void testTypeNames() throws IOException {
        List<String> names = Arrays.asList(dataStore.getTypeNames());
        assertThat(names, Matchers.hasItem("zoneAttributes"));
    }

    public void testDataStoreSchema() throws IOException {
        SimpleFeatureType schema = dataStore.getSchema("zoneAttributes");
        assertZoneAttributesSchema(schema);
    }

    public void testFeatureSourceSchema() throws IOException {
        SimpleFeatureType schema = dataStore.getFeatureSource("zoneAttributes").getSchema();
        assertZoneAttributesSchema(schema);
    }

    public void testFeatureCollectionSchema() throws IOException {
        SimpleFeatureType schema =
                dataStore.getFeatureSource("zoneAttributes").getFeatures().getSchema();
        assertZoneAttributesSchema(schema);
    }

    private void assertZoneAttributesSchema(SimpleFeatureType schema) {
        assertEquals(5, schema.getAttributeDescriptors().size());
        assertDescriptor(schema, "zoneId", String.class);
        assertDescriptor(schema, "resolution", Short.class);
        assertDescriptor(schema, "ts", Long.class);
        assertDescriptor(schema, "value", Long.class);
        assertDescriptor(schema, "geometry", Polygon.class);
    }

    private void assertDescriptor(SimpleFeatureType schema, String name, Class<?> binding) {
        AttributeDescriptor ad = schema.getDescriptor(name);
        assertNotNull(name + " not found", ad);
        assertEquals(binding, ad.getType().getBinding());
    }

    public void testCountAll() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("zoneAttributes");
        assertEquals(6, fs.getCount(Query.ALL));
    }

    public void testDelegateGroupBy() throws Exception {
        AtomicInteger visitCount = new AtomicInteger(0);
        AtomicBoolean setValueCalled = new AtomicBoolean(false);
        GroupByVisitor visitor =
                new GroupByVisitor(
                        Aggregate.MAX,
                        FF.property("value"),
                        List.of(FF.property(ClickHouseDGGSDataStore.GEOMETRY)),
                        null) {
                    @Override
                    public void visit(Feature feature) {
                        super.visit(feature);
                        visitCount.incrementAndGet();
                    }

                    @Override
                    public void setValue(List<GroupByRawResult> value) {
                        super.setValue(value);
                        setValueCalled.set(true);
                    }
                };

        // execute visit, the visitor should be optimized out
        DGGSFeatureSource fs = dataStore.getFeatureSource("zoneAttributes");
        fs.getFeatures().accepts(visitor, null);
        assertEquals(0, visitCount.get());
        assertTrue(setValueCalled.get());

        @SuppressWarnings("PMD.CloseResource") // the store manages the dggs lifecycle
        DGGSInstance ddgs = dataStore.getDggs();
        Polygon p1 = ddgs.getZone("8009fffffffffff").getBoundary();
        Polygon p2 = ddgs.getZone("8011fffffffffff").getBoundary();

        @SuppressWarnings("unchecked")
        Map<List<Object>, Object> results = visitor.getResult().toMap();
        assertEquals(2, results.size());
        assertEquals(UnsignedInteger.valueOf(150), results.get(List.of(p1)));
        assertEquals(UnsignedInteger.valueOf(75), results.get(List.of(p2)));
    }
}
