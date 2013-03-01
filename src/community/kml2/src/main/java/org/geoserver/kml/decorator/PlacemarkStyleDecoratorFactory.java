package org.geoserver.kml.decorator;

import java.awt.Color;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.kml.KMLUtils;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMapRequest;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Stroke;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicalSymbol;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.LabelStyle;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Style;

public class PlacemarkStyleDecoratorFactory implements KmlDecoratorFactory {

    public KmlDecorator getDecorator(Class<? extends Feature> featureClass, KmlEncodingContext context) {
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
            List<Symbolizer> symbolizers = context.getCurrentSymbolizers();
            SimpleFeature sf = context.getCurrentFeature();
            if (symbolizers.size() > 0) {
                // sort by point, text, line and polygon
                Map<Class, List<Symbolizer>> classified = classifySymbolizers(symbolizers);

                // if no point symbolizers, create a default one
                List<Symbolizer> points = classified.get(PointSymbolizer.class);
                if (points.size() == 0) {
                    addDefaultIconStyle(pm, sf, context);
                } else {
                    for (Symbolizer symbolizer : points) {
                        addIconStyle(pm, (PointSymbolizer) symbolizer, sf, context);
                    }
                }

                // handle label styles
                List<Symbolizer> texts = classified.get(TextSymbolizer.class);
                if (texts.size() == 0) {
                    addDefaultLabelStyle(pm);
                } else {
                    for (Symbolizer symbolizer : texts) {
                        addLabelStyle(pm, sf, (TextSymbolizer) symbolizer);
                    }
                }

                // handle line styles
                List<Symbolizer> lines = classified.get(LineSymbolizer.class);
                for (Symbolizer symbolizer : lines) {
                    addLineStyle(pm, sf, ((LineSymbolizer) symbolizer).getStroke());
                }

                // handle polygon styles
                boolean forceOutiline = lines.size() == 0;
                List<Symbolizer> polygons = classified.get(PolygonSymbolizer.class);
                for (Symbolizer symbolizer : polygons) {
                    addPolygonStyle(pm, sf, (PolygonSymbolizer) symbolizer, forceOutiline);
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

        protected void addDefaultIconStyle(Placemark pm, SimpleFeature feature,
                KmlEncodingContext context) {
            // figure out if line or polygon
            boolean line = feature.getDefaultGeometry() != null
                    && (feature.getDefaultGeometry() instanceof LineString || feature
                            .getDefaultGeometry() instanceof MultiLineString);
            boolean poly = feature.getDefaultGeometry() != null
                    && (feature.getDefaultGeometry() instanceof Polygon || feature
                            .getDefaultGeometry() instanceof MultiPolygon);

            // Final pre-flight check
            if (!line && !poly) {
                LOGGER.log(Level.FINER, "Unexpectedly entered encodeDefaultIconStyle() "
                        + "with something that does not have a multipoint geometry.");
                return;
            }

            Style style = pm.createAndAddStyle();
            IconStyle is = style.createAndSetIconStyle();
            // make transparent if they ask for attributes, since we'll have a label
            if (context.isDescriptionEnabled()) {
                is.setColor("00ffffff");
            }
            // if line or polygon scale the label
            if (line || poly) {
                is.setScale(0.4);
            }
            String imageURL = "http://icons.opengeo.org/markers/icon-"
                    + (poly ? "poly.1" : "line.1") + ".png";
            is.createAndSetIcon().setHref(imageURL);
        }

        /**
         * Encodes a transparent KML LabelStyle
         */
        protected void addDefaultLabelStyle(Placemark pm) {
            Style style = pm.createAndAddStyle();
            LabelStyle ls = style.createAndSetLabelStyle();
            ls.setColor("00ffffff");
        }

        protected void addLabelStyle(Placemark pm, SimpleFeature feature, TextSymbolizer symbolizer) {
            Style style = pm.createAndAddStyle();
            LabelStyle ls = style.createAndSetLabelStyle();

            Fill fill = symbolizer.getFill();
            if (fill != null) {
                Double opacity = fill.getOpacity().evaluate(feature, Double.class);
                if (opacity == null || Double.isNaN(opacity)) {
                    opacity = 1.0;
                }
                Color color = fill.getColor().evaluate(feature, Color.class);
                ls.setColor(KMLUtils.colorToHex(color, opacity));
            } else {
                ls.setColor("ffffffff");
            }
        }

        /**
         * Encodes a KML IconStyle + PolyStyle from a polygon style and symbolizer.
         */
        protected void addPolygonStyle(Placemark pm, SimpleFeature feature,
                PolygonSymbolizer symbolizer, boolean forceOutline) {
            // if stroke specified add line style as well (it has to be before the fill, otherwise
            // we'll get a white filling...)
            if (symbolizer.getStroke() != null) {
                addLineStyle(pm, feature, symbolizer.getStroke());
            }

            // fill
            Style style = pm.createAndAddStyle();
            PolyStyle ps = style.createAndSetPolyStyle();
            Fill fill = symbolizer.getFill();
            if (fill != null) {
                // get opacity
                Double opacity = fill.getOpacity().evaluate(feature, Double.class);
                if (opacity == null || Double.isNaN(opacity)) {
                    opacity = 1.0;
                }

                Color color = (Color) fill.getColor().evaluate(feature, Color.class);
                ps.setColor(KMLUtils.colorToHex(color, opacity));
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
         * Encodes a KML IconStyle + LineStyle from a polygon style and symbolizer.
         */
        protected void addLineStyle(Placemark pm, SimpleFeature feature, Stroke stroke) {
            Style style = pm.createAndAddStyle();
            LineStyle ls = style.createAndSetLineStyle();

            if (stroke != null) {
                // opacity
                Double opacity = stroke.getOpacity().evaluate(feature, Double.class);
                if (opacity == null || Double.isNaN(opacity)) {
                    opacity = 1.0;
                }

                Color color = null;
                Expression sc = stroke.getColor();
                if (sc != null) {
                    color = (Color) sc.evaluate(feature, Color.class);
                }
                if (color == null) {
                    color = Color.DARK_GRAY;
                }
                ls.setColor(KMLUtils.colorToHex(color, opacity));

                // width
                Double width = null;
                Expression sw = stroke.getWidth();
                if (sw != null) {
                    width = sw.evaluate(feature, Double.class);
                }
                if (width == null) {
                    width = 1d;
                }
                ls.setWidth(width);
            } else {
                // default
                ls.setColor("ffaaaaaa");
                ls.setWidth(1);
            }
        }

        /**
         * Encodes a KML IconStyle from a point style and symbolizer.
         */
        private void addIconStyle(Placemark pm, PointSymbolizer symbolizer, SimpleFeature sf,
                KmlEncodingContext context) {
            Style style = pm.createAndAddStyle();
            IconStyle is = style.createAndSetIconStyle();
            is.setColorMode(ColorMode.NORMAL);

            // default icon
            String iconHref = null;

            // try to get a color if any
            Mark mark = SLD.mark(symbolizer);
            if (mark != null && mark.getFill() != null) {
                Fill fill = mark.getFill();
                Double opacity = fill.getOpacity().evaluate(sf, Double.class);
                if (opacity == null || Double.isNaN(opacity)) {
                    // default to full opacity
                    opacity = 1.0;
                }

                if (fill != null) {
                    final Color color = (Color) fill.getColor().evaluate(sf, Color.class);
                    is.setColor(KMLUtils.colorToHex(color, opacity));
                }
            }

            // if the point symbolizer uses an external graphic use it
            ExternalGraphic graphic = getExternalGraphic(symbolizer);
            if (graphic != null) {
                try {
                    // Before doing anything else (that might mess with "$", "{",
                    // or "}" characters), we evaluate the string as an expression.
                    URL graphicLocation = graphic.getLocation();
                    iconHref = graphicLocation.toString();
                    iconHref = evaluateDynamicSymbolizer(iconHref, sf);
                    graphicLocation = new URL(iconHref);
                    String graphicProtocol = new URL(iconHref).getProtocol();

                    // special handling of local disk references
                    if ("file".equals(graphicProtocol)) {
                        // it is a local file, reference locally from "styles" directory
                        File file = DataUtilities.urlToFile(graphicLocation);
                        File styles = null;
                        File graphicFile = null;
                        if (file.isAbsolute()) {
                            GeoServerDataDirectory dataDir = (GeoServerDataDirectory) GeoServerExtensions
                                    .bean("dataDirectory");
                            // we grab the canonical path to make sure we can compare them, no
                            // relative parts in them and so on
                            styles = dataDir.findOrCreateStyleDir().getCanonicalFile();
                            graphicFile = file.getCanonicalFile();
                            file = graphicFile;
                            if (file.getAbsolutePath().startsWith(styles.getAbsolutePath())) {
                                // ok, part of the styles directory, extract only the relative path
                                file = new File(file.getAbsolutePath().substring(
                                        styles.getAbsolutePath().length() + 1));
                            } else {
                                // we wont' transform this, other dirs are not published
                                file = null;
                            }
                        }

                        // rebuild the icon href accordingly
                        if (file != null && styles != null) {
                            iconHref = ResponseUtils.buildURL(context.getRequest().getBaseUrl(),
                                    "styles/" + styles.toURI().relativize(graphicFile.toURI()),
                                    null, URLType.RESOURCE);
                        } else {
                            // we don't know how to handle this then...
                            iconHref = null;
                        }
                    } else if (!("http".equals(graphicProtocol) || "https".equals(graphicProtocol))) {
                        // TODO: should we check for http:// and use it
                        // directly?
                        // other protocols?
                        iconHref = null;
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing external graphic:" + graphic, e);
                }
            }

            if (iconHref == null) {
                iconHref = "http://maps.google.com/mapfiles/kml/pal4/icon25.png";
            }
        }

        private ExternalGraphic getExternalGraphic(PointSymbolizer symbolizer) {
            for (GraphicalSymbol s : symbolizer.getGraphic().graphicalSymbols()) {
                if (s instanceof ExternalGraphic) {
                    return (ExternalGraphic) s;
                }
            }

            return null;
        }

        /**
         * Does value substitution on a URL with embedded CQL expressions
         * 
         * @param strLocation the URL as a string, possibly with expressions
         * @param feature the feature providing the context in which the expressions are evaluated
         * @return a string containing the final URL
         */
        protected String evaluateDynamicSymbolizer(String strLocation, SimpleFeature feature) {
            if (strLocation == null)
                return null;

            // parse the eventual ${cqlExpression} embedded in the URL
            Expression location;
            try {
                location = ExpressionExtractor.extractCqlExpressions(strLocation);
            } catch (IllegalArgumentException e) {
                // in the unlikely event that a URL is using one of the chars reserved for
                // ${cqlExpression}
                // let's try and use the location as a literal
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Could not parse cql expressions out of "
                            + strLocation, e);
                location = ff.literal(strLocation);
            }

            return location.evaluate(feature, String.class);
        }

    }

}
