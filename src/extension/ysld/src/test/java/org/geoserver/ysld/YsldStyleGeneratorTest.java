/*
/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.StyleType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geotools.data.DataUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class YsldStyleGeneratorTest {

    /**
     * Test integration of {@link YsldHandler#getStyle(org.geoserver.catalog.StyleType,
     * java.awt.Color, String, String)} with the {@link StyleGenerator} by generating a generic
     * style (from {@link YsldHandler#TEMPLATES} for {@link StyleType.GENERIC}).
     */
    @Test
    public void testYsldStyleGenerator() throws Exception {
        final YsldHandler handler = new YsldHandler();
        ResourcePool rp = createNiceMock(ResourcePool.class);
        rp.writeStyle((StyleInfo) anyObject(), (InputStream) anyObject());
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                Object[] args = getCurrentArguments();
                                InputStream is = (InputStream) args[1];
                                StyledLayerDescriptor sld = handler.parse(is, null, null, null);

                                assertEquals(1, sld.getStyledLayers().length);

                                NamedLayer nl = (NamedLayer) sld.getStyledLayers()[0];
                                assertEquals(1, nl.getStyles().length);

                                Style style = nl.getStyles()[0];
                                assertEquals(1, style.featureTypeStyles().size());

                                FeatureTypeStyle fts = style.featureTypeStyles().get(0);
                                assertEquals(4, fts.rules().size());
                                assertEquals(
                                        "raster",
                                        fts.rules().get(0).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange polygon",
                                        fts.rules().get(1).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange line",
                                        fts.rules().get(2).getDescription().getTitle().toString());
                                assertEquals(
                                        "orange point",
                                        fts.rules().get(3).getDescription().getTitle().toString());

                                for (org.geotools.styling.Rule r : fts.rules()) {
                                    assertEquals(1, r.symbolizers().size());
                                }

                                return null;
                            }
                        });

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getFactory()).andReturn(new CatalogFactoryImpl(null)).anyTimes();
        expect(cat.getResourcePool()).andReturn(rp).anyTimes();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getName()).andReturn("foo").anyTimes();

        replay(rp, ft, ws, cat);

        StyleGenerator gen =
                new StyleGenerator(cat) {
                    protected void randomizeRamp() {
                        // do not randomize for this test
                    };
                };
        gen.setWorkspace(ws);

        SimpleFeatureType schema = DataUtilities.createType("foo", "geom:Geometry");
        StyleInfo style = gen.createStyle(handler, ft, schema);
        assertNotNull(style);
        assertNotNull(style.getWorkspace());
    }
}
