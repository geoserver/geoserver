/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests predicates in x-paths Only works online with joining on.
 *
 * @author Niels Charlier
 */
public class XPathPredicateTest extends AbstractAppSchemaTestSupport {

    public static final String GETFEATURE_ATTRIBUTES =
            "service=\"WFS\" " //
                    + "version=\"2.0\" " //
                    + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                    + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                    + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                    + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                    + "xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                    + AbstractAppSchemaMockData.GSML_URI
                    + " "
                    + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                    + "\""; // end of schemaLocation

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    /**
     * Making sure multi-valued attributes in nested features can be queried from the top level.
     * (GEOT-3156)
     */
    @Test
    public void testFiltering() {
        String xml =
                "<wfs:GetFeature "
                        + GETFEATURE_ATTRIBUTES
                        + ">"
                        + "    <wfs:Query typeNames=\"gsml:MappedFeature\">"
                        + "        <fes:Filter>"
                        + "            <fes:PropertyIsEqualTo>"
                        + "                <fes:Literal>Olivine basalt, tuff, microgabbro, minor sedimentary rocks</fes:Literal>"
                        + "                <fes:ValueReference>gsml:specification/gsml:GeologicUnit[gml:name='Yaugher Volcanic Group 2']/gml:description</fes:ValueReference>"
                        + "            </fes:PropertyIsEqualTo>"
                        + "        </fes:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathCount(2, "//gsml:MappedFeature", doc);
        assertXpathCount(
                1,
                "//wfs:member[1]/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit[gml:name='Yaugher Volcanic Group 2']",
                doc);
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//wfs:member[1]/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);
    }

    /**
     * Making sure multi-valued attributes in nested features can be queried from the top level.
     * (GEOT-3156)
     */
    @Test
    public void testGetPropertyValue() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&valueReference=gsml:specification/gsml:GeologicUnit[gml:name='Yaugher Volcanic Group 2']/gml:description");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathCount(2, "//wfs:member", doc);
    }
}
