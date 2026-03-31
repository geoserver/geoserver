/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.ServletContext;
import org.junit.After;
import org.junit.Test;

public class AssetVersionManagerTest {

    @After
    public void tearDown() {
        // reset cache and clear override between tests
        System.clearProperty(AssetVersionManager.ASSETS_VERSION_PROPERTY);
        AssetVersionManager.clearCache();
    }

    @Test
    public void testSystemPropertyOverrideIsUsedAndSanitized() {
        // include characters that should be stripped by sanitize()
        System.setProperty(AssetVersionManager.ASSETS_VERSION_PROPERTY, "  30-Mar-2026 08:49  ");

        String url = AssetVersionManager.versioned("/css/geoserver.css", (ServletContext) null);

        assertTrue("URL should contain version query parameter", url.contains("?v="));
        String[] parts = url.split("\\?v=");
        assertEquals("css/geoserver.css", parts[0]);

        String token = parts[1];
        // sanitize() keeps only [A-Za-z0-9._-], trims, and truncates to 32 chars
        assertEquals("30-Mar-20260849", token);
    }

    @Test
    public void testClearCacheResetsDeploymentVersion() {
        System.setProperty(AssetVersionManager.ASSETS_VERSION_PROPERTY, "first");
        String first = AssetVersionManager.versioned("img/logo.png", (ServletContext) null);

        AssetVersionManager.clearCache();

        System.setProperty(AssetVersionManager.ASSETS_VERSION_PROPERTY, "second");
        String second = AssetVersionManager.versioned("img/logo.png", (ServletContext) null);

        String firstToken = first.substring(first.indexOf("?v=") + 3);
        String secondToken = second.substring(second.indexOf("?v=") + 3);

        assertEquals("img/logo.png?v=first", first);
        assertEquals("img/logo.png?v=second", second);
        assertNotEquals("Tokens should differ after cache clear", firstToken, secondToken);
    }
}
