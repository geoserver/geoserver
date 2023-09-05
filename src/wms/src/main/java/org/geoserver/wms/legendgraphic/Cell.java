/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.commons.text.WordUtils;
import org.geoserver.wms.legendgraphic.LegendUtils.HAlign;
import org.geoserver.wms.legendgraphic.LegendUtils.VAlign;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;

/**
 * This class mimics a simple cell for the final {@link ColorMap} legend reprensentation.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public abstract class Cell {
    protected final Color bkgColor;

    protected final double bkgOpacity;

    protected final String text;

    protected final HAlign hAlign;

    protected final VAlign vAlign;

    protected final Dimension requestedDimension;

    protected final Font labelFont;

    protected final Color labelFontColor;

    protected final boolean fontAntiAliasing;

    protected final Color borderColor;

    protected final boolean wrap;
    // TODO: parameterize this width
    protected int WRAP_WIDTH = 150;

    protected Cell(
            final Color bkgColor,
            final double bkgOpacity,
            final String text,
            final HAlign hAlign,
            final VAlign vAlign,
            final Dimension requestedDimension,
            final Font labelFont,
            final Color labelFontColor,
            final boolean fontAntiAliasing,
            final Color borderColor,
            final boolean wrap) {
        this.bkgColor = bkgColor;
        this.bkgOpacity = bkgOpacity;
        this.text = text;
        this.hAlign = hAlign;
        this.vAlign = vAlign;
        this.requestedDimension = requestedDimension;
        this.labelFont = labelFont;
        this.labelFontColor = labelFontColor;
        this.fontAntiAliasing = fontAntiAliasing;
        this.borderColor = borderColor;
        this.wrap = wrap;
    }

    public abstract void draw(
            final Graphics2D graphics, final Rectangle2D clipBox, final boolean completeBorder);

    /**
     * Retrieves the preferred dimension for this {@link Cell} element within the provided graphics
     * element.
     *
     * @param graphics {@link Graphics2D} object to use for computing the preferred dimension
     * @return the preferred dimension for this {@link Cell} element within the provided graphics
     *     element.
     */
    public abstract Dimension getPreferredDimension(final Graphics2D graphics);

    /**
     * This class mimics a simple row for the final {@link ColorMap} legend representation.
     *
     * @author Simone Giannecchini, GeoSolutions SAS
     */
    public abstract static class Row {
        private final List<Cell> cells = new ArrayList<>();

        Row() {}

        Row(final List<Cell> cells) {
            this.cells.addAll(cells);
        }

        protected Cell get(final int index) {
            return cells.get(index);
        }

        protected void add(final Cell cell) {
            cells.add(cell);
        }

        protected void set(final Cell cell, int idx) {
            cells.set(idx, cell);
        }
    }

    public abstract static class ColorMapEntryLegendBuilder extends Row {

        protected ColorMapEntryLegendBuilder() {
            super();
        }

        protected ColorMapEntryLegendBuilder(List<Cell> columns) {
            super(columns);
        }

        protected ColorMapEntryLegendBuilder(
                final ColorManager colorManager,
                final TextManager labelManager,
                final TextManager ruleManager) {
            super(Arrays.asList(colorManager, ruleManager, labelManager));
        }

        public boolean hasLabel() {
            return hasLabel;
        }

        protected boolean hasLabel;

        public Cell getRuleManager() {
            return get(1);
        }

        public Cell getLabelManager() {
            return get(2);
        }

        public Cell getColorManager() {
            return get(0);
        }

        protected String formatQuantity(
                final double quantity, final int digits, final String unit) {
            final String format = "%." + digits + "f";
            return String.format(Locale.US, format, quantity) + (unit != null ? (" " + unit) : "");
        }

        protected void setLastRow() {
            // nothing to do by default
        }
    }

    public static class SingleColorMapEntryLegendBuilder extends ColorMapEntryLegendBuilder {

        public SingleColorMapEntryLegendBuilder(
                final List<ColorMapEntry> cMapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                boolean formatQuantity,
                boolean wrap) {

            final ColorMapEntry currentCME = cMapEntries.get(0);
            Color color = LegendUtils.color(currentCME);
            final double opacity = LegendUtils.getOpacity(currentCME);
            color =
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (255 * opacity));
            super.add(
                    new ColorManager.SimpleColorManager(
                            color, opacity, requestedDimension, borderColor, wrap));

            final String label = LegendUtils.getLabel(currentCME);
            final double quantity = LegendUtils.getQuantity(currentCME);
            final String symbol = " = ";

            String rule;
            // Added variant for DynamicColorMap
            if (formatQuantity) {
                String value = formatQuantity(quantity, digits, unit);
                rule = value + " " + symbol + " x";
            } else {
                rule = Double.toString(quantity) + " " + symbol + " x";
            }

            super.add(
                    new TextManager(
                            rule,
                            vAling,
                            hAlign,
                            bkgColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            fontAntiAliasing,
                            borderColor,
                            wrap));

            // add the label the label to the rule so that we draw all text just once
            if (label != null) {

                hasLabel = true;
                super.add(
                        new TextManager(
                                label,
                                vAling,
                                hAlign,
                                bkgColor,
                                requestedDimension,
                                labelFont,
                                labelFontColor,
                                fontAntiAliasing,
                                borderColor,
                                wrap));
            } else super.add(null);
        }

        public SingleColorMapEntryLegendBuilder(
                final List<ColorMapEntry> cMapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final boolean wrap) {
            this(
                    cMapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    null,
                    0,
                    false,
                    wrap);
        }

        public SingleColorMapEntryLegendBuilder(
                final List<ColorMapEntry> cMapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                final boolean wrap) {
            this(
                    cMapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    unit,
                    digits,
                    true,
                    wrap);
        }
    }

    public static class RampColorMapEntryLegendBuilder extends ColorMapEntryLegendBuilder {

        private TextManager lastRuleManager;

        public RampColorMapEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final boolean wrap) {
            this(
                    mapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    null,
                    0,
                    false,
                    wrap);
        }

        public RampColorMapEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                final boolean wrap) {
            this(
                    mapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    unit,
                    digits,
                    true,
                    wrap);
        }

        public RampColorMapEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                boolean formatQuantity,
                boolean wrap) {

            final ColorMapEntry previousCME = mapEntries.get(0);
            final ColorMapEntry currentCME = mapEntries.get(1);
            boolean leftEdge;
            if (previousCME == null) leftEdge = true;
            else leftEdge = false;

            Color previousColor;
            if (!leftEdge) {
                previousColor = LegendUtils.color(previousCME);
                final double opacity = LegendUtils.getOpacity(previousCME);
                previousColor =
                        new Color(
                                previousColor.getRed(),
                                previousColor.getGreen(),
                                previousColor.getBlue(),
                                (int) (255 * opacity + 0.5));
            } else {
                previousColor = null;
            }

            Color color = LegendUtils.color(currentCME);
            double opacity = LegendUtils.getOpacity(currentCME);

            color =
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (255 * opacity));
            super.add(
                    new ColorManager.SimpleColorManager.GradientColorManager(
                            color, opacity, previousColor, requestedDimension, borderColor, wrap));

            String label = LegendUtils.getLabel(currentCME);
            double quantity = LegendUtils.getQuantity(currentCME);

            // Added variation for DynamicColorMap
            String rule;
            String lastRuleText;

            if (formatQuantity) {
                rule = "";
                lastRuleText = "";
                if (opacity > 0) {
                    String formattedQuantity = formatQuantity(quantity, digits, unit);
                    if (leftEdge) {
                        rule = formattedQuantity + " >= x";
                        lastRuleText = "";
                    } else {
                        rule = formattedQuantity + " ";
                        lastRuleText = formattedQuantity + " <= x";
                    }
                }
            } else {
                final String formattedQuantity = Double.toString(quantity);
                if (leftEdge) {
                    rule = formattedQuantity + " >= x";
                    lastRuleText = "";
                } else {
                    rule = formattedQuantity + " = x";
                    lastRuleText = formattedQuantity + " <= x";
                }
            }

            super.add(
                    new TextManager(
                            rule,
                            vAling,
                            hAlign,
                            bkgColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            leftEdge,
                            borderColor,
                            wrap));
            lastRuleManager =
                    new TextManager(
                            lastRuleText,
                            vAling,
                            hAlign,
                            bkgColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            leftEdge,
                            borderColor,
                            wrap);

            // add the label the label to the rule so that we draw all text just once
            if (label != null) {

                hasLabel = true;
                super.add(
                        new TextManager(
                                label,
                                vAling,
                                hAlign,
                                bkgColor,
                                requestedDimension,
                                labelFont,
                                labelFontColor,
                                leftEdge,
                                borderColor,
                                wrap));
            } else {
                super.add(null);
            }
        }

        @Override
        protected void setLastRow() {
            set(lastRuleManager, 1);
        }
    }

    public static class ClassesEntryLegendBuilder extends ColorMapEntryLegendBuilder {

        public ClassesEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final boolean wrap) {
            this(
                    mapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    null,
                    0,
                    false,
                    wrap);
        }

        public ClassesEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                final boolean wrap) {
            this(
                    mapEntries,
                    hAlign,
                    vAling,
                    bkgColor,
                    bkgOpacity,
                    text,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    unit,
                    digits,
                    true,
                    wrap);
        }

        public ClassesEntryLegendBuilder(
                final List<ColorMapEntry> mapEntries,
                final HAlign hAlign,
                final VAlign vAling,
                final Color bkgColor,
                final double bkgOpacity,
                final String text,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final String unit,
                final int digits,
                boolean formatQuantity,
                boolean wrap) {

            final ColorMapEntry previousCME = mapEntries.get(0);
            final ColorMapEntry currentCME = mapEntries.get(1);
            boolean leftEdge;
            if (previousCME == null) leftEdge = true;
            else leftEdge = false;

            Color color = LegendUtils.color(currentCME);
            final double opacity = LegendUtils.getOpacity(currentCME);
            color =
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (255 * opacity));
            super.add(
                    new ColorManager.SimpleColorManager(
                            color, opacity, requestedDimension, borderColor, wrap));

            String label = LegendUtils.getLabel(currentCME);
            double quantity1 =
                    leftEdge
                            ? LegendUtils.getQuantity(currentCME)
                            : LegendUtils.getQuantity(previousCME);
            double quantity2 = LegendUtils.getQuantity(currentCME);

            // Added variation for DynamicColorMap
            String ruleText;
            String symbol1 = null, symbol2 = null;
            if (leftEdge) symbol1 = " < ";
            else {
                symbol1 = " <= ";
                symbol2 = " < ";
            }
            if (formatQuantity) {
                ruleText = "";
                if (opacity > 0) {
                    String value1 = formatQuantity(quantity1, digits, unit);
                    String value2 = formatQuantity(quantity2, digits, unit);
                    if (leftEdge) {
                        ruleText = "x" + symbol1 + value1;
                    } else if (Double.isInfinite(quantity2)) {
                        ruleText = value1 + symbol1 + "x";
                    } else {
                        ruleText = value1 + symbol1 + "x" + symbol2 + value2;
                    }
                }
            } else {
                final String value1 = Double.toString(quantity1);
                final String value2 = Double.toString(quantity2);
                if (leftEdge) {
                    ruleText = "x" + symbol1 + value1;
                } else if (Double.isInfinite(quantity2)) {
                    ruleText = value1 + symbol1 + "x";
                } else {
                    ruleText = value1 + symbol1 + "x" + symbol2 + value2;
                }
            }

            super.add(
                    new TextManager(
                            ruleText,
                            vAling,
                            hAlign,
                            bkgColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            leftEdge,
                            borderColor,
                            wrap));

            // add the label the label to the rule so that we draw all text just once
            if (label != null) {

                hasLabel = true;
                super.add(
                        new TextManager(
                                label,
                                vAling,
                                hAlign,
                                bkgColor,
                                requestedDimension,
                                labelFont,
                                labelFontColor,
                                leftEdge,
                                borderColor,
                                wrap));
            } else super.add(null);
        }
    }

    /**
     * This class mimics a simple text cell for the final {@link ColorMap} legend representation.
     *
     * @author Simone Giannecchini, GeoSolutions SAS
     */
    public static class TextManager extends Cell {

        public TextManager(
                final String text,
                final VAlign vAlign,
                final HAlign hAlign,
                final Color bkgColor,
                final Dimension requestedDimension,
                final Font labelFont,
                final Color labelFontColor,
                final boolean fontAntiAliasing,
                final Color borderColor,
                final boolean wrap) {
            super(
                    bkgColor,
                    1.0,
                    text,
                    hAlign,
                    vAlign,
                    requestedDimension,
                    labelFont,
                    labelFontColor,
                    fontAntiAliasing,
                    borderColor,
                    wrap);
        }

        @Override
        public Dimension getPreferredDimension(final Graphics2D graphics) {
            // get old font
            final Font oldFont = graphics.getFont();

            // set new font
            graphics.setFont(labelFont);
            // computing label dimension and creating buffered image on which we can draw the label
            // on
            // it

            final int labelHeight =
                    (int)
                            Math.ceil(
                                    graphics.getFontMetrics()
                                            .getStringBounds(text, graphics)
                                            .getHeight());
            final int width =
                    (int)
                            Math.ceil(
                                    graphics.getFontMetrics()
                                            .getStringBounds(text, graphics)
                                            .getWidth());
            final int labelWidth = wrap ? Math.min(WRAP_WIDTH, width) : width;

            Rectangle2D bounds = new Rectangle2D.Double(0, 0, labelWidth, labelHeight);
            // restore the old font
            graphics.setFont(oldFont);
            String nText;
            if (!wrap) {
                return new Dimension(labelWidth, labelHeight);
            } else {
                if (width > labelWidth) {
                    FontMetrics fm = graphics.getFontMetrics();
                    int widthChars = WRAP_WIDTH / fm.stringWidth("m");
                    nText = WordUtils.wrap(text, widthChars, "\n", true);
                } else {
                    nText = text;
                }
            }
            if ((nText.indexOf("\n") != -1) || (nText.indexOf("\\n") != -1)) {
                // this is a label WITH line-breaks...we need to figure out it's height *and*
                // width, and then adjust the legend size accordingly
                ArrayList<Integer> lineHeight = new ArrayList<>();
                // four backslashes... "\\" -> '\', so "\\\\n" -> '\' + '\' + 'n'
                final String realLabel = nText.replaceAll("\\\\n", "\n");
                StringTokenizer st = new StringTokenizer(realLabel, "\n\r\f");

                while (st.hasMoreElements()) {
                    final String token = st.nextToken();
                    Rectangle2D thisLineBounds =
                            graphics.getFontMetrics().getStringBounds(token, graphics);

                    // if this is directly added as thisLineBounds.getHeight(), then there are
                    // rounding
                    // errors
                    // because we can only DRAW fonts at discrete integer coords.
                    final int thisLineHeight = (int) Math.ceil(thisLineBounds.getHeight());
                    bounds.add(0, thisLineHeight + bounds.getHeight());
                    bounds.add(thisLineBounds.getWidth(), 0);
                    lineHeight.add((int) Math.ceil(thisLineBounds.getHeight()));
                }
            }
            return new Dimension(
                    (int) Math.ceil(bounds.getWidth()), (int) Math.ceil(bounds.getHeight()));
        }

        @Override
        public void draw(
                final Graphics2D graphics,
                final Rectangle2D clipBox,
                final boolean completeBorder) {

            // save old font
            final Font oldFont = graphics.getFont();

            // set font and font color and the antialising
            graphics.setColor(labelFontColor);
            graphics.setFont(labelFont);
            if (fontAntiAliasing)
                graphics.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Halign==center vAlign==bottom
            final double minx = clipBox.getMinX();
            final double miny = clipBox.getMinY();
            final double w = clipBox.getWidth();
            final double h = clipBox.getHeight();
            final Dimension dimension = getPreferredDimension(graphics);
            // where do we draw?
            final int xText;
            switch (hAlign) {
                case CENTERED:
                    xText = (int) (minx + (w - dimension.getWidth()) / 2.0 + 0.5);
                    break;
                case LEFT:
                    xText = (int) (minx + 0.5);
                    break;
                case RIGHT:
                    xText = (int) (minx + (w - dimension.getWidth()) + 0.5);
                    break;
                case JUSTIFIED:
                    throw new UnsupportedOperationException("Unsupported");
                default:
                    throw new IllegalStateException("Unsupported horizontal alignment " + hAlign);
            }

            final int yText;
            switch (vAlign) {
                case BOTTOM:
                    yText = (int) (miny + h - graphics.getFontMetrics().getDescent() + 0.5);
                    break;
                case TOP:
                    yText = (int) (miny + graphics.getFontMetrics().getHeight() + 0.5);
                    break;
                case MIDDLE:
                    yText = (int) (miny + (h + graphics.getFontMetrics().getHeight()) / 2 + 0.5);
                    break;
                default:
                    throw new IllegalStateException("Unsupported vertical alignment " + vAlign);
            }

            if (wrap) {
                Rectangle2D labelBounds =
                        labelFont.getStringBounds(text, graphics.getFontRenderContext());

                if (text.contains("\n")
                        || text.contains("\\n")
                        || labelBounds.getWidth() > dimension.getWidth()) {
                    FontMetrics fm = graphics.getFontMetrics();
                    int widthChars = (int) Math.floor(dimension.getWidth() / fm.stringWidth("m"));
                    String realLabel;
                    if (labelBounds.getWidth() > dimension.getWidth()) {
                        realLabel = WordUtils.wrap(text, widthChars, "\n", true);
                    } else {
                        realLabel = text;
                    }
                    StringTokenizer st = new StringTokenizer(realLabel, "\n\r\f");
                    int y = 0 - graphics.getFontMetrics().getDescent();
                    int c = 0;
                    Rectangle2D bounds = new Rectangle2D.Double(0, 0, 0, 0);
                    ArrayList<Integer> lineHeight = new ArrayList<>();
                    // four backslashes... "\\" -> '\', so "\\\\n" -> '\' + '\' + 'n'
                    realLabel = realLabel.replaceAll("\\\\n", "\n");

                    while (st.hasMoreElements()) {
                        final String token = st.nextToken();
                        Rectangle2D thisLineBounds =
                                graphics.getFontMetrics().getStringBounds(token, graphics);

                        // if this is directly added as thisLineBounds.getHeight(), then there are
                        // rounding
                        // errors
                        // because we can only DRAW fonts at discrete integer coords.
                        final int thisLineHeight = (int) Math.ceil(thisLineBounds.getHeight());
                        bounds.add(0, thisLineHeight + bounds.getHeight());
                        bounds.add(thisLineBounds.getWidth(), 0);
                        lineHeight.add((int) Math.ceil(thisLineBounds.getHeight()));
                    }
                    st = new StringTokenizer(realLabel, "\n\r\f");
                    while (st.hasMoreElements()) {
                        y += lineHeight.get(c++).intValue();
                        graphics.drawString(st.nextToken(), xText, y);
                    }
                } else {
                    graphics.drawString(text, xText, yText);
                }
            } else {
                // draw
                graphics.drawString(text, xText, yText);
            }
            // restore the old font
            graphics.setFont(oldFont);
        }
    }

    /**
     * This class mimics a simple color cell for the final {@link ColorMap} legend representation.
     * It is responsible for for drawing colors for a {@link ColorMapEntry}.
     *
     * @author Simone Giannecchini, GeoSolutions SAS
     */
    public abstract static class ColorManager extends Cell {

        public ColorManager(
                final Color color,
                final double opacity,
                final Dimension requestedDimension,
                final Color borderColor,
                final boolean wrap) {
            super(
                    color,
                    opacity,
                    null,
                    null,
                    null,
                    requestedDimension,
                    null,
                    null,
                    false,
                    borderColor,
                    wrap);
        }

        @Override
        public abstract void draw(
                final Graphics2D graphics, final Rectangle2D clipBox, final boolean completeBorder);

        @Override
        public Dimension getPreferredDimension(final Graphics2D graphics) {
            return new Dimension(requestedDimension);
        }

        public static class SimpleColorManager extends ColorManager {

            public SimpleColorManager(
                    final Color color,
                    final double opacity,
                    final Dimension requestedDimension,
                    final Color borderColor,
                    final boolean wrap) {
                super(color, opacity, requestedDimension, borderColor, wrap);
            }

            @Override
            public void draw(
                    final Graphics2D graphics,
                    final Rectangle2D clipBox,
                    final boolean completeBorder) {
                // bkgColor fill
                if (bkgOpacity > 0) {
                    // OPAQUE
                    final Color oldColor = graphics.getColor();
                    final Color newColor =
                            new Color(
                                    bkgColor.getRed(),
                                    bkgColor.getGreen(),
                                    bkgColor.getBlue(),
                                    (int) (255 * bkgOpacity + 0.5));
                    graphics.setColor(newColor);
                    graphics.fill(clipBox);
                    // make bkgColor customizable
                    graphics.setColor(borderColor);
                    if (completeBorder) {

                        final int minx = (int) (clipBox.getMinX() + 0.5);
                        final int miny = (int) (clipBox.getMinY() + 0.5);
                        final int w = (int) (clipBox.getWidth() + 0.5) - 1;
                        final int h = (int) (clipBox.getHeight() + 0.5) - 1;
                        graphics.draw(new Rectangle2D.Double(minx, miny, w, h));
                    }
                    // restore bkgColor
                    graphics.setColor(oldColor);
                } else {
                    // TRANSPARENT
                    final Color oldColor = graphics.getColor();

                    // white background
                    graphics.setColor(Color.white);
                    graphics.fill(clipBox);

                    // now the red cross
                    graphics.setColor(Color.RED);
                    final int minx = (int) (clipBox.getMinX() + 0.5);
                    final int miny = (int) (clipBox.getMinY() + 0.5);
                    final int maxx = (int) (minx + clipBox.getWidth() - 1 + 0.5);
                    final int maxy = (int) (miny + clipBox.getHeight() - 1 + 0.5);
                    graphics.drawLine(minx, miny, maxx, maxy);
                    graphics.drawLine(minx, maxy, maxx, miny);

                    graphics.setColor(borderColor);
                    if (completeBorder) {

                        final int w = (int) (clipBox.getWidth() + 0.5) - 1;
                        final int h = (int) (clipBox.getHeight() + 0.5) - 1;
                        graphics.draw(new Rectangle2D.Double(minx, miny, w, h));
                    }

                    // restore bkgColor
                    graphics.setColor(oldColor);
                }
            }

            public static class GradientColorManager extends SimpleColorManager {

                @Override
                public Dimension getPreferredDimension(Graphics2D graphics) {
                    // twice as much space for the Height to account for the gradient
                    return new Dimension(
                            requestedDimension.width,
                            (int) (1.5 * requestedDimension.height + 0.5));
                }

                private Color previousColor = null;

                private boolean leftEdge;

                public GradientColorManager(
                        final Color color,
                        final double opacity,
                        final Color previousColor,
                        final Dimension requestedDimension,
                        final Color borderColor,
                        final boolean wrap) {
                    super(color, opacity, requestedDimension, borderColor, wrap);
                    this.previousColor = previousColor;
                    if (previousColor == null) leftEdge = true;
                }

                @Override
                public void draw(
                        final Graphics2D graphics,
                        final Rectangle2D clipBox,
                        final boolean completeBorder) {

                    // getting clipbox dimensions
                    final double minx = clipBox.getMinX();
                    final double miny = clipBox.getMinY();
                    final double w = clipBox.getWidth();
                    final double h = clipBox.getHeight();

                    // GRADIENT
                    if (!leftEdge) {
                        // rectangle for the gradient
                        final Rectangle2D.Double rectLegend =
                                new Rectangle2D.Double(minx, miny, w, h / 2);

                        // gradient paint
                        final Paint oldPaint = graphics.getPaint();
                        final GradientPaint paint =
                                new GradientPaint(
                                        (float) minx,
                                        (float) miny,
                                        previousColor,
                                        (float) minx,
                                        (float) (miny + h / 2),
                                        bkgColor);

                        // do the magic
                        graphics.setPaint(paint);
                        graphics.fill(rectLegend);

                        // restore paint
                        graphics.setPaint(oldPaint);
                    }

                    // COLOR BOX
                    // careful with handling the leftEdge case
                    final Rectangle2D rectLegend =
                            new Rectangle2D.Double(
                                    minx, miny + (leftEdge ? 0 : h / 2), w, !leftEdge ? h / 2 : h);
                    super.draw(graphics, rectLegend, completeBorder);
                    if (completeBorder) {
                        final Color oldColor = graphics.getColor();
                        // make bkgColor customizable
                        graphics.setColor(borderColor);
                        final int minx_ = (int) (clipBox.getMinX() + 0.5);
                        final int maxx = (int) (minx + clipBox.getWidth() + 0.5) - 1;
                        final int maxy = (int) (miny + clipBox.getHeight() + 0.5) - 1;
                        graphics.drawLine(minx_, maxy, maxx, maxy);
                        // restore bkgColor
                        graphics.setColor(oldColor);
                    }
                }
            }
        }
    }
}
