/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.apache.wicket.serialize.ISerializer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class NewCachedLayerPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        login();
        NewCachedLayerPage page = new NewCachedLayerPage();

        tester.startPage(page);
        tester.assertRenderedPage(NewCachedLayerPage.class);

        // print(page, true, true);
    }

    @Test // GEOS-10374
    public void testSerializable() throws IOException {
        login();
        NewCachedLayerPage page = new NewCachedLayerPage();
        ISerializer serializer = getGeoServerApplication().getFrameworkSettings().getSerializer();
        // this would only result in an ERROR level log
        serializer.serialize(page);

        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(page);
        assertTrue(true);
    }
}
