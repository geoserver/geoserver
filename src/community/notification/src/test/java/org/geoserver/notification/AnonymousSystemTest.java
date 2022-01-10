/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.notification.common.NotificationConfiguration;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.TransactionCallback;
import org.junit.Test;

public class AnonymousSystemTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // TODO Auto-generated method stub
        super.setUpTestData(testData);
        new File(testData.getDataDirectoryRoot(), "notifier").mkdir();
        testData.copyTo(
                getClass().getClassLoader().getResourceAsStream("notifierAnonymous.xml"),
                "notifier/" + NotifierInitializer.PROPERTYFILENAME);
    }

    @Test
    public void testCatalogNotifierIntialization() throws IOException {
        NotificationConfiguration cfg = null;
        int counter = 0;
        for (CatalogListener listener : getGeoServer().getCatalog().getListeners()) {
            if (listener instanceof INotificationCatalogListener) {
                counter++;
            }
        }
        assertEquals(1, counter);
    }

    @Test
    public void testTransactionNotifierIntialization() throws IOException {
        NotificationConfiguration cfg = null;
        int counter = 0;
        for (TransactionCallback listener :
                GeoServerExtensions.extensions(TransactionCallback.class)) {
            if (listener instanceof INotificationTransactionListener) {
                counter++;
            }
        }
        assertEquals(1, counter);
    }
}
