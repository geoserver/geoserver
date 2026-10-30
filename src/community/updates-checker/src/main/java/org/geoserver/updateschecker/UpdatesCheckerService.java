/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ManifestLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.Status.UpdateState;
import org.geotools.util.logging.Logging;

/**
 * Runs, evaluates and logs every update check, whatever started it: server startup, the first view of an admin session,
 * Check Now or REST. {@link #getStatus()} reads the cached feed; {@link #check} fetches it first.
 */
public class UpdatesCheckerService {

    /** The logger of the module, named after it; a package name cannot hold the hyphen. */
    public static final String LOGGER_NAME = "org.geoserver.updates-checker";

    private static final Logger LOGGER = Logging.getLogger(LOGGER_NAME);

    /** What started a check. */
    public enum Trigger {
        STARTUP(false),
        FIRST_VIEW(false),
        MANUAL(true);

        private final boolean ignoresEnabled;

        Trigger(boolean ignoresEnabled) {
            this.ignoresEnabled = ignoresEnabled;
        }
    }

    /** Developer aid, not documented: a version, such as {@code 2.28.3}, that replaces the real one. */
    public static final String FORCE_VERSION_PROPERTY = "GS_UPDATES_CHECKER_OVERRIDE";

    private final ConfigPersister configPersister;

    private final FeedClient feedClient;

    public UpdatesCheckerService(ConfigPersister configPersister, FeedClient feedClient) {
        this.configPersister = configPersister;
        this.feedClient = feedClient;
    }

    /** @return the {@code Implementation-Version} of the GeoServer manifest, or {@code null} */
    protected String readManifestVersion() {
        try {
            Manifest manifest = ManifestLoader.getManifest(GeoServer.class);
            return manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (Exception e) {
            LOGGER.log(Level.FINE, Messages.base("log.versionReadFailed"), e);
            return null;
        }
    }

    public RunningVersion getRunningVersion() {
        String forced = GeoServerExtensions.getProperty(FORCE_VERSION_PROPERTY);
        return RunningVersion.of(forced == null || forced.isBlank() ? readManifestVersion() : forced.trim());
    }

    /** @return the advisories named by {@link Suppression#PROPERTY}, ignoring entries that are not ids */
    public Suppression getSuppression() {
        return parseSuppression(new ArrayList<>());
    }

    private static Suppression parseSuppression(List<String> rejected) {
        return Suppression.parse(GeoServerExtensions.getProperty(Suppression.PROPERTY), rejected);
    }

    /** Logs the suppressed advisories and the ignored entries at WARN, which the production logging profile shows. */
    public void logSuppression() {
        List<String> rejected = new ArrayList<>();
        Suppression suppression = parseSuppression(rejected);
        if (!suppression.isEmpty()) {
            LOGGER.warning(Messages.base("log.suppression.configured", Suppression.PROPERTY, suppression));
        }
        if (!rejected.isEmpty()) {
            LOGGER.warning(Messages.base("log.suppression.invalid", Suppression.PROPERTY, String.join(", ", rejected)));
        }
    }

    /** @return the status from the cached feed, without touching the network */
    public Status getStatus() {
        return buildStatus(getRunningVersion(), feedClient.getState(), getSuppression());
    }

    /**
     * Fetches the feed, unless the check is disabled for this trigger or the version is unknown, then evaluates and
     * logs the result. Never throws.
     */
    public Status check(Trigger trigger) {
        return check(trigger, null);
    }

    /** @param override the advisories to suppress for this call alone, or {@code null} for the server's */
    public Status check(Trigger trigger, Suppression override) {
        Suppression suppression = override == null ? getSuppression() : override;
        Config config = configPersister.getConfig();
        RunningVersion running = getRunningVersion();
        if (!trigger.ignoresEnabled && !config.isEnabled()) {
            return buildStatus(running, feedClient.getState(), suppression);
        }
        if (running.getKind() == RunningVersion.Kind.UNKNOWN) {
            if (trigger == Trigger.STARTUP) {
                LOGGER.warning(Messages.base("log.versionUnknown"));
            }
            return buildStatus(running, feedClient.getState(), suppression);
        }
        FeedClient.State state = feedClient.fetch();
        Status status = buildStatus(running, state, suppression);
        if (!state.isLastAttemptFailed() && config.isLogUpdateChecks()) {
            logResult(trigger, config, running, status, override != null);
        }
        return status;
    }

    private Status buildStatus(RunningVersion running, FeedClient.State state, Suppression suppression) {
        List<Notification> notifications =
                Evaluator.evaluate(running, state.getFeed(), state.isAttempted(), suppression);
        List<String> suppressed = Evaluator.suppressedAdvisories(running, state.getFeed(), suppression);
        return new Status(running, state.getFeed(), notifications, suppressed, state);
    }

    private void logResult(
            Trigger trigger, Config config, RunningVersion running, Status status, boolean suppressedByRequest) {
        boolean hide = config.isHideVersionNumbersInLogs();
        List<Notification> notifications = status.getNotifications();
        String text;
        Level level;
        if (notifications.isEmpty()) {
            String key = running.getKind() == RunningVersion.Kind.DEVELOPMENT ? "log.devBuild" : "log.upToDate";
            text = hide ? Messages.base(key + ".redacted") : Messages.base(key, running.getRaw());
            level = Level.INFO;
        } else {
            UpdateState state = status.getUpdateState();
            if (hide) {
                text = Messages.base("log.state", state);
            } else if (status.getNewerVersion() != null) {
                text = Messages.base("log.state.newer", state, running.getRaw(), status.getNewerVersion());
            } else {
                text = Messages.base("log.state.running", state, running.getRaw());
            }
            level = state == UpdateState.UP_TO_DATE ? Level.INFO : Level.WARNING;
        }
        if (!status.getSuppressed().isEmpty()) {
            text += Messages.base(
                    suppressedByRequest ? "log.suppressed.request" : "log.suppressed",
                    status.getSuppressed().size());
        }
        LOGGER.log(
                level,
                Messages.base("log.line", Messages.base("log.trigger." + trigger.name()), Messages.forLog(text)));
    }
}
