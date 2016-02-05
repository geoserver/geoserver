/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RuleTest {

    @Test
    public void testPositionRule() throws Exception {
        Rule ruleA = new RuleBuilder().withId("0")
                .withPosition(3)
                .withParameter("CQL_FILTER")
                .withTransform("CFCC='$2'")
                .build();
        Rule ruleB = new RuleBuilder().withId("1")
                .withPosition(4)
                .withParameter("CQL_FILTER")
                .withTransform("CFCC='$2'")
                .withCombine("$1 AND $2")
                .build();
        UrlTransform urlTransform = new UrlTransform("/geoserver/tiger/wms/H11/D68", Utils.parseParameters("REQUEST=GetMap"));
        ruleA.apply(urlTransform);
        assertThat(urlTransform.toString(),
                is("/geoserver/tiger/wms/D68?REQUEST=GetMap&CQL_FILTER=CFCC%3D%27H11%27"));
        ruleB.apply(urlTransform);
        assertThat(urlTransform.toString(),
                is("/geoserver/tiger/wms?REQUEST=GetMap&CQL_FILTER=CFCC%3D%27H11%27+AND+CFCC%3D%27D68%27"));
    }

    @Test
    public void testMatchRule() throws Exception {
        Rule rule = new RuleBuilder().withId("0")
                .withMatch("^.*?(/([^/]+)/([^/]+))$")
                .withParameter("CQL_FILTER")
                .withTransform("CFCC='$2' AND CFCC='$3'")
                .build();
        UrlTransform urlTransform = new UrlTransform("/geoserver/tiger/wms/H11/D68", Utils.parseParameters("REQUEST=GetMap"));
        rule.apply(urlTransform);
        assertThat(urlTransform.toString(),
                is("/geoserver/tiger/wms?REQUEST=GetMap&CQL_FILTER=CFCC%3D%27H11%27+AND+CFCC%3D%27D68%27"));
    }

    @Test
    public void testMatchRuleWithExistingParameter() throws Exception {
        Rule rule = new RuleBuilder().withId("0")
                .withMatch("^.*?(/([^/]+)/([^/]+))$")
                .withParameter("CQL_FILTER")
                .withTransform("CFCC='$2' AND CFCC='$3'")
                .withCombine("$1 OR ($2)")
                .build();
        UrlTransform urlTransform = new UrlTransform("/geoserver/tiger/wms/H11/D68",
                Utils.parseParameters("REQUEST=GetMap&CQL_FILTER=CFCC%3D%27Y56%27"));
        rule.apply(urlTransform);
        assertThat(urlTransform.toString(),
                is("/geoserver/tiger/wms?REQUEST=GetMap&CQL_FILTER=CFCC%3D%27Y56%27+OR+%28CFCC%3D%27H11%27+AND+CFCC%3D%27D68%27%29"));
    }

    @Test
    public void testMatchRuleWithExistingParameterDifferentCases() throws Exception {
        Rule rule = new RuleBuilder().withId("0")
                .withMatch("^.*?(/([^/]+)/([^/]+))$")
                .withParameter("CQL_filter")
                .withTransform("CFCC='$2' AND CFCC='$3'")
                .withCombine("$1 OR ($2)")
                .build();
        UrlTransform urlTransform = new UrlTransform("/geoserver/tiger/wms/H11/D68",
                Utils.parseParameters("REQUEST=GetMap&cql_filter=CFCC%3D%27Y56%27"));
        rule.apply(urlTransform);
        assertThat(urlTransform.toString(),
                is("/geoserver/tiger/wms?REQUEST=GetMap&cql_filter=CFCC%3D%27Y56%27+OR+%28CFCC%3D%27H11%27+AND+CFCC%3D%27D68%27%29"));
    }
}
