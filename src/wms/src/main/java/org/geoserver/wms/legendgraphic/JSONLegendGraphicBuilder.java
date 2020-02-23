/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.swing.Icon;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.CascadedLegendRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.icons.IconProperties;
import org.geoserver.wms.icons.IconPropertyExtractor;
import org.geoserver.wms.icons.MiniRule;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Description;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.ExternalMark;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.LabelPlacement;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.LineSymbolizerImpl;
import org.geotools.styling.Mark;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PointSymbolizerImpl;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.RasterSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizer2;
import org.geotools.styling.TextSymbolizerImpl;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.style.ContrastMethod;
import org.opengis.style.GraphicalSymbol;
import org.opengis.util.InternationalString;

/** @author Ian Turton */
public class JSONLegendGraphicBuilder extends LegendGraphicBuilder {

    /** ANCHOR_POINT */
    public static final String ANCHOR_POINT = "anchor-point";
    /** ANCHOR_Y */
    public static final String ANCHOR_Y = "anchor-y";
    /** ANCHOR_X */
    public static final String ANCHOR_X = "anchor-x";
    /** DISPLACEMENT */
    public static final String DISPLACEMENT = "displacement";
    /** DISPLACEMENT_Y */
    public static final String DISPLACEMENT_Y = "displacement-y";
    /** DISPLACEMENT_X */
    public static final String DISPLACEMENT_X = "displacement-x";
    /** ELSE_FILTER */
    public static final String ELSE_FILTER = "ElseFilter";
    /** VENDOR_OPTIONS */
    public static final String VENDOR_OPTIONS = "vendor-options";
    /** COLORMAP_TYPE */
    public static final String COLORMAP_TYPE = "type";
    /** CONTRAST_ENHANCEMENT */
    public static final String CONTRAST_ENHANCEMENT = "contrast-enhancement";
    /** HISTOGRAM */
    public static final String HISTOGRAM = "histogram";
    /** EXPONENTIAL */
    public static final String EXPONENTIAL = "exponential";
    /** LOGARITHMIC */
    public static final String LOGARITHMIC = "logarithmic";
    /** NORMALIZE */
    public static final String NORMALIZE = "normalize";
    /** GRAY */
    public static final String GRAY = "gray";
    /** BLUE */
    public static final String BLUE = "blue";
    /** GREEN */
    public static final String GREEN = "green";
    /** RED */
    public static final String RED = "red";
    /** GAMMA_VALUE */
    public static final String GAMMA_VALUE = "gamma-value";
    /** STROKE_LINEJOIN */
    public static final String STROKE_LINEJOIN = "stroke-linejoin";
    /** ABSTRACT */
    public static final String ABSTRACT = "abstract";
    /** COLOR */
    public static final String COLOR = "color";
    /** COLORMAP */
    public static final String COLORMAP = "colormap";
    /** ENTRIES */
    public static final String ENTRIES = "entries";
    /** EXTERNAL_GRAPHIC_TYPE */
    public static final String EXTERNAL_GRAPHIC_TYPE = "external-graphic-type";
    /** EXTERNAL_GRAPHIC_URL */
    public static final String EXTERNAL_GRAPHIC_URL = "external-graphic-url";
    /** FILL */
    public static final String FILL = "fill";
    /** FILL_OPACITY */
    public static final String FILL_OPACITY = "fill-opacity";
    /** FILTER */
    public static final String FILTER = "filter";
    /** FONT_FAMILY */
    public static final String FONT_FAMILY = "font-family";
    /** FONT_SIZE */
    public static final String FONT_SIZE = "font-size";
    /** FONT_STYLE */
    public static final String FONT_STYLE = "font-style";
    /** FONT_WEIGHT */
    public static final String FONT_WEIGHT = "font-weight";
    /** FONTS */
    public static final String FONTS = "fonts";

    public static final String FORMAT = "format";
    /** GEOMETRY */
    public static final String GEOMETRY = "geometry";

