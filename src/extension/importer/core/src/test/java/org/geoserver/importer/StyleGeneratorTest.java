/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class StyleGeneratorTest {

    @Test
    public void testCreateInWorkspace() throws Exception {
        ResourcePool rp = createNiceMock(ResourcePool.class);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getFactory()).andReturn(new CatalogFactoryImpl(null)).anyTimes();
        expect(cat.getResourcePool()).andReturn(rp).anyTimes();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getName()).andReturn("foo").anyTimes();

        replay(rp, ft, ws, cat);

        StyleGenerator gen = new StyleGenerator(cat);
        gen.setWorkspace(ws);

        SimpleFeatureType schema = DataUtilities.createType("foo", "geom:Point");
        StyleInfo style = gen.createStyle(ft, schema);
        assertNotNull(style);
        assertNotNull(style.getWorkspace());
    }

    @Test
    public void testGenericStyle() throws Exception {
        ResourcePool rp = createNiceMock(ResourcePool.class);
        rp.writeStyle((StyleInfo) anyObject(), (InputStream) anyObject());
        expectLastCall().andAnswer(new IAnswer<Void>() {

            @Override
            public Void answer() throws Throwable {
                Object[] args = getCurrentArguments();
                InputStream is = (InputStream) args[1];
                SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
                parser.setInput(is);
                StyledLayerDescriptor sld = parser.parseSLD();
                
                NamedLayer nl = (NamedLayer) sld.getStyledLayers()[0];
                assertEquals("foo", nl.getName());
                Style style = nl.getStyles()[0];
                assertEquals("A orange generic style", style.getDescription().getTitle()
                        .toString());
                assertEquals(1, style.featureTypeStyles().size());
                FeatureTypeStyle fts = style.featureTypeStyles().get(0);
                assertEquals("first", fts.getOptions().get("ruleEvaluation"));
                assertEquals(4, fts.rules().size());
                assertEquals("raster", fts.rules().get(0).getDescription().getTitle().toString());
                assertEquals("orange polygon", fts.rules().get(1).getDescription().getTitle()
                        .toString());
                assertEquals("orange line", fts.rules().get(2).getDescription().getTitle()
                        .toString());
                assertEquals("orange point", fts.rules().get(3).getDescription().getTitle()
                        .toString());
                
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

        StyleGenerator gen = new StyleGenerator(cat) {
            protected void randomizeRamp() {
                // do not randomize for this test
            };
        };
        gen.setWorkspace(ws);

        SimpleFeatureType schema = DataUtilities.createType("foo", "geom:Geometry");
        StyleInfo style = gen.createStyle(ft, schema);
        assertNotNull(style);
        assertNotNull(style.getWorkspace());
    }
}
