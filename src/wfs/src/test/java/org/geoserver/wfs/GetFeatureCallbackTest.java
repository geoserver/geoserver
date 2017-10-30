/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.transform.TransformFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.List;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetFeatureCallbackTest extends WFSTestSupport {

    private GetFeatureCallbackTester tester;

    @Before
    public void getTester() throws Exception {
        tester = (GetFeatureCallbackTester) applicationContext.getBean(GetFeatureCallbackTester.class);
        tester.clear();
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wfs/GetFeatureCallbackTesterContext.xml");
    }

    @Test
    public void testNoOp() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
        print(doc);
        assertXpathEvaluatesTo("15", "count(//cdf:Fifteen)", doc);
    }

    @Test
    public void testAlterQuery() throws Exception {
        tester.contextConsumer = (GetFeatureContext ctx) -> {
            Query query = new Query(ctx.getQuery());
            try {
                query.setFilter(CQL.toFilter("NAME = 'Main Street'"));
                ctx.setQuery(query);
            } catch (CQLException e) {
                throw new RuntimeException(e);
            }

        };
        Document doc = getAsDOM("wfs?request=GetFeature&typename=" + getLayerId(ROAD_SEGMENTS) + "&version=1.1.0&service=wfs");
        print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:RoadSegments)", doc);
        assertXpathEvaluatesTo("Main Street", "//cite:RoadSegments/cite:NAME", doc);
    }

}
