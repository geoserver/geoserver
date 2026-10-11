/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import org.geoserver.platform.ContextLoadedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Runs the update check once at server startup, so a headless deployment that nobody logs into still gets a server-log
 * line. The check runs on a single short-lived daemon thread, so an unreachable feed can never delay startup and
 * nothing is left running after undeploy.
 */
public class UpdatesCheckerStartupChecker implements ApplicationListener<ContextLoadedEvent> {

    public static final String THREAD_NAME = "updates-checker-startup";

    private final UpdatesCheckerService service;

    public UpdatesCheckerStartupChecker(UpdatesCheckerService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextLoadedEvent event) {
        start();
    }

    /** @return the started check thread, so a test can wait for it. */
    Thread start() {
        Thread thread = new Thread(this::runCheck, THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void runCheck() {
        service.check(UpdatesCheckerService.Trigger.STARTUP);
    }
}
