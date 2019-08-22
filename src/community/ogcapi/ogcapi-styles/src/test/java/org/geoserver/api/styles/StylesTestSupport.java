/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.util.Collections;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;

public class StylesTestSupport extends OGCApiTestSupport {

    protected static final String POLYGON_COMMENT = "PolygonComment";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no actual need for data
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // add a work-spaced style
        testData.addWorkspace("ws", "http://www.geoserver.org/ws", getCatalog());
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("ws");
        testData.addStyle(ws, "NamedPlaces", "NamedPlaces.sld", SystemTestData.class, getCatalog());
        // add a style with a comment that can be used for raw style return
        testData.addStyle(
                POLYGON_COMMENT, "polygonComment.sld", StylesTestSupport.class, getCatalog());

        testData.addStyle(
                null,
                "cssSample",
                "cssSample.css",
                StylesTestSupport.class,
                getCatalog(),
                Collections.singletonMap(StyleProperty.FORMAT, "css"));
    }
}
