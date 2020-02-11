/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features.tiled;

import java.util.Set;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.mime.ApplicationMime;

public class TiledFeaturesTestSupport extends OGCApiTestSupport {

    protected static final String NATURE_GROUP = "nature";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        // add vector tiles as a format
        String roadId = getLayerId(MockData.ROAD_SEGMENTS);
        GeoServerTileLayer roadTiles = (GeoServerTileLayer) getGWC().getTileLayerByName(roadId);
        Set<String> formats = roadTiles.getInfo().getMimeFormats();
        formats.add(ApplicationMime.mapboxVector.getFormat());
        formats.add(ApplicationMime.topojson.getFormat());
        formats.add(ApplicationMime.geojson.getFormat());
        getGWC().save(roadTiles);

        // reduce it to its actual bounding box to see grid subsets
        CatalogBuilder cb = new CatalogBuilder(catalog);
        FeatureTypeInfo roadInfo = catalog.getFeatureTypeByName(roadId);
        cb.setupBounds(roadInfo);
        catalog.save(roadInfo);

        // setup a layer group
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakesLayer = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forestsLayer = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if (lakesLayer != null && forestsLayer != null) {
            group.setName(NATURE_GROUP);
            group.getLayers().add(lakesLayer);
            group.getLayers().add(forestsLayer);
            group.getStyles().add(null);
            group.getStyles().add(null);
            new CatalogBuilder(catalog).calculateLayerGroupBounds(group);
            catalog.add(group);
        }
    }

    protected GWC getGWC() {
        return applicationContext.getBean(GWC.class);
    }
}
