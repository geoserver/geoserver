/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.stac;

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
import java.util.Map;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class STACQueryablesBuilderTest {

    private static final String FAKE_ID = "foobar";
    static OpenSearchAccess data;

    @BeforeClass
    public static void setupClass() throws IOException, SQLException {
        data = JDBCOpenSearchAccessTest.setupAndReturnStore();
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
        STACQueryablesBuilder builder =
                new STACQueryablesBuilder(FAKE_ID, template.getRootBuilder(), products.getSchema());
        Queryables queryables = builder.getQueryables();
        System.out.println(queryables.getProperties().keySet());

        // check the time range properties have been replaced by a single property
        Map<String, Schema> properties = queryables.getProperties();
        assertThat(properties, not(hasKey("start_datetime")));
        assertThat(properties, not(hasKey("end_datetime")));

        // check the geometry
        Schema geometry = properties.get("geometry");
        assertNotNull(geometry);
        assertEquals("https://geojson.org/schema/Polygon.json", geometry.get$ref());

        // check the datetime
        Schema datetime = properties.get("datetime");
        assertNotNull(datetime);
        assertEquals("string", datetime.getType());
        assertEquals("date-time", datetime.getFormat());

        // check the creation time
        Schema created = properties.get("created");
        assertNotNull(datetime);
        assertEquals("string", created.getType());
        assertEquals("date-time", created.getFormat());

        // check the constellation
        Schema constellation = properties.get("constellation");
        assertNotNull(constellation);
        assertEquals("string", constellation.getType());
        assertNull(constellation.getFormat());

        // check the cloud cover, which has a math expression
        Schema cloudCover = properties.get("eo:cloud_cover");
        assertNotNull(cloudCover);
        assertEquals("integer", cloudCover.getType());

        // check the minimum incidence angle, uses a float property
        Schema mia = properties.get("sar:minimum_incidence_angle");
        assertNotNull(mia);
        assertEquals("number", mia.getType());

        // check the test:the_max, uses a function between doubles
        Schema max = properties.get("test:the_max");
        assertNotNull(max);
        assertEquals("number", max.getType());

        // check a nested property
        Schema nested = properties.get("one.two.three");
        assertNotNull(nested);
        assertEquals("number", max.getType());
    }
}
