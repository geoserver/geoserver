/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ManifestLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * The single place where an update check is performed, evaluated and logged, whatever triggered it: server startup, the
 * first view of an admin session, the settings page's Check Now button or the REST endpoint.
 *
 * <p>The feed is held process-wide by {@link UpdatesCheckerFeedClient}. {@link #getStatus()} reads that cache and never
 * touches the network; {@link #check} fetches first. Whether a given admin session has already been checked is the web
 * layer's business, not this class's.
 *
 * <p>Logging: one result line per check, subject to {@code logUpdateChecks}; {@code hideVersionNumbersInLogs} removes
 * version numbers from that line only. Fetch and parse failures are logged by the feed client at WARN, always.
 */
public class UpdatesCheckerService {

    /**
     * Named after the module, so log configuration can address it as {@code org.geoserver.updates-checker}; a Java
     * package name cannot contain a hyphen, so it is not the package name.
     */
    public static final String LOGGER_NAME = "org.geoserver.updates-checker";

    private static final Logger LOGGER = Logging.getLogger(LOGGER_NAME);

    /** What started a check; its label in the log line comes from the message bundle. */
    public enum Trigger {
        STARTUP(false),
        FIRST_VIEW(false),
        MANUAL(true);

        private final boolean ignoresEnabled;

        Trigger(boolean ignoresEnabled) {
            this.ignoresEnabled = ignoresEnabled;
        }
    }

    /** Developer aid, not documented: the version, such as {@code 2.28.3}, this check pretends GeoServer is. */
    public static final String FORCE_VERSION_PROPERTY = "GS_UPDATES_CHECKER_OVERRIDE";

    private final UpdatesCheckerConfigPersister configPersister;

    private final UpdatesCheckerFeedClient feedClient;

    public UpdatesCheckerService(UpdatesCheckerConfigPersister configPersister, UpdatesCheckerFeedClient feedClient) {
        this.configPersister = configPersister;
        this.feedClient = feedClient;
    }

    /** @return the {@code Implementation-Version} of the GeoServer manifest, or {@code null} if it cannot be read. */
    protected String readManifestVersion() {
        try {
            Manifest manifest = ManifestLoader.getManifest(GeoServer.class);
            return manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (Exception e) {
            LOGGER.log(Level.FINE, UpdatesCheckerMessages.base("log.versionReadFailed"), e);
            return null;
        }
    }

    /**
     * @return the version this check treats as running: the real one, unless {@link #FORCE_VERSION_PROPERTY} is set.
     */
    public UpdatesCheckerRunningVersion getRunningVersion() {
        String forced = GeoServerExtensions.getProperty(FORCE_VERSION_PROPERTY);
        return UpdatesCheckerRunningVersion.of(
                forced == null || forced.isBlank() ? readManifestVersion() : forced.trim());
    }

    /** @return the status from what is already cached. Never touches the network. */
    public UpdatesCheckerStatus getStatus() {
        return buildStatus(getRunningVersion(), feedClient.getState());
    }

    /**
     * Fetches the feed (unless the check is disabled for this trigger, or the running version is unknown), then
     * evaluates and logs the result. Blocks for at most the feed client's timeouts. Never throws.
     */
    public UpdatesCheckerStatus check(Trigger trigger) {
        UpdatesCheckerConfig config = configPersister.getConfig();
        UpdatesCheckerRunningVersion running = getRunningVersion();
        if (!trigger.ignoresEnabled && !config.isEnabled()) {
            return buildStatus(running, feedClient.getState());
        }
        if (running.getKind() == UpdatesCheckerRunningVersion.Kind.UNKNOWN) {
            if (trigger == Trigger.STARTUP) {
                LOGGER.warning(UpdatesCheckerMessages.base("log.versionUnknown"));
            }
            return buildStatus(running, feedClient.getState());
        }
        UpdatesCheckerFeedClient.State state = feedClient.fetch();
        UpdatesCheckerStatus status = buildStatus(running, state);
        if (!state.isLastAttemptFailed() && config.isLogUpdateChecks()) {
            logResult(trigger, config, running, status);
        }
        return status;
    }

    private UpdatesCheckerStatus buildStatus(
            UpdatesCheckerRunningVersion running, UpdatesCheckerFeedClient.State state) {
        List<UpdatesCheckerNotification> notifications =
                UpdatesCheckerEvaluator.evaluate(running, state.getFeed(), state.isAttempted());
        return new UpdatesCheckerStatus(running, state.getFeed(), notifications, state);
    }

    private void logResult(
            Trigger trigger,
            UpdatesCheckerConfig config,
            UpdatesCheckerRunningVersion running,
            UpdatesCheckerStatus status) {
        boolean hide = config.isHideVersionNumbersInLogs();
        List<UpdatesCheckerNotification> notifications = status.getNotifications();
        String text;
        Level level;
        if (notifications.isEmpty()) {
            String key = running.getKind() == UpdatesCheckerRunningVersion.Kind.DEVELOPMENT
                    ? "log.devBuild"
                    : "log.upToDate";
            text = hide
                    ? UpdatesCheckerMessages.base(key + ".redacted")
                    : UpdatesCheckerMessages.base(key, running.getRaw());
            level = Level.INFO;
        } else {
            UpdatesCheckerState state = status.getUpdateState();
            if (hide) {
                text = UpdatesCheckerMessages.base("log.state", state);
            } else if (status.getNewerVersion() != null) {
                text = UpdatesCheckerMessages.base(
                        "log.state.newer", state, running.getRaw(), status.getNewerVersion());
            } else {
                text = UpdatesCheckerMessages.base("log.state.running", state, running.getRaw());
            }
            level = state == UpdatesCheckerState.UP_TO_DATE ? Level.INFO : Level.WARNING;
        }
        LOGGER.log(
                level,
                UpdatesCheckerMessages.base(
                        "log.line",
                        UpdatesCheckerMessages.base("log.trigger." + trigger.name()),
                        UpdatesCheckerFeedText.forLog(text)));
    }
}
