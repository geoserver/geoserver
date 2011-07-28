package org.geoserver.wfs;

import org.geoserver.data.test.MockData;
import org.opengis.feature.Feature;
import org.w3c.dom.Document;

/**
 * This test must be run with the server configured with the wfs 1.0 cite
 * configuration, with data initialized.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class TransactionListenerTest extends WFSTestSupport {
    
    TransactionListenerTester listener;

    protected String[] getSpringContextLocations() {
        String[] base = super.getSpringContextLocations();
        String[] extended = new String[base.length + 1];
        System.arraycopy(base, 0, extended, 0, base.length);
        extended[base.length] = "classpath:/org/geoserver/wfs/TransactionListenerTestContext.xml";
        return extended;
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        listener = (TransactionListenerTester) applicationContext.getBean("transactionListenerTester");
        listener.clear();
    }
    
    public void testDelete() throws Exception {
        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cgf:Points\"> " + "<ogc:Filter> "
                + "<ogc:PropertyIsEqualTo> "
                + "<ogc:PropertyName>cgf:id</ogc:PropertyName> "
                + "<ogc:Literal>t0000</ogc:Literal> "
                + "</ogc:PropertyIsEqualTo> " + "</ogc:Filter> "
                + "</wfs:Delete> " + "</wfs:Transaction>";

        postAsDOM("wfs", delete);
        assertEquals(1, listener.events.size());
        TransactionEvent event = (TransactionEvent) listener.events.get(0);
        assertEquals(TransactionEventType.PRE_DELETE, event.getType());
        assertEquals(MockData.POINTS, event.getLayerName());
        assertEquals(1, listener.features.size());
        Feature deleted = (Feature) listener.features.get(0);
        assertEquals("t0000", deleted.getProperty("id").getValue());
    }

    public void testInsert() throws Exception {
        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
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
                + "</gml:coordinates>" + "</gml:LineString>"
                + "</cgf:lineStringProperty>" + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>" + "</wfs:Insert>" + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());
        
        TransactionEvent firstEvent = (TransactionEvent) listener.events.get(0);
        assertEquals(TransactionEventType.PRE_INSERT, firstEvent.getType());
        assertEquals(MockData.LINES, firstEvent.getLayerName());
        // one feature from the pre-insert hook, one from the post-insert hook
        assertEquals(2, listener.features.size());
        
        // what was the fid of the inserted feature?
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>id</ogc:PropertyName> "
                + "<ogc:Filter>" + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
                + "</wfs:Query> " + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", getFeature);
        String fid = dom.getElementsByTagName("cgf:Lines").item(0)
                .getAttributes().item(0).getNodeValue();

        TransactionEvent secondEvent = (TransactionEvent) listener.events.get(1);
        assertEquals(TransactionEventType.POST_INSERT, secondEvent.getType());
        Feature inserted = (Feature) listener.features.get(1);
        assertEquals(fid, inserted.getIdentifier().getID());
    }

    public void testUpdate() throws Exception {
        // perform an update
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:Polygons\" > " + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>" + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>" + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
                + "</wfs:Update>" + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());
        TransactionEvent firstEvent = (TransactionEvent) listener.events.get(0);
        assertEquals(TransactionEventType.PRE_UPDATE, firstEvent.getType());
        assertEquals(MockData.POLYGONS, firstEvent.getLayerName());
        Feature updatedBefore = (Feature) listener.features.get(0);
        assertEquals("t0002", updatedBefore.getProperty("id").getValue());
        
        TransactionEvent secondEvent = (TransactionEvent) listener.events.get(1);
        assertEquals(TransactionEventType.POST_UPDATE, secondEvent.getType());
        assertEquals(MockData.POLYGONS, secondEvent.getLayerName());
        Feature updatedAfter = (Feature) listener.features.get(1);
        assertEquals("t0003", updatedAfter.getProperty("id").getValue());
        
        assertEquals(2, listener.features.size());
    }
}
