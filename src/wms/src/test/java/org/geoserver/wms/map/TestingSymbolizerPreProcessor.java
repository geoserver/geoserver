/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.Layer;
import org.geotools.renderer.SymbolizersPreProcessor;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizerImpl;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyleFactoryImpl;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;

/**
 * SymbolizerPreProcessor implementation used for testing purposes only. Needs explicit activation
 * via the enabled attribute.
 */
public class TestingSymbolizerPreProcessor implements SymbolizersPreProcessor {

    private static final String TEST_ATTR = "test_attr";
    private static final String SF_POI = "sf:Poi";
    private static final String COLOR1 = "#25BF20";

    final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
    final StyleFactory styleFactory = new StyleFactoryImpl();

    private boolean enabled = false;

    @Override
    public boolean appliesTo(Layer layer) {
        return enabled;
    }

    @Override
    public List<String> getAttributes(Layer layer) {
        if (SF_POI.equals(layer.getTitle())) {
            return Arrays.asList(TEST_ATTR);
        }
        return Collections.emptyList();
    }

    @Override
    public double getBuffer(Layer layer, Style style) {
        return 60.0d;
    }

    @Override
    public List<Symbolizer> apply(Feature feature, Layer layer, List<Symbolizer> symbolizers) {
        if (!enabled) return null;
        if (SF_POI.equals(layer.getTitle()))
            return enhancePoiSymbolizer(feature, layer, symbolizers);
        Symbolizer symbolizer = symbolizers.get(0);
        if (symbolizer instanceof PolygonSymbolizerImpl) {
            PolygonSymbolizerImpl polygonSymb = (PolygonSymbolizerImpl) symbolizer;
            polygonSymb.setFill(null);
        }
        return Arrays.asList(symbolizer);
    }

    private List<Symbolizer> enhancePoiSymbolizer(
            Feature feature, Layer layer, List<Symbolizer> symbolizers) {
        PropertyName testAttrProperty = filterFactory.property(TEST_ATTR);
        String testAttr = testAttrProperty.evaluate(feature, String.class);
        if ("ok".equals(testAttr)) {
            PointSymbolizer highlightSymbolizer =
                    styleFactory.pointSymbolizer(
                            "name",
                            filterFactory.property("pointProperty"),
                            null,
                            null,
                            createHighlightGraphic());
            List<Symbolizer> resultList = new ArrayList<Symbolizer>(symbolizers);
            resultList.add(highlightSymbolizer);
            return resultList;
        }
        return symbolizers;
    }

    private Graphic createHighlightGraphic() {
        return styleFactory.graphic(
                Arrays.asList(createHighlightMark(COLOR1)),
                filterFactory.literal(1.0),
                filterFactory.literal(60),
                filterFactory.literal(0.0),
                styleFactory.anchorPoint(filterFactory.literal(0.0), filterFactory.literal(0.0)),
                styleFactory.displacement(filterFactory.literal(-30), filterFactory.literal(30)));
    }

    private Mark createHighlightMark(String color) {
        Mark circleMark = styleFactory.getCircleMark();
        circleMark.setStroke(
                styleFactory.createStroke(filterFactory.literal(color), filterFactory.literal(2)));
        circleMark.setFill(
                styleFactory.createFill(filterFactory.literal(color), filterFactory.literal(0.0)));
        return circleMark;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
