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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.dggs.gstore.DGGSFeatureSource;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.dggs.rhealpix.RHealPixDGGSFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.spatial.BBOX;

@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation") // JUnit 3 tests here
public class ClickHouseRHealPixOnlineTest extends ClickHouseOnlineTestCase {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Override
    protected String getDGGSId() {
        return new RHealPixDGGSFactory().getId();
    }

    @Override
    protected void connect() throws Exception {
        super.connect();

        try (Connection cx =
                        ((JDBCDataStore) dataStore.getDelegate())
                                .getConnection(Transaction.AUTO_COMMIT);
                Statement st = cx.createStatement();
                PreparedStatement ps =
                        cx.prepareStatement("INSERT INTO \"rpix\" VALUES (?, ?, ?)")) {
            // cleanup
            st.execute("DROP TABLE IF EXISTS rpix");
            st.execute("DROP TABLE IF EXISTS not_dggs");

            st.execute(
                    "CREATE TABLE \"rpix\"(\"zoneId\" String, "
                            + "\"resolution\" Int32, \"value\" Float64) "
                            + "ENGINE = MergeTree() "
                            + "PARTITION BY substring(zoneId, 1, 1)\n"
                            + "ORDER BY (resolution, zoneId)");

            // populate first 3 levels of the zone hierarchy with zones
            String[] zones = {"N", "O", "P", "Q", "R", "S"};
            for (int i = 0; i < zones.length; i++) {
                String zoneId = zones[i];
                ps.setString(1, zoneId);
                ps.setInt(2, 0);
                ps.setDouble(3, i);
                ps.addBatch();
                for (int j = 0; j < 9; j++) {
                    String subZone = zoneId + j;
                    int subZoneValue = i * 10 + j;
                    ps.setString(1, subZone);
                    ps.setInt(2, 1);
                    ps.setDouble(3, subZoneValue);
                    ps.addBatch();
                    // just a few at resolution 2, to avoid long test setups
                    for (int k = 0; k < 2; k++) {
                        String subSubZone = subZone + k;
                        int subSubZoneValue = subZoneValue * 10 + k;
                        ps.setString(1, subSubZone);
                        ps.setInt(2, 2);
                        ps.setDouble(3, subSubZoneValue);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();

            // create a table that does not qualify as a DGGS source
            st.execute(
                    "CREATE TABLE \"not_dggs\"(\"fid\" String, "
                            + "\"test\" Int32) "
                            + "ENGINE = MergeTree() "
                            + "PARTITION BY fid\n"
                            + "ORDER BY fid");
        }
    }

    public void testTypeNames() throws IOException {
        String[] typeNames = dataStore.getTypeNames();
        assertEquals(1, typeNames.length);
        assertEquals("rpix", typeNames[0]);
    }

    public void testDataStoreSchema() throws IOException {
        SimpleFeatureType schema = dataStore.getSchema("rpix");
        assertRPixSchema(schema);
    }

    public void testFeatureSourceSchema() throws IOException {
        SimpleFeatureType schema = dataStore.getFeatureSource("rpix").getSchema();
        assertRPixSchema(schema);
    }

    public void testFeatureCollectionSchema() throws IOException {
        SimpleFeatureType schema = dataStore.getFeatureSource("rpix").getFeatures().getSchema();
        assertRPixSchema(schema);
    }

    private void assertRPixSchema(SimpleFeatureType schema) {
        assertEquals(4, schema.getAttributeDescriptors().size());
        assertDescriptor(schema, "zoneId", String.class);
        assertDescriptor(schema, "resolution", Integer.class);
        assertDescriptor(schema, "value", Double.class);
        assertDescriptor(schema, "geometry", Polygon.class);
    }

    private void assertDescriptor(SimpleFeatureType schema, String name, Class<?> binding) {
        AttributeDescriptor ad = schema.getDescriptor(name);
        assertNotNull(name + " not found", ad);
        assertEquals(binding, ad.getType().getBinding());
    }

    public void testCountAll() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");
        assertEquals(6 + 54 + (54 * 2), fs.getCount(Query.ALL));
    }

    public void testCountLikeZone() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");
        PropertyIsLike like = FF.like(FF.property(ClickHouseDGGSDataStore.ZONE_ID), "R%");
        assertEquals(1 + 9 + 9 * 2, fs.getCount(new Query(null, like)));
    }

    public void testCountFilterResolution() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");
        PropertyIsEqualTo eq =
                FF.equals(FF.property(ClickHouseDGGSDataStore.RESOLUTION), FF.literal(2));
        assertEquals(54 * 2, fs.getCount(new Query(null, eq)));
    }

