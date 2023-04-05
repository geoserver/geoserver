/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class HTMLGetSimpleFeaturesResponseWFSTest extends TemplateComplexTestSupport {

    static final QName HTML_FEATURES =
            new QName(CiteTestData.CITE_URI, "HtmlFeatures", CiteTestData.CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addVectorLayer(
                HTML_FEATURES,
                Collections.emptyMap(),
                "HtmlFeatures.properties",
                getClass(),
                getCatalog());
        Catalog catalog = getCatalog();
        FeatureTypeInfo htmlFeatures =
                catalog.getFeatureTypeByName(
                        HTML_FEATURES.getPrefix(), HTML_FEATURES.getLocalPart());
        String htmlTemplate = "HTMLHtmlFeature.xhtml";
        setUpTemplate(
                "requestParam('html')='true'",
                SupportedFormat.HTML,
                htmlTemplate,
                "html-html-features",
                ".xhtml",
                HTML_FEATURES.getPrefix(),
                htmlFeatures);
    }

    /**
     * Tests if GetFeature response is escaped correctly
     *
     * @throws Exception
     */
    @Test
    public void testHtmlResponse() throws Exception {
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=cite:HtmlFeatures&outputFormat=");
        sb.append("text/html&cql_filter=NAME='Features1'&html=true");
        Document doc = getAsJSoup(sb.toString());
        assertEquals(1, doc.select("ul.nested li ul.nested li:contains(60°)").size());
    }
}
