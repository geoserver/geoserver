/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.mime.ApplicationMime;

public class TilesTestSupport extends OGCApiTestSupport {

    protected static final String POLYGON_COMMENT = "PolygonComment";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add vector tiles as a format
        String roadId = getLayerId(MockData.ROAD_SEGMENTS);
        GeoServerTileLayer roadTiles = (GeoServerTileLayer) getGWC().getTileLayerByName(roadId);
        roadTiles.getInfo().getMimeFormats().add(ApplicationMime.mapboxVector.getMimeType());
        getGWC().save(roadTiles);

        // reduce it to its actual bounding box to see grid subsets
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        FeatureTypeInfo roadInfo = catalog.getFeatureTypeByName(roadId);
        cb.setupBounds(roadInfo);
        catalog.save(roadInfo);

        // add a second style too
        StyleInfo generic = catalog.getStyleByName("generic");
        LayerInfo layer = catalog.getLayerByName(roadId);
        layer.getStyles().add(generic);
        catalog.save(layer);

        // configure caching headers on lakes
        FeatureTypeInfo lakes =
                catalog.getResourceByName(getLayerId(MockData.LAKES), FeatureTypeInfo.class);
        lakes.getMetadata().put(ResourceInfo.CACHING_ENABLED, true);
    }

    protected GWC getGWC() {
        return applicationContext.getBean(GWC.class);
    }
}
