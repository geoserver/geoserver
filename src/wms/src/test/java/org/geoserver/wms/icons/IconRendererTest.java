/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.image.test.ImageAssert;
import org.geotools.styling.StyleFactory;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Mark;
import org.opengis.style.Rule;
import org.opengis.style.SemanticType;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;

public class IconRendererTest {
    /** Upscaled images need a higher threshold for pdiff */
    static final int THRESHOLD = 400;

    @Test
    public void testSimpleCircle() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("circle"),
                        sfact.fill(null, ffact.literal("#FF0000"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList((GraphicalSymbol) m),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("circle-red-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testSimpleSquare() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("square"),
                        sfact.fill(null, ffact.literal("#0000FF"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList((GraphicalSymbol) m),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("square-blue-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testSquareRotated45() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("square"),
                        sfact.fill(null, ffact.literal("#0000FF"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList((GraphicalSymbol) m),
                        Expression.NIL,
                        Expression.NIL,
                        ffact.literal(45.0),
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int baseSize = 16;
        final int rotated = (int) Math.ceil(baseSize * Math.sqrt(2));
        final int size = (rotated + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected =
                ImageIO.read(this.getClass().getResource("square-blue-16-x4-45deg.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testExternalImage() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("arrow-16.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("arrow-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testExternalImageRotated45() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("arrow-16.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        ffact.literal(45.0),
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int baseSize = 16;
        final int rotated = (int) Math.ceil(baseSize * Math.sqrt(2));
        final int size = (rotated + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("arrow-16-x4-45deg.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImage() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImageSpecifySize() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        ffact.literal(42),
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImageNilExpressionSize() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.<Name>emptySet(),
                        Collections.<SemanticType>emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }
}
