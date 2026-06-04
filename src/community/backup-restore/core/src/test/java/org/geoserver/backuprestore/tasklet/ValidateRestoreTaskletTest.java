/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.junit.Test;

/**
 * Unit tests for the pre-flight validation sweep ({@link ValidateRestoreTasklet#collectProblems}). Exercised against a
 * hand-built {@link CatalogImpl} so it does not need a running restore job.
 */
public class ValidateRestoreTaskletTest {

    @Test
    public void validCatalogHasNoProblems() {
        Catalog catalog = new CatalogImpl();
        addValidContent(catalog);
        assertTrue(
                "a valid catalog must produce no validation problems",
                ValidateRestoreTasklet.collectProblems(catalog).isEmpty());
    }

    @Test
    public void invalidStyleIsReported() {
        Catalog catalog = new CatalogImpl();
        addValidContent(catalog);

        // Corrupt the style in place: unwrap the ModificationProxy and null its name on the stored object, so the
        // sweep's getStyles() sees an invalid style (a style name is mandatory).
        StyleInfo style = catalog.getStyleByName("good-style");
        ModificationProxy.unwrap(style).setName(null);

        List<String> problems = ValidateRestoreTasklet.collectProblems(catalog);
        assertEquals("exactly the corrupted style should be reported, got: " + problems, 1, problems.size());
        assertTrue("the report should identify the style, got: " + problems, problems.get(0).startsWith("style"));
    }

    private static void addValidContent(Catalog catalog) {
        CatalogFactory factory = catalog.getFactory();

        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName("good-ws");
        catalog.add(ws);

        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix("good-ws");
        ns.setURI("http://good");
        catalog.add(ns);

        StyleInfo style = factory.createStyle();
        style.setName("good-style");
        style.setFilename("good-style.sld");
        catalog.add(style);
    }
}
