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
 * Downloads the version feed and keeps the last good copy.
 *
 * <p>One attempt per call: 5 second timeouts and a 5 second deadline for the body, no redirects, only HTTP 200 and 304,
 * and at most 1 MiB. The last ETag is sent back as {@code If-None-Match}. A failure keeps the last good copy and is
 * always logged at WARN.
 */
public class FeedClient {

    private static final Logger LOGGER = Logging.getLogger(UpdatesCheckerService.LOGGER_NAME);

    /** Application property: system property, context parameter or environment variable. */
    public static final String FEED_URL_PROPERTY = "UPDATES_CHECKER_FEED_URL";

    private static final URI DEFAULT_FEED_URI =
            URI.create("https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json");

    private static final int TIMEOUT_MILLIS = 5000;

    static final int MAX_BODY_BYTES = 1024 * 1024;

    /** What is known about the feed, replaced as a whole after every attempt, so an ETag always matches its body. */
    public static final class State {
        private final Feed feed;
        private final String etag;
        private final long checkedAtMillis;
        private final boolean attempted;
        private final boolean lastAttemptFailed;
        private final Text lastError;

        private State(
                Feed feed,
                String etag,
                long checkedAtMillis,
                boolean attempted,
                boolean lastAttemptFailed,
                Text lastError) {
            this.feed = feed;
            this.etag = etag;
            this.checkedAtMillis = checkedAtMillis;
            this.attempted = attempted;
            this.lastAttemptFailed = lastAttemptFailed;
            this.lastError = lastError;
        }

        /** @return the last good feed, or {@code null} */
        public Feed getFeed() {
            return feed;
        }

        /** @return when the feed was last confirmed, in epoch milliseconds, or 0 if never */
        public long getCheckedAtMillis() {
            return checkedAtMillis;
        }

        public boolean isAttempted() {
            return attempted;
        }

        public boolean isLastAttemptFailed() {
            return lastAttemptFailed;
        }

        /** @return why the last attempt failed, or {@code null} */
        public Text getLastError() {
            return lastError;
        }
    }

    private volatile State state = new State(null, null, 0, false, false, null);

    /** @return the {@link #FEED_URL_PROPERTY} URL if set and valid, otherwise the default */
    public URI getFeedUri() {
        String override = GeoServerExtensions.getProperty(FEED_URL_PROPERTY);
        if (override == null || override.trim().isEmpty()) {
            return DEFAULT_FEED_URI;
        }
        try {
            URI uri = URI.create(override.trim());
            String scheme = uri.getScheme();
            boolean web = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
            if (!web || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(Messages.base("log.override.notHttp"));
            }
            return uri;
        } catch (IllegalArgumentException e) {
            LOGGER.warning(Messages.base(
                    "log.invalidOverride",
                    FEED_URL_PROPERTY,
                    Messages.forLog(override),
                    Messages.forLog(e.getMessage())));
            return DEFAULT_FEED_URI;
        }
    }

    /** @return what is known about the feed, without touching the network */
    public State getState() {
        return state;
    }

    /** Fetches the feed once and records the outcome. Never throws. */
    public State fetch() {
        State before = state;
        try {
            State after = fetchOnce(before);
            state = after;
            return after;
        } catch (FeedException e) {
            return recordFailure(before, e.getReason(), e, false);
        } catch (IOException e) {
            return recordFailure(before, describe(e), e, false);
        } catch (RuntimeException e) {
            return recordFailure(before, describe(e), e, true);
        }
    }

    private State fetchOnce(State before) throws IOException, FeedException {
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
                throw new FeedException(Text.of("fetch.error.http", String.valueOf(status)));
            }
            String encoding = connection.getHeaderField("Content-Encoding");
            if (encoding != null && !"identity".equalsIgnoreCase(encoding.trim())) {
                throw new FeedException(Text.of("fetch.error.encoding"));
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_BODY_BYTES) {
                throw new FeedException(Text.of("fetch.error.tooLarge", String.valueOf(MAX_BODY_BYTES)));
            }
            byte[] bytes;
            try (InputStream body = connection.getInputStream()) {
                bytes = readBounded(body, deadline);
            }
            Feed feed = Feed.parse(bytes);
            for (Text warning : feed.getWarnings()) {
                LOGGER.warning(Messages.base("log.feedWarning", warning.base()));
            }
            return new State(feed, connection.getHeaderField("ETag"), System.currentTimeMillis(), true, false, null);
        } finally {
            connection.disconnect();
        }
    }

    /** Reads the body, giving up past the size limit or the deadline, so a trickling server cannot hold the check. */
    private static byte[] readBounded(InputStream body, long deadline) throws IOException, FeedException {
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
                throw new FeedException(Text.of("fetch.error.tooLarge", String.valueOf(MAX_BODY_BYTES)));
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private State recordFailure(State before, Text reason, Exception cause, boolean unexpected) {
        // only an unexpected error gets a stack trace
        String line = Messages.base("log.fetchFailed", Messages.forLog(reason.base()));
        if (unexpected) {
            LOGGER.log(Level.WARNING, line, cause);
        } else {
            LOGGER.warning(line);
        }
        State after = new State(before.feed, before.etag, before.checkedAtMillis, true, true, reason);
        state = after;
        return after;
    }

    private static Text describe(Exception e) {
        if (e instanceof SocketTimeoutException) {
            return Text.of("fetch.error.timeout");
        }
        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return Text.of("fetch.error.other", Feed.truncate(detail));
    }
}
