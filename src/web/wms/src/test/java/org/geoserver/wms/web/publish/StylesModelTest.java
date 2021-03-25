/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class StylesModelTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle(
                catalog.getDefaultWorkspace(),
                "Streams",
                "Streams.sld",
                SystemTestData.class,
                catalog);
    }

    @Test
    public void testSorting() throws Exception {
        StylesModel model = new StylesModel();
        StyleInfo streams = getCatalog().getStyleByName((String) null /* global */, "Streams");
        StyleInfo gsStreams = getCatalog().getStyleByName("gs", "Streams");
        StyleInfo generic = getCatalog().getStyleByName("generic");
        List<StyleInfo> styles = model.load();
        assertTrue(styles.indexOf(streams) > styles.indexOf(gsStreams));
        assertTrue(styles.indexOf(generic) < styles.indexOf(gsStreams));
    }
}
