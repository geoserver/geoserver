/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.selectionwrappers.DynamicPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.IncludeFlatPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.MergePropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.StaticPropertySelection;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.RecursiveJSONParser;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.helpers.NamespaceSupport;

public class SelectionTemplateVisitorTest {
    private SimpleFeatureType simpleFeatureType;
    private FileSystemResourceStore store;

    @Before
    public void setup() {
        store =
                new FileSystemResourceStore(
                        new File(
                                "src/test/resources/org/geoserver/featurestemplating/builders/visitors"));
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("geometry", Geometry.class);
        tb.add("one", String.class);
        tb.add("b", String.class);
        tb.add("c", String.class);
        tb.add("e", String.class);
        tb.add("four", String.class);
        tb.add("five", String.class);
        tb.setName("testFeatureType");
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("b").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("c").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("e").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");

        this.simpleFeatureType = schema;
    }

    @Test
    public void testPropertySelectionVisitor() throws IOException {
        PropertySelectionHandler propertySelectionHandler = handler();
        RootBuilder root = getBuilderTree("testTemplate.json");
        PropertySelectionVisitor visitor =
                new PropertySelectionVisitor(propertySelectionHandler, simpleFeatureType);
        root = (RootBuilder) root.accept(visitor, null);
        List<TemplateBuilder> children = root.getChildren();
        // iterating builder
        TemplateBuilder builder = children.get(0);
        builder = builder.getChildren().get(0);

        TemplateBuilder geom = builder.getChildren().get(0);
        assertEquals("geometry", ((AbstractTemplateBuilder) geom).getKey(null));

        builder = builder.getChildren().get(1);
        assertEquals("properties", ((AbstractTemplateBuilder) builder).getKey(null));
        for (TemplateBuilder child : builder.getChildren()) {
            AbstractTemplateBuilder abstractBuilder = (AbstractTemplateBuilder) child;
            String key = abstractBuilder.getKey(null);
            if ("one".equals(key)) assertTrue(child instanceof DynamicValueBuilder);
            if ("b".equals(key)) {
                assertTrue(child instanceof DynamicPropertySelection);
            } else if ("three".equals(key)) {
                TemplateBuilder composite = abstractBuilder.getChildren().get(0);
                // a was filtered out;
                assertEquals(1, composite.getChildren().size());
                assertTrue(composite.getChildren().get(0) instanceof IncludeFlatPropertySelection);
            }
            assertFalse("two".equals(key));
        }
        Set<String> props = visitor.getQueryProperties();
        List<String> expected = Arrays.asList("geometry", "one", "b", "c");
        assertEquals(expected.size(), props.size());
        assertTrue(props.containsAll(expected));
    }

    @Test
    public void testPropertySelectionWithMerge() throws IOException {
        PropertySelectionHandler propertySelectionHandler = handler();
        RootBuilder root = getBuilderTree("testTemplateOvr.json");
        PropertySelectionVisitor visitor =
                new PropertySelectionVisitor(propertySelectionHandler, simpleFeatureType);
        root = (RootBuilder) root.accept(visitor, null);
        List<TemplateBuilder> children = root.getChildren();
        TemplateBuilder builder = children.get(0).getChildren().get(0).getChildren().get(1);
        assertEquals(6, builder.getChildren().size());
        for (TemplateBuilder child : builder.getChildren()) {
            AbstractTemplateBuilder abstractBuilder = (AbstractTemplateBuilder) child;
            String key = abstractBuilder.getKey(null);
            if ("b".equals(key)) assertTrue(child instanceof DynamicPropertySelection);
            else if ("e".equals(key)) assertTrue(child instanceof MergePropertySelection);
            else if (key == null) assertTrue(child instanceof StaticPropertySelection);
            else if ("d".equals(key)) assertTrue(child instanceof StaticPropertySelection);
            else if ("three".equals(key)) {
                TemplateBuilder composite = child.getChildren().get(0);
                assertEquals(1, composite.getChildren().size());
                assertTrue(composite.getChildren().get(0) instanceof IncludeFlatPropertySelection);
            } else assertTrue(child instanceof StaticBuilder);
        }
        Set<String> props = visitor.getQueryProperties();
        List<String> expected = Arrays.asList("b", "c", "d", "e", "one", "geometry");
        assertEquals(expected.size(), props.size());
        assertTrue(props.containsAll(expected));
    }

    @Test
    public void testPropertySelectionRootDynamicIncludeFlat() throws IOException {
        PropertySelectionHandler propertySelectionHandler = handler();
        RootBuilder root = getBuilderTree("testTemplateOvr2.json");
        PropertySelectionVisitor visitor =
                new PropertySelectionVisitor(propertySelectionHandler, simpleFeatureType);
        root = (RootBuilder) root.accept(visitor, null);
        List<TemplateBuilder> children = root.getChildren();
        TemplateBuilder builder = children.get(0).getChildren().get(0);

        // only the includeFlat builder
        assertEquals(1, builder.getChildren().size());

        assertTrue(builder.getChildren().get(0) instanceof IncludeFlatPropertySelection);
        // check that even the property names in the baseNode of the IncludeFlatBuilder
        // are correctly pickedUp
        List<String> expected = Arrays.asList("b", "one", "geometry", "five", "e");
        assertEquals(expected.size(), visitor.getQueryProperties().size());
        assertTrue(visitor.getQueryProperties().containsAll(expected));
    }

    private PropertySelectionHandler handler() {
        PropertySelectionHandler propertySelectionHandler =
                new AbstractPropertySelection() {

                    @Override
                    public boolean hasSelectableJsonValue(AbstractTemplateBuilder builder) {
                        if ("b".equals(builder.getKey(null))) return true;
                        return false;
                    }

                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null
                                && (key.contains("two")
                                        || key.contains("a")
                                        || key.contains("four")
                                        || key.contains("five"))) return false;
                        return true;
                    }
                };
        return propertySelectionHandler;
    }

    private RootBuilder getBuilderTree(String resourceName) throws IOException {
        Resource resource = store.get(resourceName);
        RecursiveJSONParser parser = new RecursiveJSONParser(resource);
        parser.parse();
        JSONTemplateReader templateReader =
                new JSONTemplateReader(
                        parser.parse(),
                        new TemplateReaderConfiguration(new NamespaceSupport()),
                        parser.getWatchers());
        return templateReader.getRootBuilder();
    }
}
