/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.LabelStyle;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Style;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.icons.IconProperties;
import org.geoserver.wms.icons.IconPropertyExtractor;
import org.geoserver.wms.icons.IconPropertyInjector;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Encodes the SLD styles into KML corresponding styles and adds them to the Placemark
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkStyleDecoratorFactory implements KmlDecoratorFactory {

    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator makes sense only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        if (Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkStyleDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkStyleDecorator implements KmlDecorator {

        static final Logger LOGGER = Logging.getLogger(PlacemarkStyleDecorator.class);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Placemark pm = (Placemark) feature;
            // while it's possible to have more than one style object, GE will only paint
            // the first one
            Style style = pm.createAndAddStyle();
            List<Symbolizer> symbolizers = context.getCurrentSymbolizers();
            SimpleFeature sf = context.getCurrentFeature();
            if (symbolizers.size() > 0 && sf.getDefaultGeometry() != null) {
                // sort by point, text, line and polygon
                Map<Class, List<Symbolizer>> classified = classifySymbolizers(symbolizers);

                // if no point symbolizers, create a default one
                List<Symbolizer> points = classified.get(PointSymbolizer.class);
                if (points.size() == 0) {
                    if (context.isDescriptionEnabled()) {
                        setDefaultIconStyle(style, sf, context);
                    }
                } else {
                    org.geotools.styling.Style wholeStyle = context.getCurrentLayer().getStyle();
                    IconProperties properties =
                            IconPropertyExtractor.extractProperties(wholeStyle, sf);
                    setIconStyle(style, wholeStyle, properties, context);
                }

                // handle label styles
                List<Symbolizer> texts = classified.get(TextSymbolizer.class);
                if (texts.size() == 0) {
                    if (context.isDescriptionEnabled()) {
                        setDefaultLabelStyle(style);
                    }
                } else {
                    // the XML schema allows only one text style, follow painter's model
                    // and set the last one
                    TextSymbolizer lastTextSymbolizer =
                            (TextSymbolizer) texts.get(texts.size() - 1);
                    setLabelStyle(style, sf, lastTextSymbolizer);
                }

                // handle line styles
                List<Symbolizer> lines = classified.get(LineSymbolizer.class);
                // the XML schema allows only one line style, follow painter's model
                // and set the last one
                if (lines.size() > 0) {
                    LineSymbolizer lastLineSymbolizer =
                            (LineSymbolizer) lines.get(lines.size() - 1);
                    setLineStyle(style, sf, lastLineSymbolizer.getStroke());
                }

                // handle polygon styles
                boolean forceOutiline = lines.size() == 0;
                List<Symbolizer> polygons = classified.get(PolygonSymbolizer.class);
                if (polygons.size() > 0) {
                    // the XML schema allows only one polygon style, follow painter's model
                    // and set the last one
                    PolygonSymbolizer lastPolygonSymbolizer =
                            (PolygonSymbolizer) polygons.get(polygons.size() - 1);
                    setPolygonStyle(style, sf, lastPolygonSymbolizer, forceOutiline);
                }
            }

            return feature;
        }

        private Map<Class, List<Symbolizer>> classifySymbolizers(List<Symbolizer> symbolizers) {
            Map<Class, List<Symbolizer>> result = new HashMap<Class, List<Symbolizer>>();
            result.put(PointSymbolizer.class, new ArrayList<Symbolizer>());
            result.put(LineSymbolizer.class, new ArrayList<Symbolizer>());
            result.put(PolygonSymbolizer.class, new ArrayList<Symbolizer>());
            result.put(TextSymbolizer.class, new ArrayList<Symbolizer>());

            for (Symbolizer s : symbolizers) {
                if (s instanceof PointSymbolizer) {
                    result.get(PointSymbolizer.class).add(s);
                } else if (s instanceof LineSymbolizer) {
                    result.get(LineSymbolizer.class).add(s);
                } else if (s instanceof PolygonSymbolizer) {
                    result.get(PolygonSymbolizer.class).add(s);
                } else if (s instanceof TextSymbolizer) {
                    result.get(TextSymbolizer.class).add(s);
                } else {
                    throw new IllegalArgumentException("Unrecognized symbolizer type: " + s);
                }
            }

            return result;
        }

        protected void setDefaultIconStyle(
                Style style, SimpleFeature feature, KmlEncodingContext context) {
            // figure out if line or polygon
            boolean line =
                    feature.getDefaultGeometry() != null
                            && (feature.getDefaultGeometry() instanceof LineString
                                    || feature.getDefaultGeometry() instanceof MultiLineString);
            boolean poly =
                    feature.getDefaultGeometry() != null
                            && (feature.getDefaultGeometry() instanceof Polygon
                                    || feature.getDefaultGeometry() instanceof MultiPolygon);

            // Final pre-flight check
            if (!line && !poly) {
                LOGGER.log(
                        Level.FINER,
                        "Unexpectedly entered encodeDefaultIconStyle() "
                                + "with something that does not have a multipoint geometry.");
                return;
            }

            IconStyle is = style.createAndSetIconStyle();
            // make transparent if they ask for attributes, since we'll have a label
            if (context.isDescriptionEnabled()) {
                is.setColor("00ffffff");
            }
            // if line or polygon scale the label
            if (line || poly) {
                is.setScale(0.4);
            }
            String imageURL =
                    "http://icons.opengeo.org/markers/icon-"
                            + (poly ? "poly.1" : "line.1")
                            + ".png";
            Icon icon = is.createAndSetIcon();
            icon.setHref(imageURL);
            icon.setViewBoundScale(1);
        }

        /** Encodes a KML IconStyle from a point style and symbolizer. */
        protected void setIconStyle(
                Style style,
                org.geotools.styling.Style sld,
                IconProperties properties,
                KmlEncodingContext context) {
            if (context.isLiveIcons() || properties.isExternal()) {
                setLiveIconStyle(style, sld, properties, context);
            } else {
                setInlineIconStyle(style, sld, properties, context);
            }
        }

        protected void setInlineIconStyle(
                Style style,
                org.geotools.styling.Style sld,
                IconProperties properties,
                KmlEncodingContext context) {
            final String name = properties.getIconName(sld);

            Map<String, org.geotools.styling.Style> iconStyles = context.getIconStyles();
            if (!iconStyles.containsKey(name)) {
                final org.geotools.styling.Style injectedStyle =
                        IconPropertyInjector.injectProperties(sld, properties.getProperties());

                iconStyles.put(name, injectedStyle);
            }
            final Double scale = properties.getScale();
            final String path = "icons/" + name + ".png";

            IconStyle is = style.createAndSetIconStyle();
            if (properties.getHeading() != null) {
                is.setHeading(0.0);
            }
            if (scale != null) {
                is.setScale(scale);
            }

            Icon icon = is.createAndSetIcon();
            icon.setHref(path);
        }

        protected void setLiveIconStyle(
                Style style,
                org.geotools.styling.Style sld,
                IconProperties properties,
                KmlEncodingContext context) {
            final Double opacity = properties.getOpacity();
            final Double scale = properties.getScale();
            final Double heading = properties.getHeading();

            IconStyle is = style.createAndSetIconStyle();

            if (opacity != null) {
                is.setColor(colorToHex(Color.WHITE, opacity));
            }

            if (scale != null) {
                is.setScale(scale);
            }

            if (heading != null) {
                is.setHeading(heading);
            }

            // Get the name of the workspace

            WorkspaceInfo ws =
                    context.getWms().getCatalog().getStyleByName(sld.getName()).getWorkspace();
            String wsName = null;
            if (ws != null) wsName = ws.getName();

            Icon icon = is.createAndSetIcon();
            icon.setHref(
                    properties.href(
                            context.getMapContent().getRequest().getBaseUrl(),
                            wsName,
                            sld.getName()));
        }

        /** Encodes a transparent KML LabelStyle */
        protected void setDefaultLabelStyle(Style style) {
            LabelStyle ls = style.createAndSetLabelStyle();
            ls.setColor("00ffffff");
        }

        protected void setLabelStyle(
                Style style, SimpleFeature feature, TextSymbolizer symbolizer) {
            LabelStyle ls = style.createAndSetLabelStyle();
            double scale = 1.0;
            Font font = symbolizer.getFont();
            if (font != null && font.getSize() != null) {
                // we make the scale proportional to the normal font size
                double size = evaluate(font.getSize(), feature, Font.DEFAULT_FONTSIZE);
                scale = Math.round(size / Font.DEFAULT_FONTSIZE * 100) / 100.0;
            }
            ls.setScale(scale);

            Fill fill = symbolizer.getFill();
            if (fill != null) {
                Double opacity = evaluate(fill.getOpacity(), feature, 1.0);
                Color color = evaluate(fill.getColor(), feature, Color.WHITE);
                ls.setColor(colorToHex(color, opacity));
            } else {
                ls.setColor("ffffffff");
            }
        }

        /** Encodes a KML IconStyle + PolyStyle from a polygon style and symbolizer. */
        protected void setPolygonStyle(
                Style style,
                SimpleFeature feature,
                PolygonSymbolizer symbolizer,
                boolean forceOutline) {
            // if stroke specified add line style as well (it has to be before the fill, otherwise
            // we'll get a white filling...)
            if (symbolizer.getStroke() != null) {
                setLineStyle(style, feature, symbolizer.getStroke());
            }

            // fill
            PolyStyle ps = style.createAndSetPolyStyle();
            Fill fill = symbolizer.getFill();
            if (fill != null) {
                Double opacity = evaluate(fill.getOpacity(), feature, 1.0);
                Color color = evaluate(fill.getColor(), feature, new Color(0xAAAAAA));
                ps.setColor(colorToHex(color, opacity));
            } else {
                // make it transparent
                ps.setColor("00aaaaaa");
            }

            // outline
            if (symbolizer.getStroke() != null || forceOutline) {
                ps.setOutline(true);
            }
        }

        /**
         * Safe expression execution with default fallback.
         *
         * @return evaluated value or defaultValue if unavailable
         */
        private Double evaluate(Expression expression, SimpleFeature feature, double defaultValue) {
            if (expression == null) {
                return defaultValue;
            }
            Double value = expression.evaluate(feature, Double.class);
            if (value == null || Double.isNaN(value)) {
                return defaultValue;
            }
            return value;
        }
        /**
         * Safe expression execution with default fallback.
         *
         * @return evaluated value or defaultColor if unavailable
         */
        private Color evaluate(Expression expression, SimpleFeature feature, Color defaultColor) {
            if (expression == null) {
                return defaultColor;
            }
            Color color = expression.evaluate(feature, Color.class);
            if (color == null) {
                return defaultColor;
            }
            return color;
        }

        /** Encodes a KML IconStyle + LineStyle from a polygon style and symbolizer. */
        protected void setLineStyle(Style style, SimpleFeature feature, Stroke stroke) {
            LineStyle ls = style.createAndSetLineStyle();

            if (stroke != null) {
                // opacity
                Double opacity = evaluate(stroke.getOpacity(), feature, 1.0);

                Color color = null;
                Expression sc = stroke.getColor();
                if (sc != null) {
                    color = (Color) sc.evaluate(feature, Color.class);
                }
                if (color == null) {
                    // Different from BLACK provided by Stroke.DEFAULT.getColor()
                    color = Color.DARK_GRAY;
                }
                ls.setColor(colorToHex(color, opacity));

                // width
                Double width =
                        evaluate(stroke.getWidth(), feature, 1d); // from Stroke.DEFAULT.getWidth()
                ls.setWidth(width);
            } else {
                // default
                ls.setColor("ffaaaaaa");
                ls.setWidth(1);
            }
        }

        /**
         * Does value substitution on a URL with embedded CQL expressions
         *
         * @param strLocation the URL as a string, possibly with expressions
         * @param feature the feature providing the context in which the expressions are evaluated
         * @return a string containing the final URL
         */
        protected String evaluateDynamicSymbolizer(String strLocation, SimpleFeature feature) {
            if (strLocation == null) return null;

            // parse the eventual ${cqlExpression} embedded in the URL
            Expression location;
            try {
                location = ExpressionExtractor.extractCqlExpressions(strLocation);
            } catch (IllegalArgumentException e) {
                // in the unlikely event that a URL is using one of the chars reserved for
                // ${cqlExpression}
                // let's try and use the location as a literal
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(
                            Level.SEVERE,
                            "Could not parse cql expressions out of " + strLocation,
                            e);
                location = ff.literal(strLocation);
            }

            return location.evaluate(feature, String.class);
        }

        /**
         * Utility method to convert a Color and opacity (0,1.0) into a KML color ref.
         *
         * @param c The color to convert.
         * @param opacity Opacity / alpha, double from 0 to 1.0.
         * @return A String of the form "AABBGGRR".
         */
        String colorToHex(Color c, Double opacity) {
            if (opacity == null || Double.isNaN(opacity)) {
                opacity = 1.0;
            }
            return new StringBuffer()
                    .append(intToHex((int) (255 * opacity)))
                    .append(intToHex(c.getBlue()))
                    .append(intToHex(c.getGreen()))
                    .append(intToHex(c.getRed()))
                    .toString();
        }

        /**
         * Utility method to convert an int into hex, padded to two characters. handy for generating
         * colour strings.
         *
         * @param i Int to convert
         * @return String a two character hex representation of i
         */
        String intToHex(int i) {
            String prelim = Integer.toHexString(i);

            if (prelim.length() < 2) {
                prelim = "0" + prelim;
            }

            return prelim;
        }
    }
}
