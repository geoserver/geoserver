/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class TemplateJSONSimpleTestSupport extends GeoServerSystemTestSupport {

    Catalog catalog;
    FeatureTypeInfo typeInfo;
    GeoServerDataDirectory dd;

    @Before
    public void before() {
        catalog = getCatalog();

        typeInfo =
                catalog.getFeatureTypeByName(
                        MockData.CITE_PREFIX, MockData.NAMED_PLACES.getLocalPart());
        dd = (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");
    }

    protected void setUpSimple(String fileName) throws IOException {
        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/cite/"
                                        + typeInfo.getStore().getName()
                                        + "/"
                                        + typeInfo.getName(),
                                getTemplateFileName());
        dd.getResourceLoader().copyFromClassPath(fileName, file, getClass());
    }

    @After
    public void cleanup() {
        Resource res =
                dd.getResourceLoader()
                        .get(
                                "workspaces/cite/"
                                        + typeInfo.getStore().getName()
                                        + "/"
                                        + typeInfo.getName()
                                        + "/"
                                        + getTemplateFileName());
        if (res != null) res.delete();
    }

    protected abstract String getTemplateFileName();

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    protected JSON getJson(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String contentType = response.getContentType();
        // in case of GEOSJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null) assertEquals(contentType, "application/json");
        return json(response);
    }

    protected void checkAdditionalInfo(JSONObject result) {
        assertNotNull(result.get("numberReturned"));
        assertNotNull(result.get("timeStamp"));
        if (result.has("crs")) {
            JSONObject crs = result.getJSONObject("crs");
            JSONObject props = crs.getJSONObject("properties");
            assertNotNull(props);
            assertNotNull(props.getString("name"));
        }
        if (result.has("links")) {
            JSONArray links = result.getJSONArray("links");
            assertTrue(links.size() > 0);
        }
    }
}
