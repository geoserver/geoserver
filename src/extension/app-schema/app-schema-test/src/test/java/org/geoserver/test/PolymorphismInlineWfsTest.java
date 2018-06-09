/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test polymorphism mixed with inline mappings in Geoserver app-schema.
 *
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class PolymorphismInlineWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected PolymorphismInlineMockData createTestData() {
        return new PolymorphismInlineMockData();
    }

    /**
     * Test getFeature. Previously when there are multi-valued properties mapped separately, where
     * the first attribute can be omitted upon conditions, the rest of the properties don't get
     * encoded correctly when the first attribute is omitted. This is to make sure the fix won't be
     * broken in the future. See GEOT-3304.
     */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:PolymorphicFeature");
        LOGGER.info(
                "WFS GetFeature&typename=ex:PolymorphicFeature response:\n" + prettyString(doc));
        assertXpathCount(6, "//ex:PolymorphicFeature", doc);

        // f1
        assertXpathEvaluatesTo("f1", "(//ex:PolymorphicFeature)[1]/@gml:id", doc);
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f1']/ex:firstValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f2
        assertXpathEvaluatesTo("f2", "(//ex:PolymorphicFeature)[2]/@gml:id", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "x",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue[2]/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f2']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f3
        assertXpathEvaluatesTo("f3", "(//ex:PolymorphicFeature)[3]/@gml:id", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "y",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue[2]/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f3']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f4
        assertXpathEvaluatesTo("f4", "(//ex:PolymorphicFeature)[4]/@gml:id", doc);
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f4']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:firstValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:firstValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f4']/ex:firstValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f5
        assertXpathEvaluatesTo("f5", "(//ex:PolymorphicFeature)[5]/@gml:id", doc);
        assertXpathCount(2, "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "y",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "some:uri",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue[1]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue[2]/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "0",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f5']/ex:firstValue[2]/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);

        // f6
        assertXpathEvaluatesTo("f6", "(//ex:PolymorphicFeature)[6]/@gml:id", doc);
        assertXpathCount(1, "//ex:PolymorphicFeature[@gml:id='f6']/ex:firstValue", doc);
        assertXpathEvaluatesTo(
                "approximate",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:firstValue/gsml:CGI_TermValue/@gsml:qualifier",
                doc);
        assertXpathEvaluatesTo(
                "1000",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:firstValue/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "codespace",
                "//ex:PolymorphicFeature[@gml:id='f6']/ex:firstValue/gsml:CGI_TermValue/gsml:value/@codeSpace",
                doc);
    }
}
