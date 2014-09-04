/*
 * Copyright (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;
import org.junit.Test;

/**
 * 
 * Test the proper encoding of duplicated/repeated features with Ids
 * 
 * @author Victor Tey, CSIRO Exploration and Mining
 */

public class FeatureGML32Test extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    @Test
    public void testGetMappedFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&outputFormat=gml32&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);
    }
  
    /**
     * Test content of GetFeature response.
     */
    @Test
    public void testGetFeatureWithFilter() throws Exception {
       
        String xml = //
            "<wfs:GetFeature " //
                    + "service=\"WFS\" " //
                    + "version=\"2.0\" " //
                    + "outputFormat=\"gml32\" " //                    
                    + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                    + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                    + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                    + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                    + ">" //
                    + "    <wfs:Query typeNames=\"gsml:MappedFeature\">" //
                    + "        <fes:Filter>" //
                    + "            <fes:PropertyIsEqualTo>" //
                    + "                <fes:ValueReference>gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description</fes:ValueReference>" //
                    + "                <fes:Literal>Olivine basalt</fes:Literal>" //
                    + "            </fes:PropertyIsEqualTo>" //
                    + "        </fes:Filter>" //
                    + "    </wfs:Query> " //
                    + "</wfs:GetFeature>";
          Document doc = postAsDOM("wfs", xml);
          LOGGER.info(prettyString(doc));

          assertXpathCount(1, "//gsml:MappedFeature", doc);
          assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }    
}
