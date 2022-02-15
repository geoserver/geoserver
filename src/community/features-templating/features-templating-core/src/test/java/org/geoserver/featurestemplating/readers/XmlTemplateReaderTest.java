/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class XmlTemplateReaderTest {

    FileSystemResourceStore store;

    @Before
    public void setupStore() {
        store = new FileSystemResourceStore(new File("src/test/resources/xmlincludes"));
    }

    @Test
    public void testFlatInclusion() throws IOException {
        XMLRecursiveTemplateReader reader =
                new GMLTemplateReader(
                        store.get("MappedFeatureIncludeFlat.xml"), new NamespaceSupport());
        RootBuilder rootBuilder = reader.getRootBuilder();
        TemplateBuilder mappedFeatureBuilder =
                rootBuilder.getChildren().get(0).getChildren().get(0).getChildren().get(0);
        TemplateBuilder specification = null;
        for (TemplateBuilder b : mappedFeatureBuilder.getChildren()) {
            if (((AbstractTemplateBuilder) b).getKey(null).equals("gsml:specification")) {
                specification = b;
                break;
            }
        }
        assertNotNull(specification);
        assertEquals(2, specification.getChildren().size());
        assertTrue(specification.getChildren().get(1) instanceof StaticBuilder);
        AbstractTemplateBuilder geologicUnit =
                (AbstractTemplateBuilder) specification.getChildren().get(0);
        assertEquals("gsml:GeologicUnit", geologicUnit.getKey(null));
        assertEquals(4, geologicUnit.getChildren().size());
    }

    @Test
    public void testInlineInclusion() throws IOException {
        XMLRecursiveTemplateReader reader =
                new GMLTemplateReader(
                        store.get("MappedFeatureInclude.xml"), new NamespaceSupport());
        RootBuilder rootBuilder = reader.getRootBuilder();
        TemplateBuilder mappedFeatureBuilder = rootBuilder.getChildren().get(0);
        TemplateBuilder specification = null;
        for (TemplateBuilder b : mappedFeatureBuilder.getChildren()) {
            String key = ((AbstractTemplateBuilder) b).getKey(null);
            if (key != null && key.equals("gsml:specification")) {
                specification = b;
                break;
            }
        }
        assertNotNull(specification);
        AbstractTemplateBuilder geologicUnit =
                (AbstractTemplateBuilder) specification.getChildren().get(0);
        assertEquals("gsml:GeologicUnit", geologicUnit.getKey(null));
        assertTrue(geologicUnit.getChildren().size() > 0);
    }

    @Test
    public void testNotExistingInclusion() {
        checkThrowingTemplate("MappedFeatureIncludeNotExisting.xml");
    }

    @Test
    public void testRecursiveInclusion() {
        RuntimeException ex = checkThrowingTemplate("ping.xml");
        assertThat(
                ex.getMessage(),
                containsString("Went beyond maximum expansion depth (51), chain is: [ping.xml"));
    }

    @Test
    public void testIncludedModificationAreDetected() throws IOException, InterruptedException {
        XMLRecursiveTemplateReader reader =
                new GMLTemplateReader(
                        store.get("MappedFeatureIncludeFlat.xml"), new NamespaceSupport());
        RootBuilder rootBuilder = reader.getRootBuilder();
        assertFalse(rootBuilder.needsReload());
        File file = store.get("includedGeologicUnit.xml").file();
        file.setLastModified(new Date().getTime());

        for (int i = 0; i < 600; i++) {
            if (rootBuilder.needsReload()) return; // ok worked
            Thread.sleep(100);
        }
        fail("Should have found a reload 60 seconds, but did not");
    }

    private RuntimeException checkThrowingTemplate(String s) {
        return assertThrows(
                RuntimeException.class,
                () -> new GMLTemplateReader(store.get(s), new NamespaceSupport()).getRootBuilder());
    }
}
