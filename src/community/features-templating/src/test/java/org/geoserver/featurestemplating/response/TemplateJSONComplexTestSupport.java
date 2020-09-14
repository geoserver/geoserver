/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class TemplateJSONComplexTestSupport extends AbstractAppSchemaTestSupport {

    Catalog catalog;
    FeatureTypeInfo mappedFeature;
    FeatureTypeInfo geologicUnit;
    FeatureTypeInfo parentFeature;
    GeoServerDataDirectory dd;

    @Before
    public void before() {
        catalog = getCatalog();

        mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        geologicUnit = catalog.getFeatureTypeByName("gsml", "GeologicUnit");
        parentFeature = catalog.getFeatureTypeByName("ex", "FirstParentFeature");
        dd = (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");
    }

    protected void setUpMappedFeature() throws IOException {
        setUpComplex("MappedFeature.json", mappedFeature);
    }

    protected void setUpMappedFeature(String fileName) throws IOException {
        setUpComplex(fileName, mappedFeature);
    }

    protected void setUpComplex(String fileName, FeatureTypeInfo ft) throws IOException {
        setUpComplex(fileName, "gsml", ft);
    }

    protected void setUpComplex(String fileName, String workspace, FeatureTypeInfo ft)
            throws IOException {
        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/"
                                        + workspace
                                        + "/"
                                        + ft.getStore().getName()
                                        + "/"
                                        + ft.getName(),
                                getTemplateFileName());

        dd.getResourceLoader().copyFromClassPath(fileName, file, getClass());
    }

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    protected JSON postJsonLd(String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    protected JSON getJson(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String contentType = response.getContentType();
        // in case of GEOSJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null)
            assertTrue(
                    contentType.equals("application/json")
                            || contentType.equals("application/geo+json"));
        return json(response);
    }

    protected JSON postJson(String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        String contentType = response.getContentType();
        // in case of GEOSJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null) assertEquals(contentType, "application/json");
        return json(response);
    }

    @After
    public void cleanup() {
        String templateFileName = getTemplateFileName();
        Resource res =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + mappedFeature.getStore().getName()
                                        + "/"
                                        + mappedFeature.getName()
                                        + "/"
                                        + templateFileName);
        if (res != null) res.delete();

        Resource res2 =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + geologicUnit.getStore().getName()
                                        + "/"
                                        + geologicUnit.getName()
                                        + "/"
                                        + templateFileName);
        if (res2 != null) res2.delete();

        Resource res3 =
                dd.getResourceLoader()
                        .get(
                                "workspaces/ex/"
                                        + parentFeature.getStore().getName()
                                        + "/"
                                        + parentFeature.getName()
                                        + "/"
                                        + getTemplateFileName());
        if (res3 != null) res3.delete();
    }

    protected abstract String getTemplateFileName();

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

    protected void checkContext(Object context) {
        if (context instanceof JSONArray) {
            int size = ((JSONArray) context).size();
            assertTrue(size > 0);
        }
        if (context instanceof JSONObject) {
            assertFalse(((JSONObject) context).isEmpty());
        }
    }
}
