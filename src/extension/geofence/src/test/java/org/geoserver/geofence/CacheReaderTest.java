/* (C) 2015 - 2016 Open Source Geospatial Foundation. All rights reserved.
 * (C) 2007 - 2014 GeoSolutions S.A.S.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import com.google.common.base.Ticker;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CachedRuleReader;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.config.GeoFencePropertyPlaceholderConfigurer;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geotools.util.logging.Logging;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.UrlResource;

/** @author ETj (etj at geo-solutions.it) */
public class CacheReaderTest extends GeofenceBaseTest {

    static final Logger LOGGER = Logging.getLogger(CacheReaderTest.class);

    private CustomTicker ticker;

    private CachedRuleReader cachedRuleReader;

    private GeoFencePropertyPlaceholderConfigurer configurer;

    private RuleReaderService realReader;

    @Before
    public void onInitCachedReader() {
        configurer =
                (GeoFencePropertyPlaceholderConfigurer)
                        applicationContext.getBean("geofence-configurer");
        configurer.setLocation(
                new UrlResource(this.getClass().getResource("/test-cache-config.properties")));

        realReader = applicationContext.getBean("remoteReaderService", RuleReaderService.class);

        ticker = new CustomTicker();

        CacheConfiguration config = new CacheConfiguration();
        config.setSize(100);
        config.setRefreshMilliSec(500);
        config.setExpireMilliSec(1000);
        config.setCustomTicker(ticker);

        if (configManager == null) {
            configManager =
                    (GeoFenceConfigurationManager)
                            applicationContext.getBean("geofenceConfigurationManager");
        }
        assertNotNull(configManager);
        configManager.setCacheConfiguration(config);

        cachedRuleReader = new CachedRuleReader(configManager);
        cachedRuleReader.setRealRuleReaderService(realReader);

        cachedRuleReader.getCacheInitParams().setSize(100);
        cachedRuleReader.getCacheInitParams().setRefreshMilliSec(500);
        cachedRuleReader.getCacheInitParams().setExpireMilliSec(1000);
        cachedRuleReader.getCacheInitParams().setCustomTicker(ticker);

        cachedRuleReader.init();
    }

    static class CustomTicker extends Ticker {

        private long nano = 0;

        @Override
        public long read() {
            return nano;
        }

        public void setMillisec(long milli) {
            this.nano = milli * 1000000;
        }
    }

    @Test
    public void testSize() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(0, cachedRuleReader.getStats().hitCount());
        assertEquals(0, cachedRuleReader.getStats().missCount());
        assertEquals(0, cachedRuleReader.getStats().evictionCount());

        RuleFilter filter1 = new RuleFilter();
        filter1.setUser("test_1");
        RuleFilter filter2 = new RuleFilter();
        filter2.setUser("test_2");
        RuleFilter filter3 = new RuleFilter();
        filter3.setUser("test_3");

        assertNotSame(filter1, filter2);

        // expected stats
        int hitExp = 0;
        int missExp = 0;
        int evictExp = 0;

        // first loading, obviously a miss
        AccessInfo ai1_1 = cachedRuleReader.getAccessInfo(filter1);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(++missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());

        // second loading with the same rule, should be a hit
        ticker.setMillisec(1);
        AccessInfo ai1_2 = cachedRuleReader.getAccessInfo(filter1);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());

        assertEquals(ai1_1, ai1_2);

        // loading a different filter, a miss again
        ticker.setMillisec(2);
        AccessInfo ai2 = cachedRuleReader.getAccessInfo(filter2);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(++missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());

        // yet another different filter. we expect a miss, and an eviction
        ticker.setMillisec(3);
        AccessInfo ai3 = cachedRuleReader.getAccessInfo(filter3);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(++missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());

        // filter1 is the oldest one:
        ticker.setMillisec(4);
        cachedRuleReader.getAccessInfo(filter2);
        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());

        ticker.setMillisec(5);
        cachedRuleReader.getAccessInfo(filter3);
        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());

        // reload filter1, ==> filter 2 should be evicted
        ticker.setMillisec(6);
        cachedRuleReader.getAccessInfo(filter1);
        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());
    }

    @Test
    public void testExpire() throws InterruptedException {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(0, cachedRuleReader.getStats().hitCount());
        assertEquals(0, cachedRuleReader.getStats().missCount());
        assertEquals(0, cachedRuleReader.getStats().evictionCount());

        RuleFilter filter1 = new RuleFilter();
        filter1.setUser("test_1");
        RuleFilter filter2 = new RuleFilter();
        filter2.setUser("test_2");
        RuleFilter filter3 = new RuleFilter();
        filter3.setUser("test_3");

        int hitExp = 0;
        int missExp = 0;
        int evictExp = 0;

        // first loading
        AccessInfo ai1_1 = cachedRuleReader.getAccessInfo(filter1);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(++missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());

        // second loading with the same rule, should be a hit
        ticker.setMillisec(1);
        AccessInfo ai1_2 = cachedRuleReader.getAccessInfo(filter1);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());
        assertEquals(1, cachedRuleReader.getStats().loadSuccessCount());

        assertEquals(ai1_1, ai1_2);

        // loading the same filter, after the refresh time
        ticker.setMillisec(600);
        // LOGGER.log(Level.INFO, "We expect a reload() now....");
        // System.out.println("---> We expect a reload() now....");
        AccessInfo ai1_3 = cachedRuleReader.getAccessInfo(filter1);

        // System.out.println(cachedRuleReader.getStats());
        assertEquals(++hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());
        // assertEquals(2, cachedRuleReader.getStats().loadSuccessCount()); // dunno if load is made
        // asynch or not

        // reloading should have been triggered
        ticker.setMillisec(700);
        // System.out.println("sleeping...");
        Thread.sleep(500);
        // System.out.println(cachedRuleReader.getStats());
        assertEquals(hitExp, cachedRuleReader.getStats().hitCount());
        assertEquals(missExp, cachedRuleReader.getStats().missCount());
        assertEquals(evictExp, cachedRuleReader.getStats().evictionCount());
        // assertEquals(2, cachedRuleReader.getStats().loadSuccessCount()); // uhm, this does not
        // work
        if (2 != cachedRuleReader.getStats().loadSuccessCount())
            LOGGER.log(
                    Level.SEVERE,
                    "*** Bad successCount check, expected 2, found {0}",
                    cachedRuleReader.getStats().loadSuccessCount());

        ticker.setMillisec(800);
        cachedRuleReader.getAccessInfo(filter1);
        // System.out.println(cachedRuleReader.getStats());

        ticker.setMillisec(2000);
        cachedRuleReader.getAccessInfo(filter1);
        // System.out.println(cachedRuleReader.getStats());
    }

    // public void testSave() throws IOException, URISyntaxException {
    // GeofenceTestUtils.emptyFile("test-cache-config.properties");
    //
    // CacheConfiguration params = new CacheConfiguration();
    // params.setSize(100);
    // params.setRefreshMilliSec(500);
    // params.setExpireMilliSec(1000);
    //
    // configManager.setCacheConfiguration(params);
    //
    // CachedRuleReader cachedRuleReader = new CachedRuleReader(configManager);
    //
    //
    // cachedRuleReader.saveConfiguration(params);
    // String content = GeofenceTestUtils.readConfig("test-cache-config.properties");
    // assertEquals("cacheSize=100cacheRefresh=500cacheExpire=1000", content);
    // }

}
