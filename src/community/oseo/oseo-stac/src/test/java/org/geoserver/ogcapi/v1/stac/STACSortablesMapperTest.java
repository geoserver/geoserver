/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OSEOInfoImpl;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class STACSortablesMapperTest {
    private static final String FAKE_ID = "foobar";

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
        OSEOInfo service = new OSEOInfoImpl();
        STACQueryablesBuilder qbuilder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        null,
                        service);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema(), qbuilder);
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
    public void testGetSortableLimitedByGlobalConfiguration() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        OSEOInfoImpl service = new OSEOInfoImpl();
        service.setGlobalQueryables(
                Collections.singletonList(
                        "one.two.three")); // limits sortables to standard set + one.two.three
        STACQueryablesBuilder qbuilder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        null,
                        service);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema(), qbuilder);
        Map<String, String> sortables = builder.getSortablesMap();

        // common sortables from spec
        assertThat(sortables, hasEntry("collection", "parentIdentifier"));
        assertThat(sortables, hasEntry("datetime", "timeStart"));
        assertThat(sortables, hasEntry("id", "identifier"));

        assertThat(
                sortables,
                // missing because not in the global queryables
                not(hasEntry("created", "eop:creationDate")));
        assertThat(sortables, not(hasEntry("view:sun_azimuth", "eop:illuminationAzimuthAngle")));

        // one custom, and nested, property
        assertThat(
                sortables,
                hasEntry(
                        "one.two.three",
                        // present because it is in the global queryables
                        "eop:illuminationAzimuthAngle"));
    }

    @Test
    public void testGetSortableLimitedByCollectionConfiguration() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        OSEOInfoImpl service = new OSEOInfoImpl();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter collectionSampleFilter = ff.equals(ff.property("name"), ff.literal("LANDSAT8"));
        Feature sampleCollectionFeature =
                DataUtilities.first(data.getCollectionSource().getFeatures(collectionSampleFilter));
        STACQueryablesBuilder qbuilder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        sampleCollectionFeature,
                        service);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema(), qbuilder);
        Map<String, String> sortables = builder.getSortablesMap();

        // common sortables from spec, always available to sortables
        assertThat(sortables, hasEntry("collection", "parentIdentifier"));
        assertThat(sortables, hasEntry("datetime", "timeStart"));
        assertThat(sortables, hasEntry("id", "identifier"));

        assertThat(
                sortables,
                // missing because not in the global or collection queryables
                not(hasEntry("missing", "eo:wavelength")));
        assertThat(sortables, not(hasEntry("missing", "eo:wavelength")));

        assertThat(
                sortables,
                hasEntry(
                        "view:sun_azimuth",
                        // present because it eop:illuminationAzimuthAngle is in the collection
                        // queryables
                        "eop:illuminationAzimuthAngle"));
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
        OSEOInfo service = new OSEOInfoImpl();
        STACQueryablesBuilder qbuilder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        null,
                        service);
        STACSortablesMapper builder =
                new STACSortablesMapper(template.getRootBuilder(), products.getSchema(), qbuilder);
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
