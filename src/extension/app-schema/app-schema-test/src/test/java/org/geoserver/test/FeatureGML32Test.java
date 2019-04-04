/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

/**
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
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&outputFormat=gml32&typename=gsml:MappedFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href", doc);
    }

    /** Test content of GetFeature response. */
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

    @Test
    public void testStoredQuery() throws Exception {
        String xml =
                "<wfs:CreateStoredQuery service='WFS' version='2.0.0' "
                        + "   xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "   xmlns:fes='http://www.opengis.net/fes/2.0' "
                        + "   xmlns:gml='http://www.opengis.net/gml/3.2' "
                        + "   xmlns:gsml='urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0'>"
                        + "   <wfs:StoredQueryDefinition id='myStoredQuery'> "
                        + "      <wfs:Parameter name='descr' type='xs:string'/> "
                        + "      <wfs:QueryExpressionText "
                        + "           returnFeatureTypes='gsml:MappedFeature' "
                        + "           language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression' "
                        + "           isPrivate='false'> "
                        + "         <wfs:Query typeNames=\"gsml:MappedFeature\"> "
                        + "            <fes:Filter> "
                        + "               <fes:PropertyIsEqualTo> "
                        + "                  <fes:ValueReference>gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description</fes:ValueReference> "
                        + "                  ${descr}"
                        + "               </fes:PropertyIsEqualTo> "
                        + "            </fes:Filter> "
                        + "         </wfs:Query> "
                        + "      </wfs:QueryExpressionText> "
                        + "   </wfs:StoredQueryDefinition> "
                        + "</wfs:CreateStoredQuery>";
        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:CreateStoredQueryResponse", doc.getDocumentElement().getNodeName());

        xml =
                "<wfs:GetFeature service='WFS' version='2.0.0' "
                        + "       xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' xmlns:fes='"
                        + FES.NAMESPACE
                        + "'>"
                        + "   <wfs:StoredQuery id='myStoredQuery'> "
                        + "      <wfs:Parameter name='descr'>"
                        + "        <fes:Literal>Olivine basalt</fes:Literal>"
                        + "      </wfs:Parameter> "
                        + "   </wfs:StoredQuery> "
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));

        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }

    /** Test encoding of a multivalued mapping with an xlink:href ClientProperty. */
    @Test
    public void testMultivaluedXlinkHref() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetFeature&typenames=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature, typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        // expect gsml:occurrence to appear twice for this feature with only @xlink:href
        assertXpathCount(
                2, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/@xlink:href", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[2]/@xlink:href",
                doc);
        // expect no nested features
        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature",
                doc);
    }
}
