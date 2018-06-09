/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.junit.Before;
import org.junit.Test;

/**
 * EoCatalogBuilder tests
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EoCatalogBuilderTest extends GeoServerSystemTestSupport {

    private Catalog catalog;
    private WorkspaceInfo ws;
    private EoCatalogBuilder builder;
    private List<String> styles;

    @Before
    public void setup() {
        catalog = getCatalog();
        ws = catalog.getDefaultWorkspace();
        builder = new EoCatalogBuilder(catalog);

        styles = new ArrayList<String>();
        styles.add("black");
        styles.add("blue");
        styles.add("brown");
        styles.add("cyan");
        styles.add("green");
        styles.add("magenta");
        styles.add("orange");
        styles.add("red");
        styles.add("white");
        styles.add("yellow");
    }

    private void checkStyles(LayerInfo layer) {
        assertEquals(10, layer.getStyles().size());

        List<String> stylesNotFound = new ArrayList<String>(styles);
        for (StyleInfo style : layer.getStyles()) {
            stylesNotFound.remove(style.getName());
        }

        assertEquals(0, stylesNotFound.size());
    }

    private void checkTimeDimension(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        MetadataMap metadata = resource.getMetadata();

        DimensionInfo timeDimension = (DimensionInfo) metadata.get("time");
        assertNotNull(timeDimension);
    }

    @Test
    public void testEoMasksLayerCreation() {
        String groupName = "EO-Dataset";
        String maskName = "Masks";

        LayerInfo layer = builder.createEoMasksLayer(ws, groupName, maskName, getUrl("EO_Airmass"));
        assertNotNull(layer);
        assertEquals(groupName + "_" + maskName, layer.getName());

        layer = catalog.getLayerByName(layer.getName());
        assertEquals(EoLayerType.BITMASK.name(), layer.getMetadata().get(EoLayerType.KEY));
        checkTimeDimension(layer);
        checkStyles(layer);
    }

    @Test
    public void testEoParametersLayerCreation() {
        String groupName = "EO-Dataset";
        String paramName = "Params";

        LayerInfo layer =
                builder.createEoParametersLayer(ws, groupName, paramName, getUrl("EO_Airmass"));
        assertNotNull(layer);
        assertEquals(groupName + "_" + paramName, layer.getName());

        layer = catalog.getLayerByName(layer.getName());
        assertEquals(
                EoLayerType.GEOPHYSICAL_PARAMETER.name(), layer.getMetadata().get(EoLayerType.KEY));
        checkTimeDimension(layer);
    }

    @Test
    public void testEoOutlineLayerCreation() throws Exception {
        String groupName = "EO-Dataset";

        LayerInfo browseLayer = builder.createEoBrowseImageLayer(ws, groupName, getUrl("EO_Nat"));
        assertNotNull(browseLayer);
        assertEquals(groupName + "_BROWSE", browseLayer.getName());
        assertEquals(
                EoLayerType.BROWSE_IMAGE.name(), browseLayer.getMetadata().get(EoLayerType.KEY));
        checkTimeDimension(browseLayer);

        CoverageInfo coverage = (CoverageInfo) browseLayer.getResource();
        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) coverage.getGridCoverageReader(null, null);
        LayerInfo layer =
                builder.createEoOutlineLayer(
                        getUrl("EO_Nat"), ws, groupName, coverage.getNativeCoverageName(), reader);
        assertNotNull(layer);
        assertEquals(groupName + "_outlines", layer.getName());
        checkTimeDimension(layer);

        layer = catalog.getLayerByName(layer.getName());
        assertEquals(EoLayerType.COVERAGE_OUTLINE.name(), layer.getMetadata().get(EoLayerType.KEY));
        checkStyles(layer);
    }

    @Test
    public void testEoBandsLayerCreation() {
        try {
            builder.createEoMosaicLayer(
                    ws, "EO-Band", EoLayerType.BAND_COVERAGE, getUrl("EO_Airmass"), true);
            fail("The layer must not be created because it doesn't have custom dimensions");
        } catch (IllegalArgumentException e) {
        }

        LayerInfo layer =
                builder.createEoMosaicLayer(
                        ws, "EO-Band", EoLayerType.BAND_COVERAGE, getUrl("EO_Channels"), true);
        layer = catalog.getLayerByName("EO-Band");
        assertEquals(EoLayerType.BAND_COVERAGE.name(), layer.getMetadata().get(EoLayerType.KEY));

        // check dimensions
        checkTimeDimension(layer);
        DimensionInfo customDimension =
                (DimensionInfo)
                        layer.getResource()
                                .getMetadata()
                                .get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + "CHANNEL");
        assertNotNull(customDimension);
    }

    @Test
    public void testEoBandsLayerUsage() {
        try {
            builder.createEoMosaicLayer(
                    ws, "EO-Band-2", EoLayerType.BAND_COVERAGE, getUrl("EO_Airmass"), true);
            fail("The layer must not be created because it doesn't have custom dimensions");
        } catch (IllegalArgumentException e) {
        }

        LayerInfo layer =
                builder.createEoMosaicLayer(
                        ws, "EO-Band-2", EoLayerType.BAND_COVERAGE, getUrl("EO_Channels"), true);
        layer = catalog.getLayerByName("EO-Band-2");
        assertEquals(EoLayerType.BAND_COVERAGE.name(), layer.getMetadata().get(EoLayerType.KEY));

        // check dimensions
        checkTimeDimension(layer);
        DimensionInfo customDimension =
                (DimensionInfo)
                        layer.getResource()
                                .getMetadata()
                                .get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + "CHANNEL");
        assertNotNull(customDimension);
    }

    @Test
    public void testStoreCreation() throws URISyntaxException {
        CoverageStoreInfo store = builder.createEoMosaicStore(ws, "EO-store", getUrl("EO_Airmass"));
        try {
            assertNotNull(store);
            assertEquals(ws, store.getWorkspace());
            assertEquals("EO-store", store.getName());
            assertEquals("ImageMosaic", store.getType());

            store = catalog.getStoreByName("EO-store", CoverageStoreInfo.class);
            assertNotNull(store);
            assertEquals(ws, store.getWorkspace());
            assertEquals("EO-store", store.getName());
            assertEquals("ImageMosaic", store.getType());
        } finally {
            catalog.remove(store);
        }
    }

    @Test
    public void testEoLayerGroupCreation() {
        String groupName = "EO-Dataset2";
        String groupTitle = "title";

        LayerGroupInfo group =
                builder.createEoLayerGroup(
                        ws,
                        groupName,
                        groupTitle,
                        getUrl("EO_Nat"),
                        getUrl("EO_Channels"),
                        "airmass",
                        getUrl("EO_Airmass"),
                        null,
                        null);
        assertNotNull(group);
        assertEquals(groupName, group.getName());
        assertEquals(groupTitle, group.getTitle());
        assertEquals(3, group.getLayers().size());
        assertEquals(LayerGroupInfo.Mode.EO, group.getMode());
        assertNotNull(group.getRootLayer());
        assertEquals(groupName + "_BROWSE", group.getRootLayer().getName());
    }

    private String getUrl(String resource) {
        return getClass().getResource(resource).toExternalForm();
    }
}
