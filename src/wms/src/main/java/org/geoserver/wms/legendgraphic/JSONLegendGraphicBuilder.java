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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.LineSymbolizerImpl;
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
import org.opengis.style.Fill;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.util.InternationalString;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/** @author Ian Turton */
public class JSONLegendGraphicBuilder extends LegendGraphicBuilder {

  private Feature feature;

  /**
   * @param request
   * @return
   */
  public JSONObject buildLegendGraphic(GetLegendGraphicRequest request) {
    setup(request);

    JSONObject response = new JSONObject();
    for (LegendRequest legend : layers) {
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
      final boolean buildRasterLegend =
          (!strict && layer == null && LegendUtils.checkRasterSymbolizer(gt2Style))
              || (LegendUtils.checkGridLayer(layer) && !hasVectorTransformation)
              || hasRasterTransformation;
      if (buildRasterLegend) {
        final RasterLayerLegendHelper rasterLegendHelper =
            new RasterLayerLegendHelper(request, gt2Style, ruleName);
        // TODO serialise raster legend
      } else {

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
          InternationalString title = rule.getDescription().getTitle();
          if (title != null) {
            jRule.element("title", title.toString());
          }
          InternationalString abs = rule.getDescription().getAbstract();
          if (abs != null) {
            jRule.element("abstract", abs.toString());
          }
          Filter filter = rule.getFilter();
          if (filter != null) {
            jRule.element("filter", "[" + CQL.toCQL(filter) + "]");
          }
          JSONArray jSymbolizers = new JSONArray();
          feature = getSampleFeatureForRule(layer, null, rule);
          for (Symbolizer symbolizer : rule.symbolizers()) {
            JSONObject jObj = new JSONObject();
            JSONObject value = processSymbolizer(symbolizer);
            jObj.element(symbolizerNames.get(symbolizer.getClass()), value);
            jSymbolizers.add(jObj);
          }
          jRule.element("symbolizers", jSymbolizers);
          jRules.add(jRule);
        }
        response.element("Legend", jRules);
      }
    }
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
      ret.element("name", name);
    }
    Unit<Length> uom = symbolizer.getUnitOfMeasure();
    if (uom != null) {
      ret.element("uom", uom.getName());
    } else {
      ret.element("uom", "pixel");
    }
    Description desc = symbolizer.getDescription();
    if (desc != null) {
      InternationalString title = desc.getTitle();
      if (title != null) {
        ret.element("", title.toString());
      }
      InternationalString abs = desc.getAbstract();
      if (abs != null) {
        ret.element("abstract", abs.toString());
      }
    }
    Expression geometry = symbolizer.getGeometry();
    if (geometry != null) {
      ret.element("geometry", geometry.toString());
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
    ret.element("type", "text-TODO");
    return ret;
  }

  /**
   * @param ret
   * @param symbolizer
   * @return
   */
  private JSONObject processRasterSymbolizer(JSONObject ret, RasterSymbolizer symbolizer) {
    ret.element("type", "raster-todo");
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
    ret.element("type", "point-todo");
    return ret;
  }

  private JSONObject getLayerTitle(JSONObject in, LegendRequest legend) {
    String title = legend.getTitle();
    return in.element("title", title);
  }
  /**
   * @param ret
   * @param fill
   * @return
   */
  private JSONObject processFill(JSONObject ret, Fill fill) {
    ret.element("fill", fill.getColor().evaluate(feature));
    ret.element("fill-opacity", fill.getOpacity().evaluate(feature));

    return ret;
  }
  /**
   * @param ret
   * @param stroke
   * @return
   */
  private JSONObject processStroke(JSONObject ret, Stroke stroke) {
    ret.element("stroke", stroke.getColor().evaluate(feature));
    ret.element("stroke-width", stroke.getWidth().evaluate(feature));
    List<Expression> dashArray = stroke.dashArray();
    if (dashArray != null && !dashArray.isEmpty()) {
      JSONArray dArray = new JSONArray();
      for (Expression e : dashArray) {
        dArray.add(e.evaluate(feature));
      }
      ret.element("stroke-dasharray", dArray);
      Expression dashOffset = stroke.getDashOffset();
      if (dashOffset != null) {
        ret.element("stroke-dashoffset", dashOffset.evaluate(feature));
      }
    }
    return ret;
  }

  static Map<Class, String> symbolizerNames = new HashMap<>();

  static {
    symbolizerNames.put(PolygonSymbolizer.class, "Polygon");
    symbolizerNames.put(LineSymbolizer.class, "Line");
    symbolizerNames.put(PointSymbolizer.class, "Point");
    symbolizerNames.put(RasterSymbolizer.class, "Raster");
    symbolizerNames.put(TextSymbolizer.class, "Text");
    symbolizerNames.put(PolygonSymbolizerImpl.class, "Polygon");
    symbolizerNames.put(LineSymbolizerImpl.class, "Line");
    symbolizerNames.put(PointSymbolizerImpl.class, "Point");
    symbolizerNames.put(RasterSymbolizerImpl.class, "Raster");
    symbolizerNames.put(TextSymbolizerImpl.class, "Text");
  }
}
