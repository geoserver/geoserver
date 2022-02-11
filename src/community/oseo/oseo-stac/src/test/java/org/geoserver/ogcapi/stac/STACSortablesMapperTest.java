/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class STACSortablesMapperTest {

    static OpenSearchAccess data;

    @BeforeClass
    public static void setupClass() throws IOException, SQLException {
        data = JDBCOpenSearchAccessTest.setupAndReturnStore();
    }

    @AfterClass
    public static void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    @Test
    public void testGetSortable() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema());
        Map<String, String> sortables = builder.getSortablesMap();

        // common sortables from spec
        assertThat(sortables, hasEntry("collection", "parentIdentifier"));
        assertThat(sortables, hasEntry("datetime", "timeStart"));
        assertThat(sortables, hasEntry("id", "identifier"));

        // common properties in the model
        assertThat(sortables, hasEntry("created", "eop:creationDate"));
        assertThat(sortables, hasEntry("view:sun_azimuth", "eop:illuminationAzimuthAngle"));

        // one custom, and nested, property
        assertThat(sortables, hasEntry("one.two.three", "eop:illuminationAzimuthAngle"));
    }

    @Test
    public void testSortablesIncludeFlat() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-SAS1.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema());
        Map<String, String> sortables = builder.getSortablesMap();

        // common sortables from spec
        assertThat(sortables, hasEntry("collection", "parentIdentifier"));
        assertThat(sortables, hasEntry("datetime", "timeStart"));
        assertThat(sortables, hasEntry("id", "identifier"));

        // custom sortables from template
        assertThat(sortables, hasEntry("view:sun_azimuth", "eop:illuminationAzimuthAngle"));
        assertThat(sortables, hasEntry("custom:clouds", "opt:cloudCover"));
    }
}
