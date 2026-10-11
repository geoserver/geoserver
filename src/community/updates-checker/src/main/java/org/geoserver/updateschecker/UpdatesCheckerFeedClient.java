/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Downloads the upstream version feed over HTTPS and keeps the last good copy in memory.
 *
 * <p>One attempt per call, no retry: 5 second connect and read timeouts, and the body must be complete within 5 seconds
 * of the start of the request (checked between reads, so a slow server can hold a call for about 15 seconds at most),
 * redirects are not followed, only HTTP 200 and 304 are accepted, no {@code Accept-Encoding} is sent, and the body is
 * capped at 1 MiB. The ETag of the last good response is replayed exactly as received as {@code If-None-Match}, so an
 * unchanged feed costs a {@code 304}. It uses {@link HttpURLConnection} because the Java 11 {@code java.net.http}
 * client reports a spurious timeout on about one TLS request in a hundred.
 *
 * <p>What was learned from the last fetch lives in one immutable {@link State}, replaced in a single atomic step, so a
 * reader never sees a body paired with another response's ETag. A failed fetch keeps the previous good copy and is
 * always logged at WARN, whatever {@code logUpdateChecks} says.
 */
public class UpdatesCheckerFeedClient {

    private static final Logger LOGGER = Logging.getLogger(UpdatesCheckerService.LOGGER_NAME);

    /** Application property (system property, context parameter or environment variable) overriding the feed URL. */
    public static final String FEED_URL_PROPERTY = "UPDATES_CHECKER_FEED_URL";

    private static final URI DEFAULT_FEED_URI =
            URI.create("https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json");

    private static final int TIMEOUT_MILLIS = 5000;

    /** The largest response accepted; a bigger one is discarded as a failed fetch. */
    static final int MAX_BODY_BYTES = 1024 * 1024;

    /** What is known about the feed, replaced atomically after every attempt. */
    public static final class State {
        private final UpdatesCheckerFeed feed;
        private final String etag;
        private final long checkedAtMillis;
        private final boolean attempted;
        private final boolean lastAttemptFailed;
        private final UpdatesCheckerText lastError;

        private State(
                UpdatesCheckerFeed feed,
                String etag,
                long checkedAtMillis,
                boolean attempted,
                boolean lastAttemptFailed,
                UpdatesCheckerText lastError) {
            this.feed = feed;
            this.etag = etag;
            this.checkedAtMillis = checkedAtMillis;
            this.attempted = attempted;
            this.lastAttemptFailed = lastAttemptFailed;
            this.lastError = lastError;
        }

        /** @return the last good feed, or {@code null} if no fetch has ever succeeded. */
        public UpdatesCheckerFeed getFeed() {
            return feed;
        }

        /** @return when the feed was last confirmed (epoch milliseconds), or 0 if never. */
        public long getCheckedAtMillis() {
            return checkedAtMillis;
        }

        /** @return whether a fetch has been tried at least once since the server started. */
        public boolean isAttempted() {
            return attempted;
        }

        /**
         * @return whether the most recent attempt failed (the last good feed, if any, is still in {@link #getFeed}).
         */
        public boolean isLastAttemptFailed() {
            return lastAttemptFailed;
        }

        /** @return why the last attempt failed, to show in the reader's language, or {@code null}. */
        public UpdatesCheckerText getLastError() {
            return lastError;
        }
    }

    private final UpdatesCheckerFeedParser parser = new UpdatesCheckerFeedParser();

    private volatile State state = new State(null, null, 0, false, false, null);

