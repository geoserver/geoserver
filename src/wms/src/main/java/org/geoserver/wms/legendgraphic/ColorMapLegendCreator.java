/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.geoserver.wms.legendgraphic.Cell.ClassesEntryLegendBuilder;
import org.geoserver.wms.legendgraphic.Cell.ColorMapEntryLegendBuilder;
import org.geoserver.wms.legendgraphic.Cell.RampColorMapEntryLegendBuilder;
import org.geoserver.wms.legendgraphic.Cell.SingleColorMapEntryLegendBuilder;
import org.geoserver.wms.legendgraphic.LegendUtils.HAlign;
import org.geoserver.wms.legendgraphic.LegendUtils.LegendLayout;
import org.geoserver.wms.legendgraphic.LegendUtils.VAlign;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.SelectedChannelType;

/**
 * This class is responsible for building a legend out of a {@link ColorMap} SLD 1.0 element.
 *
 * <p>Notice that the {@link ColorMapLegendCreator} is immutable.
 *
 * @author Simone Giannecchini, GeoSolutions.
 */
@SuppressWarnings("deprecation")
public class ColorMapLegendCreator {

    /**
     * Builder class for building a {@link ColorMapLegendCreator}.
     *
     * <p>The builder is not threa-safe.
     *
     * <p>The correct way to use it is as follows: <code>
     * // colormap element
     * final ColorMap cmap = rasterSymbolizer.getColorMap();
     * final Builder cmapLegendBuilder= new ColorMapLegendCreator.Builder();
     * if (cmap != null && cmap.getColorMapEntries() != null
     * && cmap.getColorMapEntries().length > 0) {
     *
     * // passing additional options
     * cmapLegendBuilder.setAdditionalOptions(request.getLegendOptions());
     *
     *
     * // setting type of colormap
     * cmapLegendBuilder.setColorMapType(cmap.getType());
     *
     * // is this colormap using extended colors
     * cmapLegendBuilder.setExtended(cmap.getExtendedColors());
     *
     *
     * // setting the requested colormap entries
     * cmapLegendBuilder.setRequestedDimension(new Dimension(width,height));
     *
     * // setting transparency and background bkgColor
     * cmapLegendBuilder.setTransparent(transparent);
     * cmapLegendBuilder.setBackgroundColor(bgColor);
     *
     * //setting band
     *
     * // Setting label font and font bkgColor
     * cmapLegendBuilder.setLabelFont(LegendUtils.getLabelFont(request));
     * cmapLegendBuilder.setLabelFontColor(LegendUtils.getLabelFontColor(request));
     *
     *
     * //set band
     * final ChannelSelection channelSelection = rasterSymbolizer.getChannelSelection();
     * cmapLegendBuilder.setBand(channelSelection!=null?channelSelection.getGrayChannel():null);
     *
     * // adding the colormap entries
     * final ColorMapEntry[] colorMapEntries = cmap.getColorMapEntries();
     * for (ColorMapEntry ce : colorMapEntries)
     * if (ce != null)
     * cmapLegendBuilder.addColorMapEntry(ce);
     *
     * cMapLegendCreator=cmapLegendBuilder.create();
     * }
     * </code>
     *
     * @author Simone Giannecchini, GeoSolutions SAS
     */
    public static class Builder {

        private final Queue<ColorMapEntryLegendBuilder> bodyRows =
                new LinkedList<ColorMapEntryLegendBuilder>();

        private ColorMapType colorMapType;

        private ColorMapEntry previousCMapEntry;

        private final CaseInsensitiveMap additionalOptions = new CaseInsensitiveMap();

        private Color backgroundColor;

        private LegendLayout layout;

        private int columnHeight;

        private int rowWidth;

        private int columns;

        private int rows;

        private String grayChannelName = LegendUtils.DEFAULT_CHANNEL_NAME;

        private boolean extended = false;

        private Color borderColor = LegendUtils.DEFAULT_BORDER_COLOR;

        private boolean fontAntiAliasing = true;

        private HAlign hAlign = HAlign.LEFT;

        private Font labelFont;

        private Color labelFontColor;

