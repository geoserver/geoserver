/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import static org.geoserver.template.TemplateUtils.FM_VERSION;
import static org.geoserver.wms.decoration.MapDecorationLayout.FF;
import static org.geoserver.wms.decoration.MapDecorationLayout.getOption;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.StringModel;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.renderer.style.FontCache;
import org.geotools.util.Converters;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/**
 * A map decoration showing a text message driven by a Freemarker template
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TextDecoration implements MapDecoration {

    /** A logger for this class. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    private static Font DEFAULT_FONT = new java.awt.Font("Serif", java.awt.Font.PLAIN, 12);

    String fontFamily;

    boolean fontBold;

    boolean fontItalic;

    float fontSize;

    float haloRadius;

    Color haloColor;

    Expression message;

    Color fontColor;

    @Override
    public void loadOptions(Map<String, Expression> options) throws Exception {
        // message
        this.message = options.get("message");
        if (message == null) {
            message = FF.literal("You forgot to set the 'message' option");
        }
        // font
        this.fontFamily = getOption(options, "font-family");
        if (options.get("font-italic") != null) {
            this.fontItalic = getOption(options, "font-italic", Boolean.class);
        }
        if (options.get("font-bold") != null) {
            this.fontBold = getOption(options, "font-bold", Boolean.class);
        }
        if (options.get("font-size") != null) {
            try {
                this.fontSize = getOption(options, "font-size", Float.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'font-size' must be a float", e);
            }
        }
        if (options.get("font-color") != null) {
            try {
                this.fontColor = MapDecorationLayout.parseColor(getOption(options, "font-color"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'font-color' must be a color in #RRGGBB[AA] format.", e);
            }
        }
        if (fontColor == null) {
            fontColor = Color.BLACK;
        }
        // halo
        if (options.get("halo-radius") != null) {
            try {
                this.haloRadius = getOption(options, "halo-radius", Float.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'halo-radius' must be a float.", e);
            }
        }
        if (options.get("halo-color") != null) {
            try {
                this.haloColor = MapDecorationLayout.parseColor(getOption(options, "halo-color"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'halo-color' must be a color in #RRGGBB[AA] format.", e);
            }
        }
        if (haloRadius > 0 && haloColor == null) {
            haloColor = Color.WHITE;
        }
    }

    Font getFont() {
        Font font = DEFAULT_FONT;
        if (fontFamily != null) {
            font = FontCache.getDefaultInstance().getFont(fontFamily);
            if (font == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Font " + fontFamily + " not found, falling back on the default");
                font = DEFAULT_FONT;
            }
        }
        if (fontSize > 0) {
            font = font.deriveFont(fontSize);
        }
        if (fontItalic) {
            font = font.deriveFont(Font.ITALIC);
        }
        if (fontBold) {
            font = font.deriveFont(Font.BOLD);
        }
        return font;
    }

    String evaluateMessage(WMSMapContent content) throws IOException, TemplateException {
        // template expansion is allowed only if the source was a literal,
        // to avoid allowing execution of templates requested from the URL (whicih coul
        // open a log4shell-like attack vector). The options loader is already taking
        // care of this, but just to be on the safe side, in case other code is building
        // decorations, the check is repeated here too.
        String expandedMessage = message.evaluate(null, String.class);
        if (message instanceof Literal) {
            return evaluateAsTemplate(content, expandedMessage);
        }
        return expandedMessage;
    }

    private String evaluateAsTemplate(WMSMapContent content, String message)
            throws IOException, TemplateException {
        final Map env = content.getRequest().getEnv();
        Template t =
                new Template(
                        "name", new StringReader(message), TemplateUtils.getSafeConfiguration());
        final BeansWrapper bw = new BeansWrapper(FM_VERSION);
        return FreeMarkerTemplateUtils.processTemplateIntoString(
                t,
                new TemplateHashModel() {

                    @Override
                    public boolean isEmpty() throws TemplateModelException {
                        return env.isEmpty();
                    }

                    @Override
                    public TemplateModel get(String key) throws TemplateModelException {
                        String value = (String) env.get(key);
                        // also allow using the other request parameters
                        if (value == null) {
                            Request request = Dispatcher.REQUEST.get();
                            if (request != null && request.getRawKvp() != null) {
                                Object obj = request.getRawKvp().get(key);
                                value = Converters.convert(obj, String.class);
                                if (obj != null && value == null) {
                                    // try also Spring converters as a fallback, can do some thing
                                    // GT converters
                                    // cannot do (e.g array -> string). Did not create a bridge to
                                    // GT converters as it might
                                    // have global consequences
                                    DefaultConversionService.getSharedInstance()
                                            .convert(obj, String.class);
                                }
                            }
                        }
                        if (value != null) {
                            return new StringModel(value, bw);
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Override
    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) throws Exception {
        Font font = getFont();
        String message = evaluateMessage(mapContent);
        GlyphVector gv = font.createGlyphVector(g2d.getFontRenderContext(), message.toCharArray());
        Shape outline = gv.getOutline();
        Rectangle2D bounds = outline.getBounds2D();
        double width = bounds.getWidth() + haloRadius * 2;
        double height = bounds.getHeight() + haloRadius * 2;
        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
            throws Exception {
        Font font = getFont();
        String message = evaluateMessage(mapContent);
        Font oldFont = g2d.getFont();
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();
        try {
            // extract the glyph vector outline (paint like the labelling system does)
            GlyphVector gv =
                    font.createGlyphVector(g2d.getFontRenderContext(), message.toCharArray());
            AffineTransform at =
                    AffineTransform.getTranslateInstance(
                            paintArea.x + haloRadius, paintArea.y + paintArea.height - haloRadius);
            Shape outline = gv.getOutline();
            outline = at.createTransformedShape(outline);

            // draw the halo if necessary
            if (haloRadius > 0) {
                g2d.setColor(haloColor);
                g2d.setStroke(
                        new BasicStroke(
                                2 * haloRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(outline);
            }

            // draw the string
            g2d.setFont(font);
            g2d.setColor(fontColor);
            g2d.fill(outline);
        } finally {
            g2d.setColor(oldColor);
            g2d.setFont(oldFont);
            g2d.setStroke(oldStroke);
        }
    }
}
