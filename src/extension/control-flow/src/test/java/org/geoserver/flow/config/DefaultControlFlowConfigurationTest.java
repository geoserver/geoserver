/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.geoserver.flow.ControllerPriorityComparator;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.HttpHeaderPriorityProvider;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.IpRequestMatcher;
import org.geoserver.flow.controller.PriorityThreadBlocker;
import org.geoserver.flow.controller.RateFlowController;
import org.geoserver.flow.controller.SingleIpFlowController;
import org.geoserver.flow.controller.ThreadBlocker;
import org.geoserver.flow.controller.UserConcurrentFlowController;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.PropertyFileWatcher;
import org.hamcrest.CoreMatchers;
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
        p.put("ip.blacklist", "192.168.1.1,192.168.1.2");
        p.put("ip.whitelist", "192.168.1.3,192.168.1.4");
        p.put("user.ows", "20/s");
        p.put("user.ows.wms", "300/m;3s");
        p.put("ip.ows.wms.getmap", "100/m;3s");
        p.put("ip.ows.wps.execute", "50/d;60s");

        DefaultControlFlowConfigurator configurator = new DefaultControlFlowConfigurator(
                new FixedWatcher(p));
        assertTrue(configurator.isStale());
        List<FlowController> controllers = configurator.buildFlowControllers();
        Collections.sort(controllers, new ControllerPriorityComparator());
        assertFalse(configurator.isStale());
        assertEquals(10 * 1000, configurator.getTimeout());

        assertEquals(10, controllers.size());

        assertTrue(controllers.get(0) instanceof RateFlowController);
        RateFlowController rfc = (RateFlowController) controllers.get(0);
        assertEquals("wps.execute", rfc.getMatcher().toString());
        assertEquals(50, rfc.getMaxRequests());
        assertEquals(Intervals.d.getDuration(), rfc.getTimeInterval());
        assertEquals(60000, rfc.getDelay());

        assertTrue(controllers.get(1) instanceof RateFlowController);
        rfc = (RateFlowController) controllers.get(1);
        assertEquals("wms.getmap", rfc.getMatcher().toString());
        assertEquals(100, rfc.getMaxRequests());
        assertEquals(Intervals.m.getDuration(), rfc.getTimeInterval());
        assertEquals(3000, rfc.getDelay());

        assertTrue(controllers.get(2) instanceof RateFlowController);
        rfc = (RateFlowController) controllers.get(2);
        assertEquals("wms", rfc.getMatcher().toString());
        assertEquals(300, rfc.getMaxRequests());
        assertEquals(Intervals.m.getDuration(), rfc.getTimeInterval());
        assertEquals(3000, rfc.getDelay());

        assertTrue(controllers.get(3) instanceof RateFlowController);
        rfc = (RateFlowController) controllers.get(3);
        assertEquals("Any OGC request", rfc.getMatcher().toString());
        assertEquals(20, rfc.getMaxRequests());
        assertEquals(Intervals.s.getDuration(), rfc.getTimeInterval());
        assertEquals(0, rfc.getDelay());

        assertTrue(controllers.get(4) instanceof UserConcurrentFlowController);
        UserConcurrentFlowController uc = (UserConcurrentFlowController) controllers.get(4);
        assertEquals(6, uc.getPriority());

        assertTrue(controllers.get(5) instanceof BasicOWSController);
        BasicOWSController oc = (BasicOWSController) controllers.get(5);
        assertEquals(8, oc.getPriority());
        assertEquals("wms.getmap", oc.getMatcher().toString());

        assertTrue(controllers.get(6) instanceof IpFlowController);
        IpFlowController ipFc = (IpFlowController) controllers.get(6);
        assertEquals(12, ipFc.getPriority());

        assertTrue(controllers.get(7) instanceof SingleIpFlowController);
        SingleIpFlowController ipSc = (SingleIpFlowController) controllers.get(7);
        assertEquals(14, ipSc.getPriority());
        IpRequestMatcher ipMatcher = (IpRequestMatcher) ipSc.getMatcher();
        assertEquals("192.168.1.8", ipMatcher.getIp());

        assertTrue(controllers.get(8) instanceof SingleIpFlowController);
        ipMatcher = (IpRequestMatcher) ((SingleIpFlowController) controllers.get(8)).getMatcher();
        assertEquals("192.168.1.10", ipMatcher.getIp());

        assertTrue(controllers.get(9) instanceof GlobalFlowController);
        GlobalFlowController gc = (GlobalFlowController) controllers.get(9);
        assertEquals(100, gc.getPriority());

        // store the properties into a temp folder and relaod
        assertTrue(configurator.getFileLocations().isEmpty());
        
        File tmpDir = createTempDir();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir);
        
        configurator.saveConfiguration(resourceLoader);
        Resource controlFlowProps = Files.asResource(resourceLoader.find("controlflow.properties"));
        assertTrue(Resources.exists(controlFlowProps));
        
        PropertyFileWatcher savedProps = new PropertyFileWatcher(controlFlowProps);
        
        assertEquals(savedProps.getProperties(), p);
    }

    @Test
    public void testParsingPriority() throws Exception {
        Properties p = new Properties();
        p.put("timeout", "10");
        p.put("ows.global", "100");
        p.put("ows.priority.http", "gs-priority,3");
        p.put("ows.wms", "6");
        p.put("ows.wfs.getFeature", "12");

        checkPriorityParsing(p);
    }

    @Test
    public void testParsingPriorityWithSpaces() throws Exception {
        Properties p = new Properties();
        p.put("timeout", "10");
        p.put("ows.global", "100");
        p.put("ows.priority.http", " gs-priority , 3 ");
        p.put("ows.wms", "6");
        p.put("ows.wfs.getFeature", "12");

        checkPriorityParsing(p);
    }

    private void checkPriorityParsing(Properties p) throws Exception {
        DefaultControlFlowConfigurator configurator = new DefaultControlFlowConfigurator(
                new FixedWatcher(p));
        assertTrue(configurator.isStale());
        List<FlowController> controllers = configurator.buildFlowControllers();
        Collections.sort(controllers, new ControllerPriorityComparator());
        assertFalse(configurator.isStale());
        assertEquals(10 * 1000, configurator.getTimeout());

        assertEquals(3, controllers.size());

        assertTrue(controllers.get(0) instanceof BasicOWSController);
        BasicOWSController wmsController = (BasicOWSController) controllers.get(0);
        assertEquals("wms", wmsController.getMatcher().toString());
        ThreadBlocker blocker = wmsController.getBlocker();
        assertPriorityThreadBlocker(blocker, "gs-priority", 3);

        assertTrue(controllers.get(1) instanceof BasicOWSController);
        BasicOWSController wfsController = (BasicOWSController) controllers.get(1);
        assertEquals("wfs.getFeature", wfsController.getMatcher().toString());
        assertPriorityThreadBlocker(blocker, "gs-priority", 3);
    }

    public void assertPriorityThreadBlocker(ThreadBlocker blocker, String headerName, int 
            defaultPriority) {
        assertThat(blocker, CoreMatchers.instanceOf(PriorityThreadBlocker.class));
        PriorityThreadBlocker ptb = (PriorityThreadBlocker) blocker;
        assertThat(ptb.getPriorityProvider(), CoreMatchers.instanceOf(HttpHeaderPriorityProvider
                .class));
        HttpHeaderPriorityProvider priorityProvider = (HttpHeaderPriorityProvider) ptb.getPriorityProvider();
        assertEquals(headerName, priorityProvider.getHeaderName());
        assertEquals(defaultPriority, priorityProvider.getDefaultPriority());
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
    
    static File createTempDir() throws IOException {
        File f = File.createTempFile("controlFlow", "data", new File("target"));
        f.delete();
        f.mkdirs();
        return f;
    }
}
