/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.ogcapi.OGCApiTestSupport;

public class StylesTestSupport extends OGCApiTestSupport {

    protected static final String POLYGON_COMMENT = "PolygonComment";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();

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
        testData.addStyle(ws, "NamedPlacesWS", "NamedPlaces.sld", SystemTestData.class, catalog);
        // add a style with a comment that can be used for raw style return
        testData.addStyle(POLYGON_COMMENT, "polygonComment.sld", StylesTestSupport.class, catalog);

        testData.addStyle(
                null,
                "cssSample",
                "cssSample.css",
                StylesTestSupport.class,
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, "css"));

        // add a style groupd
        testData.addStyle(
                "BasicStyleGroupStyle",
                "BasicStyleGroup.sld",
                CollectionCallbackIntegrationTest.class,
                getCatalog());
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        StyleInfo s = catalog.getStyleByName("BasicStyleGroupStyle");

        lg.setName("BasicStyleGroup");
        lg.getLayers().add(null);
        lg.getStyles().add(s);
        new CatalogBuilder(catalog).calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }
}
