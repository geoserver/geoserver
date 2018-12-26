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
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.swing.Icon;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.se.v1_1.bindings.NormalizeBinding;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.ContrastMethodStrategy;
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
import org.geotools.styling.NormalizeContrastMethodStrategy;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PointSymbolizerImpl;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.RasterSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizerImpl;
import org.geotools.xml.styling.SLDTransformer;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.style.ContrastMethod;
import org.opengis.style.GraphicalSymbol;
import org.opengis.util.InternationalString;

/** @author Ian Turton */
public class JSONLegendGraphicBuilder extends LegendGraphicBuilder {

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


  /**
   * @param request
   * @return
   */
  @Override
  public JSONObject buildLegendGraphic(GetLegendGraphicRequest request) {
    setup(request);
    Locale locale = request.getLocale();
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
          throw new ServiceException("Specified style does not contains a rule named " + ruleName);
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
          jRule.element(TITLE, title.toString(locale));
        }
        InternationalString abs = rule.getDescription().getAbstract();
        if (abs != null) {
          jRule.element(ABSTRACT, abs.toString(locale));
        }
        Filter filter = rule.getFilter();
        if (filter != null) {
          jRule.element(FILTER, "[" + CQL.toCQL(filter) + "]");
        }
        JSONArray jSymbolizers = new JSONArray();
        if (layer != null) {
          feature = getSampleFeatureForRule(layer, null, rule);
        } else {
          feature = null;
        }
        List<Symbolizer> symbolizers2 = rule.symbolizers();
        for (Symbolizer symbolizer : symbolizers2) {
          JSONObject jSymb = new JSONObject();
          JSONObject symb = processSymbolizer(symbolizer);
          jSymb.element(symbolizerNames.get(symbolizer.getClass()), symb);
          jSymbolizers.add(jSymb);
        }
        org.opengis.style.GraphicLegend l = rule.getLegend();
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

  private JSONObject getLayerTitle(JSONObject in, LegendRequest legend) {
    String title = legend.getTitle();
    return in.element(TITLE, title);
  }

