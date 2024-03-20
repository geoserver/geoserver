/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.geoserver.catalog.Styles;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.SLD;
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
    public void testCssStyleIsAppliedToLayer() throws Exception {
        String wmsRequest =
                "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&format=image/png&"
                        + "query_layers=sf:AggregateGeoFeature&layers=sf:AggregateGeoFeature&info_format=text/xml&"
                        + "feature_count=10&x=50&y=50&width=101&height=101&bbox=70.9,30.9,73.1,33.0";

        // GetFeatureInfo near a point but without overlapping it
        Document responseWithDefaultStyle = getAsDOM(wmsRequest);
        NodeList featuresWithDefaultStyle =
                responseWithDefaultStyle.getElementsByTagName("gml:featureMember");
        assertEquals(0, featuresWithDefaultStyle.getLength());

        // GetFeatureInfo in the same point but with a style that increases the mark size so that
        // now the point should overlap the feature marker
        String css =
                "@styleName 'sf:AggregateGeoFeature'; *{mark: symbol(square); mark-size: 100px;}";
        Document responseWithCssStyle =
                getAsDOM(wmsRequest + "&style_format=css&style_body=" + css);
        NodeList featuresWithCssStyle =
                responseWithCssStyle.getElementsByTagName("gml:featureMember");
        assertEquals(1, featuresWithCssStyle.getLength());
    }
}
