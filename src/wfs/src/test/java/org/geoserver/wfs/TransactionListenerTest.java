/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.UpdateElementType;
import org.geoserver.data.test.CiteTestData;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.w3c.dom.Document;

/**
 * This test must be run with the server configured with the wfs 1.0 cite configuration, with data
 * initialized.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TransactionListenerTest extends WFSTestSupport {

    TransactionListenerTester listener;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/wfs/TransactionListenerTestContext.xml");
    }

    @Before
    public void clearState() throws Exception {
        listener =
                (TransactionListenerTester) applicationContext.getBean("transactionListenerTester");
        listener.clear();
    }

    @Test
    public void testDelete() throws Exception {
        // perform a delete
        String delete =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                        + "<wfs:Delete typeName=\"cgf:Points\"> "
                        + "<ogc:Filter> "
                        + "<ogc:PropertyIsEqualTo> "
                        + "<ogc:PropertyName>cgf:id</ogc:PropertyName> "
                        + "<ogc:Literal>t0000</ogc:Literal> "
                        + "</ogc:PropertyIsEqualTo> "
                        + "</ogc:Filter> "
                        + "</wfs:Delete> "
                        + "</wfs:Transaction>";

        postAsDOM("wfs", delete);
        assertEquals(1, listener.events.size());
        TransactionEvent event = (TransactionEvent) listener.events.get(0);
        assertTrue(event.getSource() instanceof DeleteElementType);
        assertEquals(TransactionEventType.PRE_DELETE, event.getType());
        assertEquals(CiteTestData.POINTS, event.getLayerName());
        assertEquals(1, listener.features.size());
        Feature deleted = (Feature) listener.features.get(0);
        assertEquals("t0000", deleted.getProperty("id").getValue());
    }

    @Test
    public void testInsert() throws Exception {
        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert > "
                        + "<cgf:Lines>"
                        + "<cgf:lineStringProperty>"
                        + "<gml:LineString>"
                        + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                        + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                        + "</gml:coordinates>"
                        + "</gml:LineString>"
                        + "</cgf:lineStringProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:Lines>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());

        TransactionEvent firstEvent = (TransactionEvent) listener.events.get(0);
        assertTrue(firstEvent.getSource() instanceof InsertElementType);
        assertEquals(TransactionEventType.PRE_INSERT, firstEvent.getType());
        assertEquals(CiteTestData.LINES, firstEvent.getLayerName());
        // one feature from the pre-insert hook, one from the post-insert hook
        assertEquals(2, listener.features.size());

        // what was the fid of the inserted feature?
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Lines\"> "
                        + "<ogc:PropertyName>id</ogc:PropertyName> "
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", getFeature);
        String fid =
                dom.getElementsByTagName("cgf:Lines")
                        .item(0)
                        .getAttributes()
                        .item(0)
                        .getNodeValue();

        TransactionEvent secondEvent = (TransactionEvent) listener.events.get(1);
        assertTrue(secondEvent.getSource() instanceof InsertElementType);
        assertEquals(TransactionEventType.POST_INSERT, secondEvent.getType());
        Feature inserted = (Feature) listener.features.get(1);
        assertEquals(fid, inserted.getIdentifier().getID());
    }

    @Test
    public void testUpdate() throws Exception {
        // perform an update
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update typeName=\"cgf:Polygons\" > "
                        + "<wfs:Property>"
                        + "<wfs:Name>id</wfs:Name>"
                        + "<wfs:Value>t0003</wfs:Value>"
                        + "</wfs:Property>"
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Update>"
                        + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());
        TransactionEvent firstEvent = (TransactionEvent) listener.events.get(0);
        assertTrue(firstEvent.getSource() instanceof UpdateElementType);
        assertEquals(TransactionEventType.PRE_UPDATE, firstEvent.getType());
        assertEquals(CiteTestData.POLYGONS, firstEvent.getLayerName());
        Feature updatedBefore = (Feature) listener.features.get(0);
        assertEquals("t0002", updatedBefore.getProperty("id").getValue());

        TransactionEvent secondEvent = (TransactionEvent) listener.events.get(1);
        assertTrue(secondEvent.getSource() instanceof UpdateElementType);
        assertEquals(TransactionEventType.POST_UPDATE, secondEvent.getType());
        assertEquals(CiteTestData.POLYGONS, secondEvent.getLayerName());
        Feature updatedAfter = (Feature) listener.features.get(1);
        assertEquals("t0003", updatedAfter.getProperty("id").getValue());

        assertEquals(2, listener.features.size());
    }
}
