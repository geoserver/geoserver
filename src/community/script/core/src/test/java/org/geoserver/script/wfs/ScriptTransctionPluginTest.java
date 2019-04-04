/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wfs;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.geoserver.data.test.MockData.PRIMITIVEGEOFEATURE;

import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import java.util.Iterator;
import javax.xml.namespace.QName;
import junit.framework.TestCase;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.wfs.TransactionDetail.Entry;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.data.simple.SimpleFeatureCollection;

public class ScriptTransctionPluginTest extends TestCase {

    ScriptManager scriptMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scriptMgr = createScriptMgr();
    }

    ScriptManager createScriptMgr() throws Exception {
        ScriptManager mgr = createNiceMock(ScriptManager.class);
        expect(mgr.wfsTx())
                .andReturn(org.geoserver.platform.resource.Files.asResource(Files.createTempDir()))
                .anyTimes();
        replay(mgr);
        return mgr;
    }

    public void testTransactionDetails() throws Exception {
        SimpleFeatureCollection inserted = createNiceMock(SimpleFeatureCollection.class);
        SimpleFeatureCollection updated = createNiceMock(SimpleFeatureCollection.class);
        SimpleFeatureCollection deleted = createNiceMock(SimpleFeatureCollection.class);
        replay(inserted, updated, deleted);

        TransactionType t = WfsFactory.eINSTANCE.createTransactionType();
        TransactionEvent e1 =
                new TransactionEvent(
                        TransactionEventType.PRE_INSERT,
                        TransactionRequest.adapt(t),
                        PRIMITIVEGEOFEATURE,
                        inserted);
        TransactionEvent e2 =
                new TransactionEvent(
                        TransactionEventType.PRE_UPDATE,
                        TransactionRequest.adapt(t),
                        PRIMITIVEGEOFEATURE,
                        updated);
        TransactionEvent e3 =
                new TransactionEvent(
                        TransactionEventType.PRE_DELETE,
                        TransactionRequest.adapt(t),
                        PRIMITIVEGEOFEATURE,
                        deleted);

        ScriptTransactionPlugin plugin = new ScriptTransactionPlugin(scriptMgr);
        plugin.dataStoreChange(e1);
        plugin.dataStoreChange(e2);
        plugin.dataStoreChange(e3);

        TransactionDetail detail =
                (TransactionDetail) t.getExtendedProperties().get(TransactionDetail.class);
        assertNotNull(detail);

        Multimap<QName, Entry> entries = detail.getEntries();
        assertTrue(entries.containsKey(PRIMITIVEGEOFEATURE));

        Iterator<Entry> it = entries.get(PRIMITIVEGEOFEATURE).iterator();

        assertTrue(it.hasNext());
        Entry e = it.next();
        assertEquals(TransactionEventType.PRE_INSERT, e.type);
        assertEquals(inserted, e.features);

        assertTrue(it.hasNext());
        e = it.next();
        assertEquals(TransactionEventType.PRE_UPDATE, e.type);
        assertEquals(updated, e.features);

        assertTrue(it.hasNext());
        e = it.next();
        assertEquals(TransactionEventType.PRE_DELETE, e.type);
        assertEquals(deleted, e.features);

        assertFalse(it.hasNext());
    }
}
