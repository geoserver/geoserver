/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class JsonTemplateReaderTest {

    @Test
    public void testVendorOptionWithEnvFunction() throws IOException {

        RootBuilder root = getBuilderTree("FlatGeoJSONMappedFeature.json");
        // flat_output option is true checking type is FlatBuilder
        testBuildersType(root.getChildren(), b -> b instanceof FlatBuilder);
        Map<String, Object> envValues = new HashMap<String, Object>();
        envValues.put("flat_output", "false");
        EnvFunction.setLocalValues(envValues);
        root = getBuilderTree("FlatGeoJSONMappedFeature.json");
        // if env parametrization on VendorOptions works flat_output should be false now
        testBuildersType(root.getChildren(), b -> !(b instanceof FlatBuilder));
    }

    @Test
    public void testNestedArray() throws IOException, CQLException {
        RootBuilder root = getBuilderTree("nestedArray.json");
        IteratingBuilder it = (IteratingBuilder) root.getChildren().get(0);
        assertEquals("bbox", it.getKey());
        // the nested builder does not have a key
        IteratingBuilder nested = (IteratingBuilder) it.getChildren().get(0);
        assertNull(nested.getKey());
    }

    @Test
    public void testArrayWithoutSource() throws IOException, CQLException {
        RootBuilder root = getBuilderTree("arrayWithoutSource.json");
        IteratingBuilder it = (IteratingBuilder) root.getChildren().get(0);
        // first child is dynamic due to the filter
        CompositeBuilder cb1 = (CompositeBuilder) it.getChildren().get(0);
        assertEquals(ECQL.toFilter("xpath('name') = 'LANDSAT8'"), cb1.getFilter());
        // second is as well, but due to a property
        CompositeBuilder cb2 = (CompositeBuilder) it.getChildren().get(1);
        assertNull(cb2.getFilter());
        DynamicValueBuilder db2 = (DynamicValueBuilder) cb2.getChildren().get(0);
        assertEquals("name", db2.getKey());
        assertEquals(ECQL.toExpression("name"), db2.getXpath());
        // third is a static
        assertThat(it.getChildren().get(2), instanceOf(StaticBuilder.class));
    }

    private RootBuilder getBuilderTree(String resourceName) throws IOException {
        InputStream is = getClass().getResource(resourceName).openStream();
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JSONTemplateReader templateReader =
                new JSONTemplateReader(mapper.readTree(is), new TemplateReaderConfiguration(null));
        return templateReader.getRootBuilder();
    }

    private void testBuildersType(
            List<TemplateBuilder> builders, Predicate<TemplateBuilder> predicate) {
        for (TemplateBuilder builder : builders) {
            assertTrue(predicate.test(builder));
            if (builder.getChildren() != null) testBuildersType(builder.getChildren(), predicate);
        }
    }
}
