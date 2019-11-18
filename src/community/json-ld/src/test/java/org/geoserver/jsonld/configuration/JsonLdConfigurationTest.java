/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.easymock.EasyMock;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.StaticBuilder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.GenericApplicationContext;

public class JsonLdConfigurationTest {

    GenericApplicationContext genContext;
    FeatureTypeInfo typeInfo;

    @Before
    public void setUp() {
        typeInfo = Mockito.mock(FeatureTypeInfo.class);
        NamespaceInfo namespace = new NamespaceInfoImpl();
        namespace.setPrefix("prefix");
        when(typeInfo.getName()).thenReturn("name");
        when(typeInfo.getNamespace()).thenReturn(namespace);
        File f = new File(getClass().getResource("Station_gml31.json").getFile());
        GeoServerDataDirectory dd = getGeoserverDD(f, typeInfo, "path");
        genContext = new GenericApplicationContext();
        JsonLdConfiguration template = new JsonLdConfiguration(dd);
        genContext.getBeanFactory().registerSingleton("jsonldConfiguration", template);
        new GeoServerExtensions().setApplicationContext(genContext);
        genContext.refresh();
    }

    @Test
    public void loadingTest() throws ExecutionException, IOException {
        JsonBuilder builder = JsonLdConfiguration.get().getTemplate(typeInfo, "path");
        assertNotNull(builder);
        assertNotNull(builder.getChildren());
        testChildrenBuilder(builder);
    }

    private void testChildrenBuilder(JsonBuilder builder) {
        for (JsonBuilder b : builder.getChildren()) {
            if (b instanceof DynamicValueBuilder) {
                assertTrue(
                        ((DynamicValueBuilder) b).getCql() != null
                                || ((DynamicValueBuilder) b).getXpath() != null);
            } else if (b instanceof StaticBuilder) {
                assertNotNull(((StaticBuilder) b).getStaticValue());
            } else {
                assertNotNull(b.getChildren());
                testChildrenBuilder(b);
            }
        }
    }

    public static GeoServerDataDirectory getGeoserverDD(
            File file, FeatureTypeInfo typeInfo, String path) {
        GeoServerDataDirectory dd = EasyMock.createMock(GeoServerDataDirectory.class);
        Resource resource = Files.asResource(file);
        expect(dd.get(typeInfo, path)).andReturn(resource).anyTimes();
        replay(dd);
        return dd;
    }

    @After
    public void clear() {
        new GeoServerExtensions().setApplicationContext(null);
    }
}