  /**
   * @param symbolizer
   * @return
   */
  private String printExpression(Expression exp, Class<?> type) {

    Object value = exp.evaluate(feature, type);
    if (value != null && type.equals(Color.class)) {
      Color c = (Color) value;
      return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    } else if (value != null && type.isAssignableFrom(Number.class)) {
      Number n = (Number) value;
      if (!Double.isNaN(n.doubleValue()) && !Double.isInfinite(n.doubleValue())) {
        // probably not a function
        return n.toString();
      }
    } else if (value != null) {
      return value.toString();
    }
    return "'[" + exp + "]'";
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
      ent.element(QUANTITY, printExpression(entry.getQuantity(), Number.class));

      ent.element(COLOR, printExpression(entry.getColor(), Color.class));

      Expression opac = entry.getOpacity();
      if (opac != null) {
        ent.element(OPACITY, printExpression(opac, Double.class));
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

  /**
   * @param ret
   * @param fill
   * @return
   */
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
      ret.element(FILL, printExpression(fill.getColor(), Color.class));
      ret.element(FILL_OPACITY, printExpression(fill.getOpacity(), Number.class));
    }
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

    Expression size2 = graphic.getSize();
    if (size2 != null) {
      ret.element(SIZE, printExpression(size2, Number.class));
    }
    Expression opacity = graphic.getOpacity();
    if (opacity != null) {
      ret.element(OPACITY, printExpression(opacity, Number.class));
    }
    Expression rotation = graphic.getRotation();
    if (rotation != null) {
      ret.element(ROTATION, printExpression(rotation, Number.class));
    }
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
        jGraphic.element(MARK, printExpression(wkn, String.class));
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
            EXTERNAL_GRAPHIC_URL, eg.getOnlineResource().getLinkage().toURL().toString());
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
      jGraphic.element(EXTERNAL_GRAPHIC_TYPE, eg.getFormat());
    }
    return jGraphic;
  }

  /**
   * @param ret
   * @param symbolizer
   * @return
   */
  private JSONObject processLineSymbolizer(JSONObject ret, LineSymbolizer symbolizer) {
    ret = processStroke(ret, symbolizer.getStroke());
    if (symbolizer.getPerpendicularOffset() != null) {
      ret.element(
          PERPENDICULAR_OFFSET, printExpression(symbolizer.getPerpendicularOffset(), Number.class));
    }
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
   * @param symbolizer
   * @return
   */
  private JSONObject processPolygonSymbolizer(JSONObject ret, PolygonSymbolizer symbolizer) {
    ret = processStroke(ret, symbolizer.getStroke());
    ret = processFill(ret, symbolizer.getFill());
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
      ret.element(OPACITY, printExpression(op, Double.class));
    }
    ret = processChannelSelection(ret, symbolizer.getChannelSelection());
    ret = processContrastEnhancement(ret, symbolizer.getContrastEnhancement());
    return ret;
  }
  /**
   * @param ret
   * @param contrastEnhancement
   * @return
   */
  private JSONObject processContrastEnhancement(
      JSONObject ret, ContrastEnhancement contrastEnhancement) {
    if (contrastEnhancement != null) {
      JSONObject ce = new JSONObject();
      Expression gammaValue = contrastEnhancement.getGammaValue();
      if (gammaValue != null) ce.element(GAMMA_VALUE, printExpression(gammaValue, Number.class));
      ContrastMethod method = contrastEnhancement.getMethod();
      if (ContrastMethod.NORMALIZE == method) {
        JSONObject norm = new JSONObject();
        //TODO handle vendor option
        ce.element(NORMALIZE, "true");
      } else if (ContrastMethod.HISTOGRAM == method) {
        ce.element(HISTOGRAM, "true");
      } else if (ContrastMethod.EXPONENTIAL == method) {
        ce.element(EXPONENTIAL, "true");
      } else if (ContrastMethod.LOGARITHMIC == method) {
        ce.element(LOGARITHMIC, "true");
      }
      if (!ce.isEmpty()) ret.element(CONTRAST_ENHANCEMENT, ce);
    }

    return ret;
  }

  /**
   * @param ret
   * @param channelSelection
   * @return
   */
  private JSONObject processChannelSelection(JSONObject ret, ChannelSelection channelSelection) {
    if (channelSelection != null) {
      JSONObject chs = new JSONObject();
      SelectedChannelType[] rgbChannels = channelSelection.getRGBChannels();
      if (rgbChannels != null) {
        if (rgbChannels[0] != null)
          chs.element(RED, printExpression(rgbChannels[0].getChannelName(), String.class));
        if (rgbChannels[1] != null)
          chs.element(GREEN, printExpression(rgbChannels[1].getChannelName(), String.class));
        if (rgbChannels[2] != null)
          chs.element(BLUE, printExpression(rgbChannels[2].getChannelName(), String.class));
      }

      SelectedChannelType grayChannel = channelSelection.getGrayChannel();
      if (grayChannel != null) {
        chs.element(GRAY, printExpression(grayChannel.getChannelName(), String.class));
      }
    }
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
      ret.element(STROKE_OPACITY, printExpression(stroke.getOpacity(), Number.class));
    }
    if (stroke.getLineCap() != null) {
      ret.element(STROKE_LINECAP, printExpression(stroke.getLineCap(), String.class));
    }
    if (stroke.getLineJoin() != null) {
      ret.element(STROKE_LINEJOIN, printExpression(stroke.getLineJoin(), String.class));
    }
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
      ret.element(GEOMETRY, printExpression(geometry, Geometry.class));
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
    ret.element(LABEL, printExpression(symbolizer.getLabel(), String.class));
    JSONArray fonts = new JSONArray();
    for (Font font : symbolizer.fonts()) {
      JSONObject jFont = new JSONObject();
      JSONArray jFam = new JSONArray();
      for (Expression family : font.getFamily()) {
        jFam.add(printExpression(family, String.class));
      }
      jFont.element(FONT_FAMILY, jFam);
      jFont.element(FONT_STYLE, printExpression(font.getStyle(), String.class));
      jFont.element(FONT_WEIGHT, printExpression(font.getWeight(), String.class));
      jFont.element(FONT_SIZE, printExpression(font.getSize(), Number.class));
      fonts.add(jFont);
    }
    ret.element(FONTS, fonts);
    JSONObject jPlacement = new JSONObject();
    LabelPlacement placement = symbolizer.getLabelPlacement();
    if (placement instanceof PointPlacement) {
      PointPlacement pplacement = (PointPlacement) placement;

      AnchorPoint ap = pplacement.getAnchorPoint();
      if (ap != null) {
        jPlacement.element(X_ANCHOR, printExpression(ap.getAnchorPointX(), Number.class));
        jPlacement.element(Y_ANCHOR, printExpression(ap.getAnchorPointY(), Number.class));
      }
      jPlacement.element(ROTATION, printExpression(pplacement.getRotation(), Number.class));
      Displacement displacement = pplacement.getDisplacement();
      if (displacement != null) {
        jPlacement.element(
            X_DISPLACEMENT, printExpression(displacement.getDisplacementX(), Number.class));
        jPlacement.element(
            Y_DISPLACEMENT, printExpression(displacement.getDisplacementY(), Number.class));
      }
    }
    if (placement instanceof LinePlacement) {
      LinePlacement lPlacement = (LinePlacement) placement;
      jPlacement.element(
          PERPENDICULAR_OFFSET, printExpression(lPlacement.getPerpendicularOffset(), String.class));
    }
    ret.element(LABEL_PLACEMENT, jPlacement);
    Halo halo = symbolizer.getHalo();
    if (halo != null) {
      JSONObject jHalo = new JSONObject();
      jHalo.element(RADIUS, printExpression(halo.getRadius(), Number.class));
      jHalo = processFill(jHalo, halo.getFill());
      ret.element(HALO, jHalo);
    }
    // TODO check for Graphic background
    return ret;
  }
}
