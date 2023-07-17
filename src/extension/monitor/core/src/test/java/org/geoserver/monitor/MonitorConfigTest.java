/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.monitor;

import java.net.InetAddress;
import org.junit.Assert;
import org.junit.Test;

public class MonitorConfigTest {

    @Test
    public void shouldPostProcessorThreadsReturnDefault() {
        MonitorConfig config = new MonitorConfig();
        // Don't set a value form the property here
        int actual = config.getPostProcessorThreads();
        Assert.assertEquals(MonitorConfig.POSTPROCES_THREADS_DEFAULT, actual);
    }

    @Test
    public void shouldPostProcessorThreadsReturnConfiguredValue() {
        MonitorConfig config = new MonitorConfig();
        config.props().put("postProcessorThreads", " 5 ");
        int actual = config.getPostProcessorThreads();
        Assert.assertEquals(5, actual);
    }

    @Test
    public void shouldPostProcessorThreadsIgnoreNonNumber() {
        MonitorConfig config = new MonitorConfig();
        config.props().put("postProcessorThreads", "not a number");
        int actual = config.getPostProcessorThreads();
        Assert.assertEquals(MonitorConfig.POSTPROCES_THREADS_DEFAULT, actual);
    }

    @Test
    public void shouldPostProcessorThreadsIgnoreTooSmall() {
        MonitorConfig config = new MonitorConfig();
        config.props().put("postProcessorThreads", "0");
        int actual = config.getPostProcessorThreads();
        Assert.assertEquals(MonitorConfig.POSTPROCES_THREADS_DEFAULT, actual);
    }

    @Test
    public void shouldDNSCacheUseDefaultConfig() throws Exception {
        MonitorConfig config = new MonitorConfig();
        ReverseDNSPostProcessor dut = new ReverseDNSPostProcessor(config);
        RequestData req = new RequestData();
        req.setRemoteAddr(InetAddress.getLocalHost().getHostAddress());
        dut.run(req, null, null);
        Assert.assertNotNull(req.getRemoteHost());
        Assert.assertTrue("Cache should be working", dut.reverseLookupCache.size() > 0);
    }

    @Test
    public void shouldDNSCacheIgnoreInvalidConfig() throws Exception {
        MonitorConfig config = new MonitorConfig();
        config.props().put("dnsCacheConfiguration", "this makes no sense");
        ReverseDNSPostProcessor dut = new ReverseDNSPostProcessor(config);
        RequestData req = new RequestData();
        req.setRemoteAddr(InetAddress.getLocalHost().getHostAddress());
        dut.run(req, null, null);
        Assert.assertNotNull(req.getRemoteHost());
        Assert.assertTrue("Cache should be working", dut.reverseLookupCache.size() > 0);
    }

    @Test
    public void shouldDNSCacheUseConfig() throws Exception {
        MonitorConfig config = new MonitorConfig();
        config.props().put("dnsCacheConfiguration", "maximumSize=0");
        ReverseDNSPostProcessor dut = new ReverseDNSPostProcessor(config);
        RequestData req = new RequestData();
        req.setRemoteAddr(InetAddress.getLocalHost().getHostAddress());
        dut.run(req, null, null);
        Assert.assertNotNull(req.getRemoteHost());
        Assert.assertEquals(
                "maximumSize=0 violated",
                0,
                dut.reverseLookupCache.size()); // Still empty as max size=0
    }
}
