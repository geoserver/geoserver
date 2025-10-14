/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class TransactionCallbackWFS20Test extends WFS20TestSupport {

    public static final String DELETE_ROAD_102 = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + "xmlns:cite=\"http://www.opengis.net/cite\" "
            + "xmlns:fes='"
            + FES.NAMESPACE
            + "' "
            + "xmlns:wfs='"
            + WFS.NAMESPACE
            + "' "
            + "xmlns:gml='"
            + GML.NAMESPACE
            + "'>"
            + " <wfs:Delete typeName=\"cite:RoadSegments\">"
            + "   <fes:Filter>"
            + "     <fes:PropertyIsEqualTo>"
            + "       <fes:ValueReference>FID</fes:ValueReference>"
            + "       <fes:Literal>102</fes:Literal>"
            + "     </fes:PropertyIsEqualTo>"
            + "   </fes:Filter>"
            + " </wfs:Delete>"
            + "</wfs:Transaction>";
    private TransactionCallbackTester plugin;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wfs/TransactionCallbackTestContext.xml");
    }

    @Before
    public void clearState() throws Exception {
        revertLayer(MockData.ROAD_SEGMENTS);
        plugin = (TransactionCallbackTester) applicationContext.getBean("transactionCallbackTester");
        plugin.clear();
    }

    @Test
    public void testInsert() throws Exception {
        // perform an insert
        String insert = "<wfs:Transaction service='WFS' version='2.0.0' "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:fes='"
                + FES.NAMESPACE
                + "' "
                + "xmlns:wfs='"
                + WFS.NAMESPACE
                + "' "
                + "xmlns:gml='"
                + GML.NAMESPACE
                + "'> "
                + "<wfs:Insert > "
                + "<cgf:Points>"
                + "<cgf:pointProperty>"
                + "<gml:Point>"
                + "<gml:pos>20 40</gml:pos>"
                + "</gml:Point>"
                + "</cgf:pointProperty>"
                + "<cgf:id>t0003</cgf:id>"
                + "</cgf:Points>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(1, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the id has been modified
        Document pointFeatures = getAsDOM(
                "wfs?service=WFS&version=2.0.0&request=GetFeature&typeName=cgf:Points&CQL_FILTER=id='t0003-modified'");
        // print(pointFeatures);
        assertXpathEvaluatesTo("1", "count(//cgf:Points)", pointFeatures);
    }

    @Test
    public void testUpdate() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:fes='"
                + FES.NAMESPACE
                + "' "
                + "xmlns:wfs='"
                + WFS.NAMESPACE
                + "' "
                + "xmlns:gml='"
                + GML.NAMESPACE
                + "'>"
                + " <wfs:Update typeName=\"cite:RoadSegments\">"
                + "   <wfs:Property>"
                + "     <wfs:ValueReference>cite:the_geom</wfs:ValueReference>"
                + "     <wfs:Value>"
                + "      <gml:MultiCurve>"
                + "       <gml:curveMember>"
                + "         <gml:LineString>"
                + "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                + "         </gml:LineString>"
                + "       </gml:curveMember>"
                + "      </gml:MultiCurve>"
                + "     </wfs:Value>"
                + "   </wfs:Property>"
                + "   <fes:Filter>"
                + "     <fes:PropertyIsEqualTo>"
                + "       <fes:ValueReference>FID</fes:ValueReference>"
                + "       <fes:Literal>102</fes:Literal>"
                + "     </fes:PropertyIsEqualTo>"
                + "   </fes:Filter>"
                + " </wfs:Update>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(1, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the road name has been modified too
        Document roadSegments = getAsDOM(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments&CQL_FILTER=FID=102");
        // print(roadSegments);
        assertXpathEvaluatesTo(TransactionCallbackTester.FOLSOM_STREET, "//cite:RoadSegments/cite:NAME", roadSegments);
    }

    @Test
    public void testDelete() throws Exception {
        // the plugin swaps the filter with id > 102
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("4", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(4, plugin.result.getTotalDeleted().intValue());

        // check the one surviving road segment has id 102
        Document roadSegments = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("1", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("102", "//cite:RoadSegments/cite:FID", roadSegments);
    }

    @Test
    public void testReplaceWithInsert() throws Exception {
        // the plugin will remove all elements and replace it with an insert
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsInsert;
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(1, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("6", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("New Road", "//cite:RoadSegments[cite:FID = 107]/cite:NAME", roadSegments);
    }

    @Test
    public void testReplaceWithUpdate() throws Exception {
        // the plugin will remove all elements and replace it with a fixed update on Dirt road
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsUpdate;
        String xml = DELETE_ROAD_102;
        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(1, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("5", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("Clean Road", "//cite:RoadSegments[cite:FID = 106]/cite:NAME", roadSegments);
    }

    @Test
    public void testReplaceWithDelete() throws Exception {
        // the plugin will remove all elements and replace it with a fixed delete on road 106
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsDelete;
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(1, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("4", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("0", "count(//cite:RoadSegments[cite:FID = 106])", roadSegments);
    }
}
