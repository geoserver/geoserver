/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotools.util.logging.Logging;

public class ReverseDNSPostProcessor implements RequestPostProcessor {

    private static final class DNSCacheLoader extends CacheLoader<String, String> {
        @Override
        public String load(String remoteAddr) {
            try {
                InetAddress addr = InetAddress.getByName(remoteAddr);
                return addr.getHostName();
            } catch (UnknownHostException e) {
                LOGGER.log(Level.FINE, "Error reverse looking up " + remoteAddr, e);
                // We also cache failures, they are expensive to calculate
                return remoteAddr;
            }
        }
    }

    static final Logger LOGGER = Logging.getLogger(ReverseDNSPostProcessor.class);

    static final String PROCESSOR_NAME = "reverseDNS";

    final LoadingCache<String, String> reverseLookupCache; // Not private for test

    private static RequestPostProcessor INSTANCE;

    public ReverseDNSPostProcessor(MonitorConfig config) {
        String cacheConfiguration = config.getDNSCacheConfiguration();
        LoadingCache<String, String> cache;
        try {
            cache = CacheBuilder.from(cacheConfiguration).build(new DNSCacheLoader());
        } catch (Throwable e) {
            LOGGER.warning(
                    "Invalid config "
                            + cacheConfiguration
                            + ", reverting to default."
                            + e.getMessage());
            cache = CacheBuilder.from(MonitorConfig.DNS_CACHE_DEFAULT).build(new DNSCacheLoader());
        }
        reverseLookupCache = cache;
    }

    @Override
    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response) {
        String addr = data.getRemoteAddr();
        String host;
        try {
            host = reverseLookupCache.get(addr);
        } catch (ExecutionException e) {
            LOGGER.log(Level.WARNING, "Internal error", e);
            host = addr;
        }
        data.setRemoteHost(host);
    }

    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    public static synchronized RequestPostProcessor get(MonitorConfig config) {
        if (INSTANCE == null) {
            INSTANCE = new ReverseDNSPostProcessor(config);
        }
        return INSTANCE;
    }
}
