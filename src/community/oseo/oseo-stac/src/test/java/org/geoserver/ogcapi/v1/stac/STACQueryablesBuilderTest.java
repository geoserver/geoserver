/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.v1.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.ogcapi.Queryables;
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
import org.geotools.filter.text.cql2.CQL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

public class STACQueryablesBuilderTest {

    private static final String FAKE_ID = "foobar";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_INTEGER = "integer";
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
    public void testGetQueryables() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        OSEOInfo service = new OSEOInfoImpl();
        STACQueryablesBuilder builder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        null,
                        service);
        Queryables queryables = builder.getQueryables();

        // check the time range properties have been replaced by a single property
        Map<String, Schema> properties = queryables.getProperties();
        assertThat(properties, not(hasKey("start_datetime")));
        assertThat(properties, not(hasKey("end_datetime")));

        // check the id
        Schema id = properties.get("id");
        assertNotNull(id);
        assertEquals(STACQueryablesBuilder.ID_SCHEMA_REF, id.get$ref());

        // check the geometry
        Schema geometry = properties.get("geometry");
        assertNotNull(geometry);
        assertEquals(STACQueryablesBuilder.GEOMETRY_SCHEMA_REF, geometry.get$ref());

        // check the collection
        Schema collection = properties.get("collection");
        assertNotNull(collection);
        assertEquals(STACQueryablesBuilder.COLLECTION_SCHEMA_REF, collection.get$ref());

        // check the datetime
        Schema datetime = properties.get("datetime");
        assertNotNull(datetime);
        assertEquals(STACQueryablesBuilder.DATETIME_SCHEMA_REF, datetime.get$ref());

        // check the creation time
        Schema created = properties.get("created");
        assertNotNull(created);
        assertEquals(TYPE_STRING, created.getType());
        assertEquals("date-time", created.getFormat());

        // check the constellation
        Schema constellation = properties.get("constellation");
        assertNotNull(constellation);
        assertEquals(TYPE_STRING, constellation.getType());
        assertNull(constellation.getFormat());

        // check the cloud cover, which has a math expression
        Schema cloudCover = properties.get("eo:cloud_cover");
        assertNotNull(cloudCover);
        assertEquals(TYPE_INTEGER, cloudCover.getType());

        // check the minimum incidence angle, uses a float property
        Schema mia = properties.get("sar:minimum_incidence_angle");
        assertNotNull(mia);
        assertEquals(TYPE_NUMBER, mia.getType());

        // check the test:the_max, uses a function between doubles
        Schema max = properties.get("test:the_max");
        assertNotNull(max);
        assertEquals(TYPE_NUMBER, max.getType());

        // check a nested property
        Schema nested = properties.get("one.two.three");
        assertNotNull(nested);
        assertEquals(TYPE_NUMBER, max.getType());
    }

    @Test
    public void testGetQueryablesUncustomized() throws Exception {
        // setup data and templates
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter collectionSampleFilter = ff.equals(ff.property("name"), ff.literal("GS_TEST"));
        Feature sampleCollectionFeature =
                DataUtilities.first(data.getCollectionSource().getFeatures(collectionSampleFilter));
        // to reproduce the queryables must not have been configured, they must be null
        assertNull(
                sampleCollectionFeature.getProperty(
                        STACQueryablesBuilder.DEFINED_QUERYABLES_PROPERTY));
        OSEOInfo service = new OSEOInfoImpl();
        STACQueryablesBuilder builder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        sampleCollectionFeature,
                        service);

        // use to NPE here
        Queryables queryables = builder.getQueryables();

        // check a random queryable just in case
        // check the creation time
        Map<String, Schema> properties = queryables.getProperties();
        Schema created = properties.get("created");
        assertNotNull(created);
        assertEquals(TYPE_STRING, created.getType());
        assertEquals("date-time", created.getFormat());
    }

    @Test
    public void testGetQueryablesTopLevelProperty() throws Exception {
        // setup data and templates
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-test.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter collectionSampleFilter = ff.equals(ff.property("name"), ff.literal("SENTINEL2"));
        Feature sampleCollectionFeature =
                DataUtilities.first(data.getCollectionSource().getFeatures(collectionSampleFilter));
        OSEOInfo service = new OSEOInfoImpl();
        STACQueryablesBuilder builder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        null,
                        sampleCollectionFeature,
                        service);

        // grab the queryables
        Queryables queryables = builder.getQueryables();
        Map<String, Schema> properties = queryables.getProperties();
        System.out.println(properties);

        // check the keywords queryable, top level, not in "properties"
        Schema keywords = properties.get("keywords");
        assertNotNull(keywords);
        assertEquals(TYPE_STRING, keywords.getType());

        // this one is inside properties instead
        Schema cloudCover = properties.get("eo:cloud_cover");
        assertNotNull(cloudCover);
        assertEquals(TYPE_INTEGER, cloudCover.getType());

        // check the expression map as well
        Map<String, Expression> expressions = builder.getExpressionMap();
        assertEquals(CQL.toExpression("keywords"), expressions.get("keywords"));
    }

    @Test
    public void testQueryablesIncludeFlat() throws Exception {
        FileSystemResourceStore resourceStore =
                new FileSystemResourceStore(new File("./src/test/resources"));
        Resource templateDefinition = resourceStore.get("items-SAS1.json");
        FeatureSource<FeatureType, Feature> products = data.getProductSource();
        FeatureSource<FeatureType, Feature> collections = data.getCollectionSource();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter sampleFilter =
                ff.equals(ff.property("identifier"), ff.literal("SAS1_20180226102021.01"));
        Feature sampleFeature = DataUtilities.first(products.getFeatures(sampleFilter));
        Filter collectionSampleFilter = ff.equals(ff.property("identifier"), ff.literal("SAS1"));
        Feature sampleCollectionFeature =
                DataUtilities.first(collections.getFeatures(collectionSampleFilter));
        TemplateReaderConfiguration config =
                new TemplateReaderConfiguration(STACTemplates.getNamespaces(products));
        Template template = new Template(templateDefinition, config);
        OSEOInfo service = new OSEOInfoImpl();
        service.getGlobalQueryables().addAll(Arrays.asList("id", "geometry", "collection"));
        STACQueryablesBuilder builder =
                new STACQueryablesBuilder(
                        FAKE_ID,
                        template.getRootBuilder(),
                        products.getSchema(),
                        sampleFeature,
                        sampleCollectionFeature,
                        service);
        Queryables queryables = builder.getQueryables();
        Map<String, Schema> properties = queryables.getProperties();

        // queryables from spec
        Schema id = properties.get("id");
        assertNotNull(id);
        assertEquals(STACQueryablesBuilder.ID_SCHEMA_REF, id.get$ref());

        Schema geometry = properties.get("geometry");
        assertNotNull(geometry);
        assertEquals(STACQueryablesBuilder.GEOMETRY_SCHEMA_REF, geometry.get$ref());

        Schema collection = properties.get("collection");
        assertNotNull(collection);
        assertEquals(STACQueryablesBuilder.COLLECTION_SCHEMA_REF, collection.get$ref());

        Schema datetime = properties.get("datetime");
        assertNotNull(datetime);
        assertEquals(STACQueryablesBuilder.DATETIME_SCHEMA_REF, datetime.get$ref());

        // custom queryables from mapping file
        Schema azimuth = properties.get("view:sun_azimuth");
        assertNotNull(azimuth);
        assertEquals(TYPE_NUMBER, azimuth.getType());

        Schema clouds = properties.get("custom:clouds");
        assertNotNull(clouds);
        assertEquals(TYPE_INTEGER, clouds.getType());

        // extra queryable from inside the dynamically included JSON
        Schema meanSolarAzimuth = properties.get("s2:mean_solar_azimuth");
        assertNotNull(meanSolarAzimuth);
        assertEquals("number", meanSolarAzimuth.getType());

        Schema dataStripId = properties.get("s2:datastrip_id");
        assertNotNull(dataStripId);
        assertEquals("string", dataStripId.getType());

        Schema anxDateTime = properties.get("sat:anx_datetime");
        assertNotNull(anxDateTime);
        assertEquals("string", anxDateTime.getType());
        assertEquals("date-time", anxDateTime.getFormat());

        // confirm that properties not included in the queryables array are excluded
        Schema platform = properties.get("platform");
        assertNull(platform);

        Schema constellation = properties.get("constellation");
        assertNull(platform);
    }
}
