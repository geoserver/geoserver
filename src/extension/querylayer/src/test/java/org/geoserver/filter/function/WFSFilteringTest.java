/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WFSFilteringTest extends WFSTestSupport {

    static final String QUERY_SINGLE = //
            """
                    <wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs"
                                    xmlns:cite="http://www.opengis.net/cite"
                                    xmlns:ogc="http://www.opengis.net/ogc"
                                    service="WFS" version="${version}">
                      <wfs:Query typeName="cite:Buildings">
                        <ogc:Filter>
                          <ogc:DWithin>
                            <ogc:PropertyName>the_geom</ogc:PropertyName>
                            <ogc:Function name="querySingle">
                               <ogc:Literal>cite:Streams</ogc:Literal>
                               <ogc:Literal>the_geom</ogc:Literal>
                               <ogc:Literal>INCLUDE</ogc:Literal>
                            </ogc:Function>
                            <ogc:Distance units="meter">${distance}</ogc:Distance>
                          </ogc:DWithin>
                        </ogc:Filter>
                      </wfs:Query>
                    </wfs:GetFeature>""";

    static final String QUERY_MULTI = //
            """
                    <wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs"
                      xmlns:cite="http://www.opengis.net/cite" \
                      xmlns:ogc="http://www.opengis.net/ogc"
                      service="WFS" version="${version}">
                      <wfs:Query typeName="cite:Buildings">
                        <ogc:Filter>
                          <ogc:DWithin>
                            <ogc:PropertyName>the_geom</ogc:PropertyName>
                            <ogc:Function name="collectGeometries">
                              <ogc:Function name="queryCollection">
                                <ogc:Literal>cite:RoadSegments</ogc:Literal>
                                <ogc:Literal>the_geom</ogc:Literal>
                                <ogc:Literal>NAME = 'Route 5'</ogc:Literal>
                              </ogc:Function>
                            </ogc:Function>
                            <ogc:Distance units="meter">0.001</ogc:Distance>
                          </ogc:DWithin>
                        </ogc:Filter>
                      </wfs:Query>
                    </wfs:GetFeature>""";

    @Test
    public void testSingleSmallDistance10() throws Exception {
        _testSingleSmallDistance("1.0.0");
    }

    @Test
    public void testSingleSmallDistance11() throws Exception {
        _testSingleSmallDistance("1.1.0");
    }

    void _testSingleSmallDistance(String version) throws Exception {
        String request = QUERY_SINGLE.replace("${distance}", "0.00000001").replace("${version}", version);
        Document doc = postAsDOM("wfs", request);
        print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("0", "count(cite:Buildings)", doc);
    }

    @Test
    public void testSingleLargeDistance10() throws Exception {
        _testSingleLargeDistance("1.0.0");
    }

    @Test
    public void testSingleLargeDistance11() throws Exception {
        _testSingleLargeDistance("1.1.0");
    }

    void _testSingleLargeDistance(String version) throws Exception {
        String request = QUERY_SINGLE.replace("${distance}", "0.001").replace("${version}", version);
        Document doc = postAsDOM("wfs", request);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
    }

    @Test
    public void testMultiple10() throws Exception {
        _testMultiple("1.0.0");
    }

    @Test
    public void testMultiple11() throws Exception {
        _testMultiple("1.1.0");
    }

    void _testMultiple(String version) throws Exception {
        Document doc = postAsDOM("wfs", QUERY_MULTI.replace("${version}", version));
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("2", "count(//cite:Buildings)", doc);
    }
}
