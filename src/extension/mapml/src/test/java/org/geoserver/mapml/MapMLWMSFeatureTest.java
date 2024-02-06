/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Polygon;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Test;

public class MapMLWMSFeatureTest extends MapMLTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);
    }

    @After
    public void tearDown() {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(lgi);

        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(liRaster);
    }

    @Test
    public void testMapMLUseFeatures() throws Exception {

        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        Mapml mapmlFeatures =
                getWMSAsMapML(
                        MockData.POLYGONS.getLocalPart(), null, null, "EPSG:3857", null, true);

        assertEquals(
                "Polygons layer has one feature, so one should show up in the conversion",
                1,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "Polygons layer coordinates should be reprojected to EPSG:3857 from EPSG:32615",
                "-1.035248685501953E7,504109.89366969,-1.035248685487942E7,504160.40501648,-1.035243667965507E7,504135.14918251,-1.035243667974068E7,504109.89351299,-1.035248685501953E7,504109.89366969",
                ((Polygon)
                                mapmlFeatures
                                        .getBody()
                                        .getFeatures()
                                        .get(0)
                                        .getGeometry()
                                        .getGeometryContent()
                                        .getValue())
                        .getThreeOrMoreCoordinatePairs().get(0).getValue().stream()
                                .collect(Collectors.joining(",")));
    }

    @Test
    public void testExceptionBecauseMoreThanOneFeatureType() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, true);
        lgi.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(lgi);
        String response =
                getWMSAsMapMLString(
                        "layerGroup" + "," + MockData.POLYGONS.getLocalPart(),
                        null,
                        null,
                        "EPSG:3857",
                        null,
                        true);

        assertTrue(
                "MapML response contains an exception due to multiple feature types",
                response.contains(
                        "MapML WMS Feature format does not currently support Multiple Feature Type output."));
    }

    @Test
    public void testExceptionBecauseBecauseRaster() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getMetadata().put(MAPML_USE_FEATURES, true);
        liRaster.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(liRaster);
        String response =
                getWMSAsMapMLString(
                        MockData.WORLD.getLocalPart(), null, null, "EPSG:3857", null, true);

        assertTrue(
                "MapML response contains an exception due to non-vector type",
                response.contains(
                        "MapML WMS Feature format does not currently support non-vector layers."));
    }
}
