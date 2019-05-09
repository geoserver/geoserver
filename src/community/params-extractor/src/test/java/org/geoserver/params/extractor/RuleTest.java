/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Optional;
import org.junit.Test;

public class RuleTest {

    @Test
    public void testPositionRule() throws Exception {
        Rule ruleA =
                new RuleBuilder()
                        .withId("0")
                        .withPosition(3)
                        .withParameter("CQL_FILTER")
                        .withTransform("CFCC='$2'")
                        .build();
        Rule ruleB =
                new RuleBuilder()
                        .withId("1")
                        .withPosition(4)
                        .withParameter("CQL_FILTER")
                        .withTransform("CFCC='$2'")
                        .withCombine("$1 AND $2")
                        .build();
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/tiger/wms/H11/D68",
                        Utils.parseParameters(Optional.of("REQUEST=GetMap")));
        ruleA.apply(urlTransform);
        checkParametersSize(urlTransform, 2);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(urlTransform, "CQL_FILTER", "CFCC='H11'");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/tiger/wms/D68"));
        ruleB.apply(urlTransform);
        checkParametersSize(urlTransform, 2);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(urlTransform, "CQL_FILTER", "CFCC='H11' AND CFCC='D68'");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/tiger/wms"));
    }

    @Test
    public void testMatchRule() throws Exception {
        Rule rule =
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+)/([^/]+))$")
                        .withParameter("CQL_FILTER")
                        .withTransform("CFCC='$2' AND CFCC='$3'")
                        .build();
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/tiger/wms/H11/D68",
                        Utils.parseParameters(Optional.of("REQUEST=GetMap")));
        rule.apply(urlTransform);
        checkParametersSize(urlTransform, 2);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(urlTransform, "CQL_FILTER", "CFCC='H11' AND CFCC='D68'");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/tiger/wms"));
    }

    @Test
    public void testCombineWithRepeat() throws Exception {
        Rule rule =
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+))$")
                        .withParameter("CQL_FILTER")
                        .withTransform("CFCC='$2'")
                        .withCombine("$1;$2")
                        .withRepeat(true)
                        .build();
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/wms/H11",
                        Utils.parseParameters(Optional.of("REQUEST=GetMap&LAYERS=tiger,tiger")));
        rule.apply(urlTransform);
        checkParametersSize(urlTransform, 3);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(urlTransform, "CQL_FILTER", "CFCC='H11';CFCC='H11'");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/wms"));
    }

    @Test
    public void testMatchRuleWithExistingParameter() throws Exception {
        Rule rule =
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+)/([^/]+))$")
                        .withParameter("CQL_FILTER")
                        .withTransform("CFCC='$2' AND CFCC='$3'")
                        .withCombine("$1 OR ($2)")
                        .build();
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/tiger/wms/H11/D68",
                        Utils.parseParameters(
                                Optional.of("REQUEST=GetMap&CQL_FILTER=CFCC%3D%27Y56%27")));
        rule.apply(urlTransform);
        checkParametersSize(urlTransform, 2);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(
                urlTransform, "CQL_FILTER", "CFCC='Y56' OR (CFCC='H11' AND CFCC='D68')");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/tiger/wms"));
    }

    @Test
    public void testMatchRuleWithExistingParameterDifferentCases() throws Exception {
        Rule rule =
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+)/([^/]+))$")
                        .withParameter("CQL_filter")
                        .withTransform("CFCC='$2' AND CFCC='$3'")
                        .withCombine("$1 OR ($2)")
                        .build();
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/tiger/wms/H11/D68",
                        Utils.parseParameters(
                                Optional.of("REQUEST=GetMap&cql_filter=CFCC%3D%27Y56%27")));
        rule.apply(urlTransform);
        checkParametersSize(urlTransform, 2);
        checkParameterWithValue(urlTransform, "REQUEST", "GetMap");
        checkParameterWithValue(
                urlTransform, "cql_filter", "CFCC='Y56' OR (CFCC='H11' AND CFCC='D68')");
        assertThat(urlTransform.getRequestUri(), is("/geoserver/tiger/wms"));
    }

    private void checkParametersSize(UrlTransform urlTransform, int expectedSize) {
        assertThat(urlTransform.getParameters().size(), is(expectedSize));
    }

    private void checkParameterWithValue(UrlTransform urlTransform, String name, String value) {
        String[] foundValue = urlTransform.getParameters().get(name);
        assertThat(foundValue, notNullValue());
        assertThat(foundValue[0], is(value));
    }
}
