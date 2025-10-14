/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gsr.validation.JSONValidator;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ControllerTest extends GeoServerSystemTestSupport {

    private Catalog catalog;

    private String baseURL = "/gsr/services/";

    @Override
    protected final void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
    }

    @Override
    public void onSetUp(SystemTestData testData) throws Exception {
        catalog = getCatalog();
        CatalogFactory catalogFactory = catalog.getFactory();

        NamespaceInfo ns = catalogFactory.createNamespace();
        ns.setPrefix("nsPrefix");
        ns.setURI("nsURI");

        WorkspaceInfo ws = catalogFactory.createWorkspace();
        ws.setName("LocalWorkspace");

        DataStoreInfo ds = catalogFactory.createDataStore();
        ds.setEnabled(true);
        ds.setName("dsName");
        ds.setDescription("dsDescription");
        ds.setWorkspace(ws);

        FeatureTypeInfo ft1 = catalogFactory.createFeatureType();
        ft1.setEnabled(true);
        ft1.setName("layer1");
        ft1.setAbstract("ftAbstract");
        ft1.setDescription("ftDescription");
        ft1.setStore(ds);
        ft1.setNamespace(ns);

        LayerInfo layer1 = catalogFactory.createLayer();
        layer1.setResource(ft1);
        layer1.setName("layer1");

        FeatureTypeInfo ft2 = catalogFactory.createFeatureType();
        ft2.setEnabled(true);
        ft2.setName("layer2");
        ft2.setAbstract("ftAbstract2");
        ft2.setDescription("ftDescription2");
        ft2.setStore(ds);
        ft2.setNamespace(ns);

        LayerInfo layer2 = catalogFactory.createLayer();
        layer2.setResource(ft2);
        layer2.setName("layer2");

        catalog.add(ns);
        catalog.add(ws);
        catalog.add(ds);
        catalog.add(ft1);
        catalog.add(ft2);
        catalog.add(layer1);
        catalog.add(layer2);
    }

    @Test
    public void testConfig() {
        assertEquals("/gsr/services/", this.baseURL);
    }

    protected boolean validateJSON(String json, String schemaPath) {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/schemas/";
        return JSONValidator.isValidSchema(json, new File(workingDir + schemaPath));
    }

    public String getBaseURL() {
        return baseURL;
    }

    protected MockHttpServletResponse getAsMockHttpServletResponse(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(expectedHttpCode, response.getStatus());
        return response;
    }
}
