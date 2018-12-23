/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.swing.Icon;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Description;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.ExternalMark;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.LineSymbolizerImpl;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PointSymbolizerImpl;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.RasterSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizerImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.style.Fill;
import org.opengis.style.GraphicLegend;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.util.InternationalString;

/** @author Ian Turton */
public class JSONLegendGraphicBuilder extends LegendGraphicBuilder {

    /** GRAPHICS */
    public static final String GRAPHICS = "graphics";
    /** LEGEND_GRAPHIC */
    public static final String LEGEND_GRAPHIC = "LegendGraphic";
    /** ICON */
    public static final String ICON = "icon";
    /** EXTERNAL_GRAPHIC_TYPE */
    public static final String EXTERNAL_GRAPHIC_TYPE = "external-graphic-type";
    /** EXTERNAL_GRAPHIC_URL */
    public static final String EXTERNAL_GRAPHIC_URL = "external-graphic-url";
    /** MARK */
    public static final String MARK = "mark";
    /** TEXT */
    public static final String TEXT = "Text";
    /** RASTER */
    public static final String RASTER = "Raster";
    /** POINT */
    public static final String POINT = "Point";
    /** LINE */
    public static final String LINE = "Line";
    /** POLYGON */
    public static final String POLYGON = "Polygon";
    /** STROKE_DASHOFFSET */
    public static final String STROKE_DASHOFFSET = "stroke-dashoffset";
    /** STROKE_DASHARRAY */
    public static final String STROKE_DASHARRAY = "stroke-dasharray";
    /** STROKE_WIDTH */
    public static final String STROKE_WIDTH = "stroke-width";
    /** STROKE */
    public static final String STROKE = "stroke";
    /** FILL_OPACITY */
    public static final String FILL_OPACITY = "fill-opacity";
    /** FILL */
    public static final String FILL = "fill";
    /** COLORMAP */
    public static final String COLORMAP = "colormap";
    /** ENTRIES */
    public static final String ENTRIES = "entries";
    /** OPACITY */
    public static final String OPACITY = "opacity";
    /** COLOR */
    public static final String COLOR = "color";
    /** QUANTITY */
    public static final String QUANTITY = "quantity";
    /** LABEL */
    public static final String LABEL = "label";
    /** TYPE */
    public static final String TYPE = "type";
    /** GEOMETRY */
    public static final String GEOMETRY = "geometry";
    /** UOM */
    public static final String UOM = "uom";
    /** RULES */
    public static final String RULES = "rules";
    /** LAYER_NAME */
    public static final String LAYER_NAME = "layerName";
    /** SYMBOLIZERS */
    public static final String SYMBOLIZERS = "symbolizers";
    /** FILTER */
    public static final String FILTER = "filter";
    /** ABSTRACT */
    public static final String ABSTRACT = "abstract";
    /** TITLE */
    public static final String TITLE = "title";
    /** NAME */
    public static final String NAME = "name";
    /** LEGEND */
    public static final String LEGEND = "Legend";

    public static final String SIZE = "size";
    public static final String FORMAT = "format";
    private Feature feature;

    /**
     * @param request
     * @return
     */
    public JSONObject buildLegendGraphic(GetLegendGraphicRequest request) {
        setup(request);

        JSONObject response = new JSONObject();
        for (LegendRequest legend : layers) {
            String layerName = legend.getLayerName().getLocalPart();
            FeatureType layer = legend.getFeatureType();

            // style and rule to use for the current layer
            Style gt2Style = legend.getStyle();
            if (gt2Style == null) {
                throw new NullPointerException("request.getStyle()");
            }

            // get rule corresponding to the layer index
            // normalize to null for NO RULE
            String ruleName = legend.getRule(); // was null

            boolean strict = request.isStrict();

            gt2Style = resizeForDPI(request, gt2Style);

            final FeatureTypeStyle[] ftStyles =
                    gt2Style.featureTypeStyles().toArray(new FeatureTypeStyle[0]);
            final double scaleDenominator = request.getScale();

            Rule[] applicableRules;
            if (ruleName != null) {
                Rule rule = LegendUtils.getRule(ftStyles, ruleName);
                if (rule == null) {
                    throw new ServiceException(
                            "Specified style does not contains a rule named " + ruleName);
                }
                applicableRules = new Rule[] {rule};
            } else {
                applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
            }
            ArrayList<JSONObject> jRules = new ArrayList<>();
            for (Rule rule : applicableRules) {
                JSONObject jRule = new JSONObject();
                String name = rule.getName();
                if (name != null && !name.isEmpty()) {
                    jRule.element(NAME, name);
                }
                InternationalString title = rule.getDescription().getTitle();
                if (title != null) {
                    jRule.element(TITLE, title.toString());
                }
                InternationalString abs = rule.getDescription().getAbstract();
                if (abs != null) {
                    jRule.element(ABSTRACT, abs.toString());
                }
                Filter filter = rule.getFilter();
                if (filter != null) {
                    jRule.element(FILTER, "[" + CQL.toCQL(filter) + "]");
                }
                JSONArray jSymbolizers = new JSONArray();
                if (layer != null) {
                    feature = getSampleFeatureForRule(layer, null, rule);
                }
                for (Symbolizer symbolizer : rule.symbolizers()) {
                    JSONObject jSymb = new JSONObject();
                    JSONObject symb = processSymbolizer(symbolizer);
                    jSymb.element(symbolizerNames.get(symbolizer.getClass()), symb);
                    jSymbolizers.add(jSymb);
                }
                GraphicLegend l = rule.getLegend();
                if (l != null) {
                    for (GraphicalSymbol g : l.graphicalSymbols()) {
                        jRule.element(LEGEND_GRAPHIC, processGraphicalSymbol(g));
                    }
                }
                if (!jSymbolizers.isEmpty()) {
                    jRule.element(SYMBOLIZERS, jSymbolizers);
                }
                jRules.add(jRule);
            }
            if (!jRules.isEmpty()) {
                JSONArray legends = new JSONArray();
                if (response.containsKey(LEGEND)) {
                    legends = response.getJSONArray(LEGEND);
                }
                JSONObject jLayer = new JSONObject();

                jLayer.element(LAYER_NAME, layerName);
                jLayer = getLayerTitle(jLayer, legend);
                jLayer.element(RULES, jRules);
                legends.add(jLayer);
                response.element(LEGEND, legends);
            }
        }
        /* }*/

        return response;
    }

