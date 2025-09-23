/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.style.AnchorPoint;
import org.geotools.api.style.ChannelSelection;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.ContrastEnhancement;
import org.geotools.api.style.Displacement;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.FeatureTypeConstraint;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.GraphicalSymbol;
import org.geotools.api.style.Halo;
import org.geotools.api.style.ImageOutline;
import org.geotools.api.style.LinePlacement;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.Mark;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.OverlapBehavior;
import org.geotools.api.style.PointPlacement;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.SelectedChannelType;
import org.geotools.api.style.ShadedRelief;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleVisitor;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.Symbol;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.api.style.UserLayer;
import org.geotools.filter.FilterAttributeExtractor;

/**
 * A style visitor whose purpose is to extract a minimal palette for the provided style. This is to be used when no
 * antialiasing is used, since that would introduce various colors not included in the style. <br>
 * At the moment the palette is extracted only if external graphics aren't referenced (a future version may learn to
 * extract a palette merging the ones of the external graphics).
 */
public class PaletteExtractor extends FilterAttributeExtractor implements StyleVisitor {
    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);
    private static final int TRANSPARENT_CODE = 255 << 16 | 255 << 8 | 255;

    Set<Color> colors;
    boolean translucentSymbolizers;
    boolean externalGraphicsSymbolizers;
    boolean unknownColors;
    boolean rasterUsed;

    /**
     * Initializes a new palette extractor
     *
     * @param background background color, or null if transparent
     */
    public PaletteExtractor(Color background) {
        super(null);
        colors = new HashSet<>();
        if (background == null) background = TRANSPARENT;
        colors.add(background);
    }

    public boolean canComputePalette() {
        // hard fail conditions
        if (translucentSymbolizers || externalGraphicsSymbolizers || unknownColors || rasterUsed) return false;

        // impossible to devise a palette (0 shuold never happen, but you never know...)
        if (colors.isEmpty() || colors.size() > 256) return false;

        return true;
    }

    /** Returns the palette, or null if it wasn't possible to devise one */
    public IndexColorModel getPalette() {
        if (!canComputePalette()) return null;

        int[] cmap = new int[colors.size()];
        int i = 0;
        for (Color color : colors) {
            cmap[i++] = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        }

        // have a nice looking palette
        Arrays.sort(cmap);
        int transparentIndex = cmap[cmap.length - 1] == TRANSPARENT_CODE ? cmap.length - 1 : -1;

        // find out the minimum number of bits required to represent the palette, and return it
        int bits = 8;
        if (cmap.length <= 2) {
            bits = 1;
        } else if (cmap.length <= 4) {
            bits = 2;
        } else if (cmap.length <= 16) {
            bits = 4;
        }

        // workaround for GEOS-1341, GEOS-1337 will try to find a solution
        //      int length = (int) Math.pow(2, bits);
        int length = bits == 1 ? 2 : 256;
        if (cmap.length < length) {
            int[] temp = new int[length];
            System.arraycopy(cmap, 0, temp, 0, cmap.length);
            cmap = temp;
        }

        return new IndexColorModel(bits, cmap.length, cmap, 0, true, transparentIndex, DataBuffer.TYPE_BYTE);
    }

    /**
     * Checks whether translucency is used in the provided expression. Raises the flag of used translucency unless it's
     * possible to determine it's not.
     */
    void handleOpacity(Expression opacity) {
        if (opacity == null) return;
        if (opacity instanceof Literal lo) {
            double value = lo.evaluate(null, Double.class).doubleValue();
            translucentSymbolizers = translucentSymbolizers || value != 1;
        } else {
            // we cannot know, so we assume some will be non opaque
            translucentSymbolizers = true;
        }
    }

    /**
     * Adds a color to the color set, and raises the unknown color flag if the color is an expression other than a
     * literal
     */
    void handleColor(Expression color) {
        if (color == null) return;
        if (color instanceof Literal lc) {
            String rgbColor = lc.evaluate(null, String.class);
            colors.add(Color.decode(rgbColor));
        } else {
            unknownColors = true;
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Style) */
    @Override
    public void visit(Style style) {
        style.featureTypeStyles().forEach(ft -> ft.accept(this));
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Rule) */
    @Override
    public void visit(Rule rule) {
        Filter filter = rule.getFilter();

        if (filter != null) {
            filter.accept(this, null);
        }

        rule.symbolizers().forEach(s -> s.accept(this));
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.FeatureTypeStyle) */
    @Override
    public void visit(FeatureTypeStyle fts) {
        fts.rules().forEach(r -> r.accept(this));
    }

    @Override
    public void visit(StyledLayerDescriptor sld) {
        StyledLayer[] layers = sld.getStyledLayers();

        for (StyledLayer layer : layers) {
            if (layer instanceof NamedLayer namedLayer) {
                namedLayer.accept(this);
            } else if (layer instanceof UserLayer userLayer) {
                userLayer.accept(this);
            }
        }
    }

    @Override
    public void visit(NamedLayer layer) {
        Style[] styles = layer.getStyles();

        for (Style style : styles) {
            style.accept(this);
        }
    }

    @Override
    public void visit(UserLayer layer) {
        Style[] styles = layer.getUserStyles();

        for (Style style : styles) {
            style.accept(this);
        }
    }

    @Override
    public void visit(FeatureTypeConstraint ftc) {
        // nothing to do
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Symbolizer) */
    @Override
    public void visit(Symbolizer sym) {
        if (sym instanceof PointSymbolizer symbolizer) {
            visit(symbolizer);
        }

        if (sym instanceof LineSymbolizer symbolizer) {
            visit(symbolizer);
        }

        if (sym instanceof PolygonSymbolizer symbolizer) {
            visit(symbolizer);
        }

        if (sym instanceof TextSymbolizer symbolizer) {
            visit(symbolizer);
        }

        if (sym instanceof RasterSymbolizer symbolizer) {
            visit(symbolizer);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Fill) */
    @Override
    public void visit(Fill fill) {
        handleColor(fill.getColor());

        if (fill.getGraphicFill() != null) fill.getGraphicFill().accept(this);

        handleOpacity(fill.getOpacity());
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Stroke) */
    @Override
    public void visit(Stroke stroke) {
        handleColor(stroke.getColor());

        if (stroke.getGraphicFill() != null) {
            stroke.getGraphicFill().accept(this);
        }

        if (stroke.getGraphicStroke() != null) {
            stroke.getGraphicStroke().accept(this);
        }

        handleOpacity(stroke.getOpacity());
    }

    @Override
    public void visit(RasterSymbolizer rs) {
        rasterUsed = true;
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.PointSymbolizer) */
    @Override
    public void visit(PointSymbolizer ps) {
        if (ps.getGraphic() != null) {
            ps.getGraphic().accept(this);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.LineSymbolizer) */
    @Override
    public void visit(LineSymbolizer line) {
        if (line.getStroke() != null) {
            line.getStroke().accept(this);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.PolygonSymbolizer) */
    @Override
    public void visit(PolygonSymbolizer poly) {
        if (poly.getStroke() != null) {
            poly.getStroke().accept(this);
        }

        if (poly.getFill() != null) {
            poly.getFill().accept(this);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.TextSymbolizer) */
    @Override
    public void visit(TextSymbolizer text) {
        if (text instanceof TextSymbolizer) {
            if (text.getGraphic() != null) text.getGraphic().accept(this);
        }

        if (text.getFill() != null) {
            text.getFill().accept(this);
        }

        if (text.getHalo() != null) {
            text.getHalo().accept(this);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Graphic) */
    @Override
    public void visit(Graphic gr) {
        for (GraphicalSymbol symbol : gr.graphicalSymbols()) {
            if (symbol instanceof Symbol) {
                symbol.accept(this);
            } else {
                throw new RuntimeException("Don't know how to copy " + symbol);
            }
        }

        handleOpacity(gr.getOpacity());
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Mark) */
    @Override
    public void visit(Mark mark) {
        if (mark.getFill() != null) {
            mark.getFill().accept(this);
        }

        if (mark.getStroke() != null) {
            mark.getStroke().accept(this);
        }
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.ExternalGraphic) */
    @Override
    public void visit(ExternalGraphic exgr) {
        externalGraphicsSymbolizers = true;
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.PointPlacement) */
    @Override
    public void visit(PointPlacement pp) {
        // nothing to do
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.AnchorPoint) */
    @Override
    public void visit(AnchorPoint ap) {
        // nothing to do
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Displacement) */
    @Override
    public void visit(Displacement dis) {
        // nothing to do
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.LinePlacement) */
    @Override
    public void visit(LinePlacement lp) {
        // nothing to do
    }

    /** @see org.geotools.api.style.StyleVisitor#visit(org.geotools.api.style.Halo) */
    @Override
    public void visit(Halo halo) {
        if (halo.getFill() != null) {
            halo.getFill().accept(this);
        }
    }

    @Override
    public void visit(ColorMap map) {
        // for the moment we don't do anything
        unknownColors = true;
    }

    @Override
    public void visit(ColorMapEntry entry) {
        unknownColors = true;
    }

    @Override
    public void visit(ContrastEnhancement contrastEnhancement) {
        unknownColors = true;
    }

    @Override
    public void visit(ImageOutline outline) {
        unknownColors = true;
    }

    @Override
    public void visit(ChannelSelection cs) {
        unknownColors = true;
    }

    @Override
    public void visit(OverlapBehavior ob) {
        unknownColors = true;
    }

    @Override
    public void visit(SelectedChannelType sct) {
        unknownColors = true;
    }

    @Override
    public void visit(ShadedRelief sr) {
        unknownColors = true;
    }
}