    public static final String GRAPHIC_FILL = "graphic-fill";
    /** GRAPHIC_STROKE */
    public static final String GRAPHIC_STROKE = "graphic-stroke";
    /** GRAPHICS */
    public static final String GRAPHICS = "graphics";
    /** HALO */
    public static final String HALO = "halo";
    /** ICON */
    public static final String ICON = "icon";
    /** LABEL */
    public static final String LABEL = "label";
    /** LABEL_PLACEMENT */
    public static final String LABEL_PLACEMENT = "label-placement";
    /** LAYER_NAME */
    public static final String LAYER_NAME = "layerName";
    /** LEGEND */
    public static final String LEGEND = "Legend";
    /** LEGEND_GRAPHIC */
    public static final String LEGEND_GRAPHIC = "LegendGraphic";
    /** LINE */
    public static final String LINE = "Line";
    /** MARK */
    public static final String MARK = "mark";
    /** NAME */
    public static final String NAME = "name";
    /** OPACITY */
    public static final String OPACITY = "opacity";
    /** PERPENDICULAR_OFFSET */
    public static final String PERPENDICULAR_OFFSET = "perpendicular-offset";
    /** POINT */
    public static final String POINT = "Point";
    /** POLYGON */
    public static final String POLYGON = "Polygon";
    /** QUANTITY */
    public static final String QUANTITY = "quantity";
    /** RADIUS */
    public static final String RADIUS = "radius";
    /** RASTER */
    public static final String RASTER = "Raster";
    /** ROTATION */
    public static final String ROTATION = "rotation";
    /** RULES */
    public static final String RULES = "rules";

    public static final String SIZE = "size";
    /** STROKE */
    public static final String STROKE = "stroke";
    /** STROKE_DASHARRAY */
    public static final String STROKE_DASHARRAY = "stroke-dasharray";
    /** STROKE_DASHOFFSET */
    public static final String STROKE_DASHOFFSET = "stroke-dashoffset";
    /** STROKE_OPACITY */
    public static final String STROKE_OPACITY = "stroke-opacity";
    /** STROKE_WIDTH */
    public static final String STROKE_WIDTH = "stroke-width";

    /** SYMBOLIZERS */
    public static final String SYMBOLIZERS = "symbolizers";
    /** TEXT */
    public static final String TEXT = "Text";
    /** TITLE */
    public static final String TITLE = "title";
    /** TYPE */
    public static final String TYPE = COLORMAP_TYPE;
    /** UOM */
    public static final String UOM = "uom";

    /** X_ANCHOR */
    public static final String X_ANCHOR = "x-anchor";
    /** X_DISPLACEMENT */
    public static final String X_DISPLACEMENT = "x-displacement";
    /** Y_ANCHOR */
    public static final String Y_ANCHOR = "y-anchor";
    /** Y_DISPLACEMENT */
    public static final String Y_DISPLACEMENT = "y-displacement";

    public static final String STROKE_LINECAP = "stroke-linecap";

    public static final String GRAPHIC = "graphic";

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

    private Feature feature;
    private List<List<MiniRule>> miniStyle;
    private String styleName = "";
    private String layerName = "";
    private String baseURL = "";

    private WMS wms;
    private String ruleName;
    private int symbolizerCount;

