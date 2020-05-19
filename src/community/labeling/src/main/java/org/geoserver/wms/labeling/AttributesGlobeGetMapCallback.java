/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static org.geoserver.wms.labeling.AttributesGlobeGraphicFactory.GEOSERVER_LABEL;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbol;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.opengis.style.GraphicalSymbol;

/**
 * GetMap callback extension point implementation mainly used for requesting the attributes needed
 * on the query before sending it to rendering stage.
 */
public class AttributesGlobeGetMapCallback implements GetMapCallback {

    private static final Logger LOG = Logging.getLogger(AttributesGlobeGetMapCallback.class);

    static final double DEFAULT_DISPLACEMENT = -14d;
    static final String DEFAULT_HOST_URI = "http://geoserver.org";
    static final String LAYER_PARAM = "layer";

    protected final StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    protected final FilterFactory ff =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    @Override
    public GetMapRequest initRequest(GetMapRequest request) {
        return request;
    }

    @Override
    public void initMapContent(WMSMapContent mapContent) {}

    @Override
    public Layer beforeLayer(WMSMapContent mapContent, Layer layer) {
        return layer;
    }

    @Override
    public WMSMapContent beforeRender(WMSMapContent mapContent) {
        List<Layer> layers = mapContent.layers();
        for (Layer layer : layers) {
            // We work only with FeatureLayer instances
            if (!(layer instanceof FeatureLayer)) continue;
            processLayer((FeatureLayer) layer);
        }
        return mapContent;
    }

    @Override
    public WebMap finished(WebMap map) {
        return map;
    }

    @Override
    public void failed(Throwable t) {}

    private Layer processLayer(FeatureLayer layer) {
        // check if we have a label parameter configured for the layer name
        AttributeLabelParameter attributeLabelParameter =
                AttributesGlobeKvpParser.getParameterForLayerName(layer.getTitle());
        // if no label parameter matched current layer, continue with next layer
        if (attributeLabelParameter == null) return layer;
        LOG.log(Level.INFO, "Labeling rule found for layer: {0}", layer.getTitle());
        LOG.log(Level.INFO, "Rule found: {0}", attributeLabelParameter);
        // set the required attributes
        setRequiredAttributes(layer, attributeLabelParameter);
        // set the layer name on the symbolizer
        addGraphicSymbolizer(layer, attributeLabelParameter);
        return layer;
    }

    private void addGraphicSymbolizer(
            FeatureLayer layer, AttributeLabelParameter attributeLabelParameter) {
        // duplicate the style
        Style formerStyle = layer.getStyle();
        DuplicatingStyleVisitor duplicateVisitor = new DuplicatingStyleVisitor();
        formerStyle.accept(duplicateVisitor);
        Style newStyle = (Style) duplicateVisitor.getCopy();
        // check if a labeling external graphics already exists
        List<Symbolizer> presentSymbolizers =
                detectPresentSymbolizer(newStyle, attributeLabelParameter);
        if (presentSymbolizers.isEmpty()) {
            // create and add a new symbolizer
            buildAndAddNewSymbolizer(layer, attributeLabelParameter, newStyle);
        } else {
            // modify the existing symbolizer
            modifyExistintSymbolizer(layer, attributeLabelParameter, newStyle);
            layer.setStyle(newStyle);
        }
    }

    private void modifyExistintSymbolizer(
            FeatureLayer layer, AttributeLabelParameter attributeLabelParameter, Style style) {
        // find the target rule and setup the labeling filter
        Rule labelingRule = findLabelingRule(style);
        labelingRule.setFilter(attributeLabelParameter.getFilter());
        // get the Graphic and ExternalGraphic involved
        Pair<Graphic, ExternalGraphic> graphicPair = getLabelingExternalGraphics(labelingRule);
        Graphic graphic = graphicPair.getLeft();
        ExternalGraphic externalGraphicFormer = graphicPair.getRight();
        ExternalGraphic externalGraphic =
                sf.createExternalGraphic(
                        buildURL(externalGraphicFormer.getURI(), layer.getTitle()),
                        AttributesGlobeGraphicFactory.GEOSERVER_LABEL);
        graphic.graphicalSymbols().remove(externalGraphicFormer);
        graphic.graphicalSymbols().add(externalGraphic);
        // set anchor point and displacement
        graphic.setAnchorPoint(getAnchorPoint());
        if (graphic.getDisplacement() == null) graphic.setDisplacement(getDisplacement());
    }

