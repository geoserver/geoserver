/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Properties;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;

public class StylesTest extends GeoServerSystemTestSupport {

    @Test
    public void testLookup() throws Exception {
        assertTrue(Styles.handler(SLDHandler.FORMAT) instanceof SLDHandler);
        assertTrue(Styles.handler(PropertyStyleHandler.FORMAT) instanceof PropertyStyleHandler);
        try {
            Styles.handler(null);
            fail();
        } catch (Exception e) {
        }

        try {
            Styles.handler("foo");
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testParse() throws Exception {
        Properties props = new Properties();
        props.setProperty("type", "point");
        props.setProperty("color", "ff0000");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        props.store(bout, null);

        StyledLayerDescriptor sld =
                Styles.handler(PropertyStyleHandler.FORMAT)
                        .parse(new ByteArrayInputStream(bout.toByteArray()), null, null, null);
        assertNotNull(sld);

        Style style = Styles.style(sld);
        PointSymbolizer point = SLD.pointSymbolizer(style);
        assertNotNull(point);
    }

    @Test
    public void testEmptyStyle() throws Exception {
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(StylesTest.class.getResourceAsStream("empty.sld"));
        StyledLayerDescriptor sld = parser.parseSLD();

        assertNull(Styles.style(sld));
    }

    @Test
    public void testParseStyleTwiceLock() throws Exception {
        StyleInfo style = getCatalog().getStyles().get(0);
        FileSystemResourceStore store =
                (FileSystemResourceStore) getDataDirectory().getResourceStore();
        store.setLockProvider(new MemoryLockProvider());
        // parse twice to check we are not locking on it
        Resource resource = getDataDirectory().style(style);
        Styles.handler(style.getFormat())
                .parse(resource, style.getFormatVersion(), new DefaultResourceLocator(), null);
        Styles.handler(style.getFormat())
                .parse(resource, style.getFormatVersion(), new DefaultResourceLocator(), null);
    }

    @Test
    public void testEntityExpansionOnValidation() throws Exception {
        URL url = getClass().getResource("../data/test/externalEntities.sld");
        try {
            Styles.handler("SLD")
                    .validate(url, null, getCatalog().getResourcePool().getEntityResolver());
            fail("Should have failed due to the entity resolution attempt");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message, containsString("Entity resolution disallowed"));
            assertThat(message, containsString("/this/file/does/not/exist"));
        }
    }
}
