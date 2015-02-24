/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.geoserver.flow.ControllerPriorityComparator;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.SingleIpFlowController;
import org.geoserver.flow.controller.UserFlowController;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.junit.Test;

public class DefaultControlFlowConfigurationTest {

    @Test
    public void testParsing() throws Exception {
        Properties p = new Properties();
        p.put("timeout", "10");
        p.put("ows.global", "100");
        p.put("ows.wms.getmap", "8");
        p.put("user", "6");
        p.put("ip", "12");
        p.put("ip.192.168.1.8", "14");
        p.put("ip.192.168.1.10", "15");

        DefaultControlFlowConfigurator configurator = new DefaultControlFlowConfigurator(
                new FixedWatcher(p));
        assertTrue(configurator.isStale());
        List<FlowController> controllers = configurator.buildFlowControllers();
        Collections.sort(controllers, new ControllerPriorityComparator());
        assertFalse(configurator.isStale());
        assertEquals(10 * 1000, configurator.getTimeout());

        assertEquals(6, controllers.size());
        assertTrue(controllers.get(0) instanceof UserFlowController);
        assertTrue(controllers.get(1) instanceof BasicOWSController);
        assertTrue(controllers.get(2) instanceof IpFlowController);
        assertTrue(controllers.get(3) instanceof SingleIpFlowController);
        assertEquals("192.168.1.8", ((SingleIpFlowController)controllers.get(3)).getIp());
        assertTrue(controllers.get(4) instanceof SingleIpFlowController);
        assertEquals("192.168.1.10", ((SingleIpFlowController)controllers.get(4)).getIp());
        assertTrue(controllers.get(5) instanceof GlobalFlowController);

        UserFlowController uc = (UserFlowController) controllers.get(0);
        assertEquals(6, uc.getPriority());
        BasicOWSController oc = (BasicOWSController) controllers.get(1);
        assertEquals(8, oc.getPriority());
        assertEquals("wms", oc.getService());
        assertEquals("getmap", oc.getMethod());
        assertNull(oc.getOutputFormat());
        GlobalFlowController gc = (GlobalFlowController) controllers.get(5);
        assertEquals(100, gc.getPriority());
        IpFlowController ipFc = (IpFlowController) controllers.get(2);
        assertEquals(12, ipFc.getPriority());
        SingleIpFlowController ipSc = (SingleIpFlowController) controllers.get(3);
        assertEquals(14, ipSc.getPriority());
    }

    static class FixedWatcher extends PropertyFileWatcher {
        boolean stale = true;

        Properties properties;

        public FixedWatcher(Properties properties) {
            super((Resource)null);
            this.properties = properties;
        }

        @Override
        public boolean isStale() {
            if (stale) {
                stale = false;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Properties getProperties() throws IOException {
            return properties;
        }
    }
}
