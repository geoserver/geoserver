/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.wms_1_1_1.GetLegendGraphicTest;
import org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.junit.Before;
import org.junit.Test;

public class FeatureCountLegendGraphicTest extends WMSTestSupport {

    private static final QName SF_STATES = new QName(MockData.SF_URI, "states", MockData.SF_PREFIX);

    private LegendGraphicBuilder legendProducer;

    private List<Rule[]> ruleSets = new ArrayList<>();

    private GetLegendGraphicRequest lastRequest;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("Population", "Population.sld", GetMapIntegrationTest.class, catalog);
        testData.addStyle(
                "PopulationElse",
                "PopulationElse.sld",
                FeatureCountLegendGraphicTest.class,
                catalog);
        testData.addStyle(
                "scaleDependent", "scaleDependent.sld", GetLegendGraphicTest.class, catalog);
        testData.addVectorLayer(
                SF_STATES,
                Collections.EMPTY_MAP,
                "states.properties",
                GetMapIntegrationTest.class,
                catalog);
    }

    @Before
    public void setupLegendProducer() throws Exception {
        this.ruleSets.clear();
        this.legendProducer =
                new BufferedImageLegendGraphicBuilder() {
                    public String getContentType() {
                        return "image/png";
                    }

                    @Override
                    protected Rule[] updateRuleTitles(
                            FeatureCountProcessor processor,
                            LegendRequest legend,
                            Rule[] applicableRules) {
                        Rule[] updatedRules =
                                super.updateRuleTitles(processor, legend, applicableRules);
                        FeatureCountLegendGraphicTest.this.ruleSets.add(updatedRules);
                        return updatedRules;
                    }
                };
    }

    @Test
    public void testBasicPolygonsNoCount() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&format=image/png");

        assertEquals(0, ruleSets.size());
    }

    @Test
    public void testBasicPolygonsNoLabels() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&style="
                        + "&format=image/png&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=30,0,40,10"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true;forceLabels:off");
        assertEquals(0, ruleSets.size());
    }

    @Test
    public void testBasicPolygonsNoFeatures() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&style="
                        + "&format=image/png&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=30,0,40,10"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        assertEquals(1, rules.length);
        assertLabel("(0)", rules[0]);
    }

    @Test
    public void testBasicPolygonsTwoFeatures() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&style="
                        + "&format=image/png&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-2.4,1.4,0.4,4.2"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        assertEquals(1, rules.length);
        assertLabel("(2)", rules[0]);
    }

    @Test
    public void testBasicPolygonsTwoFeaturesWms13() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.3.0&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&style="
                        + "&format=image/png&CRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=1.4,-2.4,4.2,0.4"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        assertEquals(1, rules.length);
        assertLabel("(2)", rules[0]);
    }

    @Test
    public void testBasicPolygonsAllFeatures() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&style="
                        + "&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-180,-90,180,90"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        assertEquals(1, rules.length);
        assertLabel("(3)", rules[0]);
    }

    @Test
    public void testStatesFull() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesElse() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=PopulationElse&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(3, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("Others (23)", rules[2]);
    }

    @Test
    public void testStatesMissingBbox() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&width=550&height=250&srs=EPSG:4326"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesMissingHeightWidth() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&srs=EPSG:4326&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesMissingHeightWidthSrs() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesMissingBboxSrs() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&width=550&height=250"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesMissingHeightWidthBboxSrs() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesMissingHeightWidthBboxSrsOnWMS13() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.3.0&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (10)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (23)", rules[2]);
        // this is the rule for outline and text symbolizer, Alaska and Hawaii are not in the
        // map but Washington DC is and it's not a state (50 - 2 + 1)
        assertLabel("(49)", rules[3]);
    }

    @Test
    public void testStatesCqlFilter() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true"
                        + "&CQL_FILTER=PERSONS < 2000000");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (0)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (0)", rules[2]);
        assertLabel("(16)", rules[3]);
    }

    @Test
    public void testStatesCqlFilterHideEmptyRules() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true;"
                        + GetLegendGraphicRequest.HIDE_EMPTY_RULES
                        + ":true"
                        + "&CQL_FILTER=PERSONS < 2000000");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(2, rules.length);
        assertLabel("< 2M (16)", rules[0]);
        assertLabel("(16)", rules[1]);
    }

    @Test
    public void testStatesCqlFilterHideEmptyRulesWithoutCount() throws Exception {
        runGetLegendGraphics(
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50"
                        + "&legend_options="
                        + GetLegendGraphicRequest.HIDE_EMPTY_RULES
                        + ":true"
                        + "&CQL_FILTER=PERSONS < 2000000");
        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(2, rules.length);
        assertLabel("< 2M", rules[0]);
        assertLabel("", rules[1]);
    }

    @Test
    public void testStatesMatchFirst() throws Exception {
        String requestURL =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=Population&width=550&height=250&srs=EPSG:4326" //
                        + "&bbox="
                        + "-130,24,-66,50" //
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true"
                        + "&CQL_FILTER=PERSONS < 2000000";

        Map rawKvp = (Map) caseInsensitiveKvp(KvpUtils.parseQueryString(requestURL));
        Map kvp = parseKvp(rawKvp);
        GetLegendGraphicKvpReader reader = new GetLegendGraphicKvpReader(getWMS());
        GetLegendGraphicRequest request = reader.read(reader.createRequest(), kvp, rawKvp);

        // switch the FTS to match first, the last rule should never be hit
        final LegendRequest legend = request.getLegends().get(0);
        final Style style = legend.getStyle();
        DuplicatingStyleVisitor matchFirstCloner =
                new DuplicatingStyleVisitor() {
                    public void visit(FeatureTypeStyle fts) {
                        super.visit(fts);
                        FeatureTypeStyle copy = (FeatureTypeStyle) pages.peek();
                        copy.getOptions()
                                .put(
                                        FeatureTypeStyle.KEY_EVALUATION_MODE,
                                        FeatureTypeStyle.VALUE_EVALUATION_MODE_FIRST);
                    };
                };
        style.accept(matchFirstCloner);
        legend.setStyle((Style) matchFirstCloner.getCopy());

        // run
        legendProducer.buildLegendGraphic(request);
        this.lastRequest = request;

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(4, rules.length);
        assertLabel("2M - 4M (0)", rules[0]);
        assertLabel("< 2M (16)", rules[1]);
        assertLabel("> 4M (0)", rules[2]);
        assertLabel("(0)", rules[3]);
    }

    @Test
    public void testCountOnGroup() throws Exception {
        String url =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer=nature&width=100&height=100"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002"
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true";
        runGetLegendGraphics(url);

        for (Rule[] rules : ruleSets) {
            logLabels(rules);
        }

        // two layers, one rule each
        assertEquals(2, ruleSets.size());
        assertEquals(1, ruleSets.get(0).length);
        assertEquals(1, ruleSets.get(1).length);

        // boring case, the title is just title
        assertLabel("title (1)", ruleSets.get(0)[0]);
        assertLabel("title (1)", ruleSets.get(1)[0]);
    }

    @Test
    public void testScaleDependentHittingScale() throws Exception {
        // somewhere around 60k
        testScaleDependent(
                "-109.11157608032227,36.97002410888672,-108.97974014282227,37.02667236328125",
                "TheRule (4)");
    }

    @Test
    public void testScaleDependentBelowMinScale() throws Exception {
        // around 4k
        testScaleDependent(
                "-109.05228853225708,36.994850635528564,-109.04404878616333,36.99839115142822",
                "TheRule (0)");
    }

    @Test
    public void testScaleDependentAboveMaxScale() throws Exception {
        // around 273k
        testScaleDependent(
                "-109.31121826171875,36.88041687011719,-108.78387451171875,37.10700988769531",
                "TheRule (0)");
    }

    public void testScaleDependent(String bboxSpecification, String expectedLabel)
            throws Exception {
        // around 4k
        String requestURL =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic&format=image/png"
                        + "&layer="
                        + getLayerId(SF_STATES)
                        + "&style=scaleDependent&width=20&height=20&srs=EPSG:4326" //
                        + "&bbox="
                        + bboxSpecification
                        + "&legend_options="
                        + GetLegendGraphicRequest.COUNT_MATCHED_KEY
                        + ":true"
                        + "&srcwidht=768&srcheight=300";

        Map rawKvp = (Map) caseInsensitiveKvp(KvpUtils.parseQueryString(requestURL));
        Map kvp = parseKvp(rawKvp);
        GetLegendGraphicKvpReader reader = new GetLegendGraphicKvpReader(getWMS());
        GetLegendGraphicRequest request = reader.read(reader.createRequest(), kvp, rawKvp);

        // run
        legendProducer.buildLegendGraphic(request);
        this.lastRequest = request;

        assertEquals(1, ruleSets.size());
        Rule[] rules = ruleSets.get(0);
        logLabels(rules);
        assertEquals(1, rules.length);
        assertLabel(expectedLabel, rules[0]);
    }

    private void logLabels(Rule[] rules) {
        LOGGER.log(Level.INFO, lastRequest.toString());
        for (Rule rule : rules) {
            LOGGER.log(Level.INFO, LegendUtils.getRuleLabel(rule, lastRequest));
        }
    }

    private void assertLabel(String expected, Rule rule) {
        String actual = LegendUtils.getRuleLabel(rule, lastRequest);
        assertThat(actual, equalTo(expected));
    }

    private GetLegendGraphicRequest runGetLegendGraphics(String requestURL) throws Exception {
        Map rawKvp = (Map) caseInsensitiveKvp(KvpUtils.parseQueryString(requestURL));
        Map kvp = parseKvp(rawKvp);
        GetLegendGraphicKvpReader reader = new GetLegendGraphicKvpReader(getWMS());
        GetLegendGraphicRequest request = reader.read(reader.createRequest(), kvp, rawKvp);
        legendProducer.buildLegendGraphic(request);
        this.lastRequest = request;
        return request;
    }
}
