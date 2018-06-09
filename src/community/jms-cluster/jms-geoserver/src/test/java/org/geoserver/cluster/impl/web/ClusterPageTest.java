/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.web;

import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/** Contains tests for the cluster configuration UI. */
public final class ClusterPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testClusterPageLoads() {
        login();
        tester.startPage(ClusterPage.class);
        // the page successfully loaded, let's check its components
        tester.assertComponent("form:brokerURL", TextField.class);
        tester.assertComponent("form:instanceName", TextField.class);
        tester.assertComponent("form:group", TextField.class);
        tester.assertComponent("form:topicName", TextField.class);
        tester.assertComponent("form:connection", TextField.class);
        tester.assertComponent("form:connectionB", AjaxButton.class);
        tester.assertComponent("form:toggleMaster", TextField.class);
        tester.assertComponent("form:toggleMasterB", AjaxButton.class);
        tester.assertComponent("form:toggleSlave", TextField.class);
        tester.assertComponent("form:toggleSlaveB", AjaxButton.class);
        tester.assertComponent("form:readOnly", TextField.class);
        tester.assertComponent("form:readOnlyB", AjaxButton.class);
        tester.assertComponent("form:embeddedBroker", TextField.class);
        tester.assertComponent("form:embeddedBrokerB", AjaxButton.class);
        tester.assertComponent("form:saveB", Button.class);
    }
}