    public void testCountSouthPolar() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        BBOX bbox =
                FF.bbox(
                        FF.property(ClickHouseDGGSDataStore.GEOMETRY),
                        new ReferencedEnvelope(-180, 180, -90, -60, DefaultGeographicCRS.WGS84));
        // no way to run this one fast right now
        assertEquals(-1, fs.getCount(new Query(null, bbox)));
    }

    public void testCountSouthPolarResolutionOne() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        BBOX bbox =
                FF.bbox(
                        FF.property(ClickHouseDGGSDataStore.GEOMETRY),
                        new ReferencedEnvelope(-180, 180, -90, -60, DefaultGeographicCRS.WGS84));
        // force usage of resolution 1 to get a count
        Query q = new Query(null, bbox);
        addResolutionHint(q, 1);
        assertEquals(9, fs.getCount(q));
    }

    private void addResolutionHint(Query q, int resolution) {
        Map<String, Integer> vp = Collections.singletonMap(DGGSStore.VP_RESOLUTION, resolution);
        q.getHints().put(Hints.VIRTUAL_TABLE_PARAMETERS, vp);
    }

    public void testGetAllFeatures() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        Query q = new Query();
        q.setSortBy(new SortBy[] {FF.sort("zoneId", SortOrder.ASCENDING)});
        q.setMaxFeatures(2);

        List<SimpleFeature> features = DataUtilities.list(fs.getFeatures(q));
        assertEquals(2, features.size());

        SimpleFeature f0 = features.get(0);
        assertEquals("N", f0.getAttribute("zoneId"));
        assertEquals(0, f0.getAttribute("resolution"));
        assertEquals(fs.getDGGS().getZone("N").getBoundary(), f0.getAttribute("geometry"));

        SimpleFeature f1 = features.get(1);
        assertEquals("N0", f1.getAttribute("zoneId"));
        assertEquals(1, f1.getAttribute("resolution"));
        assertEquals(fs.getDGGS().getZone("N0").getBoundary(), f1.getAttribute("geometry"));
    }

    public void testGetFeaturesByBoundingBox() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        Query q = new Query();
        // bbox slighltly smaller than R area
        q.setFilter(
                FF.bbox(
                        FF.property(""),
                        new ReferencedEnvelope(-37, 47, -39, 40, DefaultGeographicCRS.WGS84)));
        addResolutionHint(q, 2);
        q.setSortBy(new SortBy[] {FF.sort("zoneId", SortOrder.ASCENDING)});
        q.setMaxFeatures(2);

        List<SimpleFeature> features = DataUtilities.list(fs.getFeatures(q));
        assertEquals(2, features.size());

        SimpleFeature f0 = features.get(0);
        assertEquals("R00", f0.getAttribute("zoneId"));
        assertEquals(2, f0.getAttribute("resolution"));
        assertEquals(fs.getDGGS().getZone("R00").getBoundary(), f0.getAttribute("geometry"));

        SimpleFeature f1 = features.get(1);
        assertEquals("R01", f1.getAttribute("zoneId"));
        assertEquals(2, f1.getAttribute("resolution"));
        assertEquals(fs.getDGGS().getZone("R01").getBoundary(), f1.getAttribute("geometry"));
    }

    public void testGetAllFeaturesDefaultGeometry() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        Query q = new Query();
        q.setSortBy(new SortBy[] {FF.sort("zoneId", SortOrder.ASCENDING)});
        q.setPropertyNames(new String[] {"geometry"});
        q.setMaxFeatures(1);

        // used to fail with a Clickhouse query that did not contain any attribute
        List<SimpleFeature> features = DataUtilities.list(fs.getFeatures(q));
        assertEquals(1, features.size());

        SimpleFeature f0 = features.get(0);
        assertEquals(fs.getDGGS().getZone("N").getBoundary(), f0.getAttribute("geometry"));
    }

    public void testGetAllFeaturesPaging() throws Exception {
        DGGSFeatureSource fs = dataStore.getFeatureSource("rpix");

        // do a paging without sorting, and without a pk on the primary table
        Query q = new Query();
        q.setFilter(FF.equals(FF.property("resolution"), FF.literal(0)));
        q.setStartIndex(2);
        q.setMaxFeatures(2);

        // used to fail due to lack of "natural ordering" guaranteed by the primary key
        List<SimpleFeature> features = DataUtilities.list(fs.getFeatures(q));
        assertEquals(2, features.size());

        assertEquals("P", features.get(0).getAttribute("zoneId"));
        assertEquals("Q", features.get(1).getAttribute("zoneId"));
    }
}
