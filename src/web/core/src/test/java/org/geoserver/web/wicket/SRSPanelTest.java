/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class SRSPanelTest extends GeoServerWicketTestSupport implements Serializable {

    @Test
    public void testLoad() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SRSListPanel(id) {

            @Override
            protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                // do nothing
            }
        }));

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
    }
}
