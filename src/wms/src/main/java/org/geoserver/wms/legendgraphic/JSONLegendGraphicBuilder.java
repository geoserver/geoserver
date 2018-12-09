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
import java.util.Map;
import java.util.stream.Stream;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizerImpl;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.opengis.feature.type.FeatureType;
import org.opengis.style.PolygonSymbolizer;

/** @author Ian Turton */
public class JSONLegendGraphicBuilder extends LegendGraphicBuilder {

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
                    JSONArray jSymbolizers = new JSONArray();
                    for (Symbolizer symbolizer : rule.symbolizers()) {
                        JSONObject jObj = new JSONObject();
                        JSONObject value = new JSONObject().element("stuff", true);
                        jObj.element(symbolizerNames.get(symbolizer.getClass()), value);
                        jSymbolizers.add(jObj);
                    }
                    jRule.element("symbolizers", jSymbolizers);
                    jRules.add(jRule);
                }
                JSONArray array = new JSONArray();
                array.addAll(jRules);
                JSONArray data = new JSONArray();
                Stream.of(jRules).forEach(data::add);
                response.accumulate("Legend", data);
            }
        }
        return response;
    }

    private JSONObject getLayerTitle(JSONObject in, LegendRequest legend) {
        String title = legend.getTitle();
        return in.element("title", title);
    }

    static Map<Class, String> symbolizerNames = new HashMap<>();

    static {
        symbolizerNames.put(PolygonSymbolizer.class, "Polygon");
        symbolizerNames.put(LineSymbolizerImpl.class, "Line");
        symbolizerNames.put(PointSymbolizer.class, "Point");
        symbolizerNames.put(RasterSymbolizer.class, "Raster");
        symbolizerNames.put(TextSymbolizer.class, "Text");
    }
}
