/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.dggs;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.dggs.gstore.DGGSGeometryStoreFactory;
import org.geotools.dggs.gstore.DGGSResolutionCalculator;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class DGGSIntegrationTest extends GeoServerSystemTestSupport {

    public static final String TYPENAME = "h3-geometry";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        DataStoreInfo ds = cb.buildDataStore("h3");
        ds.getConnectionParameters().put(DGGSGeometryStoreFactory.DGGS_FACTORY_ID.key, "H3");
        catalog.add(ds);

        cb.setStore(ds);
        FeatureTypeInfo ft =
                cb.buildFeatureType(new NameImpl(catalog.getDefaultNamespace().getURI(), "H3"));
        // alternative name, will ensure a retype feature source is used
        ft.setName(TYPENAME);
        // set the min/max resolutions as strings (that's how they are stored anyways)
        ft.getMetadata().put(DGGSResolutionCalculator.CONFIGURED_MINRES_KEY, "0");
        ft.getMetadata().put(DGGSResolutionCalculator.CONFIGURED_MAXRES_KEY, "3");
        catalog.add(ft);

        LayerInfo layer = cb.buildLayer(ft);
        catalog.add(layer);
    }

    /**
     * Does an end to end test of the DGGS extension, by querying the WMS service for the layer created in the setup,
     * with a different name (to test the retype feature source) and with resolution settings configured as string.
     */
    @Test
    public void testGetFeatureInfo() throws Exception {
        String url = "wms?service=WMS&version=1.1.0&request=GetFeatureInfo&layers="
                + TYPENAME
                + "&styles=&bbox=-180.0,-90.0,180.0,90.0&width=200&height=100&srs=EPSG:4326&format=image/png&info_format=application/json&query_layers="
                + TYPENAME
                + "&x=100&y=50";
        JSONObject json = (JSONObject) getAsJSON(url);
        print(json);
        assertEquals("FeatureCollection", json.getString("type"));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertEquals("Feature", feature.getString("type"));
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("0", properties.getString("resolution"));
        assertEquals("8083fffffffffff", properties.getString("zoneId"));
        assertEquals("hexagon", properties.getString("shape"));
    }
}
