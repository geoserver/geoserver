/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.geonode.kombu.KombuMessage;
import org.geoserver.notification.support.BrokerManager;
import org.geoserver.notification.support.Receiver;
import org.geoserver.notification.support.ReceiverService;
import org.geoserver.notification.support.Utils;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AnonymousIntegrationTest extends CatalogRESTTestSupport {

    private static BrokerManager brokerStarter;

    private static Receiver rc;

    @BeforeClass
    public static void startup() throws Exception {
        brokerStarter = new BrokerManager();
        brokerStarter.startBroker(true);
        rc = new Receiver();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        brokerStarter.stopBroker();
    }

    @After
    public void before() throws Exception {
        if (rc != null) {
            rc.close();
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        new File(testData.getDataDirectoryRoot(), "notifier").mkdir();
        testData.copyTo(
                getClass().getClassLoader().getResourceAsStream("notifierAnonymous.xml"),
                "notifier/" + NotifierInitializer.PROPERTYFILENAME);
    }

    @Test
    public void catalogAddNamespaces() throws Exception {
        ReceiverService service = new ReceiverService(2);
        rc.receive(service);
        String json = "{'namespace':{ 'prefix':'foo', 'uri':'http://foo.com' }}";
        postAsServletResponse("/rest/namespaces", json, "text/json");
        List<byte[]> ret = service.getMessages();

        assertEquals(2, ret.size());
        KombuMessage nsMsg = Utils.toKombu(ret.get(0));
        assertEquals(Notification.Action.Add.name(), nsMsg.getAction());
        assertEquals("Catalog", nsMsg.getType());
        assertEquals("NamespaceInfo", nsMsg.getSource().getType());
        KombuMessage wsMsg = Utils.toKombu(ret.get(1));
        assertEquals("Catalog", wsMsg.getType());
        assertEquals("WorkspaceInfo", wsMsg.getSource().getType());
    }
}
