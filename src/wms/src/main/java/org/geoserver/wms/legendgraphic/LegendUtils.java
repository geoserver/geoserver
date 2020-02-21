/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.renderer.i18n.ErrorKeys;
import org.geotools.renderer.i18n.Errors;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.SuppressFBWarnings;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.style.ChannelSelection;
import org.opengis.util.InternationalString;

/**
 * Utility class for building legends, it exposes many methods that could be reused anywhere.
 *
 * <p>I am not preventin people from subclassing this method so that they could add their own
 * utility methods.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class LegendUtils {
    /**
     * Ensures that the provided argument is not <code>null</code>.
     *
     * <p>If it <code>null</code> it must throw a {@link NullPointerException}.
     *
     * @param argument argument to check for <code>null</code>.
     */
    protected static void ensureNotNull(final Object argument) {
        ensureNotNull(argument, "Argument cannot be null");
    }

    /**
     * Ensures that the provided argument is not <code>null</code>.
     *
     * <p>If it <code>null</code> it must throw a {@link NullPointerException}.
     *
     * @param argument argument to check for <code>null</code>.
     * @param message leading message to print out in case the test fails.
     */
    protected static void ensureNotNull(final Object argument, final String message) {
        if (message == null) throw new NullPointerException("Message cannot be null");
        if (argument == null) throw new NullPointerException(message + " cannot be null");
    }

    /** Legend layouts */
    public enum LegendLayout {
        HORIZONTAL,
        VERTICAL;
    }

    public enum VAlign {
        TOP,
        MIDDLE,
        BOTTOM;
    }

    public enum HAlign {
        LEFT,
        CENTERED,
        RIGHT,
        JUSTIFIED;
    }

    /** Default {@link Font} name for legends. */
    public static final String DEFAULT_FONT_NAME = "Sans-Serif";

    /** Default channel name for {@link ChannelSelection} elements. */
    public static final String DEFAULT_CHANNEL_NAME = "1";

    /** Default {@link Font} for legends. */
    public static final int DEFAULT_FONT_TYPE = Font.PLAIN;

    /** Default {@link Font} for legends. */
    public static final int DEFAULT_FONT_SIZE = 12;

    /** Default {@link Font} for legends. */
    public static final Font DEFAULT_FONT = new Font("Sans-Serif", Font.PLAIN, 12);

    /** Default Legend graphics background color */
    public static final Color DEFAULT_BG_COLOR = Color.WHITE;
    /** Default label color */
    public static final Color DEFAULT_FONT_COLOR = Color.BLACK;

    /** padding percentage factor at both sides of the legend. */
    public static final float hpaddingFactor = 0.15f;
    /** top & bottom padding percentage factor for the legend */
    public static final float vpaddingFactor = 0.15f;

    /** padding percentage factor at both sides of the legend. */
    public static final float rowPaddingFactor = 0.15f;
    /** top & bottom padding percentage factor for the legend */
    public static final float columnPaddingFactor = 0.15f;

    /** padding percentage factor at both sides of the legend. */
    public static final float marginFactor = 0.015f;

    /** default legend graphic layout is vertical */
    private static final LegendLayout DEFAULT_LAYOUT = LegendLayout.VERTICAL;

    /** default column height is not limited */
    private static final int DEFAULT_COLUMN_HEIGHT = 0;

    /** default row width is not limited */
    private static final int DEFAULT_ROW_WIDTH = 0;

    /** default column number is not limited */
    private static final int DEFAULT_COLUMNS = 0;

    /** default row number is not limited */
    private static final int DEFAULT_ROWS = 0;

    /** shared package's logger */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(LegendUtils.class.getPackage().getName());

    public static final Color DEFAULT_BORDER_COLOR = Color.black;

    /**
     * Retrieves the legend layout from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract the {@link
     *     LegendLayout} information.
     * @return the {@link LegendLayout} specified in the provided {@link GetLegendGraphicRequest} or
     *     a default DEFAULT_LAYOUT.
     */
    public static LegendLayout getLayout(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        LegendLayout layout = DEFAULT_LAYOUT;
        if (legendOptions != null && legendOptions.get("layout") != null) {
            try {
                layout = LegendLayout.valueOf(((String) legendOptions.get("layout")).toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return layout;
    }

    /**
     * Retrieves the group legend layout from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract the group {@link
     *     LegendLayout} information.
     * @return the group {@link LegendLayout} specified in the provided {@link
     *     GetLegendGraphicRequest} or a default DEFAULT_LAYOUT.
     */
    public static LegendLayout getGroupLayout(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        LegendLayout layout = DEFAULT_LAYOUT;
        if (legendOptions != null && legendOptions.get("grouplayout") != null) {
            try {
                layout =
                        LegendLayout.valueOf(
                                ((String) legendOptions.get("grouplayout")).toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return layout;
    }

    /**
     * Retrieves column height of legend from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract column height
     *     information.
     * @return the column height specified in the provided {@link GetLegendGraphicRequest} or a
     *     default DEFAULT_COLUMN_HEIGHT.
     */
    public static int getColumnHeight(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int columnheight = DEFAULT_COLUMN_HEIGHT;
        if (legendOptions != null && legendOptions.get("columnheight") != null) {
            try {
                columnheight = Integer.parseInt((String) legendOptions.get("columnheight"));
            } catch (NumberFormatException e) {
            }
        }
        return columnheight;
    }

    /**
     * Retrieves row width of legend from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract row width
     *     information.
     * @return the row width specified in the provided {@link GetLegendGraphicRequest} or a default
     *     DEFAULT_ROW_WIDTH.
     */
    public static int getRowWidth(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int rowwidth = DEFAULT_ROW_WIDTH;
        if (legendOptions != null && legendOptions.get("rowwidth") != null) {
            try {
                rowwidth = Integer.parseInt((String) legendOptions.get("rowwidth"));
            } catch (NumberFormatException e) {
            }
        }
        return rowwidth;
    }

    /**
     * Retrieves columns of legend from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract columns
     *     information.
     * @return the columns specified in the provided {@link GetLegendGraphicRequest} or a default
     *     DEFAULT_COLUMNS.
     */
    public static int getColumns(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int columns = DEFAULT_COLUMNS;
        if (legendOptions != null && legendOptions.get("columns") != null) {
            try {
                columns = Integer.parseInt((String) legendOptions.get("columns"));
            } catch (NumberFormatException e) {
            }
        }
        return columns;
    }

    /**
     * Retrieves rows of legend from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract rows information.
     * @return the rows specified in the provided {@link GetLegendGraphicRequest} or a default
     *     DEFAULT_ROWS.
     */
    public static int getRows(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int rows = DEFAULT_ROWS;
        if (legendOptions != null && legendOptions.get("rows") != null) {
            try {
                rows = Integer.parseInt((String) legendOptions.get("rows"));
            } catch (NumberFormatException e) {
            }
        }
        return rows;
    }

    /**
     * Retrieves the font from the provided {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract the {@link Font}
     *     information.
     * @return the {@link Font} specified in the provided {@link GetLegendGraphicRequest} or a
     *     default {@link Font}.
     */
    public static Font getLabelFont(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        if (legendOptions == null) return DEFAULT_FONT;
        String legendFontName = LegendUtils.DEFAULT_FONT_NAME;
        if (legendOptions.get("fontName") != null) {
            legendFontName = (String) legendOptions.get("fontName");
        }

        int legendFontFamily = LegendUtils.DEFAULT_FONT_TYPE;
        if (legendOptions.get("fontStyle") != null) {
            String legendFontFamily_ = (String) legendOptions.get("fontStyle");
            if (legendFontFamily_.equalsIgnoreCase("italic")) {
                legendFontFamily = Font.ITALIC;
            } else if (legendFontFamily_.equalsIgnoreCase("bold")) {
                legendFontFamily = Font.BOLD;
            }
        }

        int legendFontSize = LegendUtils.DEFAULT_FONT_SIZE;
        if (legendOptions.get("fontSize") != null) {
            try {
                legendFontSize = Integer.valueOf((String) legendOptions.get("fontSize"));
            } catch (NumberFormatException e) {
                LOGGER.warning(
                        "Error trying to interpret legendOption 'fontSize': "
                                + legendOptions.get("fontSize"));
                legendFontSize = LegendUtils.DEFAULT_FONT_SIZE;
            }
        }

        double dpi = RendererUtilities.getDpi(req.getLegendOptions());
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        if (dpi != standardDpi) {
            double scaleFactor = dpi / standardDpi;
            legendFontSize = (int) Math.ceil(legendFontSize * scaleFactor);
        }

        if (legendFontFamily == LegendUtils.DEFAULT_FONT_TYPE
                && legendFontName.equalsIgnoreCase(LegendUtils.DEFAULT_FONT_NAME)
                && (legendFontSize == LegendUtils.DEFAULT_FONT_SIZE || legendFontSize <= 0))
            return DEFAULT_FONT;

        return new Font(legendFontName, legendFontFamily, legendFontSize);
    }

    /**
     * Extracts the Label {@link Font} {@link Color} from the provided {@link
     * GetLegendGraphicRequest}.
     *
     * <p>If there is no label {@link Font} specified a default {@link Font} {@link Color} will be
     * provided.
     *
     * @param req the {@link GetLegendGraphicRequest} from which to extract label color information.
     * @return the Label {@link Font} {@link Color} extracted from the provided {@link
     *     GetLegendGraphicRequest} or a default {@link Font} {@link Color}.
     */
    public static Color getLabelFontColor(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        final String color = legendOptions != null ? (String) legendOptions.get("fontColor") : null;
        if (color == null) {
            // return the default
            return DEFAULT_FONT_COLOR;
        }

        try {
            return color(color);
        } catch (NumberFormatException e) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.warning(
                        "Could not decode label color: "
                                + color
                                + ", default to "
                                + DEFAULT_FONT_COLOR.toString());
            return DEFAULT_FONT_COLOR;
        }
    }

    /**
     * Checks if the graphics should be text antialiasing
     *
     * @param req the {@link GetLegendGraphicRequest} from which to extract font antialiasing
     *     information.
     * @return true if the fontAntiAliasing is set to on
     */
    public static boolean isFontAntiAliasing(final GetLegendGraphicRequest req) {
        if (req.getLegendOptions().get("fontAntiAliasing") instanceof String) {
            String aaVal = (String) req.getLegendOptions().get("fontAntiAliasing");
            if (aaVal.equalsIgnoreCase("on")
                    || aaVal.equalsIgnoreCase("true")
                    || aaVal.equalsIgnoreCase("yes")
                    || aaVal.equalsIgnoreCase("1")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the image background color for the given {@link GetLegendGraphicRequest}.
     *
     * @param req a {@link GetLegendGraphicRequest} from which we should extract the background
     *     color.
     * @return the Color for the hexadecimal value passed as the <code>BGCOLOR</code> {@link
     *     GetLegendGraphicRequest#getLegendOptions() legend option}, or the default background
     *     color if no bgcolor were passed.
     */
    public static Color getBackgroundColor(final GetLegendGraphicRequest req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        if (legendOptions == null) return DEFAULT_BG_COLOR;
        Object clr = legendOptions.get("bgColor");
        if (clr instanceof Color) {
            return (Color) clr;
        } else if (clr == null) {
            // return the default
            return DEFAULT_BG_COLOR;
        }

        try {
            return color((String) clr);
        } catch (NumberFormatException e) {
            LOGGER.warning(
                    "Could not decode background color: "
                            + clr
                            + ", default to "
                            + DEFAULT_BG_COLOR.toString());
            return DEFAULT_BG_COLOR;
        }
    }

    /**
     * Extracts the opacity from the provided {@link ColorMapEntry}.
     *
     * <p>1.0 is returned in case the provided {@link ColorMapEntry} is null or invalid.
     *
     * @return the opacity from the provided {@link ColorMapEntry} or 1.0 if something bad happens.
     */
    public static double getOpacity(final ColorMapEntry entry)
            throws IllegalArgumentException, MissingResourceException {

        ensureNotNull(entry, "ColorMapEntry");
        // //
        //
        // As stated in <a
        // href="https://portal.opengeospatial.org/files/?artifact_id=1188">
        // OGC Styled-Layer Descriptor Report (OGC 02-070) version
        // 1.0.0.</a>:
        // "Not all systems can support opacity in colormaps. The default
        // opacity is 1.0 (fully opaque)."
        //
        // //
        Expression opacity = entry.getOpacity();
        Double opacityValue = null;
        if (opacity != null) opacityValue = opacity.evaluate(null, Double.class);
        else return 1.0;
        if (opacityValue == null && opacity instanceof Literal) {
            String opacityExp = opacity.evaluate(null, String.class);
            opacity = ExpressionExtractor.extractCqlExpressions(opacityExp);
            opacityValue = opacity.evaluate(null, Double.class);
        }
        if (opacityValue == null
                || (opacityValue.doubleValue() - 1) > 0
                || opacityValue.doubleValue() < 0) {
            throw new IllegalArgumentException(
                    Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2, "Opacity", opacityValue));
        }
        return opacityValue.doubleValue();
    }

    /**
     * Tries to decode the provided {@link String} into an HEX color definition in RRGGBB, 0xRRGGBB
     * or #RRGGBB format
     *
     * <p>In case the {@link String} is not correct a {@link NumberFormatException} will be thrown.
     *
     * @param hex a {@link String} that should contain an Hexadecimal color representation.
     * @return a {@link Color} representing the provided {@link String}.
     * @throws NumberFormatException in case the string is badly formatted.
     */
    public static Color color(String hex) {
        ensureNotNull(hex, "hex value");
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return Color.decode(hex);
    }

    /**
     * Get the {@link Color} out of this {@link ColorMapEntry}.
     *
     * @param entry the {@link ColorMapEntry} from which to extract the {@link Color} component.
     * @return the {@link Color} out of this {@link ColorMapEntry}.
     * @throws NumberFormatException in case the color string is badly formatted.
     */
    @SuppressFBWarnings({
        "NP_NULL_ON_SOME_PATH",
        "NP_NULL_PARAM_DEREF"
    }) // SP does not recognize the check in ensureNotNull
    public static Color color(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        Expression color = entry.getColor();
        ensureNotNull(color, "color");
        String colorString = color.evaluate(null, String.class);
        if (colorString != null && colorString.startsWith("${")) {
            color = ExpressionExtractor.extractCqlExpressions(colorString);
            colorString = color.evaluate(null, String.class);
        }
        ensureNotNull(colorString, "colorString");
        return color(colorString);
    }

    /**
     * Extracts the quantity part from the provided {@link ColorMapEntry}.
     *
     * @param entry the provided {@link ColorMapEntry} from which we should extract the quantity
     *     part.
     * @return the quantity part for the provided {@link ColorMapEntry}.
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH") // SP does not recognize the check in ensureNotNull
    public static double getQuantity(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        Expression quantity = entry.getQuantity();
        ensureNotNull(quantity, "quantity");
        Double quantityString = quantity.evaluate(null, Double.class);
        if (quantityString == null && quantity instanceof Literal) {
            String quantityExp = quantity.evaluate(null, String.class);
            quantity = ExpressionExtractor.extractCqlExpressions(quantityExp);
            quantityString = quantity.evaluate(null, Double.class);
        }
        ensureNotNull(quantityString, "quantityString");
        return quantityString.doubleValue();
    }

    /**
     * Extracts the label part from the provided {@link ColorMapEntry}.
     *
     * @param entry the provided {@link ColorMapEntry} from which we should extract the label part.
     * @return the label part for the provided {@link ColorMapEntry}.
     */
    public static String getLabel(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        String labelString = entry.getLabel();
        if (labelString != null && labelString.startsWith("${")) {
            Expression label = ExpressionExtractor.extractCqlExpressions(labelString);
            labelString = label.evaluate(null, String.class);
        }
        return labelString;
    }

    /**
     * Finds the applicable Rules for the given scale denominator.
     *
     * @return an array of {@link Rule}s.
     */
    public static Rule[] getApplicableRules(
            final FeatureTypeStyle[] ftStyles, double scaleDenominator) {
        ensureNotNull(ftStyles, "FeatureTypeStyle array ");
        /** Holds both the rules that apply and the ElseRule's if any, in the order they appear */
        final List<Rule> ruleList = new ArrayList<Rule>();

        // get applicable rules at the current scale
        for (int i = 0; i < ftStyles.length; i++) {
            FeatureTypeStyle fts = ftStyles[i];
            for (Rule r : fts.rules()) {
                if (isWithInScale(r, scaleDenominator)) {
                    ruleList.add(r);

                    /*
                    * I'm commented this out since I guess it has no sense
                    * for producing the legend, since whether or not the rule
                    * has an else filter, the legend is drawn only if the
                    * scale denominator lies inside the rule's scale range.
                             if (r.hasElseFilter()) {
                                 ruleList.add(r);
                             }
                    */
                }
            }
        }

        return ruleList.toArray(new Rule[ruleList.size()]);
    }

    /**
     * Checks if a rule can be triggered at the current scale level
     *
     * @param r The rule
     * @param scaleDenominator the scale denominator to check if it is between the rule's scale
     *     range. -1 means that it allways is.
     * @return true if the scale is compatible with the rule settings
     */
    public static boolean isWithInScale(final Rule r, final double scaleDenominator) {
        return (scaleDenominator == -1)
                || (((r.getMinScaleDenominator() - LegendGraphicBuilder.TOLERANCE)
                                <= scaleDenominator)
                        && ((r.getMaxScaleDenominator() + LegendGraphicBuilder.TOLERANCE)
                                > scaleDenominator));
    }

    /**
     * Return a {@link BufferedImage} representing this label. The characters '\n' '\r' and '\f' are
     * interpreted as linebreaks, as is the characater combination "\n" (as opposed to the actual
     * '\n' character). This allows people to force line breaks in their labels by including the
     * character "\" followed by "n" in their label.
     *
     * @param label - the label to render
     * @param g - the Graphics2D that will be used to render this label
     * @return a {@link BufferedImage} of the properly rendered label.
     */
    public static BufferedImage renderLabel(
            final String label, final Graphics2D g, final GetLegendGraphicRequest req) {
        ensureNotNull(label);
        ensureNotNull(g);
        ensureNotNull(req);
        // We'll accept '/n' as a text string
        // to indicate a line break, as well as a traditional 'real' line-break in the XML.
        BufferedImage renderedLabel;
        Color labelColor = getLabelFontColor(req);
        if ((label.indexOf("\n") != -1) || (label.indexOf("\\n") != -1)) {
            // this is a label WITH line-breaks...we need to figure out it's height *and*
            // width, and then adjust the legend size accordingly
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, 0, 0);
            ArrayList<Integer> lineHeight = new ArrayList<Integer>();
            // four backslashes... "\\" -> '\', so "\\\\n" -> '\' + '\' + 'n'
            final String realLabel = label.replaceAll("\\\\n", "\n");
            StringTokenizer st = new StringTokenizer(realLabel, "\n\r\f");

            while (st.hasMoreElements()) {
                final String token = st.nextToken();
                Rectangle2D thisLineBounds = g.getFontMetrics().getStringBounds(token, g);

                // if this is directly added as thisLineBounds.getHeight(), then there are rounding
                // errors
                // because we can only DRAW fonts at discrete integer coords.
                final int thisLineHeight = (int) Math.ceil(thisLineBounds.getHeight());
                bounds.add(0, thisLineHeight + bounds.getHeight());
                bounds.add(thisLineBounds.getWidth(), 0);
                lineHeight.add((int) Math.ceil(thisLineBounds.getHeight()));
            }

            // make the actual label image
            renderedLabel =
                    new BufferedImage(
                            (int) Math.ceil(bounds.getWidth()),
                            (int) Math.ceil(bounds.getHeight()),
                            BufferedImage.TYPE_INT_ARGB);

            st = new StringTokenizer(realLabel, "\n\r\f");

            Graphics2D rlg = renderedLabel.createGraphics();
            rlg.setColor(labelColor);
            rlg.setFont(g.getFont());
            rlg.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
            rlg.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    g.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
            int y = 0 - g.getFontMetrics().getDescent();
            int c = 0;

            while (st.hasMoreElements()) {
                y += lineHeight.get(c++).intValue();
                rlg.drawString(st.nextToken(), 0, y);
            }
            rlg.dispose();
        } else {
            // this is a traditional 'regular-old' label.  Just figure the
            // size and act accordingly.
            int height = (int) Math.ceil(g.getFontMetrics().getStringBounds(label, g).getHeight());
            int width = (int) Math.ceil(g.getFontMetrics().getStringBounds(label, g).getWidth());
            renderedLabel = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D rlg = renderedLabel.createGraphics();
            rlg.setColor(labelColor);
            rlg.setFont(g.getFont());
            rlg.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
            rlg.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    g.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
            rlg.drawString(label, 0, height - rlg.getFontMetrics().getDescent());
            rlg.dispose();
        }

        return renderedLabel;
    }

    /**
     * This method tries to merge horizontally 3 {@link BufferedImage}. The first one must be not
     * null, the others can be null.
     *
     * @param left first {@link BufferedImage} to merge.
     * @param center second {@link BufferedImage} to merge.
     * @param right third {@link BufferedImage} to merge.
     * @param hintsMap hints to use for drawing
     * @param dx buffer between images
     * @param transparent tells me whether or not the bkg should be transparent
     * @param backgroundColor the background color
     * @return a {@link BufferedImage} for the union of the provided images.
     */
    public static BufferedImage hMergeBufferedImages(
            final BufferedImage left,
            final BufferedImage center,
            final BufferedImage right,
            final Map<Key, Object> hintsMap,
            final boolean transparent,
            final Color backgroundColor,
            final double dx) {
        if (right == null && center == null) return left;
        if (left == null) throw new NullPointerException("Left image cannot be null");
        int totalHeight =
                (int)
                        (Math.max(
                                        left.getHeight(),
                                        Math.max(
                                                (center != null
                                                        ? center.getHeight()
                                                        : Double.NEGATIVE_INFINITY),
                                                right != null ? right.getHeight() : 0))
                                + 0.5);
        final int totalWidth =
                (int)
                        (left.getWidth()
                                + (center != null ? center.getWidth() : 0)
                                + (right != null ? right.getWidth() : 0)
                                + 2 * dx
                                + 0.5);
        final BufferedImage finalImage =
                ImageUtils.createImage(
                        totalWidth, totalHeight, (IndexColorModel) null, transparent);
        final Graphics2D finalGraphics =
                ImageUtils.prepareTransparency(transparent, backgroundColor, finalImage, hintsMap);

        // place the left element
        int offsetX = 0;
        finalGraphics.drawImage(left, offsetX, 0, null);

        /// place the central element
        offsetX = (int) (left.getWidth() + dx + 0.5);
        if (center != null) {
            finalGraphics.drawImage(center, offsetX, 0, null);
            offsetX += (int) (center.getWidth() + dx + 0.5);
        }

        /// place the right element in case we have it
        if (right != null) {
            finalGraphics.drawImage(right, offsetX, 0, null);
        }

        finalGraphics.dispose();
        return (BufferedImage) finalImage;
    }

    /**
     * Checks if the provided {@link FeatureType} contains a coverage as per used by the {@link
     * StreamingRenderer}.
     *
     * @param layer a {@link FeatureType} to check if it contains a grid.
     * @return <code>true</code> if this layer contains a gridcoverage, <code>false</code>
     *     otherwise.
     */
    public static boolean checkGridLayer(final FeatureType layer) {
        if (!(layer instanceof SimpleFeatureType)) return false;
        boolean found = false;
        final Collection<PropertyDescriptor> descriptors = layer.getDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {

            // get the type
            final PropertyType type = descriptor.getType();
            if (type.getBinding().isAssignableFrom(GridCoverage2D.class)
                    || type.getBinding().isAssignableFrom(GridCoverage2DReader.class)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /** Checks if the provided style contains at least one {@link RasterSymbolizer} */
    public static boolean checkRasterSymbolizer(final Style style) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                for (Symbolizer s : r.symbolizers()) {
                    if (s instanceof RasterSymbolizer) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Locates the specified rule by name */
    public static Rule getRule(FeatureTypeStyle[] fts, String rule) {
        Rule sldRule = null;
        for (int i = 0; i < fts.length; i++) {
            for (Rule r : fts[i].rules()) {
                if (rule.equalsIgnoreCase(r.getName())) {
                    sldRule = r;
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                new StringBuffer("found requested rule: ").append(rule).toString());
                    }

                    break;
                }
            }
        }

        return sldRule;
    }

    static String getRuleLabel(Rule rule, GetLegendGraphicRequest req) {
        // What's the label on this rule? We prefer to use
        // the 'title' if it's available, but fall-back to 'name'
        final Description description = rule.getDescription();

        String label = "";
        if (description != null && description.getTitle() != null) {
            final InternationalString title = description.getTitle();
            if (req.getLocale() != null) {
                label = title.toString(req.getLocale());
            } else {
                label = title.toString();
            }
        } else if (rule.getName() != null) {
            label = rule.getName();
        }
        return label;
    }
}
