/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.geoserver.catalog.Styles;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.SLD;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class CssHandlerTest extends GeoServerSystemTestSupport {

    @Test
    public void testParseThroughStyles() throws IOException {
        String css = "* { fill: lightgrey; }";
        StyledLayerDescriptor sld = Styles.handler(CssHandler.FORMAT).parse(css, null, null, null);
        assertNotNull(sld);

        PolygonSymbolizer ps = SLD.polySymbolizer(Styles.style(sld));
        assertNotNull(ps);
    }

    @Test
    public void testParseMultiLayer() throws IOException {
        String css = "topp:states {fill:gray} sf:roads {stroke:black}";
        StyledLayerDescriptor sld = Styles.handler(CssHandler.FORMAT).parse(css, null, null, null);
        assertNotNull(sld);
        assertEquals(2, sld.getStyledLayers().length);

        // states
        NamedLayer l1 = (NamedLayer) sld.getStyledLayers()[0];
        assertEquals("topp:states", l1.getName());
        assertEquals(1, l1.getStyles().length);
        FeatureTypeStyle fts1 = l1.getStyles()[0].featureTypeStyles().get(0);
        // won't filter on type names, using a fully qualified name here would prevent any rendering
        assertThat(fts1.featureTypeNames(), Matchers.empty());
        PolygonSymbolizer poly = SLD.polySymbolizer(fts1);
        assertEquals("#808080", poly.getFill().getColor().evaluate(null, String.class));

        // roads
        NamedLayer l2 = (NamedLayer) sld.getStyledLayers()[1];
        assertEquals("sf:roads", l2.getName());
        assertEquals(1, l2.getStyles().length);
        FeatureTypeStyle fts2 = l2.getStyles()[0].featureTypeStyles().get(0);
        assertThat(fts2.featureTypeNames(), Matchers.empty());
        LineSymbolizer line = SLD.lineSymbolizer(fts2);
        assertEquals("#000000", line.getStroke().getColor().evaluate(null, String.class));
    }

    @Test
    public void testCssStyleIsAppliedToLayer() throws Exception {
        String wmsRequest = "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&format=image/png&"
                + "query_layers=sf:AggregateGeoFeature&layers=sf:AggregateGeoFeature&info_format=text/xml&"
                + "feature_count=10&x=50&y=50&width=101&height=101&bbox=70.9,30.9,73.1,33.0";

        // GetFeatureInfo near a point but without overlapping it
        Document responseWithDefaultStyle = getAsDOM(wmsRequest);
        NodeList featuresWithDefaultStyle = responseWithDefaultStyle.getElementsByTagName("gml:featureMember");
        assertEquals(0, featuresWithDefaultStyle.getLength());

        // GetFeatureInfo in the same point but with a style that increases the mark size so that
        // now the point should overlap the feature marker
        String css = "@styleName 'sf:AggregateGeoFeature'; *{mark: symbol(square); mark-size: 100px;}";
        Document responseWithCssStyle = getAsDOM(wmsRequest + "&style_format=css&style_body=" + css);
        NodeList featuresWithCssStyle = responseWithCssStyle.getElementsByTagName("gml:featureMember");
        assertEquals(1, featuresWithCssStyle.getLength());
    }

    @Test
    public void testZoomLevelEquals() throws Exception {
        String css = "[@z = 10] {stroke: black}";
        StyledLayerDescriptor sld = Styles.handler(CssHandler.FORMAT).parse(css, null, null, null);
        Rule rule = ((NamedLayer) sld.getStyledLayers()[0])
                .getStyles()[0]
                .featureTypeStyles()
                .get(0)
                .rules()
                .get(0);
        assertEquals(543262, rule.getMinScaleDenominator(), 1d);
        assertEquals(1086524, rule.getMaxScaleDenominator(), 1d);
    }

    @Test
    public void testZoomLevelEqualsUTM32N() throws Exception {
        String css = "@tileMatrixSet 'UTM32WGS84Quad'; [@z = 10] {stroke: black}";
        StyledLayerDescriptor sld = Styles.handler(CssHandler.FORMAT).parse(css, null, null, null);
        Rule rule = ((NamedLayer) sld.getStyledLayers()[0])
                .getStyles()[0]
                .featureTypeStyles()
                .get(0)
                .rules()
                .get(0);
        assertEquals(271176, rule.getMinScaleDenominator(), 1d);
        assertEquals(542352, rule.getMaxScaleDenominator(), 1d);
    }
}
