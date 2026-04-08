/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class DGGSApiTest extends OGCApiTestSupport {

    private static final String COLLECTION = "gs:H3"; // workspace:layerName
    private static final String API_ROOT = "ogc/dggs/v1/collections";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // create a H3 store and layer to be listed
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getDefaultWorkspace());
        DataStoreInfo ds = cb.buildDataStore("h3");
        ds.getConnectionParameters().put(DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key, "H3");
        String nsURI = catalog.getDefaultNamespace().getURI();
        ds.getConnectionParameters().put(DGGSGeometryStoreFactory.NAMESPACE.key, nsURI);
        catalog.add(ds);

        cb.setStore(ds);
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(nsURI, "H3"));
        cb.setupBounds(ft);
        catalog.add(ft);
        LayerInfo li = cb.buildLayer(ft);
        catalog.add(li);

        // disable feature bounding
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setFeatureBounding(false);
        gs.save(wfs);
    }

    @Test
    public void testGetZonesPaginationAndSchema() throws Exception {
        DocumentContext json = getAsJSONPath(endpoint("zones?f=application/json&limit=50"), 200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> features = json.read("$.features");
        assertThat(features, hasSize(50));

        // Basic feature schema checks
        assertEquals("Feature", json.read("$.features[0].type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));

        // Properties presence
        assertEquals("80f3fffffffffff", json.read("$.features[0].properties.zoneId"));
        assertEquals(
                0,
                json.read("$.features[0].properties.resolution", Integer.class).intValue());
        assertEquals("hexagon", json.read("$.features[0].properties.shape", String.class));

        // Collection-level metadata
        Integer numberReturned = json.read("$.numberReturned", Integer.class);
        Integer numberMatched = json.read("$.numberMatched", Integer.class);
        Integer totalFeatures = json.read("$.totalFeatures", Integer.class);

        assertEquals(Integer.valueOf(50), numberReturned);
        assertThat(numberMatched, greaterThanOrEqualTo(50));
        assertThat(totalFeatures, greaterThanOrEqualTo(numberMatched));

        // CRS
        assertEquals("name", json.read("$.crs.type"));
        assertEquals("urn:ogc:def:crs:EPSG::4326", json.read("$.crs.properties.name"));

        // Next link (pagination)
        String nextRel = json.read("$.links[0].rel");
        String nextType = json.read("$.links[0].type");
        String nextHref = json.read("$.links[0].href");
        assertEquals("next", nextRel);
        assertEquals("application/json", nextType);
        assertThat(nextHref, containsString("startIndex=50"));

        json = getAsJSONPath(endpoint("zones?f=application/json&limit=5&resolution=2"), 200);

        features = json.read("$.features");
        assertThat(features, hasSize(5));

        // Properties presence
        assertEquals("82f3b7fffffffff", json.read("$.features[0].properties.zoneId"));
        assertEquals(
                2,
                json.read("$.features[0].properties.resolution", Integer.class).intValue());
        assertEquals("hexagon", json.read("$.features[0].properties.shape", String.class));
    }

    @Test
    public void testGetZonesParameters() throws Exception {
        DocumentContext json = getAsJSONPath(
                endpoint("zones?f=application/json&limit=50" + "&geom=POLYGON((10 10, 10 20, 20 20, 20 10, 10 10))"
                        + "&properties=zoneId, resolution, shape"),
                200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> features = json.read("$.features");
        assertThat(features, hasSize(3));

        // Basic feature schema checks
        assertEquals("Feature", json.read("$.features[0].type"));

        // Properties presence
        assertEquals("806bfffffffffff", json.read("$.features[0].properties.zoneId"));
        assertEquals(
                0,
                json.read("$.features[0].properties.resolution", Integer.class).intValue());
        assertEquals("hexagon", json.read("$.features[0].properties.shape", String.class));
        assertEquals("8059fffffffffff", json.read("$.features[1].properties.zoneId"));
        assertEquals("803ffffffffffff", json.read("$.features[2].properties.zoneId"));
    }

    @Test
    public void testGetZoneByIdRoundtrip() throws Exception {
        // 1) First page to grab a valid zone id (id looks like "H3.<hex>")
        DocumentContext page = getAsJSONPath(endpoint("zones?f=application/json&limit=1"), 200);
        String featureId = page.read("$.features[0].id", String.class); // e.g. "H3.80f3fffffffffff"
        assertThat(featureId, startsWith("H3."));

        // 2) GET the single zone
        DocumentContext single = getAsJSONPath(endpointWithZone("zone", featureId), 200);

        assertEquals("FeatureCollection", single.read("$.type"));
        assertEquals(featureId, single.read("$.features[0].id"));
        assertEquals("Polygon", single.read("$.features[0].geometry.type"));
        assertEquals("urn:ogc:def:crs:EPSG::4326", single.read("$.crs.properties.name"));
        assertEquals(single.read("$.features[0].properties.zoneId", String.class), featureId.substring("H3.".length()));
    }

    @Test
    public void testGetNeighbors() throws Exception {
        // Seed zone
        String zoneId = getAnyZoneIdWithPrefix();
        // k=1 is the usual immediate neighborhood;
        DocumentContext json = getAsJSONPath(endpointWithZone("neighbors", zoneId) + "&k=1", 200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> neigh = json.read("$.features");
        assertThat(neigh.size(), allOf(greaterThanOrEqualTo(5), lessThanOrEqualTo(7))); // hex: 6, pent: 5

        // All neighbors should be different from the center
        List<String> ids = json.read("$.features[*].id");
        assertThat(ids, not(hasItem(zoneId)));

        // Resolution should match the seed zone
        Integer res = getZoneResolution(zoneId);
        List<Integer> nres = json.read("$.features[*].properties.resolution");
        nres.forEach(r -> assertEquals(res, r));
    }

    @Test
    public void testGetChildrenNextResolution() throws Exception {
        String parentId = getAnyZoneIdWithPrefix();
        int parentRes = getZoneResolution(parentId);

        // children at next resolution (parentRes+1)
        DocumentContext json =
                getAsJSONPath(endpointWithZone("children", parentId) + "&resolution=" + (parentRes + 1), 200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> kids = json.read("$.features");
        assertThat(kids.size(), allOf(greaterThanOrEqualTo(6), lessThanOrEqualTo(7))); // pent 6, hex 7

        // All children should report the requested resolution
        List<Integer> kres = json.read("$.features[*].properties.resolution");
        kres.forEach(r -> assertEquals(Integer.valueOf(parentRes + 1), r));
    }

    @Test
    public void testGetParentsUpToResolution0() throws Exception {
        // Pick a non-base zone (ensure res > 0)
        String childId = getAnyZoneIdWithPrefixAtMinResolution(2);
        int childRes = getZoneResolution(childId);

        DocumentContext json = getAsJSONPath(endpointWithZone("parents", childId), 200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<Integer> pres = json.read("$.features[*].properties.resolution");
        assertThat(pres, not(empty()));
        // Strictly less than child's resolution, all >= 0
        pres.forEach(r -> {
            assertThat(r, lessThan(childRes));
            assertThat(r, greaterThanOrEqualTo(0));
        });
    }

    @Test
    public void testPointQuery() throws Exception {
        // Antarctic-ish sample; adjust if your service bounds are restricted
        double lon = 10.0;
        double lat = -70.0;
        int resolution = 2;

        DocumentContext json = getAsJSONPath(
                endpoint("point?f=application/json&point=" + lon + "," + lat + "&resolution=" + resolution), 200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> feats = json.read("$.features");
        assertThat(feats, hasSize(1));
        assertEquals(Integer.valueOf(resolution), json.read("$.features[0].properties.resolution", Integer.class));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
    }

    @Test
    public void testPolygon() throws Exception {
        // Simple bbox polygon around the Mediterranean (very rough)
        String polyWkt = "POLYGON((5 35, 35 35, 35 45, 5 45, 5 35))";
        int resolution = 3;
        int limit = 100;

        DocumentContext json = getAsJSONPath(
                endpoint("polygon?f=application/json&polygon=" + encode(polyWkt) + "&resolution=" + resolution
                        + "&limit=" + limit),
                200);

        assertEquals("FeatureCollection", json.read("$.type"));
        List<?> feats = json.read("$.features");
        assertThat(feats.size(), greaterThan(0));
        assertEquals(Integer.valueOf(resolution), json.read("$.features[0].properties.resolution", Integer.class));

        // If paginated, you should see next link when coverage > limit
        Integer numberMatched = json.read("$.numberMatched", Integer.class);
        if (numberMatched != null && numberMatched > limit) {
            String rel = json.read("$.links[0].rel");
            assertEquals("next", rel);
        }
    }

    private String endpointWithZone(String resource, String zoneId) {
        return endpoint("/" + resource) + "?zone_id=" + extractZone(zoneId) + "&f=application/json";
    }

    /** Returns any valid feature id like "H3.80f3fffffffffff" from the collection. */
    private String getAnyZoneIdWithPrefix() throws Exception {
        DocumentContext page = getAsJSONPath(endpoint("zones?f=application/json&limit=1"), 200);
        return page.read("$.features[0].id", String.class);
    }

    /** Returns resolution of a single zone (by id with prefix). */
    private int getZoneResolution(String zoneIdWithPrefix) throws Exception {
        DocumentContext single = getAsJSONPath(
                endpoint("zone?zone_id=") + extractZone(zoneIdWithPrefix) + "&f=application/json&limit=1", 200);
        return single.read("$.features[0].properties.resolution", Integer.class);
    }

    /**
     * Returns any zone id with prefix whose resolution is >= minRes. If the first page contains only res 0, it fetches
     * another page.
     */
    private String getAnyZoneIdWithPrefixAtMinResolution(int minRes) throws Exception {
        // First try: first page
        DocumentContext page = getAsJSONPath(endpoint("zones?f=application/json&limit=50&resolution=" + minRes), 200);
        List<String> ids = page.read("$.features[*].id");
        List<Integer> res = page.read("$.features[*].properties.resolution");
        for (int i = 0; i < ids.size(); i++) {
            if (res.get(i) >= minRes) return ids.get(i);
        }
        // Fallback: ask service to get children from a base cell
        String base = getAnyZoneIdWithPrefix(); // likely res 0
        DocumentContext kids =
                getAsJSONPath(extractZone(base) + "/children?f=application/json&resolution=" + minRes, 200);
        return kids.read("$.features[0].id", String.class);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Base path for collection-scoped endpoints. */
    private static String endpoint(String suffix) {
        // Example: ogc/dggs/v1/collections/ne%3AH3/zones?...
        return API_ROOT + "/" + encode(COLLECTION) + (suffix.startsWith("/") ? suffix : "/" + suffix);
    }

    /** Build a zones/{zoneId} path. */
    private static String extractZone(String zoneIdWithPrefix) {
        // Example expects "H3.80f3fffffffffff" as in your sample Feature.id
        return zoneIdWithPrefix.split("\\.", 2)[1]; // strip "H3.";
    }
}
