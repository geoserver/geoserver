/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateLayerConfig;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class TemplateComplexTestSupport extends AbstractAppSchemaTestSupport {

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

    protected void setUpComplex(
            String fileName, String workspace, String templateFileName, FeatureTypeInfo ft)
            throws IOException {
        String resourceLocation =
                "workspaces/" + workspace + "/" + ft.getStore().getName() + "/" + ft.getName();
        File file = dd.get(resourceLocation, templateFileName).file();
        file.createNewFile();
        dd.getResourceLoader().copyFromClassPath(fileName, file, getClass());
    }

    protected void setUpComplex(String fileName, String workspace, FeatureTypeInfo ft)
            throws IOException {
        setUpComplex(fileName, workspace, getTemplateFileName(), ft);
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
            assertThat(
                    contentType,
                    anyOf(equalTo("application/json"), equalTo("application/geo+json")));
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

    protected void setUpTemplate(
            String cqlRuleCondition,
            SupportedFormat outputFormat,
            String templateFileName,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        String rawTemplate =
                IOUtils.toString(getClass().getResourceAsStream(templateFileName), Charsets.UTF_8);
        TemplateInfo info = new TemplateInfo();
        info.setExtension(templateExtension);
        info.setTemplateName(templateName);
        info.setWorkspace(workspace);
        info.setFeatureType(ft.getNativeName());
        TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, rawTemplate);
        TemplateRule rule = new TemplateRule();
        rule.setTemplateName(info.getFullName());
        rule.setCqlFilter(cqlRuleCondition);
        rule.setOutputFormat(outputFormat);
        rule.setTemplateIdentifier(info.getIdentifier());
        TemplateLayerConfig config = new TemplateLayerConfig();
        config.addTemplateRule(rule);
        ft.getMetadata().put(TemplateLayerConfig.METADATA_KEY, config);
        getCatalog().save(ft);
    }
}
