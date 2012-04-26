package org.geoserver.wfs.versioning;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;

public class TransactionEventListenerTest extends WFS20VersioningTestSupport {

    /**
     * Override spring app context locations to provide one with a
     * {@link RecordingTransactionPlugin} for this test
     * 
     * @return
     */
    @Override
    protected String[] getSpringContextLocations() {
        return new String[] { "classpath*:/applicationContext.xml",
                "classpath*:/applicationSecurityContext.xml",
                "classpath*:/TransactionEventListenerTestContext.xml" };
    }

    /**
     * Singleton instance added to application context through
     * TransactionEventListenerTestContext.xml
     * 
     */
    public static class RecordingTransactionPlugin implements TransactionPlugin {

        public LinkedHashMap<TransactionEvent, SimpleFeatureCollection> dataStoreChanges = new LinkedHashMap<TransactionEvent, SimpleFeatureCollection>();

        public List<TransactionType> beforeTransaction = new LinkedList<TransactionType>();

        public List<TransactionType> beforeCommit = new LinkedList<TransactionType>();

        public LinkedHashMap<TransactionType, TransactionResponseType> afterTransaction = new LinkedHashMap<TransactionType, TransactionResponseType>();

        public void clear() {
            dataStoreChanges.clear();
            beforeTransaction.clear();
            beforeCommit.clear();
            afterTransaction.clear();
        }

        @Override
        public void dataStoreChange(TransactionEvent event) throws WFSException {
            SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();
            DefaultFeatureCollection copy = DataUtilities.collection(affectedFeatures);
            dataStoreChanges.put(event, copy);
        }

        @Override
        public TransactionType beforeTransaction(TransactionType request) throws WFSException {
            beforeTransaction.add(request);
            return request;
        }

        @Override
        public void beforeCommit(TransactionType request) throws WFSException {
            beforeCommit.add(request);
        }

        @Override
        public void afterTransaction(TransactionType request, TransactionResponseType result,
                boolean committed) {
            afterTransaction.put(request, result);
        }

        @Override
        public int getPriority() {
            return 0;
        }

    }

    private RecordingTransactionPlugin transactionPlugin;

    @Override
    public void setUpInternal() throws Exception {
        transactionPlugin = GeoServerExtensions.bean(RecordingTransactionPlugin.class);
        assertNotNull(transactionPlugin);
    }

    @Override
    public void tearDownInternal() throws Exception {
        if (transactionPlugin != null) {
            transactionPlugin.clear();
        }
    }

    public void testInsert() throws Exception {
        // do an insert
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + " xmlns:wfs='"
                + WFS.NAMESPACE
                + "' "
                + " xmlns:gml='"
                + GML.NAMESPACE
                + "' "
                + " xmlns:cite=\"http://www.opengis.net/cite\">"
                + "<wfs:Insert>"
                + " <cite:Buildings>"
                + "  <cite:the_geom>"
                + "<gml:MultiSurface> "
                + " <gml:surfaceMember> "
                + "  <gml:Polygon> "
                + "   <gml:exterior> "
                + "    <gml:LinearRing> "
                + "     <gml:posList>-123.9 40.0 -124.0 39.9 -124.1 40.0 -124.0 40.1 -123.9 40.0</gml:posList>"
                + "    </gml:LinearRing> " + "   </gml:exterior> " + "  </gml:Polygon> "
                + " </gml:surfaceMember> " + "</gml:MultiSurface> " + "  </cite:the_geom>"
                + "  <cite:FID>115</cite:FID>" + "  <cite:ADDRESS>987 Foo St</cite:ADDRESS>"
                + " </cite:Buildings>" + "</wfs:Insert>" + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + "wfs:totalInserted" + ")", dom);

        assertEquals(2, transactionPlugin.dataStoreChanges.size());
        Iterator<Entry<TransactionEvent, SimpleFeatureCollection>> iterator = transactionPlugin.dataStoreChanges
                .entrySet().iterator();

        Entry<TransactionEvent, SimpleFeatureCollection> first = iterator.next();
        Entry<TransactionEvent, SimpleFeatureCollection> second = iterator.next();
        assertEquals(1, first.getValue().size());
        assertEquals(1, second.getValue().size());
    }
}
