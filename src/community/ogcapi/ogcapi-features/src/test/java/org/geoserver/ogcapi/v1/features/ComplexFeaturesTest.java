/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class ComplexFeaturesTest extends AbstractAppSchemaTestSupport {
    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Test
    public void testHTMLMappedFeature() throws Exception {
        Document doc = getAsJSoup("ogc/features/v1/collections/gsml:MappedFeature/items?f=text/html");

        // all the five root feature are present
        assertEquals(5, doc.select("ul[id=rootUL]").size());
        // nested features are present
        assertEquals(4, doc.select("li>span:containsOwn(GeologicUnitType)").size());
        assertEquals(6, doc.select("li>span:containsOwn(CompositionPartType)").size());
    }

    @Test
    public void testHTMLMappedFeatureRemovedInlineJS() throws Exception {
        String html = getAsString("ogc/features/v1/collections/gsml:MappedFeature/items?f=text/html");
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/common.js\"></script>"));
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/features.js\"></script>"));
        assertThat(html, not(containsString("<script>")));
    }
}
