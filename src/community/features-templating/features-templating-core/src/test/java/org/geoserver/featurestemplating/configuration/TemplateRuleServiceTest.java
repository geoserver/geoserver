/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class TemplateRuleServiceTest extends GeoServerSystemTestSupport {

    @Test
    public void testMultipleSave() {
        Catalog catalog = getCatalog();
        FeatureTypeInfo typeInfo =
                catalog.getFeatureTypeByName(MockData.CDF_PREFIX, MockData.FIFTEEN.getLocalPart());
        TemplateRuleService ruleService = new TemplateRuleService(typeInfo);
        TemplateRule rule = new TemplateRule();
        rule.setOutputFormat(SupportedFormat.HTML);
        rule.setTemplateName("html-fifteen-template");
        ruleService.saveRule(rule);
        TemplateRule rule2 = new TemplateRule();
        rule2.setOutputFormat(SupportedFormat.GEOJSON);
        rule2.setTemplateName("geojson-fifteen-template");
        ruleService.saveRule(rule2);
        Set<TemplateRule> rules = ruleService.getRules();
        assertEquals(2, rules.size());
        assertTrue(
                rules.stream().anyMatch(r -> r.getOutputFormat().equals(SupportedFormat.GEOJSON)));
        assertTrue(rules.stream().anyMatch(r -> r.getOutputFormat().equals(SupportedFormat.HTML)));
    }
}
