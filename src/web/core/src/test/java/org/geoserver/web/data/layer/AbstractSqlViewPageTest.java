/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeatureType;

public class AbstractSqlViewPageTest extends GeoServerWicketTestSupport {

    public static final String STORE_NAME = "foo";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup an H2 datastore for the purpose of doing joins
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName(STORE_NAME);
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo");
        cat.add(ds);

        FeatureSource fs1 = getFeatureSource(SystemTestData.FORESTS);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init((SimpleFeatureType) fs1.getSchema());
        // tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        FeatureStore fs = (FeatureStore) store.getFeatureSource("Forests");
        fs.addFeatures(fs1.getFeatures());
        addFeature(
                fs,
                "MULTIPOLYGON (((0.008151604330777 -0.0023208963631571, 0.0086527358638763 -0.0012374917185382, 0.0097553137885805 -0.0004505798694767, 0.0156132468328575 0.001226912691216, 0.0164282119026783 0.0012863836826631, 0.0171241513076058 0.0011195104764988, 0.0181763809803841 0.0003258121477801, 0.018663180519973 -0.0007914339515293, 0.0187 -0.0054, 0.0185427596344991 -0.0062643098258021, 0.0178950534559435 -0.0072336706251426, 0.0166538015456463 -0.0078538015456464, 0.0160336706251426 -0.0090950534559435, 0.0150643098258021 -0.0097427596344991, 0.0142 -0.0099, 0.0086 -0.0099, 0.0077356901741979 -0.0097427596344991, 0.0067663293748574 -0.0090950534559435, 0.0062572403655009 -0.0082643098258021, 0.0061 -0.0074, 0.0061055767515099 -0.0046945371967831, 0.0062818025956546 -0.0038730531083409, 0.0066527358638763 -0.0032374917185382, 0.0072813143786463 -0.0026800146279973, 0.008151604330777 -0.0023208963631571)))",
                "110",
                "Foo Forest");
        addFeature(
                fs,
                "MULTIPOLYGON (((-0.0023852705061082 -0.005664537521815, -0.0026781637249217 -0.0063716443030016, -0.0033852705061082 -0.006664537521815, -0.0040923772872948 -0.0063716443030016, -0.0043852705061082 -0.005664537521815, -0.0040923772872947 -0.0049574307406285, -0.0033852705061082 -0.004664537521815, -0.0026781637249217 -0.0049574307406285, -0.0023852705061082 -0.005664537521815)))",
                "111",
                "Bar Forest");
        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);
    }

    void addFeature(FeatureStore store, String wkt, Object... atts) throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder((SimpleFeatureType) store.getSchema());
        b.add(new WKTReader().read(wkt));
        for (Object att : atts) {
            b.add(att);
        }

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        features.add(b.buildFeature(null));
        store.addFeatures(features);
    }
}
