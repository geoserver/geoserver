/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;
import org.opengis.filter.expression.Function;

public class RasterSymbolizerVisitorTest {

    @Test
    public void testRasterRenderingTransformation() throws IOException {
        Style style = parseStyle("CropTransform.sld");
        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor(1000, null);
        style.accept(visitor);
        List<RasterSymbolizer> symbolizers = visitor.getRasterSymbolizers();
        assertEquals(1, symbolizers.size());
        Function tx = (Function) visitor.getRasterRenderingTransformation();
        assertNotNull(tx);
        assertEquals("ras:CropCoverage", tx.getName());
    }

    @Test
    public void testRasterToVectorTransformation() throws IOException {
        Style style = parseStyle("ContourTransform.sld");
        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor(1000, null);
        style.accept(visitor);
        List<RasterSymbolizer> symbolizers = visitor.getRasterSymbolizers();
        assertEquals(0, symbolizers.size());
        Function tx = (Function) visitor.getRasterRenderingTransformation();
        assertNull(tx);
    }

    @Test
    public void testVectorToRasterRenderingTransformation() throws IOException {
        Style style = parseStyle("HeatmapTransform.sld");
        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor(1000, null);
        style.accept(visitor);
        List<RasterSymbolizer> symbolizers = visitor.getRasterSymbolizers();
        assertEquals(1, symbolizers.size());
        Function tx = (Function) visitor.getRasterRenderingTransformation();
        assertNotNull(tx);
        assertEquals("vec:Heatmap", tx.getName());
    }

    private Style parseStyle(String styleName) throws IOException {
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(RasterSymbolizerVisitorTest.class.getResource(styleName));
        StyledLayerDescriptor sld = parser.parseSLD();
        NamedLayer ul = (NamedLayer) sld.getStyledLayers()[0];
        return ul.getStyles()[0];
    }
}
