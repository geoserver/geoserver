/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Test;

public class SLDNamedLayerRenameHelperTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws IOException {
        testData.addStyle(
                "singleStyleGroup",
                "singleStyleGroup.sld",
                CatalogIntegrationTest.class,
                getCatalog());
    }

    @Test
    public void testGetStyles() throws IOException {
        SLDNamedLayerRenameHelper helper = new SLDNamedLayerRenameHelper(getCatalog(), true);
        helper.registerLayerRename("Streams", "Stream");
        List<StyleInfo> stylesToRename = helper.visitStyles(false);
        assertEquals(1, stylesToRename.size());
        assertTrue(stylesToRename.contains(getCatalog().getStyleByName("singleStyleGroup")));
    }

    @Test
    public void testRenameStyles() throws IOException {
        SLDNamedLayerRenameHelper helper = new SLDNamedLayerRenameHelper(getCatalog(), true);
        helper.registerLayerRename("Streams", "Stream");
        List<StyleInfo> stylesToRename = helper.visitStyles(true);
        try {
            assertEquals(1, stylesToRename.size());
            StyleInfo s = getCatalog().getStyleByName("singleStyleGroup");

            Resource newStyle = getCatalog().getResourceLoader().get("styles/singleStyleGroup.sld");
            Resource oldStyle =
                    getCatalog().getResourceLoader().get("styles/singleStyleGroup_BACKUP.sld");

            // Make sure both exist
            assertEquals(Resource.Type.RESOURCE, newStyle.getType());
            assertEquals(Resource.Type.RESOURCE, oldStyle.getType());

            assertEquals("singleStyleGroup.sld", s.getFilename());

            StyledLayerDescriptor newSld =
                    Styles.handler(s.getFormat())
                            .parse(
                                    newStyle.file(),
                                    s.getFormatVersion(),
                                    null,
                                    getCatalog().getResourcePool().getEntityResolver());
            StyledLayerDescriptor oldSld =
                    Styles.handler(s.getFormat())
                            .parse(
                                    oldStyle.file(),
                                    s.getFormatVersion(),
                                    null,
                                    getCatalog().getResourcePool().getEntityResolver());

            assertNotNull(newSld);
            assertNotNull(oldSld);

            assertEquals("Streams", oldSld.getStyledLayers()[0].getName());
            assertEquals("Stream", newSld.getStyledLayers()[0].getName());
        } finally {
            // Clean up
            helper = new SLDNamedLayerRenameHelper(getCatalog(), true);
            helper.registerLayerRename("Stream", "Streams");
            helper.visitStyles(true);
        }
    }
}
