/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.junit.Test;

/**
 * Unit tests for {@link CatalogFileReader#stripDynamicProxyMarkers(String)}.
 *
 * <p>The backup writer serialises a catalog reference whose value is an unresolved {@code ResolvingProxy} (e.g. a layer
 * whose {@code <defaultStyle>} points at a missing/dangling style) with an XStream {@code class="dynamic-proxy"} type
 * marker around the {@code <id>}/{@code <name>} body. Core's {@code ReferenceConverter} only accepts the declared
 * reference type, so without stripping the marker the restore step fails with {@code ConversionException: Explicit
 * selected converter cannot handle type}.
 */
public class CatalogFileReaderTest {

    private static String layerWithDynamicProxyStyle(String styleRef) {
        return "<layer>\n"
                + "  <name>dyn_layer</name>\n"
                + "  <id>LayerInfoImpl-DYN</id>\n"
                + "  <type>VECTOR</type>\n"
                + "  <defaultStyle class=\"dynamic-proxy\">\n"
                + "    <id>"
                + styleRef
                + "</id>\n"
                + "  </defaultStyle>\n"
                + "</layer>";
    }

    @Test
    public void stripRemovesDynamicProxyMarkerButKeepsBody() {
        String stripped = CatalogFileReader.stripDynamicProxyMarkers(layerWithDynamicProxyStyle("ws:missing"));
        assertFalse("dynamic-proxy marker removed", stripped.contains("dynamic-proxy"));
        assertTrue("reference body left intact", stripped.contains("<id>ws:missing</id>"));
    }

    @Test
    public void strippedReferenceUnmarshalsAndResolves() {
        CatalogImpl catalog = new CatalogImpl();
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("dyn_style");
        s.setFilename("dyn_style.sld");
        catalog.add(s);

        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.setCatalog(catalog);
        xp.setReferenceByName(true);

        String stripped = CatalogFileReader.stripDynamicProxyMarkers(layerWithDynamicProxyStyle(s.getId()));
        LayerInfo l = (LayerInfo) xp.getXStream().fromXML(stripped);

        // unmarshalling no longer throws "Explicit selected converter cannot handle type", and the
        // dynamic-proxy-wrapped reference resolves to the style (LayerInfo.getName() is derived from the
        // resource, which this minimal fixture omits, so it is not asserted here)
        assertNotNull(l);
        assertEquals(s, l.getDefaultStyle());
    }
}
