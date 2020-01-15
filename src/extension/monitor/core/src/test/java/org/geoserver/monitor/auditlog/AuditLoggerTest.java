/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.auditlog;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;

public class AuditLoggerTest {

    @Rule public TemporaryFolder directory = new TemporaryFolder();

    private File bad;
    private File good;
    private GeoServerResourceLoader loader;
    private MonitorConfig config;
    private GenericApplicationContext context;

    @Before
    public void setUp() throws Exception {
        this.bad = this.directory.newFolder("bad");
        this.good = this.directory.newFolder("good");
        System.setProperty("GEOSERVER_AUDIT_PATH", this.good.getAbsolutePath());
        this.loader = new GeoServerResourceLoader(this.directory.getRoot());
        this.config = new MonitorConfig(this.loader);
        this.config.getProperties().put("audit.enabled", "true");
        this.config.getProperties().put("audit.path", this.bad.getAbsolutePath());
        this.context = new GenericApplicationContext();
        this.context.getBeanFactory().registerSingleton("resourceLoader", this.loader);
        this.context.refresh();
        new GeoServerExtensions().setApplicationContext(this.context);
    }

    @After
    public void tearDown() {
        System.clearProperty("GEOSERVER_AUDIT_PATH");
        new GeoServerExtensions().setApplicationContext(null);
    }

    @Test
    public void testInitDumperRaceCondition() throws Exception {
        // Run the test multiple times since the race condition can only
        // happen during the audit logger initialization.
        int numTests = 10;
        int numRequests = 8;
        for (int i = 0; i < numTests; i++) {
            AuditLogger logger = new AuditLogger(this.config, this.loader);
            List<Callable<Object>> tasks = new ArrayList<>(numRequests);
            for (int j = 0; j < numRequests; j++) {
                RequestData data = new RequestData();
                data.setId(1);
                data.setHost("localhost");
                data.setStartTime(new Date(0));
                data.setEndTime(new Date(0));
                tasks.add(Executors.callable(() -> logger.requestPostProcessed(data)));
            }
            ExecutorService executor = Executors.newFixedThreadPool(numRequests);
            try {
                for (Future<Object> future : executor.invokeAll(tasks)) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
                logger.onApplicationEvent(new ContextClosedEvent(this.context));
            }
        }
        // Verify that no log files were created in the properties file directory.
        assertThat(this.bad.listFiles(), emptyArray());
        // Verify that each test wrote a single log file to the system property directory.
        File[] files = this.good.listFiles();
        assertThat(files, arrayWithSize(numTests));
        // Verify that all log files have the same byte size.
        long size = files[0].length();
        assertThat(size, greaterThan(0L));
        for (int i = 1; i < numTests; i++) {
            assertEquals(size, files[i].length());
        }
    }
}
