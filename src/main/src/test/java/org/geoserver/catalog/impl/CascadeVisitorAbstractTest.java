/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.geoserver.data.test.CiteTestData.*;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;

public class CascadeVisitorAbstractTest extends GeoServerSystemTestSupport {
    protected static final String LAKES_GROUP = "lakesGroup";
    protected static final String NEST_GROUP = "nestGroup";

    protected static final String WS_STYLE = "wsStyle";

    protected void setUpTestData(org.geoserver.data.test.SystemTestData testData) throws Exception {
        // add nothing here
    };

    protected void onSetUp(org.geoserver.data.test.SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();

        // add layers
        testData.addVectorLayer(LAKES, catalog);
        testData.addVectorLayer(BRIDGES, catalog);
        testData.addVectorLayer(FORESTS, catalog);
        testData.addVectorLayer(BUILDINGS, catalog);

        setupExtras(testData, catalog);
    }

    void setupExtras(org.geoserver.data.test.SystemTestData testData, Catalog catalog)
            throws IOException {
        // associate Lakes to Buildings as an extra style
        LayerInfo buildings = catalog.getLayerByName(getLayerId(BUILDINGS));
        buildings.getStyles().add(catalog.getStyleByName(LAKES.getLocalPart()));
        catalog.save(buildings);

        // add a layer group
        CatalogFactory factory = catalog.getFactory();
        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName(LAKES_GROUP);
        globalGroup.getLayers().add(catalog.getLayerByName(getLayerId(LAKES)));
        globalGroup.getLayers().add(catalog.getLayerByName(getLayerId(FORESTS)));
        globalGroup.getLayers().add(catalog.getLayerByName(getLayerId(BRIDGES)));
        globalGroup.getStyles().add(null);
        globalGroup.getStyles().add(null);
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        // add a layer group containing a layer group
        LayerGroupInfo nestGroup = factory.createLayerGroup();
        nestGroup.setName(NEST_GROUP);
        nestGroup.getLayers().add(catalog.getLayerByName(getLayerId(LAKES)));
        nestGroup.getLayers().add(globalGroup);
        nestGroup.getStyles().add(null);
        nestGroup.getStyles().add(null);
        catalog.add(nestGroup);

        // add a workspace specific style
        WorkspaceInfo ws = catalog.getWorkspaceByName(CITE_PREFIX);
        testData.addStyle(ws, WS_STYLE, "Streams.sld", SystemTestData.class, catalog);
    };
}
