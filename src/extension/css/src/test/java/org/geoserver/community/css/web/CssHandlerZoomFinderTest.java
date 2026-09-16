/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.zoom.ListZoomContext;
import org.geotools.styling.zoom.ZoomContext;
import org.geotools.styling.zoom.ZoomContextFinder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Checks that the CSS handler picks up zoom context finders registered after it was built. Looking them up in the
 * constructor makes GeoServer fail with a circular bean reference at startup.
 */
public class CssHandlerZoomFinderTest {

    /** Scale denominators of the test tile matrix set, far from any real gridset. */
    private static final List<Double> SCALES = List.of(1000d, 500d, 250d);

    @Rule
    public GeoServerExtensionsHelper.ExtensionsHelperRule extensions =
            new GeoServerExtensionsHelper.ExtensionsHelperRule();

    @Test
    public void testZoomFinderRegisteredAfterConstruction() throws Exception {
        CssHandler handler = new CssHandler(new GeoServerExtensions(), new SLDHandler());

        extensions.singleton("testZoomContextFinder", new TestZoomContextFinder(), ZoomContextFinder.class);

        StyledLayerDescriptor sld = handler.convertToSLD("@tileMatrixSet 'testGrid'; [@z = 1] {stroke: black}");
        org.geotools.api.style.Rule rule = ((NamedLayer) sld.getStyledLayers()[0])
                .getStyles()[0]
                .featureTypeStyles()
                .get(0)
                .rules()
                .get(0);
        assertEquals(353.5, rule.getMinScaleDenominator(), 0.1);
        assertEquals(707.1, rule.getMaxScaleDenominator(), 0.1);
    }

    private static class TestZoomContextFinder implements ZoomContextFinder {

        private final ZoomContext context = new ListZoomContext(SCALES);

        @Override
        public ZoomContext get(String name) {
            return "testGrid".equals(name) ? context : null;
        }

        @Override
        public Set<String> getNames() {
            return Set.of("testGrid");
        }

        @Override
        public Set<String> getCanonicalNames() {
            return getNames();
        }
    }
}
