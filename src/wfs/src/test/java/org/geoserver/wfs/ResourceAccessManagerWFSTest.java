/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceAccessManagerWFSTest extends WFSTestSupport {

    static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    static final String INSERT_RESTRICTED_STREET =
            """
            <wfs:Transaction service="WFS" version="1.0.0"
              xmlns:wfs="http://www.opengis.net/wfs" xmlns:cite="http://www.opengis.net/cite"
              xmlns:gml="http://www.opengis.net/gml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd ">
              <wfs:Insert>
                <cite:Buildings fid="Buildings.123">
                  <cite:the_geom>
                    <gml:MultiPolygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                      <gml:polygonMember>
                        <gml:Polygon>
                          <gml:outerBoundaryIs>
                            <gml:LinearRing>
                              <gml:coordinates cs="," decimal="."
                                ts=" " xmlns:gml="http://www.opengis.net/gml">0.0020,0.0008 0.0020,0.0010
                                0.0024,0.0010 0.0024,0.0008 0.0020,0.0008</gml:coordinates>
                            </gml:LinearRing>
                          </gml:outerBoundaryIs>
                        </gml:Polygon>
                      </gml:polygonMember>
                    </gml:MultiPolygon>
                  </cite:the_geom>
                  <cite:FID>151</cite:FID>
                  <cite:ADDRESS>123 Restricted Street</cite:ADDRESS>
                </cite:Buildings>
              </wfs:Insert>
            </wfs:Transaction>""";

    static final String UPDATE_ADDRESS =
            """
            <wfs:Transaction service="WFS" version="1.1.0"
              xmlns:cite="http://www.opengis.net/cite"
              xmlns:ogc="http://www.opengis.net/ogc"
              xmlns:wfs="http://www.opengis.net/wfs">
              <wfs:Update typeName="cite:Buildings">
                <wfs:Property>
                  <wfs:Name>ADDRESS</wfs:Name>
                  <wfs:Value>123 ABC Street</wfs:Value>
                </wfs:Property>
              </wfs:Update>
            </wfs:Transaction>""";

    static final String DELETE_ADDRESS =
            """
            <wfs:Transaction service="WFS" version="1.1.0"
              xmlns:cite="http://www.opengis.net/cite"
              xmlns:ogc="http://www.opengis.net/ogc"
              xmlns:wfs="http://www.opengis.net/wfs"\
              xmlns:gml="http://www.opengis.net/gml">
              <wfs:Delete typeName="cite:Buildings">\
              <ogc:Filter>
                <ogc:BBOX>
                    <ogc:PropertyName>the_geom</ogc:PropertyName>
                    <gml:Envelope srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                       <gml:lowerCorner>-180 -90</gml:lowerCorner>
                       <gml:upperCorner>180 90</gml:upperCorner>
                    </gml:Envelope>
                  </ogc:BBOX>
              </ogc:Filter>
              </wfs:Delete>
            </wfs:Transaction>""";

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.BUILDINGS);
    }

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wfs/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_readfilter", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite,ROLE_DUMMY", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_readatts", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_readattsnf", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_insertfilter", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_writefilter", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_writeatts", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_mixed", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_nowrite_challenge", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // ------

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings = catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));

        // limits for mr readfilter
        Filter fid113 = ff.equal(ff.property("FID"), ff.literal("113"), false);
        tam.putLimits("cite_readfilter", buildings, new VectorAccessLimits(CatalogMode.HIDE, null, fid113, null, null));

        // limits for mr readatts (both limited read attributes and features)
        List<PropertyName> readAtts = Arrays.asList(ff.property("the_geom"), ff.property("FID"));
        tam.putLimits(
                "cite_readatts", buildings, new VectorAccessLimits(CatalogMode.HIDE, readAtts, fid113, null, null));

        // limits the attributes, but specifies no filtering
        tam.putLimits(
                "cite_readattsnf",
                buildings,
                new VectorAccessLimits(CatalogMode.HIDE, readAtts, Filter.INCLUDE, null, Filter.INCLUDE));

        // disallow writing on Restricted Street
        addRestrictedStreetLimit(tam, CatalogMode.HIDE, "cite_insertfilter");

        // allows writing only on 113
        tam.putLimits(
                "cite_writefilter", buildings, new VectorAccessLimits(CatalogMode.HIDE, null, null, null, fid113));

        // disallow writing on the ADDRESS attribute
        List<PropertyName> writeAtts = Arrays.asList(ff.property("the_geom"), ff.property("FID"));
        tam.putLimits(
                "cite_writeatts", buildings, new VectorAccessLimits(CatalogMode.HIDE, null, null, writeAtts, null));

        // user with mixed mode access
        tam.putLimits(
                "cite_mixed",
                buildings,
                new VectorAccessLimits(CatalogMode.MIXED, null, Filter.EXCLUDE, null, Filter.EXCLUDE));

        // disallow write
        tam.putLimits(
                "cite_nowrite_challenge",
                buildings,
                new VectorAccessLimits(CatalogMode.CHALLENGE, null, Filter.INCLUDE, null, Filter.EXCLUDE));
        tam.putLimits(
                "anonymous",
                buildings,
                new VectorAccessLimits(CatalogMode.CHALLENGE, null, Filter.INCLUDE, null, Filter.EXCLUDE));
    }

    private void addRestrictedStreetLimit(TestResourceAccessManager tam, CatalogMode mode, String user) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings = catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        Filter restrictedStreet = ff.not(ff.like(ff.property("ADDRESS"), "*Restricted Street*", "*", "?", "\\"));
        tam.putLimits(user, buildings, new VectorAccessLimits(mode, null, null, null, restrictedStreet));
    }

    @Test
    public void testNoLimits() throws Exception {
        // no limits, should see all of the attributes and rows
        setRequestAuth("cite", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        print(doc);
        assertXpathEvaluatesTo("2", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("2", "count(//cite:ADDRESS)", doc);
    }

    @Test
    public void testReadFilter() throws Exception {
        // should only see one feature and all attributes
        setRequestAuth("cite_readfilter", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("113", "//cite:FID", doc);
        assertXpathEvaluatesTo("1", "count(//cite:ADDRESS)", doc);
    }

    @Test
    public void testReadFilterReproject() throws Exception {
        // should only see one feature and all attributes
        setRequestAuth("cite_readfilter", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS)
                + "&srsName=EPSG:4269");
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("113", "//cite:FID", doc);
        assertXpathEvaluatesTo("1", "count(//cite:ADDRESS)", doc);
        assertXpathEvaluatesTo("http://www.opengis.net/gml/srs/epsg.xml#4269", "//gml:MultiPolygon/@srsName", doc);
    }

    @Test
    public void testFilterAttribute() throws Exception {
        // should only see one feature
        setRequestAuth("cite_readatts", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("113", "//cite:FID", doc);
        assertXpathEvaluatesTo("0", "count(//cite:ADDRESS)", doc);
    }

    @Test
    public void testDescribeLimitedAttributes() throws Exception {
        // this one should see all attributes
        setRequestAuth("admin", "geoserver");
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.0.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='the_geom'])", doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='FID'])", doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='ADDRESS'])", doc);

        // this one should see only 2
        setRequestAuth("cite_readatts", "cite");
        doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.0.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='the_geom'])", doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='FID'])", doc);
        assertXpathEvaluatesTo("0", "count(//xsd:element[@name='ADDRESS'])", doc);

        // paranoid check to make sure there is no caching
        setRequestAuth("admin", "geoserver");
        doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.0.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='the_geom'])", doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='FID'])", doc);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name='ADDRESS'])", doc);
    }

    @Test
    public void testCapabilitiesMixed() throws Exception {
        setRequestAuth("admin", "geoserver");
        Document doc = getAsDOM("cite/wfs?request=GetCapabilities&version=1.1.0&service=wfs");
        print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureType[wfs:Name='cite:Buildings'])", doc);

        // this one not
        setRequestAuth("cite_mixed", "cite");
        doc = getAsDOM("cite/wfs?request=GetCapabilities&version=1.1.0&service=wfs");
        print(doc);
        assertXpathEvaluatesTo("0", "count(//wfs:FeatureType[wfs:Name='cite:Buildings'])", doc);
    }

    @Test
    public void testDescribeMixed() throws Exception {
        // this one should see all types
        setRequestAuth("admin", "geoserver");
        Document doc = getAsDOM("cite/wfs?request=DescribeFeatureType&version=1.1.0&service=wfs");
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//xsd:complexType[@name='BuildingsType'])", doc);

        // this one not
        setRequestAuth("cite_mixed", "cite");
        doc = getAsDOM("cite/wfs?request=DescribeFeatureType&version=1.1.0&service=wfs");
        // print(doc);
        assertXpathEvaluatesTo("0", "count(//xsd:complexType[@name='BuildingsType'])", doc);

        // and a direct access will fail with an auth challenge
        setRequestAuth("cite_mixed", "cite");
        MockHttpServletResponse response =
                getAsServletResponse("cite/wfs?request=DescribeFeatureType&version=1.1.0&service=wfs&typeName="
                        + getLayerId(SystemTestData.BUILDINGS));
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testFilterRequestedAttribute() throws Exception {
        // should only see one feature
        setRequestAuth("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS)
                + "&propertyName=FID,ADDRESS");
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//ows:ExceptionText", doc);
        Pattern pattern = Pattern.compile(".*ADDRESS.*not available.*", Pattern.MULTILINE | Pattern.DOTALL);
        assertTrue(pattern.matcher(message).matches());
    }

    @Test
    public void testExtraAttributesNoFilter() throws Exception {
        // should only see one feature
        setRequestAuth("cite_readattsnf", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&typeName="
                + getLayerId(SystemTestData.BUILDINGS)
                + "&propertyName=FID,ADDRESS");
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//ows:ExceptionText", doc);
        Pattern pattern = Pattern.compile(".*ADDRESS.*not available.*", Pattern.MULTILINE | Pattern.DOTALL);
        assertTrue(pattern.matcher(message).matches());
    }

    @Test
    public void testLimitAttributesNoFilter() throws Exception {
        // should only see one feature
        setRequestAuth("cite_readattsnf", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.1.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);

        assertXpathEvaluatesTo("0", "count(//cite:ADDRESS)", doc);
    }

    @Test
    public void testInsertNoLimits() throws Exception {
        setRequestAuth("cite", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//ogc:FeatureId)", dom);
        assertXpathEvaluatesTo("new0", "//ogc:FeatureId/@fid", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:SUCCESS)", dom);
    }

    @Test
    public void testInsertRestrictedChallenge() throws Exception {
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        addRestrictedStreetLimit(tam, CatalogMode.CHALLENGE, "cite_insertfilter");
        try {
            setRequestAuth("cite_insertfilter", "cite");
            MockHttpServletResponse response = postAsServletResponse("wfs", INSERT_RESTRICTED_STREET);
            // this should state it's not authorized, since the user was authenticaed
            assertEquals(403, response.getStatus());
        } finally {
            addRestrictedStreetLimit(tam, CatalogMode.HIDE, "cite_insertfilter");
        }
    }

    @Test
    public void testInsertRestrictedChallengeAnonymous() throws Exception {
        setRequestAuth(null, null);
        MockHttpServletResponse response = postAsServletResponse("wfs", INSERT_RESTRICTED_STREET);
        // this should prompt to authenticat since the user was the anonymous one
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testInsertRestricted() throws Exception {
        setRequestAuth("cite_insertfilter", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:FAILED)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//wfs:Message", dom);
        assertTrue(message.matches(".*write restrictions.*"));
    }

    @Test
    public void testInsertAttributeRestricted() throws Exception {
        setRequestAuth("cite_writeatts", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:FAILED)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//wfs:Message", dom);
        assertTrue(message.matches(".*write protected.*ADDRESS.*"));
    }

    @Test
    public void testUpdateNoLimits() throws Exception {
        setRequestAuth("cite", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        // print(dom);
        assertXpathEvaluatesTo("2", "//wfs:totalUpdated", dom);
    }

    @Test
    public void testUpdateLimitWrite() throws Exception {
        setRequestAuth("cite_writefilter", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        // print(dom)
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);

        // double check
        setRequestAuth("cite", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);

        // check this one has been updated
        assertXpathEvaluatesTo("123 ABC Street", "//cite:Buildings[cite:FID = '113']/cite:ADDRESS", doc);
        // but the other did not
        assertXpathEvaluatesTo("215 Main Street", "//cite:Buildings[cite:FID = '114']/cite:ADDRESS", doc);
    }

    @Test
    public void testUpdateAttributeRestricted() throws Exception {
        setRequestAuth("cite_writeatts", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        // print(dom);

        // not sure why CITE tests make us throw an exception for this one instead of using
        // in transaction reporting...
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//ows:ExceptionText", dom);
        assertTrue(message.matches(".*write protected.*ADDRESS.*"));
    }

    @Test
    public void testUpdateNoWrite() throws Exception {
        setRequestAuth("cite_nowrite_challenge", "cite");
        MockHttpServletResponse response = postAsServletResponse("wfs", UPDATE_ADDRESS);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testUpdateNoWriteAnonymous() throws Exception {
        setRequestAuth(null, null);
        MockHttpServletResponse response = postAsServletResponse("wfs", UPDATE_ADDRESS);
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testDeleteLimitWrite() throws Exception {
        setRequestAuth("cite_writefilter", "cite");
        Document dom = postAsDOM("wfs", DELETE_ADDRESS);
        // print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalDeleted", dom);

        // double check
        setRequestAuth("cite", "cite");
        Document doc = getAsDOM(
                "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);

        // check this one has been deleted
        assertXpathEvaluatesTo("0", "count(//cite:Buildings[cite:FID = '113'])", doc);
        // but the other did not
        assertXpathEvaluatesTo("1", "count(//cite:Buildings[cite:FID = '114'])", doc);
    }

    @Test
    public void testDeleteNoWrite() throws Exception {
        setRequestAuth("cite_nowrite_challenge", "cite");
        MockHttpServletResponse response = postAsServletResponse("wfs", DELETE_ADDRESS);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteNoWriteAnonymous() throws Exception {
        setRequestAuth(null, null);
        MockHttpServletResponse response = postAsServletResponse("wfs", DELETE_ADDRESS);
        assertEquals(401, response.getStatus());
    }
}
