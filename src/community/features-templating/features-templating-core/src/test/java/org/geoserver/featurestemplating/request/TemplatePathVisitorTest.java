/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.request;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class TemplatePathVisitorTest {

    FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Test
    public void testBackwardMapping() throws IOException {
        RootBuilder rootBuilder = getBuilderTree("testTemplateBackwardMapping.json");
        TemplatePathVisitor pathVisitor = new TemplatePathVisitor(false);
        PropertyName pn = FF.property("root/nested/nested2/g");
        PropertyName result = (PropertyName) pn.accept(pathVisitor, rootBuilder);
        assertEquals("a/b/c/d/e/e/f/g", result.getPropertyName());
    }

    @Test
    public void testBackwardMappingWithBackDotsProps() throws IOException {
        RootBuilder rootBuilder = getBuilderTree("testTemplateBackwardMapping.json");
        TemplatePathVisitor pathVisitor = new TemplatePathVisitor(false);
        PropertyName pn = FF.property("root/e");
        PropertyName result = (PropertyName) pn.accept(pathVisitor, rootBuilder);
        assertEquals("b/c/d/e", result.getPropertyName());
    }

    @Test
    public void testBackwardMappingWithBackDotsProps2() throws IOException {
        RootBuilder rootBuilder = getBuilderTree("testTemplateBackwardMapping.json");
        TemplatePathVisitor pathVisitor = new TemplatePathVisitor(false);
        PropertyName pn = FF.property("root/nested/d");
        PropertyName result = (PropertyName) pn.accept(pathVisitor, rootBuilder);
        assertEquals("a/b/b/c/d", result.getPropertyName());
    }

    @Test
    public void testBackwardMappingWithBackDotsProps3() throws IOException {
        RootBuilder rootBuilder = getBuilderTree("testTemplateBackwardMapping.json");
        TemplatePathVisitor pathVisitor = new TemplatePathVisitor(false);
        PropertyName pn = FF.property("root/nested/nested2/f");
        PropertyName result = (PropertyName) pn.accept(pathVisitor, rootBuilder);
        assertEquals("a/b/b/e/f", result.getPropertyName());
    }

    private RootBuilder getBuilderTree(String resourceName) throws IOException {
        InputStream is = getClass().getResource(resourceName).openStream();
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JSONTemplateReader templateReader =
                new JSONTemplateReader(
                        mapper.readTree(is),
                        new TemplateReaderConfiguration(new NamespaceSupport()),
                        Collections.emptyList());
        return templateReader.getRootBuilder();
    }
}