    /** */
    @Override
    public JSONObject buildLegendGraphic(GetLegendGraphicRequest request) {
        setup(request);
        wms = request.getWms();
        Locale locale = request.getLocale();
        JSONObject response = new JSONObject();
        for (LegendRequest legend : layers) {
            FeatureType layer = legend.getFeatureType();

            Style gt2Style = legend.getStyle();
            if (gt2Style == null) {
                throw new NullPointerException("request.getStyle()");
            }

            // style and rule to use for the current layer
            miniStyle = MiniRule.minify(gt2Style, true);

            layerName = legend.getLayerName().getLocalPart();

            baseURL = request.getBaseUrl();

            styleName = legend.getStyleName();

            // get rule corresponding to the layer index
            // normalize to null for NO RULE
            ruleName = legend.getRule(); // was null

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
            if (legend instanceof CascadedLegendRequest) {
                CascadedLegendRequest cascadedLegend = (CascadedLegendRequest) legend;
                JSONArray cascadedRules = null;

                cascadedRules = cascadedLegend.getCascadedJSONRules();

                // if null or empty..go back to default behavior
                if (cascadedRules != null) {
                    if (!cascadedRules.isEmpty()) {
                        for (int i = 0; i < cascadedRules.size(); i++)
                            jRules.add(cascadedRules.getJSONObject(i));
                        // we have all the rules from cascaded JSON request
                        // no need to loop
                        layerName = cascadedLegend.getLayer();
                        // erase rules to skip the next for loop
                        applicableRules = new Rule[] {};
                    }
                }
            }
            for (Rule rule : applicableRules) {
                ruleName = rule.getName();
                JSONObject jRule = new JSONObject();
                String name = rule.getName();
                if (name != null && !name.isEmpty()) {
                    jRule.element(NAME, name);
                }
                InternationalString title = rule.getDescription().getTitle();
                if (title != null) {
                    jRule.element(TITLE, title.toString(locale));
                }
                InternationalString abs = rule.getDescription().getAbstract();
                if (abs != null) {
                    jRule.element(ABSTRACT, abs.toString(locale));
                }
                Filter filter = rule.getFilter();
                if (filter != null) {
                    jRule.element(FILTER, "[" + ECQL.toCQL(filter) + "]");
                }
                if (rule.isElseFilter()) {
                    jRule.element(ELSE_FILTER, "true");
                }
                JSONArray jSymbolizers = new JSONArray();
                if (layer != null) {
                    feature = getSampleFeatureForRule(layer, null, rule);
                } else {
                    feature = null;
                }
                List<Symbolizer> symbolizers = rule.symbolizers();
                symbolizerCount = 0;
                for (Symbolizer symbolizer : symbolizers) {
                    JSONObject jSymb = new JSONObject();
                    JSONObject symb = processSymbolizer(symbolizer);
                    jSymb.element(symbolizerNames.get(symbolizer.getClass()), symb);
                    jSymbolizers.add(jSymb);
                }
                org.opengis.style.GraphicLegend l = rule.getLegend();
                if (l != null) {
                    for (GraphicalSymbol g : l.graphicalSymbols()) {
                        String href =
                                IconPropertyExtractor.extractProperties(
                                                gt2Style, (SimpleFeature) feature)
                                        .href(
                                                request.getBaseUrl(),
                                                legend.getLayer(),
                                                legend.getStyleName());

                        JSONObject jGraphicSymb = processGraphicalSymbol(g, href);
                        jGraphicSymb.element("url", href);
                        jRule.element(LEGEND_GRAPHIC, jGraphicSymb);
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

    private JSONObject getLayerTitle(JSONObject in, LegendRequest legend) {
        String title = legend.getTitle();
        return in.element(TITLE, title);
    }

    /** */
    private String toJSONValue(Expression exp, Class<?> type) {
        FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        exp.accept(extractor, null);
        if (extractor.isConstantExpression()) {
            Object value = exp.evaluate(feature, type);
            if (value != null && type.equals(Color.class)) {
                Color c = (Color) value;
                return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
            } else if (value != null && type.isAssignableFrom(Number.class)) {
                Number n = (Number) value;
                if (!Double.isNaN(n.doubleValue()) && !Double.isInfinite(n.doubleValue())) {
                    return n.toString();
                }

            } else if (value != null) {
                return value.toString();
            }
        }

        return "'[" + ECQL.toCQL(exp) + "]'";
    }

    /** */
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
            ent.element(QUANTITY, toJSONValue(entry.getQuantity(), Number.class));

            ent.element(COLOR, toJSONValue(entry.getColor(), Color.class));

            Expression opac = entry.getOpacity();
            if (opac != null) {
                ent.element(OPACITY, toJSONValue(opac, Double.class));
            }
            entries.add(ent);
        }
        JSONObject cm = new JSONObject();
        if (entries.size() > 0) {
            cm.element(ENTRIES, entries);
            int type = colorMap.getType();
            switch (type) {
                case ColorMap.TYPE_INTERVALS:
                    cm.element(COLORMAP_TYPE, "intervals");
                    break;
                case ColorMap.TYPE_RAMP:
                    cm.element(COLORMAP_TYPE, "ramp");
                    break;
                case ColorMap.TYPE_VALUES:
                    cm.element(COLORMAP_TYPE, "values");
                    break;
            }

            ret.element(COLORMAP, cm);
        }
        return ret;
    }

    /** */
    private JSONObject processFill(JSONObject ret, Fill fill) {
        if (fill == null) {
            return ret;
        }
        boolean filled = false;

        if (fill.getGraphicFill() != null) {
            filled = true;
            JSONObject jGraphic = new JSONObject();
            jGraphic = processGraphic(jGraphic, fill.getGraphicFill());
            ret.element(GRAPHIC_FILL, jGraphic);
        }
        if (!filled) {
            ret.element(FILL, toJSONValue(fill.getColor(), Color.class));
            ret.element(FILL_OPACITY, toJSONValue(fill.getOpacity(), Number.class));
        }
        return ret;
    }

    /** */
    private JSONObject processGraphic(JSONObject ret, Graphic graphic) {
        if (graphic == null) {
            return ret;
        }
        Catalog catalog = wms.getCatalog();
        StyleInfo styleByName = catalog.getStyleByName(styleName);
        String wsName = null;
        if (styleByName != null) {
            WorkspaceInfo ws = styleByName.getWorkspace();
            if (ws != null) wsName = ws.getName();
        }
        List<List<MiniRule>> newStyle = new ArrayList<>();
        List<Integer> origRuleNo = new ArrayList<>();
        int ruleCount = 0;
        for (List<MiniRule> m : miniStyle) {
            List<MiniRule> newRules = new ArrayList<>(miniStyle.size());
            for (MiniRule r : m) {
                String rName = r.getName();
                if (rName != null && !rName.equalsIgnoreCase(ruleName)) {
                    ruleCount++;
                    continue;
                }
                MiniRule n = new MiniRule(Filter.INCLUDE, r.isElseFilter, r.symbolizers);
                newRules.add(n);
                origRuleNo.add(ruleCount);
                ruleCount++;
            }
            newStyle.add(newRules);
        }

        IconProperties props =
                IconPropertyExtractor.extractProperties(newStyle, (SimpleFeature) feature);

        String iconUrl = props.href(baseURL, wsName, styleName);
        int index = iconUrl.indexOf('?');
        if (index >= 0) {
            String base = iconUrl.substring(0, index + 1);
            String[] refs = iconUrl.substring(index + 1).split("&");
            for (int i = 0; i < refs.length; i++) {
                if (refs[i].matches("(\\d\\.)\\d(\\.\\d[\\.\\w+]*=[\\d.]*)")) {
                    String ref =
                            refs[i].replaceAll(
                                    "(\\d\\.)\\d(\\.\\d=)", "$1" + origRuleNo.get(0) + "$2");
                    String[] split = refs[i].split("\\.");
                    int symCount = Integer.parseInt(split[2].replaceAll("=", ""));
                    if (symbolizerCount == symCount) base += ref + "&";
                } else {
                    base += refs[i] + "&";
                }
            }
            if (base.endsWith("&")) {
                iconUrl = base.substring(0, base.length() - 1);
            } else {
                iconUrl = base;
            }
        }
        ret.element("url", iconUrl);

        JSONArray jGraphics = new JSONArray();
        List<GraphicalSymbol> gSymbols = graphic.graphicalSymbols();
        for (GraphicalSymbol g : gSymbols) {
            JSONObject jGraphic = processGraphicalSymbol(g, iconUrl);

            jGraphics.add(jGraphic);
        }

        Expression size = graphic.getSize();
        if (size != null) {
            String jSize = toJSONValue(size, Number.class);
            if (!jSize.equalsIgnoreCase("'[\"\"]'")) ret.element(SIZE, jSize);
        }
        Expression opacity = graphic.getOpacity();
        if (opacity != null) {
            ret.element(OPACITY, toJSONValue(opacity, Number.class));
        }
        Expression rotation = graphic.getRotation();
        if (rotation != null) {
            ret.element(ROTATION, toJSONValue(rotation, Number.class));
        }
        Displacement disp = graphic.getDisplacement();
        if (disp != null) {
            JSONObject displacement = new JSONObject();
            displacement.element(DISPLACEMENT_X, disp.getDisplacementX());
            displacement.element(DISPLACEMENT_Y, disp.getDisplacementY());
            ret.element(DISPLACEMENT, displacement);
        }
        AnchorPoint anc = graphic.getAnchorPoint();
        if (anc != null) {
            JSONObject anchor = new JSONObject();
            anchor.element(ANCHOR_X, anc.getAnchorPointX());
            anchor.element(ANCHOR_Y, anc.getAnchorPointY());
            ret.element(ANCHOR_POINT, anchor);
        }
        ret.element(GRAPHICS, jGraphics);
        symbolizerCount++;
        return ret;
    }

    /** */
    private JSONObject processGraphicalSymbol(GraphicalSymbol g, String iconUrl) {
        JSONObject jGraphic = new JSONObject();

        if (g instanceof Mark) {
            Mark m = ((Mark) g);
            Expression wkn = m.getWellKnownName();
            if (wkn != null) {
                jGraphic.element(MARK, toJSONValue(wkn, String.class));
            }
            jGraphic = processFill(jGraphic, m.getFill());
            jGraphic = processStroke(jGraphic, m.getStroke());
            ExternalMark em = m.getExternalMark();
            if (em != null) {
                OnLineResource or = em.getOnlineResource();
                if (or != null) {
                    try {
                        URL url = or.getLinkage().toURL();
                        if (url.getProtocol().equals("file")) {
                            // no point in adding a local file.
                            jGraphic.element(EXTERNAL_GRAPHIC_URL, iconUrl);
                        } else {
                            jGraphic.element(EXTERNAL_GRAPHIC_URL, url.toString());
                        }
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
                URL url = eg.getOnlineResource().getLinkage().toURL();
                if (url.getProtocol().equals("file")) {
                    // no point in adding a local file.
                    jGraphic.element(EXTERNAL_GRAPHIC_URL, iconUrl);
                } else {
                    jGraphic.element(EXTERNAL_GRAPHIC_URL, url.toString());
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            jGraphic.element(EXTERNAL_GRAPHIC_TYPE, eg.getFormat());
        }
        return jGraphic;
    }

    /** */
    private JSONObject processLineSymbolizer(JSONObject ret, LineSymbolizer symbolizer) {
        ret = processStroke(ret, symbolizer.getStroke());
        if (symbolizer.getPerpendicularOffset() != null) {
            ret.element(
                    PERPENDICULAR_OFFSET,
                    toJSONValue(symbolizer.getPerpendicularOffset(), Number.class));
        }
        return ret;
    }

    /** */
    private JSONObject processPointSymbolizer(JSONObject ret, PointSymbolizer symbolizer) {
        ret = processGraphic(ret, symbolizer.getGraphic());
        return ret;
    }
    /** */
    private JSONObject processPolygonSymbolizer(JSONObject ret, PolygonSymbolizer symbolizer) {
        ret = processStroke(ret, symbolizer.getStroke());
        ret = processFill(ret, symbolizer.getFill());
        return ret;
    }
    /** */
    private JSONObject processRasterSymbolizer(JSONObject ret, RasterSymbolizer symbolizer) {
        ret = processColorMap(symbolizer.getColorMap(), ret);
        Expression op = symbolizer.getOpacity();
        if (op != null) {
            ret.element(OPACITY, toJSONValue(op, Double.class));
        }
        ret = processChannelSelection(ret, symbolizer.getChannelSelection());
        ret = processContrastEnhancement(ret, symbolizer.getContrastEnhancement());
        return ret;
    }
    /** */
    private JSONObject processContrastEnhancement(
            JSONObject ret, ContrastEnhancement contrastEnhancement) {
        if (contrastEnhancement != null) {
            JSONObject ce = new JSONObject();
            Expression gammaValue = contrastEnhancement.getGammaValue();
            if (gammaValue != null) ce.element(GAMMA_VALUE, toJSONValue(gammaValue, Number.class));
            ContrastMethod method = contrastEnhancement.getMethod();
            if (ContrastMethod.NORMALIZE == method) {
                ce.element(NORMALIZE, "true");
            } else if (ContrastMethod.HISTOGRAM == method) {
                ce.element(HISTOGRAM, "true");
            } else if (ContrastMethod.EXPONENTIAL == method) {
                ce.element(EXPONENTIAL, "true");
            } else if (ContrastMethod.LOGARITHMIC == method) {
                ce.element(LOGARITHMIC, "true");
            }

            ce = processVendorOptions(ce, contrastEnhancement.getOptions());
            if (!ce.isEmpty()) ret.element(CONTRAST_ENHANCEMENT, ce);
        }

        return ret;
    }

    /** */
    private JSONObject processChannelSelection(JSONObject ret, ChannelSelection channelSelection) {
        if (channelSelection != null) {
            JSONObject chs = new JSONObject();
            SelectedChannelType[] rgbChannels = channelSelection.getRGBChannels();
            if (rgbChannels != null) {
                if (rgbChannels[0] != null)
                    chs.element(RED, toJSONValue(rgbChannels[0].getChannelName(), String.class));
                if (rgbChannels[1] != null)
                    chs.element(GREEN, toJSONValue(rgbChannels[1].getChannelName(), String.class));
                if (rgbChannels[2] != null)
                    chs.element(BLUE, toJSONValue(rgbChannels[2].getChannelName(), String.class));
            }

            SelectedChannelType grayChannel = channelSelection.getGrayChannel();
            if (grayChannel != null) {
                chs.element(GRAY, toJSONValue(grayChannel.getChannelName(), String.class));
            }
        }
        return ret;
    }

    /** */
    private JSONObject processStroke(JSONObject ret, Stroke stroke) {
        if (stroke == null) {
            return ret;
        }
        boolean stroked = false;
        if (stroke.getGraphicStroke() != null) {
            stroked = true;
            JSONObject jGraphic = new JSONObject();
            jGraphic = processGraphic(jGraphic, stroke.getGraphicStroke());
            ret.element(GRAPHIC_STROKE, jGraphic);
        }
        if (stroke.getGraphicFill() != null) {
            stroked = true;
            JSONObject jGraphic = new JSONObject();
            jGraphic = processGraphic(jGraphic, stroke.getGraphicFill());
            ret.element(GRAPHIC_FILL, jGraphic);
        }
        if (!stroked) { // otherwise we get a default black line here
            ret.element(STROKE, stroke.getColor().evaluate(feature));
            ret.element(STROKE_WIDTH, stroke.getWidth().evaluate(feature));
        }
        if (stroke.getOpacity() != null) {
            ret.element(STROKE_OPACITY, toJSONValue(stroke.getOpacity(), Number.class));
        }
        if (stroke.getLineCap() != null) {
            ret.element(STROKE_LINECAP, toJSONValue(stroke.getLineCap(), String.class));
        }
        if (stroke.getLineJoin() != null) {
            ret.element(STROKE_LINEJOIN, toJSONValue(stroke.getLineJoin(), String.class));
        }
        List<Expression> dashArray = stroke.dashArray();
        if (dashArray != null && !dashArray.isEmpty()) {
            JSONArray dArray = new JSONArray();
            for (Expression e : dashArray) {
                dArray.add(toJSONValue(e, Double.class));
            }
            ret.element(STROKE_DASHARRAY, dArray);
            Expression dashOffset = stroke.getDashOffset();
            if (dashOffset != null) {
                ret.element(STROKE_DASHOFFSET, toJSONValue(dashOffset, Double.class));
            }
        }
        return ret;
    }

    /** */
    private JSONObject processSymbolizer(Symbolizer symbolizer) {
        JSONObject ret = new JSONObject();
        String name = symbolizer.getName();
        if (name != null && !name.isEmpty()) {
            ret.element(NAME, name);
        }
        Unit<Length> uom = symbolizer.getUnitOfMeasure();
        if (uom != null) {
            ret.element(UOM, uom.toString());
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
            ret.element(GEOMETRY, toJSONValue(geometry, Geometry.class));
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
        } else {
            LOGGER.warning("unknown symbolizer type " + symbolizer.getClass().getName());
        }

        Map<String, String> opts = symbolizer.getOptions();
        ret = processVendorOptions(ret, opts);
        return ret;
    }

    /** */
    private JSONObject processVendorOptions(JSONObject ret, Map<String, ?> map) {
        if (!map.isEmpty()) {
            JSONObject vendorOpts = new JSONObject();

            for (Entry<String, ?> opt : map.entrySet()) {
                if (opt.getValue() instanceof Expression) {
                    vendorOpts.element(
                            opt.getKey(), toJSONValue((Expression) opt.getValue(), Object.class));
                } else {
                    vendorOpts.element(opt.getKey(), opt.getValue());
                }
            }
            ret.element(VENDOR_OPTIONS, vendorOpts);
        }
        return ret;
    }

    /** */
    private JSONObject processTextSymbolizer(JSONObject ret, TextSymbolizer symbolizer) {
        ret.element(LABEL, toJSONValue(symbolizer.getLabel(), String.class));
        JSONArray fonts = new JSONArray();
        for (Font font : symbolizer.fonts()) {
            JSONObject jFont = new JSONObject();
            JSONArray jFam = new JSONArray();
            for (Expression family : font.getFamily()) {
                jFam.add(toJSONValue(family, String.class));
            }
            jFont.element(FONT_FAMILY, jFam);
            jFont.element(FONT_STYLE, toJSONValue(font.getStyle(), String.class));
            jFont.element(FONT_WEIGHT, toJSONValue(font.getWeight(), String.class));
            jFont.element(FONT_SIZE, toJSONValue(font.getSize(), Number.class));
            fonts.add(jFont);
        }
        ret.element(FONTS, fonts);
        ret = processFill(ret, symbolizer.getFill());
        if (symbolizer instanceof TextSymbolizer2) {
            // handle font graphic
            TextSymbolizer2 tSymb = (TextSymbolizer2) symbolizer;
            JSONObject graphic = new JSONObject();
            graphic = processGraphic(graphic, tSymb.getGraphic());
            ret.element(GRAPHIC, graphic);
        }
        JSONObject jPlacement = new JSONObject();
        LabelPlacement placement = symbolizer.getLabelPlacement();
        if (placement instanceof PointPlacement) {
            PointPlacement pplacement = (PointPlacement) placement;

            AnchorPoint ap = pplacement.getAnchorPoint();
            if (ap != null) {
                jPlacement.element(X_ANCHOR, toJSONValue(ap.getAnchorPointX(), Number.class));
                jPlacement.element(Y_ANCHOR, toJSONValue(ap.getAnchorPointY(), Number.class));
            }
            jPlacement.element(ROTATION, toJSONValue(pplacement.getRotation(), Number.class));
            Displacement displacement = pplacement.getDisplacement();
            if (displacement != null) {
                jPlacement.element(
                        X_DISPLACEMENT, toJSONValue(displacement.getDisplacementX(), Number.class));
                jPlacement.element(
                        Y_DISPLACEMENT, toJSONValue(displacement.getDisplacementY(), Number.class));
            }
        }
        if (placement instanceof LinePlacement) {
            LinePlacement lPlacement = (LinePlacement) placement;
            jPlacement.element(
                    PERPENDICULAR_OFFSET,
                    toJSONValue(lPlacement.getPerpendicularOffset(), String.class));
        }
        ret.element(LABEL_PLACEMENT, jPlacement);
        Halo halo = symbolizer.getHalo();
        if (halo != null) {
            JSONObject jHalo = new JSONObject();
            jHalo.element(RADIUS, toJSONValue(halo.getRadius(), Number.class));
            jHalo = processFill(jHalo, halo.getFill());
            ret.element(HALO, jHalo);
        }
        // TODO check for Graphic background
        return ret;
    }
}
