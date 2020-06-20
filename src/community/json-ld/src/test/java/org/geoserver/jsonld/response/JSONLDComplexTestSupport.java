/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.response;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import net.sf.json.JSON;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDComplexTestSupport extends AbstractAppSchemaTestSupport {

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

    protected void setUpComplex() throws IOException {
        setUpComplex("MappedFeature.json", "gsml", mappedFeature);
    }

    protected void setUpComplex(String templateFileName, FeatureTypeInfo ft) throws IOException {
        setUpComplex(templateFileName, "gsml", ft);
    }

    protected void setUpComplex(String templateFileName, String workspace, FeatureTypeInfo ft)
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
                                JsonLdConfiguration.JSON_LD_NAME);

        dd.getResourceLoader().copyFromClassPath(templateFileName, file, getClass());
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

    @After
    public void cleanup() {
        Resource res =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + mappedFeature.getStore().getName()
                                        + "/"
                                        + mappedFeature.getName()
                                        + "/"
                                        + JsonLdConfiguration.JSON_LD_NAME);
        if (res != null) res.delete();

        Resource res2 =
                dd.getResourceLoader()
                        .get(
                                "workspaces/gsml/"
                                        + geologicUnit.getStore().getName()
                                        + "/"
                                        + geologicUnit.getName()
                                        + "/"
                                        + JsonLdConfiguration.JSON_LD_NAME);
        if (res2 != null) res2.delete();

        Resource res3 =
                dd.getResourceLoader()
                        .get(
                                "workspaces/ex/"
                                        + parentFeature.getStore().getName()
                                        + "/"
                                        + parentFeature.getName()
                                        + "/"
                                        + JsonLdConfiguration.JSON_LD_NAME);
        if (res3 != null) res3.delete();
    }
}
