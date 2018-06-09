/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.onlineTest.support.AbstractDataReferenceWfsTest;
import org.junit.Test;
import org.w3c.dom.Document;

public abstract class WfsOnlineTest extends AbstractDataReferenceWfsTest {

    public WfsOnlineTest() throws Exception {
        super();
    }

    /** URI for om namespace. */
    protected static final String OM_URI = "http://www.opengis.net/om/1.0";

    /** Schema URL for observation and measurements */
    protected static final String OM_SCHEMA_LOCATION_URL =
            "http://schemas.opengis.net/om/1.0.0/observation.xsd";

    @Test
    public void testNoPrimaryKey() {
        String path =
                "wfs?request=GetFeature&version=1.1.0&typename=gsml:ShearDisplacementStructure&featureid=gsml.sheardisplacementstructure.46216";
        Document doc = getAsDOM(path);
        LOGGER.info(prettyString(doc));
        assertXpathCount(
                2,
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent",
                doc);

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\">"
                        + "<wfs:Query typeName=\"gsml:ShearDisplacementStructure\">"
                        + "    <ogc:Filter>"
                        + "          <ogc:PropertyIsEqualTo>"
                        + "              <ogc:Literal>E</ogc:Literal>"
                        + "              <ogc:PropertyName>gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>"
                        + "          </ogc:PropertyIsEqualTo>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo(
                "E",
                "//gsml:ShearDisplacementStructure[@gml:id='gsml.sheardisplacementstructure.46216']/gsml:geologicHistory/gsml:DisplacementEvent/gsml:incrementalDisplacement/gsml:DisplacementValue/gsml:hangingWallDirection/gsml:CGI_LinearOrientation/gsml:descriptiveOrientation/gsml:CGI_TermValue/gsml:value",
                doc);
    }
}
