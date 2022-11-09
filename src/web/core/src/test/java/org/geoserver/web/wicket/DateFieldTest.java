/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertTrue;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class DateFieldTest extends GeoServerWicketTestSupport {

    @Test
    public void testDateFieldsRendering() {
        tester.startPage(new DateTestPage());
        String html = tester.getLastResponseAsString();

        assertTrue(html.contains("initJQDatepicker('date',false,'yyyy-MM-dd',' ');"));
        assertTrue(html.contains("initJQDatepicker('dateTime',true,'yyyy-MM-dd HH:mm',' ');"));

        assertTrue(html.contains("initJQDatepicker('date2',false,'YYYY/MM/DD',' ');"));
        assertTrue(
                html.contains("initJQDatepicker('dateTime2',true,'YYYY/MM/DD HH:mm:ss.SSS',' ');"));
    }
}
