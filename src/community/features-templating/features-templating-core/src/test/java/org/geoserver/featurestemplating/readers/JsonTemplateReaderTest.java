/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geotools.filter.function.EnvFunction;
import org.junit.Test;

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

    private RootBuilder getBuilderTree(String resourceName) throws IOException {
        InputStream is = getClass().getResource(resourceName).openStream();
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JsonTemplateReader templateReader = new JsonTemplateReader(mapper.readTree(is), null);
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