    /**
     * @param symbolizer
     * @return
     */
    private JSONObject processSymbolizer(Symbolizer symbolizer) {
        JSONObject ret = new JSONObject();
        String name = symbolizer.getName();
        if (name != null && !name.isEmpty()) {
            ret.element(NAME, name);
        }
        Unit<Length> uom = symbolizer.getUnitOfMeasure();
        if (uom != null) {
            ret.element(UOM, uom.getSymbol());
        }
        Description desc = symbolizer.getDescription();
        if (desc != null) {
            InternationalString title = desc.getTitle();
            if (title != null) {
                ret.element(TITLE, title.toString());
            }
            InternationalString abs = desc.getAbstract();
            if (abs != null) {
                ret.element(ABSTRACT, abs.toString());
            }
        }
        Expression geometry = symbolizer.getGeometry();
        if (geometry != null) {
            ret.element(GEOMETRY, geometry.toString());
        }
        if (symbolizer instanceof PointSymbolizer) {
            ret = processPointSymbolizer(ret, (PointSymbolizer) symbolizer);
        } else if (symbolizer instanceof LineSymbolizer) {
            ret = processLineSymbolizer(ret, (LineSymbolizer) symbolizer);
        } else if (symbolizer instanceof PolygonSymbolizer) {
            ret = processPolygonSymbolizer(ret, (PolygonSymbolizer) symbolizer);
        } else if (symbolizer instanceof RasterSymbolizer) {
            ret = processRasterSymbolizer(ret, (RasterSymbolizer) symbolizer);
        } else if (symbolizer instanceof TextSymbolizer) {
            ret = processTextSymbolizer(ret, (TextSymbolizer) symbolizer);
        }
        return ret;
    }

    /**
     * @param ret
     * @param symbolizer
     * @return
     */
    private JSONObject processTextSymbolizer(JSONObject ret, TextSymbolizer symbolizer) {
        ret.element(TYPE, "text-TODO");
        return ret;
    }

    /**
     * @param ret
     * @param symbolizer
     * @return
     */
    private JSONObject processRasterSymbolizer(JSONObject ret, RasterSymbolizer symbolizer) {
        ret = processColorMap(symbolizer.getColorMap(), ret);
        Expression op = symbolizer.getOpacity();
        if (op != null) {
            ret.element(OPACITY, op.evaluate(null));
        }
        return ret;
    }

