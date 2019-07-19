/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collections;
import java.util.List;
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

    /** Retuns a single element out of an array, checking that there is just one */
    protected Object getSingle(DocumentContext json, String path) {
        List items = json.read(path);
        assertEquals(1, items.size());
        return items.get(0);
    }

    /** Checks the specified jsonpath exists in the document */
    protected boolean exists(DocumentContext json, String path) {
        List items = json.read(path);
        return items.size() > 0;
    }
}
