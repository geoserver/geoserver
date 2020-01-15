/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.util.Collections;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;

public class StylesTestSupport extends OGCApiTestSupport {

    protected static final String POLYGON_COMMENT = "PolygonComment";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data here
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // add just the WMS test data sets, besides NamedPlaces, which we have for workspace
        // specific tests, and Buil
        Catalog catalog = getCatalog();
        testData.addVectorLayer(CiteTestData.LAKES, catalog);
        testData.addVectorLayer(CiteTestData.BUILDINGS, catalog);

        // fix the bbox of the layers to get a good thumbnail
        CatalogBuilder cb = new CatalogBuilder(catalog);
        for (FeatureTypeInfo ft :
                catalog.getResourcesByNamespace(CiteTestData.CITE_URI, FeatureTypeInfo.class)) {
            cb.setupBounds(ft);
            catalog.save(ft);
        }

        // add a work-spaced style
        testData.addWorkspace("ws", "http://www.geoserver.org/ws", catalog);
        WorkspaceInfo ws = catalog.getWorkspaceByName("ws");
        testData.addStyle(ws, "NamedPlaces", "NamedPlaces.sld", SystemTestData.class, catalog);
        // add a style with a comment that can be used for raw style return
        testData.addStyle(POLYGON_COMMENT, "polygonComment.sld", StylesTestSupport.class, catalog);

        testData.addStyle(
                null,
                "cssSample",
                "cssSample.css",
                StylesTestSupport.class,
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, "css"));
    }
}
