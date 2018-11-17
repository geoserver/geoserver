/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class OSEORestTestSupport extends OSEOTestSupport {

    protected static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    public void loginAdmin() {
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
    }

    @Before
    public void cleanupTestCollection() throws IOException {
        DataStoreInfo ds = getCatalog().getDataStoreByName("oseo");
        OpenSearchAccess access = (OpenSearchAccess) ds.getDataStore(null);
        FeatureStore store = (FeatureStore) access.getCollectionSource();
        store.removeFeatures(
                FF.equal(
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("TEST123"),
                        true));
    }

    @Before
    public void cleanupTestCollectionPublishing() throws IOException {
        Catalog catalog = getCatalog();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        removePublishing(catalog, visitor, "gs", "test123");
        removePublishing(catalog, visitor, "gs", "test123-secondary");
    }

    private void removePublishing(
            Catalog catalog, CascadeDeleteVisitor visitor, String workspace, String resourceName) {
        CoverageStoreInfo store =
                catalog.getStoreByName(workspace, resourceName, CoverageStoreInfo.class);
        if (store != null) {
            visitor.visit(store);
        }
        StyleInfo style = catalog.getStyleByName(workspace, resourceName);
        if (style != null) {
            visitor.visit(style);
        }
        Resource data = catalog.getResourceLoader().get("data/" + workspace + "/" + resourceName);
        if (data != null && Resources.exists(data)) {
            data.delete();
        }
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            System.out.println(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }

    protected byte[] getTestData(String location) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream(location));
    }

    protected void createTest123Collection() throws Exception, IOException {
        // create the collection
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections",
                        getTestData("/collection.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/TEST123",
                response.getHeader("location"));
    }
}
