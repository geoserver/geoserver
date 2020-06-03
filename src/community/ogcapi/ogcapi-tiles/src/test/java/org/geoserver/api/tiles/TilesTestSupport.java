/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import java.util.Set;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.mime.ApplicationMime;
import org.geowebcache.mime.ImageMime;

public class TilesTestSupport extends OGCApiTestSupport {

    protected static final String POLYGON_COMMENT = "PolygonComment";
    protected static final String NATURE_GROUP = "nature";
    protected static final String BASIC_STYLE_GROUP = "BasicStyleGroup";
    protected static final String BASIC_STYLE_GROUP_STYLE = "BasicStyleGroupStyle";

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
        Set<String> formats = roadTiles.getInfo().getMimeFormats();
        formats.add(ApplicationMime.mapboxVector.getFormat());
        formats.add(ApplicationMime.topojson.getFormat());
        formats.add(ApplicationMime.geojson.getFormat());
        // also add png8
        formats.add(ImageMime.png8.getFormat());
        // add support for CQL_FILTER caching
        RegexParameterFilter cqlFilter = new RegexParameterFilter();
        cqlFilter.setKey("CQL_FILTER");
        cqlFilter.setDefaultValue("INCLUDE");
        cqlFilter.setRegex(".*");
        roadTiles.getInfo().addParameterFilter(cqlFilter);
        // save
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

        // prepare a layer with vector formats only
        String forestsId = getLayerId(MockData.FORESTS);
        addVectorTileFormats(forestsId, true);

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
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
            addVectorTileFormats(NATURE_GROUP, false);
        }

        // add a style groupd
        testData.addStyle(
                BASIC_STYLE_GROUP_STYLE,
                "BasicStyleGroup.sld",
                TilesTestSupport.class,
                getCatalog());
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        StyleInfo s = catalog.getStyleByName("BasicStyleGroupStyle");

        lg.setName(BASIC_STYLE_GROUP);
        lg.getLayers().add(null);
        lg.getStyles().add(s);
        new CatalogBuilder(catalog).calculateLayerGroupBounds(lg);
        catalog.add(lg);
        addVectorTileFormats(BASIC_STYLE_GROUP, false);
    }

    private void addVectorTileFormats(String layerId, boolean clear) {
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) getGWC().getTileLayerByName(layerId);
        Set<String> formats = tileLayer.getInfo().getMimeFormats();
        if (clear) {
            formats.clear();
        }
        formats.add(ApplicationMime.mapboxVector.getFormat());
        formats.add(ApplicationMime.geojson.getFormat());
        formats.add(ApplicationMime.topojson.getFormat());
        getGWC().save(tileLayer);
    }

    protected GWC getGWC() {
        return applicationContext.getBean(GWC.class);
    }
}
