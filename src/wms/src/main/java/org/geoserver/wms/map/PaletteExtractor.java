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
import java.util.Iterator;
import java.util.Set;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.ImageOutline;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.OverlapBehavior;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbol;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizer2;
import org.geotools.styling.UserLayer;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.style.GraphicalSymbol;

/**
 * A style visitor whose purpose is to extract a minimal palette for the provided style. This is to
 * be used when no antialiasing is used, since that would introduce various colors not included in
 * the style. <br>
 * At the moment the palette is extracted only if external graphics aren't referenced (a future
 * version may learn to extract a palette merging the ones of the external graphics).
 */
public class PaletteExtractor extends FilterAttributeExtractor implements StyleVisitor {
    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);
    private static final int TRANSPARENT_CODE = 255 << 16 | 255 << 8 | 255;

    Set /*<Color>*/ colors;
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
        colors = new HashSet();
        if (background == null) background = TRANSPARENT;
        colors.add(background);
    }

    public boolean canComputePalette() {
        // hard fail conditions
        if (translucentSymbolizers || externalGraphicsSymbolizers || unknownColors || rasterUsed)
            return false;

        // impossible to devise a palette (0 shuold never happen, but you never know...)
        if (colors.size() == 0 || colors.size() > 256) return false;

        return true;
    }

    /** Returns the palette, or null if it wasn't possible to devise one */
    public IndexColorModel getPalette() {
        if (!canComputePalette()) return null;

        int[] cmap = new int[colors.size()];
        int i = 0;
        for (Iterator it = colors.iterator(); it.hasNext(); ) {
            Color color = (Color) it.next();
            cmap[i++] =
                    (color.getAlpha() << 24)
                            | (color.getRed() << 16)
                            | (color.getGreen() << 8)
                            | color.getBlue();
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

        return new IndexColorModel(
                bits, cmap.length, cmap, 0, true, transparentIndex, DataBuffer.TYPE_BYTE);
    }

    /**
     * Checks whether translucency is used in the provided expression. Raises the flag of used
     * translucency unless it's possible to determine it's not.
     */
    void handleOpacity(Expression opacity) {
        if (opacity == null) return;
        if (opacity instanceof Literal) {
            Literal lo = (Literal) opacity;
            double value = ((Double) lo.evaluate(null, Double.class)).doubleValue();
            translucentSymbolizers = translucentSymbolizers || value != 1;
        } else {
            // we cannot know, so we assume some will be non opaque
            translucentSymbolizers = true;
        }
    }

    /**
     * Adds a color to the color set, and raises the unknown color flag if the color is an
     * expression other than a literal
     */
    void handleColor(Expression color) {
        if (color == null) return;
        if (color instanceof Literal) {
            Literal lc = (Literal) color;
            String rgbColor = (String) lc.evaluate(null, String.class);
            colors.add(Color.decode(rgbColor));
        } else {
            unknownColors = true;
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Style) */
    public void visit(Style style) {
        style.featureTypeStyles().forEach(ft -> ft.accept(this));
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Rule) */
    public void visit(Rule rule) {
        Filter filter = rule.getFilter();

        if (filter != null) {
            filter.accept(this, null);
        }

        rule.symbolizers().forEach(s -> s.accept(this));
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.FeatureTypeStyle) */
    public void visit(FeatureTypeStyle fts) {
        fts.rules().forEach(r -> r.accept(this));
    }

    public void visit(StyledLayerDescriptor sld) {
        StyledLayer[] layers = sld.getStyledLayers();

        for (int i = 0; i < layers.length; i++) {
            if (layers[i] instanceof NamedLayer) {
                ((NamedLayer) layers[i]).accept(this);
            } else if (layers[i] instanceof UserLayer) {
                ((UserLayer) layers[i]).accept(this);
            }
        }
    }

    public void visit(NamedLayer layer) {
        Style[] styles = layer.getStyles();

        for (int i = 0; i < styles.length; i++) {
            styles[i].accept(this);
        }
    }

    public void visit(UserLayer layer) {
        Style[] styles = layer.getUserStyles();

        for (int i = 0; i < styles.length; i++) {
            styles[i].accept(this);
        }
    }

    public void visit(FeatureTypeConstraint ftc) {
        // nothing to do
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Symbolizer) */
    public void visit(Symbolizer sym) {
        if (sym instanceof PointSymbolizer) {
            visit((PointSymbolizer) sym);
        }

        if (sym instanceof LineSymbolizer) {
            visit((LineSymbolizer) sym);
        }

        if (sym instanceof PolygonSymbolizer) {
            visit((PolygonSymbolizer) sym);
        }

        if (sym instanceof TextSymbolizer) {
            visit((TextSymbolizer) sym);
        }

        if (sym instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) sym);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Fill) */
    public void visit(Fill fill) {
        handleColor(fill.getColor());

        if (fill.getGraphicFill() != null) fill.getGraphicFill().accept(this);

        handleOpacity(fill.getOpacity());
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Stroke) */
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

    public void visit(RasterSymbolizer rs) {
        rasterUsed = true;
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.PointSymbolizer) */
    public void visit(PointSymbolizer ps) {
        if (ps.getGraphic() != null) {
            ps.getGraphic().accept(this);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.LineSymbolizer) */
    public void visit(LineSymbolizer line) {
        if (line.getStroke() != null) {
            line.getStroke().accept(this);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.PolygonSymbolizer) */
    public void visit(PolygonSymbolizer poly) {
        if (poly.getStroke() != null) {
            poly.getStroke().accept(this);
        }

        if (poly.getFill() != null) {
            poly.getFill().accept(this);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.TextSymbolizer) */
    public void visit(TextSymbolizer text) {
        if (text instanceof TextSymbolizer2) {
            if (((TextSymbolizer2) text).getGraphic() != null)
                ((TextSymbolizer2) text).getGraphic().accept(this);
        }

        if (text.getFill() != null) {
            text.getFill().accept(this);
        }

        if (text.getHalo() != null) {
            text.getHalo().accept(this);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Graphic) */
    public void visit(Graphic gr) {
        for (GraphicalSymbol symbol : gr.graphicalSymbols()) {
            if (symbol instanceof Symbol) {
                ((Symbol) symbol).accept(this);
            } else {
                throw new RuntimeException("Don't know how to copy " + symbol);
            }
        }

        handleOpacity(gr.getOpacity());
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Mark) */
    public void visit(Mark mark) {
        if (mark.getFill() != null) {
            mark.getFill().accept(this);
        }

        if (mark.getStroke() != null) {
            mark.getStroke().accept(this);
        }
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.ExternalGraphic) */
    public void visit(ExternalGraphic exgr) {
        externalGraphicsSymbolizers = true;
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.PointPlacement) */
    public void visit(PointPlacement pp) {
        // nothing to do
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.AnchorPoint) */
    public void visit(AnchorPoint ap) {
        // nothing to do
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Displacement) */
    public void visit(Displacement dis) {
        // nothing to do
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.LinePlacement) */
    public void visit(LinePlacement lp) {
        // nothing to do
    }

    /** @see org.geotools.styling.StyleVisitor#visit(org.geotools.styling.Halo) */
    public void visit(Halo halo) {
        if (halo.getFill() != null) {
            halo.getFill().accept(this);
        }
    }

    public void visit(ColorMap map) {
        // for the moment we don't do anything
        unknownColors = true;
    }

    public void visit(ColorMapEntry entry) {
        unknownColors = true;
    }

    public void visit(ContrastEnhancement contrastEnhancement) {
        unknownColors = true;
    }

    public void visit(ImageOutline outline) {
        unknownColors = true;
    }

    public void visit(ChannelSelection cs) {
        unknownColors = true;
    }

    public void visit(OverlapBehavior ob) {
        unknownColors = true;
    }

    public void visit(SelectedChannelType sct) {
        unknownColors = true;
    }

    public void visit(ShadedRelief sr) {
        unknownColors = true;
    }
}
