/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import org.geoserver.platform.ContextLoadedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Checks once at startup, so a server nobody logs into still gets a log line. It runs on a daemon thread, so an
 * unreachable feed cannot delay startup.
 */
public class StartupChecker implements ApplicationListener<ContextLoadedEvent> {

    public static final String THREAD_NAME = "updates-checker-startup";

    private final UpdatesCheckerService service;

    public StartupChecker(UpdatesCheckerService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextLoadedEvent event) {
        start();
    }

    private void runCheck() {
        service.logSuppression();
        service.check(UpdatesCheckerService.Trigger.STARTUP);
    }

    /** @return the thread, so a test can wait for it */
    Thread start() {
        Thread thread = new Thread(this::runCheck, THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
