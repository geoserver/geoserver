/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.LegendUtils.LegendLayout;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.styling.Description;
import org.geotools.styling.Rule;
import org.opengis.util.InternationalString;

public class LegendMerger {

    /**
     * Receives a list of <code>BufferedImages</code> and produces a new one which holds all the images in <code>imageStack</code> one above the
     * other.
     * 
     * @param imageStack the list of BufferedImages
     * @param dx horizontal space between images
     * @param dy vertical space between images
     * @param margin padding around image
     * @param backgroundColor background color of legend
     * @param transparent if true make legend transparent
     * @param antialias if true applies anti aliasing
     * @param layout orientation of legend, my be horizontal or vertical
     * @param rowWidth maximum width for horizontal legend
     * @param rows maximum number of rows for horizontal legend
     * @param columnHeight maximum height for vertical legend
     * @param columns maximum number of columns for vertical legend
     * @return the legend image with all the images on the argument list.
     */
    public static BufferedImage mergeRasterLegends(List<BufferedImage> imageStack, int dx, int dy,
            int margin, Color backgroundColor, boolean transparent, boolean antialias,
            LegendLayout layout, int rowWidth, int rows, int columnHeight, int columns) {

        List<BufferedImage> nodes = new ArrayList<BufferedImage>();
        final int imgCount = imageStack.size();
        for (int i = 0; i < imgCount; i++) {
            nodes.add((BufferedImage) imageStack.get(i));
        }

        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] r = createRows(nodes, rowWidth, rows);
            finalLegend = buildFinalHLegend(dx, dy, margin, r, transparent, backgroundColor,
                    antialias);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] c = createColumns(nodes, columnHeight, columns);
            finalLegend = buildFinalVLegend(dx, dy, margin, c, transparent, backgroundColor,
                    antialias);
        }

        return finalLegend;
    }

    /**
     * Receives a list of <code>BufferedImages</code> and produces a new one which holds all the images in <code>imageStack</code> one above the
     * other, handling labels.
     * 
     * @param imageStack the list of BufferedImages, one for each applicable Rule
     * @param rules The applicable rules, one for each image in the stack (if not null it's used to compute labels)
     * @param request The request.
     * @param forceLabelsOn true for force labels on also with a single image.
     * @param forceLabelsOff true for force labels off also with more than one rule.
     * 
     * @return the image with all the images on the argument list.
     * 
     */
    public static BufferedImage mergeLegends(List<RenderedImage> imageStack, Rule[] rules,
            GetLegendGraphicRequest req, boolean forceLabelsOn, boolean forceLabelsOff) {

        final boolean transparent = req.isTransparent();
        final Color backgroundColor = LegendUtils.getBackgroundColor(req);
        Font labelFont = LegendUtils.getLabelFont(req);
        boolean useAA = LegendUtils.isFontAntiAliasing(req);

        // Builds legend nodes (graphics + label)
        final int imgCount = imageStack.size();
        List<BufferedImage> nodes = new ArrayList<BufferedImage>();
        // Single legend, no rules, no force label
        if (imgCount == 1 && (!forceLabelsOn || rules == null)) {
            return (BufferedImage) imageStack.get(0);
        } else {
            for (int i = 0; i < imgCount; i++) {
                BufferedImage img = (BufferedImage) imageStack.get(i);
                if (rules != null && rules[i] != null) {
                    BufferedImage label = renderLabel(img, rules[i], req, forceLabelsOff);
                    if (label != null) {
                        img = joinBufferedImageHorizzontally(img, label, labelFont, useAA,
                                transparent, backgroundColor);
                    }
                    nodes.add(img);
                } else {
                    nodes.add(img);
                }
            }
        }

        // Sets legend nodes into a matrix according to layout rules
        LegendLayout layout = LegendUtils.getLayout(req);
        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] rows = createRows(nodes, LegendUtils.getRowWidth(req), LegendUtils.getRows(req));
            finalLegend = buildFinalHLegend(0, 0, 0, rows, transparent, backgroundColor, useAA);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] columns = createColumns(nodes, LegendUtils.getColumnHeight(req), LegendUtils.getColumns(req));
            finalLegend = buildFinalVLegend(0, 0, 0, columns, transparent, backgroundColor, useAA);
        }

        return finalLegend;

    }

    /**
     * Receives a list of <code>BufferedImages</code> and produces a new one which holds all the images in <code>imageStack</code> one above the
     * other, handling labels.
     * 
     * @param imageStack the list of BufferedImages, one for each applicable Rule
     * @param rules The applicable rules, one for each image in the stack (if not null it's used to compute labels)
     * @param request The request.
     * @param forceLabelsOn true for force labels on also with a single image.
     * @param forceLabelsOff true for force labels off also with more than one rule.
     * 
     * @return the image with all the images on the argument list.
     * 
     */
    public static BufferedImage mergeGroups(List<RenderedImage> imageStack, Rule[] rules,
            GetLegendGraphicRequest req, boolean forceLabelsOn, boolean forceLabelsOff) {

        final boolean transparent = req.isTransparent();
        final Color backgroundColor = LegendUtils.getBackgroundColor(req);
        Font labelFont = LegendUtils.getLabelFont(req);
        boolean useAA = LegendUtils.isFontAntiAliasing(req);

        final int imgCount = imageStack.size();
        if (imgCount == 1 && (!forceLabelsOn || rules == null)) {
            return (BufferedImage) imageStack.get(0);
        }
        
        List<BufferedImage> nodes = new ArrayList<BufferedImage>(imgCount / 2);
        // Single legend, no rules, no force label
        for (int i = 0; i < imgCount; i = i + 2) {
            BufferedImage lbl = (BufferedImage) imageStack.get(i);
            BufferedImage img = (BufferedImage) imageStack.get(i + 1);
            img = joinBufferedImageVertically(lbl, img, labelFont, useAA, transparent,
                    backgroundColor);
            nodes.add(img);
        }

        // Sets legend nodes into a matrix according to layout rules
        LegendLayout layout =  LegendUtils.getGroupLayout(req);
        BufferedImage finalLegend = null;
        if (layout == LegendLayout.HORIZONTAL) {
            Row[] rows = createRows(nodes, 0, 0);
            finalLegend = buildFinalHLegend(0, 0, 0, rows, transparent, backgroundColor, useAA);
        }

        if (layout == LegendLayout.VERTICAL) {
            Column[] columns = createColumns(nodes, 0, 0);
            finalLegend = buildFinalVLegend(0, 0, 0, columns, transparent, backgroundColor, useAA);
        }

        return finalLegend;

    }

    /**
     * 
     * Represents a column of legends images
     *
     */
    private static class Column {
        private int width;

        private int height;

        private List<BufferedImage> nodes = new ArrayList<BufferedImage>();

        public void addNode(BufferedImage img) {
            nodes.add(img);
            width = Math.max(width, img.getWidth());
            height = height + img.getHeight();
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public List<BufferedImage> getNodes() {
            return nodes;
        }

    }

    /**
     * 
     * Represents a row of legends images
     *
     */
    private static class Row {
        private int width;

        private int height;

        private List<BufferedImage> nodes = new ArrayList<BufferedImage>();

        public void addNode(BufferedImage img) {
            nodes.add(img);
            height = Math.max(height, img.getHeight());
            width = width + img.getWidth();
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public List<BufferedImage> getNodes() {
            return nodes;
        }

    }

    /**
     * Creates legends columns for vertical layout according to max height and max columns limits
     * 
     * @param nodes legend images
     * @param maxHeight maximum height of legend
     * @param maxColumns maximum number of columns
     * 
     */
    private static Column[] createColumns(List<BufferedImage> nodes, int maxHeight, int maxColumns) {
        Column[] legendMatrix = new Column[0];
        /*
         * Limit max height
         */
        if (maxHeight > 0) {
            /*
             * Limit max column
             */
            int cnLimit = maxColumns > 0 ? maxColumns : nodes.size();
            legendMatrix = new Column[cnLimit];
            legendMatrix[0] = new Column();
            int cn = 0;
            int columnHeight = 0;
            for (int i = 0; i < nodes.size(); i++) {
                BufferedImage node = nodes.get(i);
                if (columnHeight <= maxHeight) {
                    // Fill current column
                    legendMatrix[cn].addNode(node);
                    columnHeight = columnHeight + node.getHeight();
                } else {
                    // Add current node to next column
                    i--;
                    cn++;
                    // Stop if column limits is reached
                    if (cn == cnLimit) {
                        break;
                    }
                    // Reset column counter
                    columnHeight = 0;
                    // Create new column
                    legendMatrix[cn] = new Column();
                }
            }
        } else {
            /*
             * Limit max column, if no limit set it to 1
             */
            int colNumber = maxColumns > 0 ? maxColumns : 1;
            legendMatrix = new Column[colNumber];
            legendMatrix[0] = new Column();
            int rowNumber = (int) Math.ceil((float) nodes.size() / colNumber);
            int cn = 0;
            int rc = 0;
            for (int i = 0; i < nodes.size(); i++) {
                if (rc < rowNumber) {
                    legendMatrix[cn].addNode(nodes.get(i));
                    rc++;
                } else {
                    i--;
                    cn++;
                    rc = 0;
                    legendMatrix[cn] = new Column();
                }
            }
        }

        return legendMatrix;
    }

    /**
     * Creates legends rows for horizontal layout according to max width and max rows limits
     * 
     * @param nodes legend images
     * @param maxWidth maximum width of legend
     * @param maxRows maximum number of rows
     * 
     */
    private static Row[] createRows(List<BufferedImage> nodes, int maxWidth, int maxRows) {
        Row[] legendMatrix = new Row[0];
        /*
         * Limit max height
         */
        if (maxWidth > 0) {
            /*
             * Limit max column
             */
            int rnLimit = maxRows > 0 ? maxRows : nodes.size();
            legendMatrix = new Row[rnLimit];
            legendMatrix[0] = new Row();
            int rn = 0;
            int rowWidth = 0;
            for (int i = 0; i < nodes.size(); i++) {
                BufferedImage node = nodes.get(i);
                if (rowWidth <= maxWidth) {
                    // Fill current column
                    legendMatrix[rn].addNode(node);
                    rowWidth = rowWidth + node.getWidth();
                } else {
                    // Add current node to next column
                    i--;
                    rn++;
                    // Stop if column limits is reached
                    if (rn == rnLimit) {
                        break;
                    }
                    // Reset column counter
                    rowWidth = 0;
                    // Create new column
                    legendMatrix[rn] = new Row();
                }
            }
        } else {
            /*
             * Limit max column, if no limit set it to 1
             */
            int rowNumber = maxRows > 0 ? maxRows : 1;
            legendMatrix = new Row[rowNumber];
            legendMatrix[0] = new Row();
            int colNumber = (int) Math.ceil((float) nodes.size() / rowNumber);
            int rn = 0;
            int cc = 0;
            for (int i = 0; i < nodes.size(); i++) {
                if (cc < colNumber) {
                    legendMatrix[rn].addNode(nodes.get(i));
                    cc++;
                } else {
                    i--;
                    rn++;
                    cc = 0;
                    legendMatrix[rn] = new Row();
                }
            }
        }

        return legendMatrix;
    }

    /**
     * Renders legend columns and cut off the node that exceeds the maximum limits
     * 
     * @param dx horizontal space between images
     * @param dy vertical space between images
     * @param margin padding around image
     * @param columns list of columns to draw
     * @param transparent if true make legend transparent
     * @param backgroundColor background color of legend
     * @param useAA if true applies anti aliasing
     * @param legendMatrix the matrix of nodes of legend
     * @return BufferedImage of legend
     * 
     */
    private static BufferedImage buildFinalVLegend(int dx, int dy, int margin, Column[] columns,
            boolean transparent, Color backgroundColor, boolean useAA) {

        int totalWidth = 0;
        int totalHeight = 0;

        for (Column c : columns) {
            if (c != null) {
                if (totalWidth > 0) {
                    totalWidth = totalWidth + dx;
                }
                totalWidth = totalWidth + c.getWidth();
                int h = c.getHeight() + (c.nodes.size() - 1) * dy;
                totalHeight = Math.max(totalHeight, h);
            }
        }
        totalWidth = totalWidth + margin * 2;
        totalHeight = totalHeight + margin * 2;
        // buffer the width a bit
        totalWidth += 2;
        final BufferedImage finalLegend = ImageUtils.createImage(totalWidth, totalHeight,
                (IndexColorModel) null, transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D finalGraphics = ImageUtils.prepareTransparency(transparent, backgroundColor,
                finalLegend, hintsMap);
        // finalGraphics.setFont(labelFont);
        if (useAA) {
            finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        int vOffset = margin;
        int hOffset = margin;
        for (Column c : columns) {
            if (c != null) {
                for (BufferedImage n : c.getNodes()) {
                    finalGraphics.drawImage(n, hOffset, vOffset, null);
                    vOffset = vOffset + n.getHeight() + dy;
                }
                hOffset = hOffset + c.getWidth() + dx;
                vOffset = margin;
            }
        }

        finalGraphics.dispose();

        return finalLegend;
    }

    /**
     * Renders legend rows and cut off the node that exceeds the maximum limits
     * 
     * @param dx horizontal space between images
     * @param dy vertical space between images
     * @param margin padding around image
     * @param rows list of rows to draw
     * @param transparent if true make legend transparent
     * @param backgroundColor background color of legend
     * @param useAA if true applies anti aliasing
     * @param legendMatrix the matrix of nodes of legend
     * @return BufferedImage of legend
     * 
     */
    private static BufferedImage buildFinalHLegend(int dx, int dy, int margin, Row[] rows,
            boolean transparent, Color backgroundColor, boolean useAA) {

        int totalWidth = 0;
        int totalHeight = 0;

        for (Row r : rows) {
            if (r != null) {
                if (totalHeight > 0) {
                    totalHeight = totalHeight + dy;
                }
                totalHeight = totalHeight + r.getHeight();
                int w = r.getWidth() + (r.nodes.size() - 1) * dx;
                totalWidth = Math.max(totalWidth, w);
            }
        }
        totalWidth = totalWidth + margin * 2;
        totalHeight = totalHeight + margin * 2;
        // buffer the width a bit
        totalWidth += 2;
        final BufferedImage finalLegend = ImageUtils.createImage(totalWidth, totalHeight,
                (IndexColorModel) null, transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D finalGraphics = ImageUtils.prepareTransparency(transparent, backgroundColor,
                finalLegend, hintsMap);
        // finalGraphics.setFont(labelFont);
        if (useAA) {
            finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        int vOffset = margin;
        int hOffset = margin;
        for (Row r : rows) {
            if (r != null) {
                for (BufferedImage n : r.getNodes()) {
                    finalGraphics.drawImage(n, hOffset, vOffset, null);
                    hOffset = hOffset + n.getWidth() + dx;
                }
                vOffset = vOffset + r.getHeight() + dy;
                hOffset = margin;
            }
        }

        finalGraphics.dispose();

        return finalLegend;
    }

    /**
     * Join image and label to create a single legend node image horizzontally
     * 
     * @param img image of legend
     * @param label label of legend
     * @param labelFont font to use
     * @param useAA if true applies anti aliasing
     * @param transparent if true make legend transparent
     * @param backgroundColor background color of legend
     * @return BufferedImage of image and label side by side and vertically center
     */
    private static BufferedImage joinBufferedImageHorizzontally(BufferedImage img,
            BufferedImage label, Font labelFont, boolean useAA, boolean transparent,
            Color backgroundColor) {
        // do some calculate first
        int offset = 0;
        int wid = img.getWidth() + label.getWidth() + offset;
        int height = Math.max(img.getHeight(), label.getHeight()) + offset;
        // create a new buffer and draw two image into the new image
        BufferedImage newImage = ImageUtils.createImage(wid, height, (IndexColorModel) null,
                transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D g2 = ImageUtils.prepareTransparency(transparent, backgroundColor, newImage,
                hintsMap);
        g2.setFont(labelFont);
        if (useAA) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        // move the images to the vertical center of the row
        int imgOffset = (int) Math.round((height - img.getHeight()) / 2d);
        int labelOffset = (int) Math.round((height - label.getHeight()) / 2d);
        g2.drawImage(img, null, 0, imgOffset);
        g2.drawImage(label, null, img.getWidth() + offset, labelOffset);
        g2.dispose();
        return newImage;
    }

    /**
     * Join image and label to create a single legend node image vertically
     * 
     * @param img image of legend
     * @param label label of legend
     * @param labelFont font to use
     * @param useAA if true applies anti aliasing
     * @param transparent if true make legend transparent
     * @param backgroundColor background color of legend
     * @return BufferedImage of image and label side by side and vertically center
     */
    private static BufferedImage joinBufferedImageVertically(BufferedImage label,
            BufferedImage img, Font labelFont, boolean useAA, boolean transparent,
            Color backgroundColor) {
        // do some calculate first
        int offset = 0;
        int height = img.getHeight() + label.getHeight() + offset;
        int wid = Math.max(img.getWidth(), label.getWidth()) + offset;
        // create a new buffer and draw two image into the new image
        BufferedImage newImage = ImageUtils.createImage(wid, height, (IndexColorModel) null,
                transparent);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        Graphics2D g2 = ImageUtils.prepareTransparency(transparent, backgroundColor, newImage,
                hintsMap);
        g2.setFont(labelFont);
        if (useAA) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        // move the images to the vertical center of the row
        g2.drawImage(label, null, 0, 0);
        g2.drawImage(img, null, 0, label.getHeight());
        g2.dispose();
        return newImage;
    }

    /**
     * Renders the legend image label
     * 
     * @param img the BufferedImage
     * @param rule the applicable rule for img, if rule is not null the label will be rendered
     * @param req the request
     * @param forceLabelsOff true for force labels off also with more than one rule
     * @return the BufferedImage of label
     * 
     */
    private static BufferedImage renderLabel(RenderedImage img, Rule rule,
            GetLegendGraphicRequest req, boolean forceLabelsOff) {
        BufferedImage labelImg = null;
        if (!forceLabelsOff && rule != null) {
            // What's the label on this rule? We prefer to use
            // the 'title' if it's available, but fall-back to 'name'
            final Description description = rule.getDescription();
            Locale locale = req.getLocale();
            String label = "";
            if (description != null && description.getTitle() != null) {
                final InternationalString title = description.getTitle();
                if (locale != null) {
                    label = title.toString(locale);
                } else {
                    label = title.toString();
                }
            } else if (rule.getName() != null) {
                label = rule.getName();
            }
            if (label != null && label.length() > 0) {
                final BufferedImage renderedLabel = getRenderedLabel((BufferedImage) img, label,
                        req);
                labelImg = renderedLabel;
            }
        }
        return labelImg;
    }

    /**
     * Renders a label on the given image, using parameters from the request for the rendering style.
     * 
     * @param image
     * @param label
     * @param request
     *
     */
    protected static BufferedImage getRenderedLabel(BufferedImage image, String label,
            GetLegendGraphicRequest request) {
        Font labelFont = LegendUtils.getLabelFont(request);
        boolean useAA = LegendUtils.isFontAntiAliasing(request);

        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(labelFont);
        if (useAA) {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        return LegendUtils.renderLabel(label, graphics, request);
    }

}