        private Dimension requestedDimension;

        private boolean transparent;

        private VAlign vAlign = VAlign.BOTTOM;

        private boolean forceRule = false;

        private double rowMarginPercentage = LegendUtils.rowPaddingFactor;

        private double vMarginPercentage = LegendUtils.marginFactor;

        private double columnMarginPercentage = LegendUtils.columnPaddingFactor;

        private double hMarginPercentage = LegendUtils.marginFactor;

        private boolean absoluteMargins = true;

        private boolean border = false;

        private boolean borderLabel = false;

        private boolean borderRule = false;

        private boolean bandInformation = false;

        private String unit = "";

        private int digits;

        private boolean alternativeColorMapEntryBuilder = false;

        /**
         * Adds a {@link ColorMapEntry} element to this builder so that it can take it into account
         * for building the legend.
         *
         * @param cEntry a {@link ColorMapEntry} element for this builder so that it can take it
         *     into account for building the legend. It must be not <code>null</code>.
         */
        public ColorMapEntryLegendBuilder addColorMapEntry(final ColorMapEntry cEntry) {
            PackagedUtils.ensureNotNull(cEntry, "cEntry");

            // build a ColorMapEntryLegendBuilder for the specified colorMapEntry
            final ColorMapEntryLegendBuilder element;
            //
            //
            // NOTE for using the unit and digits values, the field
            // "alternativeColorMapEntryBuilder" must be set to
            // TRUE with the method "setalternativeColorMapEntryBuilder()"
            //
            switch (colorMapType) {
                case UNIQUE_VALUES:
                    element =
                            new SingleColorMapEntryLegendBuilder(
                                    Arrays.asList(cEntry),
                                    hAlign,
                                    vAlign,
                                    backgroundColor,
                                    1.0,
                                    grayChannelName,
                                    requestedDimension,
                                    labelFont,
                                    labelFontColor,
                                    extended,
                                    borderColor,
                                    unit,
                                    digits,
                                    alternativeColorMapEntryBuilder);
                    break;
                case RAMP:
                    element =
                            new RampColorMapEntryLegendBuilder(
                                    Arrays.asList(previousCMapEntry, cEntry),
                                    hAlign,
                                    vAlign,
                                    backgroundColor,
                                    1.0,
                                    grayChannelName,
                                    requestedDimension,
                                    labelFont,
                                    labelFontColor,
                                    extended,
                                    borderColor,
                                    unit,
                                    digits,
                                    alternativeColorMapEntryBuilder);
                    break;
                case CLASSES:
                    element =
                            new ClassesEntryLegendBuilder(
                                    Arrays.asList(previousCMapEntry, cEntry),
                                    hAlign,
                                    vAlign,
                                    backgroundColor,
                                    1.0,
                                    grayChannelName,
                                    requestedDimension,
                                    labelFont,
                                    labelFontColor,
                                    extended,
                                    borderColor,
                                    unit,
                                    digits,
                                    alternativeColorMapEntryBuilder);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized colormap type");
            }

            // add to the table, we can use matrix algebra knowing that W==3 for this matrix
            bodyRows.add(element);

            // set last used element
            previousCMapEntry = cEntry;

            return element;
        }

        /** @uml.property name="additionalOptions" */
        public void setAdditionalOptions(final Map<String, Object> legendOptions) {
            this.additionalOptions.putAll(legendOptions);
        }

        /** @uml.property name="backgroundColor" */
        public void setBackgroundColor(final Color backGroundColor) {
            PackagedUtils.ensureNotNull(backGroundColor, "backGroundColor");
            this.backgroundColor = backGroundColor;
        }

        public void setBand(final SelectedChannelType grayChannel) {
            if (grayChannel != null)
                this.grayChannelName = grayChannel.getChannelName().evaluate(null, String.class);

            if (grayChannelName == null) this.grayChannelName = LegendUtils.DEFAULT_CHANNEL_NAME;
        }

        /**
         * Sets the {@link ColorMapType} for this legend builder in order to instruct it on how to
         * build the legend.
         *
         * @param colorMapType the {@link ColorMapType} for this legend builder in order to instruct
         *     it on how to build the legend.
         */
        public void setColorMapType(final ColorMapType colorMapType) {
            this.colorMapType = colorMapType;
        }