    private String buildURL(String formerUri, String layerName) {
        URIBuilder builder = getUriBuilder(formerUri);
        builder.setParameter(LAYER_PARAM, layerName);
        try {
            return builder.build().toString() + "&random=${random()}";
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private URIBuilder getUriBuilder(String url) {
        if (StringUtils.isBlank(url)) return getDefaultUriBuilder();
        try {
            return new URIBuilder(url);
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private URIBuilder getDefaultUriBuilder() {
        try {
            return new URIBuilder(DEFAULT_HOST_URI);
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private AnchorPoint getAnchorPoint() {
        return sf.anchorPoint(ff.literal(0.5d), ff.literal(0d));
    }

    private void buildAndAddNewSymbolizer(
            FeatureLayer layer, AttributeLabelParameter attributeLabelParameter, Style newStyle) {
        // detect which symbolizer is our external graphic call
        FeatureTypeStyle featureTypeStyle =
                newStyle.featureTypeStyles()
                        .stream()
                        .filter(ts -> ts.featureTypeNames().isEmpty())
                        .findFirst()
                        .orElse(null);
        if (featureTypeStyle != null) {
            featureTypeStyle.rules().add(buildGraphicRule(layer, attributeLabelParameter));
            layer.setStyle(newStyle);
        }
    }

    private Rule buildGraphicRule(
            FeatureLayer layer, AttributeLabelParameter attributeLabelParameter) {
        // build the URI
        String uri = buildURL(DEFAULT_HOST_URI, layer.getTitle());
        ExternalGraphic externalGraphic =
                sf.createExternalGraphic(uri, AttributesGlobeGraphicFactory.GEOSERVER_LABEL);
        AnchorPoint anchorPoint = getAnchorPoint();
        Displacement displacement = getDisplacement();
        Graphic graphic =
                sf.createGraphic(
                        new ExternalGraphic[] {externalGraphic},
                        new Mark[] {},
                        new Symbol[] {},
                        null,
                        null,
                        ff.literal(0d));
        graphic.setAnchorPoint(anchorPoint);
        graphic.setDisplacement(displacement);
        PointSymbolizer symbolizer = sf.createPointSymbolizer(graphic, null);
        // create the rule
        Rule rule = sf.createRule();
        rule.setFilter(attributeLabelParameter.getFilter());
        rule.symbolizers().add(symbolizer);
        return rule;
    }

    private Displacement getDisplacement() {
        return sf.createDisplacement(ff.literal(0d), ff.literal(DEFAULT_DISPLACEMENT));
    }

    private void setRequiredAttributes(
            FeatureLayer layer, AttributeLabelParameter attributeLabelParameter) {
        Query query = layer.getQuery();
        query.setPropertyNames(
                attributeLabelParameter
                        .getAttributes()
                        .toArray(new String[attributeLabelParameter.getAttributes().size()]));
        if (LOG.isLoggable(Level.INFO)) {
            String queryStr = query.toString();
            LOG.info("Produced query for layer '" + layer.getTitle() + "':");
            LOG.info(queryStr);
        }
    }

    /**
     * Detects if currently there is a symbolizer containing the labeling configuration. If no one
     * is found, returns null;
     */
    private List<Symbolizer> detectPresentSymbolizer(
            Style style, AttributeLabelParameter attributeLabelParameter) {
        return style.featureTypeStyles()
                .stream()
                .filter(fts -> CollectionUtils.isEmpty(fts.featureTypeNames()))
                .map(fts -> fts.rules())
                .flatMap(List::stream)
                .map(rule -> rule.symbolizers())
                .flatMap(List::stream)
                .filter(symb -> isLabelingSymbolizer(symb))
                .collect(Collectors.toList());
    }

    private Rule findLabelingRule(Style style) {
        return style.featureTypeStyles()
                .stream()
                .filter(fts -> CollectionUtils.isEmpty(fts.featureTypeNames()))
                .map(fts -> fts.rules())
                .flatMap(List::stream)
                .filter(rule -> isLabelingRule(rule))
                .findFirst()
                .orElse(null);
    }

    private boolean isLabelingRule(Rule rule) {
        for (Symbolizer symb : rule.getSymbolizers()) {
            if (isLabelingSymbolizer(symb)) return true;
        }
        return false;
    }

    private boolean isLabelingSymbolizer(Symbolizer symbolizer) {
        if (!(symbolizer instanceof PointSymbolizer)) return false;
        PointSymbolizer pointSymbolizer = (PointSymbolizer) symbolizer;
        Graphic graphic = pointSymbolizer.getGraphic();

        return !getLabelingExternalGraphics(graphic).isEmpty();
    }

    private List<ExternalGraphic> getLabelingExternalGraphics(Graphic graphic) {
        List<GraphicalSymbol> graphicalSymbols =
                graphic != null ? graphic.graphicalSymbols() : Collections.emptyList();
        return graphicalSymbols
                .stream()
                .filter(gs -> gs instanceof ExternalGraphic)
                .map(gs -> (ExternalGraphic) gs)
                .filter(eg -> GEOSERVER_LABEL.equals(eg.getFormat()))
                .collect(Collectors.toList());
    }

    private Pair<Graphic, ExternalGraphic> getLabelExternalGraphic(Symbolizer symbolizer) {
        if (!(symbolizer instanceof PointSymbolizer)) return null;
        PointSymbolizer pointSymbolizer = (PointSymbolizer) symbolizer;
        Graphic graphic = pointSymbolizer.getGraphic();
        List<ExternalGraphic> externalGraphics = getLabelingExternalGraphics(graphic);
        if (!CollectionUtils.isEmpty(externalGraphics)) {
            return Pair.of(graphic, externalGraphics.get(0));
        }
        return null;
    }

    private Pair<Graphic, ExternalGraphic> getLabelingExternalGraphics(Rule rule) {
        for (Symbolizer symb : rule.getSymbolizers()) {
            Pair<Graphic, ExternalGraphic> pair = getLabelExternalGraphic(symb);
            if (pair != null) return pair;
        }
        return null;
    }
}
