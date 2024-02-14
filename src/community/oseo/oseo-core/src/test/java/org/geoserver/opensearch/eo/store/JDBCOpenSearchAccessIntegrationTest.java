/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geoserver.data.test.CiteTestData.TASMANIA_DEM;
import static org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport.setupBasicOpenSearch;
import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest.getAttribute;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JDBCOpenSearchAccessIntegrationTest extends GeoServerSystemTestSupport {

    public static final String TEST_NAMESPACE = "http://www.test.com/os/eo";

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // for opportunistic reuse of the layer definitions
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer gs = getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.setTitle("STAC");
        gs.save(service);

        setupBasicOpenSearch(testData, getCatalog(), gs, false);

        // Create fake layer matching the collection one
        Catalog catalog = getCatalog();
        NamespaceInfo ns = catalog.getNamespaceByPrefix("gs");

        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        ci.setNamespace(ns);
        ci.setName("sentinel2");
        ci.setTitle("The second sentinel");
        ci.setDescription("The eye in the sky");
        catalog.save(ci);

        LayerInfo li = catalog.getLayerByName(ci.prefixedName());
        StyleInfo polygon = catalog.getStyleByName("polygon");
        li.getStyles().add(polygon);
        catalog.save(li);
    }

    @BeforeClass
    public static void checkOnLine() {
        GeoServerOpenSearchTestSupport.checkOnLine();
    }

    public OpenSearchAccess getOpenSearchAccess() throws IOException {
        OpenSearchAccessProvider provider =
                GeoServerExtensions.bean(OpenSearchAccessProvider.class);
        return provider.getOpenSearchAccess();
    }

    @Test
    public void testLayers() throws Exception {
        // check expected property is there
        OpenSearchAccess osAccess = getOpenSearchAccess();
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        Name name = schema.getName();
        assertEquals("http://geoserver.org", name.getNamespaceURI());

        // read it
        FeatureSource<FeatureType, Feature> source = osAccess.getCollectionSource();
        Query q = new Query();
        q.setFilter(
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("SENTINEL2"),
                        false));
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(q);

        // get the collection and check it
        Feature collection = DataUtilities.first(features);
        Assert.assertNotNull(collection);
        Property layerProperty = collection.getProperty(osAccess.getName(LAYERS));
        final Feature layerValue = (Feature) layerProperty;
        assertThat(layerValue, notNullValue());

        // check table attributes
        assertEquals("gs", getAttribute(layerValue, "workspace"));
        assertEquals("sentinel2", getAttribute(layerValue, "layer"));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "separateBands"));
        assertThat(
                getAttribute(layerValue, "bands"),
                equalTo(
                        new String[] {
                            "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10",
                            "B11", "B12"
                        }));
        assertThat(
                getAttribute(layerValue, "browseBands"),
                equalTo(new String[] {"B04", "B03", "B02"}));
        assertEquals(Boolean.TRUE, getAttribute(layerValue, "heterogeneousCRS"));
        assertEquals("EPSG:4326", getAttribute(layerValue, "mosaicCRS"));

        // virtual attributes coming from the layer
        assertEquals("The second sentinel", getAttribute(layerValue, "title"));
        assertEquals("The eye in the sky", getAttribute(layerValue, "description"));

        // list of styles attached to the layer
        List<Attribute> styles = (List<Attribute>) getAttribute(layerValue, "styles");
        SimpleFeature raster = (SimpleFeature) styles.get(0);
        assertEquals("raster", getAttribute(raster, "name"));
        assertEquals("Raster", getAttribute(raster, "title"));
        SimpleFeature polygon = (SimpleFeature) styles.get(1);
        assertEquals("polygon", getAttribute(polygon, "name"));
        assertEquals("Grey Polygon", getAttribute(polygon, "title"));

        // list of services for the layer
        Feature services = (Feature) layerValue.getProperty("services");
        Feature wms = (Feature) services.getProperty("wms");
        assertEquals(true, getAttribute(wms, "enabled"));
        List<String> formats =
                ((List<Attribute>) getAttribute(wms, "formats"))
                        .stream().map(a -> (String) a.getValue()).collect(Collectors.toList());
        assertThat(formats, Matchers.hasItems("image/png", "image/jpeg"));
    }
}