    /**
     * @param colorMap
     * @param ret
     * @return
     */
    private JSONObject processColorMap(ColorMap colorMap, JSONObject ret) {
        boolean first = true;
        JSONArray entries = new JSONArray();
        for (ColorMapEntry entry : colorMap.getColorMapEntries()) {
            JSONObject ent = new JSONObject();
            final Double qty = entry.getQuantity().evaluate(null, Double.class);
            if (colorMap.getType() == ColorMap.TYPE_INTERVALS
                    && first
                    && qty < 0
                    && Double.isInfinite(qty)) {
                continue;
            }
            first = false;
            ent.element(LABEL, entry.getLabel());
            ent.element(QUANTITY, qty);
            Color color = entry.getColor().evaluate(null, Color.class);
            String hex =
                    String.format(
                            "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            ent.element(COLOR, hex);
            Expression opac = entry.getOpacity();
            if (opac != null) {
                ent.element(OPACITY, opac.evaluate(null, Double.class));
            }
            entries.add(ent);
        }
        JSONObject cm = new JSONObject();
        if (entries.size() > 0) {
            cm.element(ENTRIES, entries);
            ret.element(COLORMAP, cm);
        }
        return ret;
    }

    /**
     * @param ret
     * @param symbolizer
     * @return
     */
    private JSONObject processPolygonSymbolizer(JSONObject ret, PolygonSymbolizer symbolizer) {
        ret = processStroke(ret, (Stroke) symbolizer.getStroke());
        ret = processFill(ret, symbolizer.getFill());
        return ret;
    }

    /**
     * @param ret
     * @param symbolizer
     * @return
     */
    private JSONObject processLineSymbolizer(JSONObject ret, LineSymbolizer symbolizer) {
        ret = processStroke(ret, symbolizer.getStroke());
        return ret;
    }

    /**
     * @param ret
     * @param symbolizer
     * @return
     */
    private JSONObject processPointSymbolizer(JSONObject ret, PointSymbolizer symbolizer) {
        ret = processGraphic(ret, symbolizer.getGraphic());
        return ret;
    }

    /**
     * @param ret
     * @param graphic
     * @return
     */
    private JSONObject processGraphic(JSONObject ret, Graphic graphic) {
        JSONArray jGraphics = new JSONArray();
        List<GraphicalSymbol> gSymbols = graphic.graphicalSymbols();
        for (GraphicalSymbol g : gSymbols) {
            JSONObject jGraphic = processGraphicalSymbol(g);

            jGraphics.add(jGraphic);
        }
        ret.element(SIZE, graphic.getSize().evaluate(feature));
        ret.element(GRAPHICS, jGraphics);
        return ret;
    }

    /**
     * @param g
     * @return
     */
    private JSONObject processGraphicalSymbol(GraphicalSymbol g) {
        JSONObject jGraphic = new JSONObject();
        jGraphic.element("url", "IconService");
        if (g instanceof Mark) {
            Mark m = ((Mark) g);
            Expression wkn = m.getWellKnownName();
            if (wkn != null) {
                jGraphic.element(MARK, wkn.evaluate(feature));
            }
            jGraphic = processFill(jGraphic, m.getFill());
            jGraphic = processStroke(jGraphic, m.getStroke());
            ExternalMark em = m.getExternalMark();
            if (em != null) {
                OnLineResource or = em.getOnlineResource();
                if (or != null) {
                    try {
                        jGraphic.element(EXTERNAL_GRAPHIC_URL, or.getLinkage().toURL().toString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                Icon icon = em.getInlineContent();
                if (icon != null) {
                    jGraphic.element(ICON, icon.toString());
                }
                jGraphic.element(FORMAT, em.getFormat());
            }
        } else if (g instanceof ExternalGraphic) {
            ExternalGraphic eg = (ExternalGraphic) g;
            try {
                jGraphic.element(
                        EXTERNAL_GRAPHIC_URL,
                        eg.getOnlineResource().getLinkage().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            jGraphic.element(EXTERNAL_GRAPHIC_TYPE, eg.getFormat());
        }
        return jGraphic;
    }

    private JSONObject getLayerTitle(JSONObject in, LegendRequest legend) {
        String title = legend.getTitle();
        return in.element(TITLE, title);
    }
    /**
     * @param ret
     * @param fill
     * @return
     */
    private JSONObject processFill(JSONObject ret, Fill fill) {
        if (fill == null) {
            return ret;
        }
        ret.element(FILL, fill.getColor().evaluate(feature));
        ret.element(FILL_OPACITY, fill.getOpacity().evaluate(feature));

        return ret;
    }
    /**
     * @param ret
     * @param stroke
     * @return
     */
    private JSONObject processStroke(JSONObject ret, Stroke stroke) {
        if (stroke == null) {
            return ret;
        }
        ret.element(STROKE, stroke.getColor().evaluate(feature));
        ret.element(STROKE_WIDTH, stroke.getWidth().evaluate(feature));
        List<Expression> dashArray = stroke.dashArray();
        if (dashArray != null && !dashArray.isEmpty()) {
            JSONArray dArray = new JSONArray();
            for (Expression e : dashArray) {
                dArray.add(e.evaluate(feature));
            }
            ret.element(STROKE_DASHARRAY, dArray);
            Expression dashOffset = stroke.getDashOffset();
            if (dashOffset != null) {
                ret.element(STROKE_DASHOFFSET, dashOffset.evaluate(feature));
            }
        }
        return ret;
    }

    static Map<Class, String> symbolizerNames = new HashMap<>();

    static {
        symbolizerNames.put(PolygonSymbolizer.class, POLYGON);
        symbolizerNames.put(LineSymbolizer.class, LINE);
        symbolizerNames.put(PointSymbolizer.class, POINT);
        symbolizerNames.put(RasterSymbolizer.class, RASTER);
        symbolizerNames.put(TextSymbolizer.class, TEXT);
        symbolizerNames.put(PolygonSymbolizerImpl.class, POLYGON);
        symbolizerNames.put(LineSymbolizerImpl.class, LINE);
        symbolizerNames.put(PointSymbolizerImpl.class, POINT);
        symbolizerNames.put(RasterSymbolizerImpl.class, RASTER);
        symbolizerNames.put(TextSymbolizerImpl.class, TEXT);
    }
}
