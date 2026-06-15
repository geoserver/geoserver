/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that Set fields in catalog objects use LinkedHashSet to preserve insertion order during XStream serialization.
 * This ensures stable XML output across repeated serializations, preventing spurious diffs when config files are
 * re-written.
 */
public class XStreamPersisterSetOrderTest {

    private Catalog catalog;
    private CatalogFactory cFactory;

    @Before
    public void setUp() {
        catalog = new CatalogImpl();
        cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("test");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("test");
        ns.setURI("http://test.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("testStore");
        catalog.add(ds);
    }

    /**
     * Verifies that LayerInfo.styles is a LinkedHashSet, preserving insertion order so that serialization output is
     * stable across repeated writes.
     */
    @Test
    public void testLayerStylesPreservesInsertionOrder() throws Exception {
        StyleInfo styleZ = cFactory.createStyle();
        styleZ.setName("z-style");
        styleZ.setFilename("z-style.sld");

        StyleInfo styleA = cFactory.createStyle();
        styleA.setName("a-style");
        styleA.setFilename("a-style.sld");

        StyleInfo styleM = cFactory.createStyle();
        styleM.setName("m-style");
        styleM.setFilename("m-style.sld");

        StyleInfo defaultStyle = cFactory.createStyle();
        defaultStyle.setName("default");
        defaultStyle.setFilename("default.sld");

        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore(catalog.getDataStores().get(0));
        ft.setNamespace(catalog.getNamespaces().get(0));
        ft.setName("testFeature");
        ft.setSRS("EPSG:4326");
        ft.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        ft.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        catalog.add(ft);

        LayerInfo layer = cFactory.createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(defaultStyle);

        // Verify the styles Set is a LinkedHashSet (preserves insertion order)
        assertTrue(
                "LayerInfo.styles should be a LinkedHashSet for stable serialization order",
                layer.getStyles() instanceof LinkedHashSet);

        // Add in a specific order: z, a, m
        layer.getStyles().add(styleZ);
        layer.getStyles().add(styleA);
        layer.getStyles().add(styleM);

        // Serialize
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(layer, out);
        String xml = out.toString(StandardCharsets.UTF_8);

        // Verify insertion order is preserved: z before a before m
        int posZ = xml.indexOf("<name>z-style</name>");
        int posA = xml.indexOf("<name>a-style</name>");
        int posM = xml.indexOf("<name>m-style</name>");

        assertTrue("z-style should be found in output", posZ >= 0);
        assertTrue("a-style should be found in output", posA >= 0);
        assertTrue("m-style should be found in output", posM >= 0);

        assertTrue(
                "z-style (pos " + posZ + ") should appear before a-style (pos " + posA
                        + ") — insertion order preserved",
                posZ < posA);
        assertTrue(
                "a-style (pos " + posA + ") should appear before m-style (pos " + posM
                        + ") — insertion order preserved",
                posA < posM);
    }

    /**
     * Verifies that repeated serialization of the same object produces identical output, since LinkedHashSet preserves
     * insertion order deterministically.
     */
    @Test
    public void testRepeatedSerializationIsDeterministic() throws Exception {
        StyleInfo styleC = cFactory.createStyle();
        styleC.setName("cherry");
        styleC.setFilename("cherry.sld");

        StyleInfo styleB = cFactory.createStyle();
        styleB.setName("banana");
        styleB.setFilename("banana.sld");

        StyleInfo styleA = cFactory.createStyle();
        styleA.setName("apple");
        styleA.setFilename("apple.sld");

        StyleInfo defaultStyle = cFactory.createStyle();
        defaultStyle.setName("default");
        defaultStyle.setFilename("default.sld");

        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore(catalog.getDataStores().get(0));
        ft.setNamespace(catalog.getNamespaces().get(0));
        ft.setName("deterministicFeature");
        ft.setSRS("EPSG:4326");
        ft.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        ft.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        catalog.add(ft);

        LayerInfo layer = cFactory.createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().add(styleC);
        layer.getStyles().add(styleB);
        layer.getStyles().add(styleA);

        // Serialize once as reference
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        ByteArrayOutputStream refOut = new ByteArrayOutputStream();
        persister.save(layer, refOut);
        String reference = refOut.toString(StandardCharsets.UTF_8);

        // Serialize 20 more times and verify identical output
        for (int i = 0; i < 20; i++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            persister.save(layer, out);
            String result = out.toString(StandardCharsets.UTF_8);
            assertEquals("Serialization attempt " + (i + 1) + " differs from reference", reference, result);
        }
    }
}