    /** @return the URL the version feed is fetched from: the override property if set and valid, else the default. */
    public URI getFeedUri() {
        String override = GeoServerExtensions.getProperty(FEED_URL_PROPERTY);
        if (override == null || override.trim().isEmpty()) {
            return DEFAULT_FEED_URI;
        }
        try {
            URI uri = URI.create(override.trim());
            // never plain http, whatever the host: the feed decides what administrators are told to do
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(UpdatesCheckerMessages.base("log.override.notHttps"));
            }
            return uri;
        } catch (IllegalArgumentException e) {
            LOGGER.warning(UpdatesCheckerMessages.base(
                    "log.invalidOverride",
                    FEED_URL_PROPERTY,
                    UpdatesCheckerFeedText.forLog(override),
                    UpdatesCheckerFeedText.forLog(e.getMessage())));
            return DEFAULT_FEED_URI;
        }
    }

    /** @return what is currently known about the feed. Never blocks and never touches the network. */
    public State getState() {
        return state;
    }

    /**
     * Fetches the feed once, blocking for at most the timeouts above, and records the outcome. Never throws.
     *
     * @return the state after this attempt
     */
    public State fetch() {
        State before = state;
        try {
            State after = fetchOnce(before);
            state = after;
            return after;
        } catch (UpdatesCheckerFeedException e) {
            return recordFailure(before, e.getReason(), e, false);
        } catch (IOException e) {
            return recordFailure(before, describe(e), e, false);
        } catch (RuntimeException e) {
            return recordFailure(before, describe(e), e, true);
        }
    }

    private State fetchOnce(State before) throws IOException, UpdatesCheckerFeedException {
        long deadline = System.nanoTime() + TIMEOUT_MILLIS * 1_000_000L;
        HttpURLConnection connection = (HttpURLConnection) getFeedUri().toURL().openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            boolean conditional = before.feed != null && before.etag != null;
            if (conditional) {
                connection.setRequestProperty("If-None-Match", before.etag);
            }
            int status = connection.getResponseCode();
            if (status == 304 && conditional) {
                return new State(before.feed, before.etag, System.currentTimeMillis(), true, false, null);
            }
            if (status != 200) {
                throw new UpdatesCheckerFeedException(
                        UpdatesCheckerText.of("fetch.error.http", String.valueOf(status)));
            }
            String encoding = connection.getHeaderField("Content-Encoding");
            if (encoding != null && !"identity".equalsIgnoreCase(encoding.trim())) {
                throw new UpdatesCheckerFeedException(UpdatesCheckerText.of("fetch.error.encoding"));
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_BODY_BYTES) {
                throw new UpdatesCheckerFeedException(
                        UpdatesCheckerText.of("fetch.error.tooLarge", String.valueOf(MAX_BODY_BYTES)));
            }
            byte[] bytes;
            try (InputStream body = connection.getInputStream()) {
                bytes = readBounded(body, deadline);
            }
            UpdatesCheckerFeed feed = parser.parse(bytes);
            for (UpdatesCheckerText warning : feed.getWarnings()) {
                LOGGER.warning(UpdatesCheckerMessages.base("log.feedWarning", warning.base()));
            }
            return new State(feed, connection.getHeaderField("ETag"), System.currentTimeMillis(), true, false, null);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Reads the body, giving up as soon as it exceeds the size limit or the request has run past its deadline, so a
     * server that trickles bytes cannot hold the check for longer than the timeout.
     */
    private static byte[] readBounded(InputStream body, long deadline) throws IOException, UpdatesCheckerFeedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = body.read(buffer, 0, buffer.length)) != -1) {
            if (System.nanoTime() - deadline > 0) {
                throw new SocketTimeoutException("deadline");
            }
            total += read;
            if (total > MAX_BODY_BYTES) {
                throw new UpdatesCheckerFeedException(
                        UpdatesCheckerText.of("fetch.error.tooLarge", String.valueOf(MAX_BODY_BYTES)));
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private State recordFailure(State before, UpdatesCheckerText reason, Exception cause, boolean unexpected) {
        // routine network trouble gets one line; only a genuinely unexpected error gets its stack trace
        String line = UpdatesCheckerMessages.base("log.fetchFailed", UpdatesCheckerFeedText.forLog(reason.base()));
        if (unexpected) {
            LOGGER.log(Level.WARNING, line, cause);
        } else {
            LOGGER.warning(line);
        }
        State after = new State(before.feed, before.etag, before.checkedAtMillis, true, true, reason);
        state = after;
        return after;
    }

    /**
     * The reason for a failure that came from the network stack or an unexpected error: its own detail, as an argument.
     */
    private static UpdatesCheckerText describe(Exception e) {
        if (e instanceof SocketTimeoutException) {
            return UpdatesCheckerText.of("fetch.error.timeout");
        }
        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return UpdatesCheckerText.of("fetch.error.other", UpdatesCheckerFeedText.truncate(detail));
    }
}
