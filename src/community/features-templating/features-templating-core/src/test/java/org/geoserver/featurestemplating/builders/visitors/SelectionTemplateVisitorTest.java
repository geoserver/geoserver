package org.geoserver.featurestemplating.builders.visitors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.selectionwrappers.DynamicPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.PropertySelectionWrapper;
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
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
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
        tb.setName("testFeatureType");
        SimpleFeatureType schema = tb.buildFeatureType();
        schema.getDescriptor("b").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        schema.getDescriptor("c").getUserData().put(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
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
        TemplateBuilder builder = children.get(0);
        builder = builder.getChildren().get(0);
        for (TemplateBuilder child : builder.getChildren()) {
            AbstractTemplateBuilder abstractBuilder = (AbstractTemplateBuilder) child;
            String key = abstractBuilder.getKey(null);
            if ("b".equals(key)) {
                assertTrue(child instanceof DynamicPropertySelection);
            } else if ("three".equals(key)) {
                TemplateBuilder dynamicInclude = abstractBuilder.getChildren().get(1);
                assertTrue(dynamicInclude instanceof DynamicIncludeFlatBuilder);
            }
            assertFalse("a".equals(key));
            assertFalse("two".equals(key));
        }
    }

    @Test
    public void testPropertySelection2() throws IOException {
        PropertySelectionHandler propertySelectionHandler = handler();
        RootBuilder root = getBuilderTree("testTemplateOvr.json");
        PropertySelectionVisitor visitor =
                new PropertySelectionVisitor(propertySelectionHandler, simpleFeatureType);
        root = (RootBuilder) root.accept(visitor, null);
        List<TemplateBuilder> children = root.getChildren();
        TemplateBuilder builder = children.get(0);
        builder = builder.getChildren().get(0).getChildren().get(0);
        for (TemplateBuilder child : builder.getChildren()) {
            AbstractTemplateBuilder abstractBuilder = (AbstractTemplateBuilder) child;
            Expression key = abstractBuilder.getKey();
            if (key != null && !(key instanceof Literal)) {
                // dynamic key are wrapped
                assertTrue(child instanceof PropertySelectionWrapper);
            }
        }
    }

    private PropertySelectionHandler handler() {
        PropertySelectionHandler propertySelectionHandler =
                new AbstractPropertySelection() {

                    @Override
                    public boolean mustWrapJsonValueBuilder(AbstractTemplateBuilder builder) {
                        if ("b".equals(builder.getKey(null))) return true;
                        return false;
                    }

                    @Override
                    protected boolean isKeySelected(String key) {
                        if (key != null && (key.contains("two") || key.contains("a"))) return false;
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