        /**
         * Sets the {@link ColorMapType} for this legend builder in order to instruct it on how to
         * build the legend.
         *
         * @param type a int representing a {@link ColorMapType} for this legend builder in order to
         *     instruct it on how to build the legend.
         */
        public void setColorMapType(final int type) {
            this.colorMapType = ColorMapType.create(type);
        }

        /** @uml.property name="extended" */
        public void setExtended(final boolean extended) {
            this.extended = extended;
        }

        /** @uml.property name="labelFont" */
        public void setLabelFont(final Font labelFont) {
            PackagedUtils.ensureNotNull(labelFont, "labelFont");
            this.labelFont = labelFont;
        }

        /** @uml.property name="labelFontColor" */
        public void setLabelFontColor(final Color labelFontColor) {
            PackagedUtils.ensureNotNull(labelFontColor, "labelFontColor");
            this.labelFontColor = labelFontColor;
        }

        /** @uml.property name="requestedDimension" */
        public void setRequestedDimension(final Dimension dimension) {
            this.requestedDimension = (Dimension) dimension.clone();
        }

        /** @uml.property name="transparent" */
        public void setTransparent(final boolean transparent) {
            this.transparent = transparent;
        }

        public void setBorderLabel(boolean borderLabel) {
            this.borderLabel = borderLabel;
        }

        public void setBorderRule(boolean borderRule) {
            this.borderRule = borderRule;
        }

        public void setLayout(LegendLayout layout) {
            this.layout = layout;
        }

        public void setColumnHeight(int columnHeight) {
            this.columnHeight = columnHeight;
        }

        public void setRowWidth(int rowWidth) {
            this.rowWidth = rowWidth;
        }

