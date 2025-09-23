/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2009 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test GetPropertyValue request, combined with local resolves
 *
 * <p>!! THIS TEST IS ONLY TO BE RUN ONLINE Property Files don't support sorting.
 *
 * @author Niels Charlier
 */
public class SortByTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    @Test
    public void testGetMappedFeature() {
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=2.0&outputFormat=gml32&typename=gsml:MappedFeature&sortBy=gml:name&featureID=mf1,mf2,mf3,mf4");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));

        checkSortByNameResponse(doc);
    }

    @Test
    public void testGetMappedFeatureMultipleSortby() throws Exception {
        String query =
                """
                <wfs:GetFeature service="WFS" version="2.0.0"
                    xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0"
                    xmlns:wfs="http://www.opengis.net/wfs/2.0"
                    xmlns:fes="http://www.opengis.net/fes/2.0"
                    xmlns:gml="http://www.opengis.net/gml/3.2">
                    <wfs:Query typeNames="gsml:MappedFeature" srsName="EPSG:4326">
                        <fes:Filter>
                            <fes:PropertyIsLike wildCard="*"
                                singleChar="." escapeChar="\\">
                                <fes:ValueReference>gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description
                                </fes:ValueReference>
                                <fes:Literal>*</fes:Literal>
                            </fes:PropertyIsLike>
                        </fes:Filter>
                        <fes:SortBy>
                            <fes:SortProperty>
                                <fes:ValueReference>gsml:MappedFeature/gml:name</fes:ValueReference>
                                <fes:SortOrder>ASC</fes:SortOrder>
                            </fes:SortProperty>
                            <fes:SortProperty>
                                <fes:ValueReference>gsml:MappedFeature</fes:ValueReference>
                                <fes:SortOrder>ASC</fes:SortOrder>
                            </fes:SortProperty>
                        </fes:SortBy>
                    </wfs:Query>
                </wfs:GetFeature>
                """;
        Document doc = postAsDOM("wfs?request=GetFeature&version=2.0&outputFormat=gml32", query);
        checkSortByNameResponse(doc);
    }

    private void checkSortByNameResponse(Document doc) {
        assertXpathEvaluatesTo("mf3", "//wfs:FeatureCollection/wfs:member[1]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo("mf1", "//wfs:FeatureCollection/wfs:member[2]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo("mf2", "//wfs:FeatureCollection/wfs:member[3]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo("mf4", "//wfs:FeatureCollection/wfs:member[4]/gsml:MappedFeature/@gml:id", doc);

        // check proper chaining
        assertXpathEvaluatesTo(
                "gu.25678", "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/@gml:id", doc);
        assertXpathEvaluatesTo(
                "gu.25682", "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit/@gml:id", doc);
        assertXpathEvaluatesTo("#gu.25678", "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href", doc);
    }
}
