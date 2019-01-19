/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import static org.geoserver.wms.decoration.ScaleLineDecoration.MeasurementSystem.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wms.WMSMapContent;

public class ScaleLineDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    private static Map<String, Double> INCHES_PER_UNIT = new HashMap<String, Double>();

    static {
        INCHES_PER_UNIT.put("inches", 1.0);
        INCHES_PER_UNIT.put("ft", 12.0);
        INCHES_PER_UNIT.put("mi", 63360.0);
        INCHES_PER_UNIT.put("m", 39.3701);
        INCHES_PER_UNIT.put("km", 39370.1);
        INCHES_PER_UNIT.put("dd", 4374754.0);
        INCHES_PER_UNIT.put("yd", 36.0);
    }

    public static final String topOutUnits = "km";
    public static final String topInUnits = "m";
    public static final String bottomOutUnits = "mi";
    public static final String bottomInUnits = "ft";
    public static final int suggestedWidth = 100;

    private float fontSize = 10;
    private float dpi = 25.4f / 0.28f; // / OGC Spec for SLD

    private Color bgcolor = Color.WHITE;
    private Color fgcolor = Color.BLACK;

    private Boolean transparent = Boolean.FALSE;

    private MeasurementSystem measurementSystem = BOTH;

    static enum MeasurementSystem {
        METRIC,
        IMPERIAL,
        BOTH;

        static MeasurementSystem mapToEnum(String type) throws Exception {
            switch (type) {
                case "metric":
                    return METRIC;
                case "imperial":
                    return IMPERIAL;
                case "both":
                    return BOTH;
                default:
                    throw new Exception("Wrong input parameter");
            }
        }
    }

    public void loadOptions(Map<String, String> options) {
        if (options.get("fontsize") != null) {
            try {
                this.fontSize = Float.parseFloat(options.get("fontsize"));
            } catch (Exception e) {
                this.LOGGER.log(Level.WARNING, "'fontsize' must be a float.", e);
            }
        }

        if (options.get("dpi") != null) {
            try {
                this.dpi = Float.parseFloat(options.get("dpi"));
            } catch (Exception e) {
                this.LOGGER.log(Level.WARNING, "'dpi' must be a float.", e);
            }
        }

        Color tmp = MapDecorationLayout.parseColor(options.get("bgcolor"));
        if (tmp != null) bgcolor = tmp;

        tmp = MapDecorationLayout.parseColor(options.get("fgcolor"));
        if (tmp != null) fgcolor = tmp;

        // Creates a rectangle only if is defined, if not is "transparent" like Google Maps
        if (options.get("transparent") != null) {
            try {
                this.transparent = Boolean.parseBoolean(options.get("transparent"));
            } catch (Exception e) {
                this.LOGGER.log(Level.WARNING, "'transparent' must be a boolean.", e);
            }
        }

        if (options.get("measurement-system") != null) {
            try {
                LOGGER.log(Level.INFO, options.get("measurement-system"));
                this.measurementSystem =
                        MeasurementSystem.mapToEnum(options.get("measurement-system"));
            } catch (Exception e) {
                this.LOGGER.log(
                        Level.WARNING,
                        "'measurement-system' must be one of 'metric', 'imperial' or 'both'.",
                        e);
            }
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        return new Dimension(suggestedWidth, 8 + (metrics.getHeight() + metrics.getDescent()) * 2);
    }

    public int getBarLength(double maxLength) {
        int digits = (int) (Math.log(maxLength) / Math.log(10));
        double pow10 = Math.pow(10, digits);

        // Find first character
        int firstCharacter = (int) (maxLength / pow10);

        int barLength;
        if (firstCharacter > 5) {
            barLength = 5;
        } else if (firstCharacter > 2) {
            barLength = 2;
        } else {
            barLength = 1;
        }

        return (int) (barLength * pow10);
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
            throws Exception {
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();
        Font oldFont = g2d.getFont();
        Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Set the font size.
        g2d.setFont(oldFont.deriveFont(this.fontSize));

        double scaleDenominator = mapContent.getScaleDenominator(true);

        String curMapUnits = "m";

        double normalizedScale =
                (scaleDenominator > 1.0) ? (1.0 / scaleDenominator) : scaleDenominator;

        double resolution = 1 / (normalizedScale * INCHES_PER_UNIT.get(curMapUnits) * this.dpi);

        int maxWidth = suggestedWidth;

        if (maxWidth > paintArea.getWidth()) {
            maxWidth = (int) paintArea.getWidth();
        }

        maxWidth = maxWidth - 6;

        double maxSizeData = maxWidth * resolution * INCHES_PER_UNIT.get(curMapUnits);

        String topUnits;
        String bottomUnits;

        if (maxSizeData > 100000) {
            topUnits = topOutUnits;
            bottomUnits = bottomOutUnits;
        } else {
            topUnits = topInUnits;
            bottomUnits = bottomInUnits;
        }

        double topMax = maxSizeData / INCHES_PER_UNIT.get(topUnits);
        double bottomMax = maxSizeData / INCHES_PER_UNIT.get(bottomUnits);

        int topRounded = this.getBarLength(topMax);
        int bottomRounded = this.getBarLength(bottomMax);

        topMax = topRounded / INCHES_PER_UNIT.get(curMapUnits) * INCHES_PER_UNIT.get(topUnits);
        bottomMax =
                bottomRounded / INCHES_PER_UNIT.get(curMapUnits) * INCHES_PER_UNIT.get(bottomUnits);

        int topPx = (int) (topMax / resolution);
        int bottomPx = (int) (bottomMax / resolution);

        int centerY = (int) paintArea.getCenterY();
        int leftX =
                (int) paintArea.getMinX()
                        + ((int) paintArea.getWidth() - Math.max(topPx, bottomPx)) / 2;

        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        int prongHeight = metrics.getHeight() + metrics.getDescent();

        // Do not antialias scaleline lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Creates a rectangle only if is defined, if not is "transparent" like Google Maps
        if (!this.transparent) {
            Rectangle frame =
                    new Rectangle(
                            leftX - 4,
                            centerY - prongHeight - 4,
                            Math.max(topPx, bottomPx) + 8,
                            8 + prongHeight * 2);

            // fill the rectangle
            g2d.setColor(bgcolor);
            g2d.fill(frame);

            // draw the border
            frame.height -= 1;
            frame.width -= 1;
            g2d.setColor(fgcolor);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(frame);
        } else {
            g2d.setColor(fgcolor);
        }

        g2d.setStroke(new BasicStroke(2));

        if (measurementSystem == METRIC || measurementSystem == BOTH) {

            // Left vertical top bar
            g2d.drawLine(leftX, centerY, leftX, centerY - prongHeight);

            // Right vertical top bar
            g2d.drawLine(leftX + topPx, centerY, leftX + topPx, centerY - prongHeight);

            // Draw horizontal line for metric
            g2d.drawLine(leftX, centerY, leftX + topPx, centerY);

            // Antialias text if enabled
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

            // Draw text metric
            String topText = topRounded + " " + topUnits;
            g2d.drawString(
                    topText,
                    leftX + (int) ((topPx - metrics.stringWidth(topText)) / 2),
                    centerY - prongHeight + metrics.getAscent());
        }

        if (measurementSystem == IMPERIAL || measurementSystem == BOTH) {

            // Do not antialias scaleline lines
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            // Left vertical bottom bar
            g2d.drawLine(leftX, centerY + prongHeight, leftX, centerY);

            // Right vertical bottom bar
            g2d.drawLine(leftX + bottomPx, centerY, leftX + bottomPx, centerY + prongHeight);

            // Draw horizontal for imperial
            g2d.drawLine(leftX, centerY, leftX + bottomPx, centerY);

            // Antialias text if enabled
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);

            // Draw text imperial
            String bottomText = bottomRounded + " " + bottomUnits;
            g2d.drawString(
                    bottomText,
                    leftX + (int) ((bottomPx - metrics.stringWidth(bottomText)) / 2),
                    centerY + metrics.getHeight());
        }

        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
        g2d.setFont(oldFont);
    }
}
