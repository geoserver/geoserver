/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.VendorOptions.COLLECTION_NAME;
import static org.geoserver.featurestemplating.builders.VendorOptions.JSONLD_TYPE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.flat.FlatBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.expressions.XpathFunction;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class JsonTemplateReaderTest {

    NamespaceSupport namespaceSuport;

    @Before
    public void setupNamespaceSupport() {
        namespaceSuport = new NamespaceSupport();
        namespaceSuport.declarePrefix("", "http://www.geoserver.org");
    }

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
        assertEquals("bbox", it.getKey(null));
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
        assertEquals(ECQL.toFilter("name = 'LANDSAT8'"), cb1.getFilter());
        // check the namespace support has been set in the filter
        PropertyIsEqualTo pe = (PropertyIsEqualTo) cb1.getFilter();
        PropertyName pnName = (PropertyName) pe.getExpression1();
        assertEquals(namespaceSuport, pnName.getNamespaceContext());

        // second is as well, but due to a property
        CompositeBuilder cb2 = (CompositeBuilder) it.getChildren().get(1);
        assertNull(cb2.getFilter());
        DynamicValueBuilder db2 = (DynamicValueBuilder) cb2.getChildren().get(0);
        assertEquals("name", db2.getKey(null));
        assertEquals(ECQL.toExpression("strSubstring(name, 1, 5)"), db2.getCql());
        // check the namespace support has been set in the CQL expression too
        Function fn = (Function) db2.getCql();
        PropertyName fname = (PropertyName) fn.getParameters().get(0);
        assertEquals(namespaceSuport, fname.getNamespaceContext());

        // this is another dynamic, this time with a filter using the xpath function
        CompositeBuilder cb3 = (CompositeBuilder) it.getChildren().get(2);
        assertEquals(ECQL.toFilter("xpath('name') = 'SENTINEL2'"), cb3.getFilter());
        // check the xpath function also has namespace support
        PropertyIsEqualTo peXPath = (PropertyIsEqualTo) cb3.getFilter();
        XpathFunction xpath = (XpathFunction) peXPath.getExpression1();
        assertEquals(namespaceSuport, xpath.getNamespaceContext());

        // third is a static
        assertThat(it.getChildren().get(3), instanceOf(StaticBuilder.class));
    }

    @Test
    public void testOptionsParsingForRootAttributes() throws IOException {
        RootBuilder root = getBuilderTree("jsonld_custom_root_attrs.json");
        VendorOptions options = root.getVendorOptions();
        assertEquals("diseaseSpreadStatistics", options.get(COLLECTION_NAME, String.class));
        assertEquals("schema:SpecialAnnouncement", options.get(JSONLD_TYPE, String.class));
    }

    private RootBuilder getBuilderTree(String resourceName) throws IOException {
        InputStream is = getClass().getResource(resourceName).openStream();
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JSONTemplateReader templateReader =
                new JSONTemplateReader(
                        mapper.readTree(is),
                        new TemplateReaderConfiguration(namespaceSuport),
                        Collections.emptyList());
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