        public void setColumns(int columns) {
            this.columns = columns;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        /**
         * Creates a {@link ColorMapLegendCreator} using the provided elements.
         *
         * @return a {@link ColorMapLegendCreator}.
         */
        public ColorMapLegendCreator create() {
            return new ColorMapLegendCreator(this);
        }

        public void checkAdditionalOptions() {

            fontAntiAliasing = false;
            if (additionalOptions.get("fontAntiAliasing") instanceof String) {
                String aaVal = (String) additionalOptions.get("fontAntiAliasing");
                if (aaVal.equalsIgnoreCase("on")
                        || aaVal.equalsIgnoreCase("true")
                        || aaVal.equalsIgnoreCase("yes")
                        || aaVal.equalsIgnoreCase("1")) {
                    fontAntiAliasing = true;
                }
            }

            if (additionalOptions.get("dx") instanceof String) {
                columnMarginPercentage = Double.parseDouble((String) additionalOptions.get("dx"));
            }

            if (additionalOptions.get("absoluteMargins") instanceof String) {
                absoluteMargins =
                        Boolean.parseBoolean((String) additionalOptions.get("absoluteMargins"));
            }

            if (additionalOptions.get("bandInfo") instanceof String) {
                bandInformation = Boolean.parseBoolean((String) additionalOptions.get("bandInfo"));
            }

            if (additionalOptions.get("dy") instanceof String) {
                rowMarginPercentage = Double.parseDouble((String) additionalOptions.get("dy"));
            }

            if (additionalOptions.get("mx") instanceof String) {
                hMarginPercentage = Double.parseDouble((String) additionalOptions.get("mx"));
            }

            if (additionalOptions.get("my") instanceof String) {
                vMarginPercentage = Double.parseDouble((String) additionalOptions.get("my"));
            }

            if (additionalOptions.get("borderColor") instanceof String) {
                borderColor = LegendUtils.color((String) additionalOptions.get("borderColor"));
            }

            if (additionalOptions.get("border") instanceof String) {
                border = Boolean.valueOf((String) additionalOptions.get("border"));
            }

            if (additionalOptions.get("forceRule") instanceof String) {
                forceRule = Boolean.parseBoolean((String) additionalOptions.get("forceRule"));
            }

            // if all the labels are null, we MUST draw the rules
            if (!forceRule)
                for (ColorMapEntryLegendBuilder row : bodyRows) {

                    //
                    // row number i
                    //

                    // label
                    final Cell labelM = row.getLabelManager();
                    if (labelM == null) forceRule = true;
                    else {
                        forceRule = false;
                        break;
                    }
                }
        }

        public void setBandInformation(boolean bandInformation) {
            this.bandInformation = bandInformation;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public void setDigits(int digits) {
            this.digits = digits;
        }

        public void setAlternativeColorMapEntryBuilder(boolean alternativeColorMapEntryBuilder) {
            this.alternativeColorMapEntryBuilder = alternativeColorMapEntryBuilder;
        }

        public Queue<ColorMapEntryLegendBuilder> getBodyRows() {
            return bodyRows;
        }
    }

    /** @author Simone Giannecchini, GeoSolutions SAS */
    enum ColorMapType {
        UNIQUE_VALUES,
        RAMP,
        CLASSES;

        public static ColorMapType create(final String value) {
            if (value.equalsIgnoreCase("intervals")) return CLASSES;
            else if (value.equalsIgnoreCase("ramp")) {
                return RAMP;
            } else if (value.equalsIgnoreCase("values")) {
                return UNIQUE_VALUES;
            } else return ColorMapType.valueOf(value);
        }

        public static ColorMapType create(final int value) {
            switch (value) {
                case ColorMap.TYPE_INTERVALS:
                    return ColorMapType.CLASSES;
                case ColorMap.TYPE_RAMP:
                    return ColorMapType.RAMP;
                case ColorMap.TYPE_VALUES:
                    return ColorMapType.UNIQUE_VALUES;

                default:
                    throw new IllegalArgumentException(
                            "Unable to create ColorMapType for value " + value);
            }
        }
    }

    private ColorMapType colorMapType;

    private boolean extended = false;

    private boolean transparent;

    private Dimension requestedDimension;

    private Color backgroundColor;

    private Font labelFont;

    private Color labelFontColor;

    private final Queue<ColorMapEntryLegendBuilder> bodyRows =
            new LinkedList<ColorMapEntryLegendBuilder>();

    private final List<Cell> footerRows = new ArrayList<Cell>();

    private HAlign hAlign = HAlign.LEFT;

    private VAlign vAlign = VAlign.BOTTOM;

    private double vMarginPercentage = LegendUtils.marginFactor;

    private double hMarginPercentage = LegendUtils.marginFactor;

    private double rowMarginPercentage = LegendUtils.rowPaddingFactor;

    private double columnMarginPercentage = LegendUtils.columnPaddingFactor;

    private boolean absoluteMargins = true;

    private Color borderColor = LegendUtils.DEFAULT_BORDER_COLOR;

    private boolean borderLabel = false;

    private boolean borderRule = false;

    private double margin;

    private double rowH;

    private double colorW;

    private double ruleW;

    private double labelW;

    private double footerW;

    private String grayChannelName = "1";

    private boolean fontAntiAliasing = true;

    private boolean forceRule = false;

    private BufferedImage legend;

    private boolean border = false;

    private double dx;

    private double dy;

    private boolean bandInformation;

    private LegendLayout layout;

    private int columnHeight;

    private int rowWidth;

    private int columns;

    private int rows;

    public ColorMapLegendCreator(final Builder builder) {
        this.backgroundColor = builder.backgroundColor;
        this.bodyRows.addAll(builder.bodyRows);
        this.border = builder.border;
        this.borderColor = builder.borderColor;
        this.borderLabel = builder.borderLabel;
        this.borderRule = builder.borderRule;
        this.colorMapType = builder.colorMapType;
        this.columnMarginPercentage = builder.columnMarginPercentage;
        this.extended = builder.extended;
        this.fontAntiAliasing = builder.fontAntiAliasing;
        this.forceRule = builder.forceRule;
        this.grayChannelName = builder.grayChannelName;
        this.hAlign = builder.hAlign;
        this.vAlign = builder.vAlign;
        this.labelFont = builder.labelFont;
        this.labelFontColor = builder.labelFontColor;
        this.rowMarginPercentage = builder.rowMarginPercentage;
        this.columnMarginPercentage = builder.columnMarginPercentage;
        this.absoluteMargins = builder.absoluteMargins;
        this.hMarginPercentage = builder.hMarginPercentage;
        this.vMarginPercentage = builder.vMarginPercentage;
        this.requestedDimension = (Dimension) builder.requestedDimension.clone();
        this.transparent = builder.transparent;
        this.bandInformation = builder.bandInformation;
        this.layout = builder.layout;
        this.rowWidth = builder.rowWidth;
        this.rows = builder.rows;
        this.columnHeight = builder.columnHeight;
        this.columns = builder.columns;
    }

    public synchronized BufferedImage getLegend() {

        // do we laraedy have a legend
        if (legend == null) {

            // init all the values
            init();

            // now build the individuals legends

            //
            // header
            //
            // XXX no header for the moment

            //
            // body
            //
            final Queue<BufferedImage> body = createBody();

            //
            // footer
            //
            if (bandInformation) {
                final Queue<BufferedImage> footer = createFooter();
                body.addAll(footer);
            }

            // now merge them
            legend = mergeRows(body);
        }
        return legend;
    }

    private void init() {

        //
        // create a sample image for computing dimensions of text strings
        //
        BufferedImage image = ImageUtils.createImage(1, 1, (IndexColorModel) null, transparent);
        final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
        Graphics2D graphics =
                ImageUtils.prepareTransparency(transparent, backgroundColor, image, hintsMap);

        // elements used to compute maximum dimensions for rows and cells
        rowH = 0;
        colorW = 0;
        ruleW = 0;
        labelW = 0;

        //
        // BODY
        //
        // cycle over all the body elements
        cycleBodyRows(graphics);

        //
        // FOOTER
        //
        // set footer strings
        if (bandInformation) {
            final String bandNameString = "Band selection is " + this.grayChannelName;
            footerRows.add(
                    new Cell.TextManager(
                            bandNameString,
                            vAlign,
                            hAlign,
                            backgroundColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            fontAntiAliasing,
                            borderColor));
            // set footer strings
            final String colorMapTypeString = "ColorMap type is " + this.colorMapType.toString();
            footerRows.add(
                    new Cell.TextManager(
                            colorMapTypeString,
                            vAlign,
                            hAlign,
                            backgroundColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            fontAntiAliasing,
                            borderColor));
            // extended colors or not
            final String extendedCMapString =
                    "ColorMap is " + (this.extended ? "" : "not") + " extended";
            footerRows.add(
                    new Cell.TextManager(
                            extendedCMapString,
                            vAlign,
                            hAlign,
                            backgroundColor,
                            requestedDimension,
                            labelFont,
                            labelFontColor,
                            fontAntiAliasing,
                            borderColor));
            cycleFooterRows(graphics);
        }

        //
        // compute dimensions
        // this.
        // final dimension are different between ramp and others since ramp does not have margin for
        // rows
        final double maxW = Math.max(colorW + ruleW + labelW, footerW);
        dx = absoluteMargins ? columnMarginPercentage : (maxW * columnMarginPercentage);
        dy = colorMapType == ColorMapType.RAMP ? 0 : rowH * rowMarginPercentage;

        final double mx = maxW * hMarginPercentage;
        final double my = rowH * vMarginPercentage;
        margin = Math.max(mx, my);
    }

    private void cycleFooterRows(Graphics2D graphics) {
        int numRows = this.footerRows.size(), i = 0;
        footerW = Double.NEGATIVE_INFINITY;
        for (i = 0; i < numRows; i++) {

            //
            // row number i
            //

            // color element
            final Cell cell = this.footerRows.get(i);
            final Dimension cellDim = cell.getPreferredDimension(graphics);
            rowH = Math.max(rowH, cellDim.getHeight());
            footerW = Math.max(footerW, cellDim.getWidth());
        }
    }

    /** @param graphics */
    private void cycleBodyRows(Graphics2D graphics) {
        for (ColorMapEntryLegendBuilder row : bodyRows) {

            //
            // row number i
            //

            // color element
            final Cell cm = row.getColorManager();
            final Dimension colorDim = cm.getPreferredDimension(graphics);
            rowH = Math.max(rowH, colorDim.getHeight());
            colorW = Math.max(colorW, colorDim.getWidth());

            // rule
            if (forceRule) {
                final Cell ruleM = row.getRuleManager();
                final Dimension ruleDim = ruleM.getPreferredDimension(graphics);
                rowH = Math.max(rowH, ruleDim.getHeight());
                ruleW = Math.max(ruleW, ruleDim.getWidth());
            }

            // label
            final Cell labelM = row.getLabelManager();
            if (labelM == null) continue;
            final Dimension labelDim = labelM.getPreferredDimension(graphics);
            rowH = Math.max(rowH, labelDim.getHeight());
            labelW = Math.max(labelW, labelDim.getWidth());
        }
    }

    private Queue<BufferedImage> createFooter() {

        // creating a backbuffer image on which we should draw the bkgColor for this colormap
        // element
        final BufferedImage image =
                ImageUtils.createImage(1, 1, (IndexColorModel) null, transparent);
        final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
        final Graphics2D graphics =
                ImageUtils.prepareTransparency(transparent, backgroundColor, image, hintsMap);

        // list where we store the rows for the footer
        final Queue<BufferedImage> queue = new LinkedList<BufferedImage>();
        // //the height is already fixed
        // final int rowHeight=(int)Math.round(rowH);
        final int rowWidth = (int) Math.round(footerW);
        // final Rectangle clipboxA=new Rectangle(0,0,rowWidth,rowHeight);
        //
        // footer
        //
        //
        // draw the various bodyCells
        for (Cell cell : footerRows) {

            // get dim
            final Dimension dim = cell.getPreferredDimension(graphics);
            // final int rowWidth=(int)Math.round(dim.getWidth());
            final int rowHeight = (int) Math.round(dim.getHeight());
            final Rectangle clipboxA = new Rectangle(0, 0, rowWidth, rowHeight);

            // draw it
            final BufferedImage colorCellLegend =
                    new BufferedImage(rowWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D rlg = colorCellLegend.createGraphics();
            rlg.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cell.draw(rlg, clipboxA, border);
            rlg.dispose();

            queue.add(colorCellLegend);
        }

        graphics.dispose();

        return queue; // mergeRows(queue);
    }

    private Queue<BufferedImage> createBody() {

        final Queue<BufferedImage> queue = new LinkedList<BufferedImage>();

        //
        // draw the various elements
        //
        // create the boxes for drawing later
        final int rowHeight = (int) Math.round(rowH);
        final int colorWidth = (int) Math.round(colorW);
        final int ruleWidth = (int) Math.round(ruleW);
        final int labelWidth = (int) Math.round(labelW);
        final Rectangle clipboxA = new Rectangle(0, 0, colorWidth, rowHeight);
        final Rectangle clipboxB = new Rectangle(0, 0, ruleWidth, rowHeight);
        final Rectangle clipboxC = new Rectangle(0, 0, labelWidth, rowHeight);

        //
        // Body
        //
        //
        // draw the various bodyCells
        for (ColorMapEntryLegendBuilder row : bodyRows) {

            //
            // row number i
            //
            // get element for color default behavior
            final Cell colorCell = row.getColorManager();
            // draw it
            final BufferedImage colorCellLegend =
                    new BufferedImage(colorWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D rlg = colorCellLegend.createGraphics();
            rlg.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            rlg.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            colorCell.draw(rlg, clipboxA, border);
            rlg.dispose();

            BufferedImage ruleCellLegend = null;
            if (forceRule) {
                // get element for rule
                final Cell ruleCell = row.getRuleManager();
                // draw it
                ruleCellLegend =
                        new BufferedImage(ruleWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
                rlg = ruleCellLegend.createGraphics();
                rlg.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rlg.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                ruleCell.draw(rlg, clipboxB, borderRule);
                rlg.dispose();
            }

            // draw it if it is present
            if (labelWidth > 0) {
                // get element for label
                final Cell labelCell = row.getLabelManager();
                if (labelCell != null) {
                    final BufferedImage labelCellLegend =
                            new BufferedImage(labelWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
                    rlg = labelCellLegend.createGraphics();
                    rlg.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    rlg.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    labelCell.draw(rlg, clipboxC, borderLabel);
                    rlg.dispose();

                    //
                    // merge the bodyCells for this row
                    //
                    //

                    final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
                    queue.add(
                            LegendUtils.hMergeBufferedImages(
                                    colorCellLegend,
                                    ruleCellLegend,
                                    labelCellLegend,
                                    hintsMap,
                                    transparent,
                                    backgroundColor,
                                    dx));
                } else {
                    final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
                    queue.add(
                            LegendUtils.hMergeBufferedImages(
                                    colorCellLegend,
                                    ruleCellLegend,
                                    null,
                                    hintsMap,
                                    transparent,
                                    backgroundColor,
                                    dx));
                }

            } else {
                //
                // merge the bodyCells for this row
                //
                //

                final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
                queue.add(
                        LegendUtils.hMergeBufferedImages(
                                colorCellLegend,
                                ruleCellLegend,
                                null,
                                hintsMap,
                                transparent,
                                backgroundColor,
                                dx));
            }
        }

        // return the list of legends
        return queue; // mergeRows(queue);
    }

    private BufferedImage mergeRows(Queue<BufferedImage> legendsQueue) {
        // I am doing a straight cast since I know that I built this
        // dimension object by using the widths and heights of the various
        // bufferedimages for the various bkgColor map entries.
        final Dimension finalDimension = new Dimension();
        final int numRows = legendsQueue.size();
        finalDimension.setSize(
                Math.max(footerW, colorW + ruleW + labelW) + 2 * dx + 2 * margin,
                rowH * numRows + 2 * margin + (numRows - 1) * dy);

        final int totalWidth = (int) finalDimension.getWidth();
        final int totalHeight = (int) finalDimension.getHeight();
        BufferedImage finalLegend =
                ImageUtils.createImage(
                        totalWidth, totalHeight, (IndexColorModel) null, transparent);

        /*
         * For RAMP type, only HORIZONTAL or VERTICAL condition is valid
         */
        if (colorMapType == ColorMapType.RAMP) {

            final Map<Key, Object> hintsMap = new HashMap<Key, Object>();
            Graphics2D finalGraphics =
                    ImageUtils.prepareTransparency(
                            transparent, backgroundColor, finalLegend, hintsMap);
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            finalGraphics.setRenderingHints(hintsMap);

            int topOfRow = (int) (margin + 0.5);
            for (int i = 0; i < numRows; i++) {
                final BufferedImage img = legendsQueue.remove();

                // draw the image
                finalGraphics.drawImage(img, (int) (margin + 0.5), topOfRow, null);
                topOfRow += img.getHeight() + dy;
            }

            if (this.layout == LegendLayout.HORIZONTAL) {
                BufferedImage newImage =
                        new BufferedImage(totalHeight, totalWidth, finalLegend.getType());
                Graphics2D g2 = newImage.createGraphics();
                g2.rotate(-Math.PI / 2, 0, 0);
                g2.drawImage(finalLegend, null, -totalWidth, 0);
                finalLegend = newImage;
                g2.dispose();
                finalGraphics.dispose();
            }
        } else {
            List<RenderedImage> imgs = new ArrayList<RenderedImage>(legendsQueue);

            LegendMerger.MergeOptions options =
                    new LegendMerger.MergeOptions(
                            imgs,
                            (int) dx,
                            (int) dy,
                            (int) margin,
                            0,
                            backgroundColor,
                            transparent,
                            true,
                            layout,
                            rowWidth,
                            rows,
                            columnHeight,
                            columns,
                            null,
                            false,
                            false,
                            false);
            finalLegend = LegendMerger.mergeRasterLegends(options);
        }

        return finalLegend;
    }

    protected Queue<ColorMapEntryLegendBuilder> getBodyRows() {
        return bodyRows;
    }
}
