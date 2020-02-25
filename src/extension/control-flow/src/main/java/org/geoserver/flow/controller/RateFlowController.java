/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import com.google.common.base.Predicate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.FlowController;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geotools.util.CanonicalSet;
import org.geotools.util.logging.Logging;

/**
 * Limits the rate of requests, and slows them down after the number of requests per unit of time is
 * filled, or throws a HTTP 429 if no delay if configured
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RateFlowController implements FlowController {

    /** The next epoc at which the counter will reset */
    public static final String X_RATE_LIMIT_RESET = "X-Rate-Limit-Reset";

    /** How many request remain in this time slot before the rate limiting occurs */
    public static final String X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";

    /** How many requests per time slot before the rate limiting kicks in */
    public static final String X_RATE_LIMIT_LIMIT = "X-Rate-Limit-Limit";

    /** The context in which the rate limiting occurs */
    public static final String X_RATE_LIMIT_CONTEXT = "X-Rate-Limit-Context";

    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    /** The minimum number of counters we have need to have around before a cleanup is initiated */
    static int COUNTERS_CLEANUP_THRESHOLD =
            Integer.parseInt(
                    System.getProperty("org.geoserver.flow.countersCleanupThreshold", "200"));

    /** The cleanup interval before a cleanup is initiated */
    static int COUNTERS_CLEANUP_INTERVAL =
            Integer.parseInt(
                    System.getProperty("org.geoserver.flow.countersCleanupInterval", "10000"));

    final class Counter {
        volatile long timePeriodId;

        AtomicInteger requests = new AtomicInteger(0);

        public int addRequest(long currPeriodId) {
            if (currPeriodId != timePeriodId) {
                synchronized (this) {
                    if (currPeriodId != timePeriodId) {
                        timePeriodId = currPeriodId;
                        requests.set(0);
                    }
                }
            }

            // increment and return if we have gone above the limit
            return requests.incrementAndGet();
        }

        public synchronized long getTimePeriodId() {
            return timePeriodId;
        }
    }

    /** Thread local holding the current user id */
    static ThreadLocal<String> USER_ID = new ThreadLocal<String>();

    /** Generates a unique key identifying the user making the request */
    KeyGenerator keyGenerator;

    /** Contains all active counters */
    Map<String, Counter> counters = new ConcurrentHashMap<>();

    /** Used to make user keys unique before using them as synchronization locks */
    CanonicalSet<String> canonicalizer = CanonicalSet.newInstance(String.class);

    /** Checks if we should apply this request rate limit to the request */
    Predicate<Request> matcher;

    int maxRequests;

    long timeInterval;

    long delay;

    String action;

    /** Last time we've performed a queue cleanup */
    volatile long lastCleanup = System.currentTimeMillis();

    /**
     * Builds a UserFlowController that will trigger stale queue expiration once 100 queues have
     * been accumulated and
     */
    public RateFlowController(
            Predicate<Request> matcher,
            int maxRequests,
            long timeInterval,
            long delay,
            KeyGenerator keyGenerator) {
        this.matcher = matcher;
        this.maxRequests = maxRequests;
        this.timeInterval = timeInterval;
        this.delay = delay;
        this.keyGenerator = keyGenerator;

        if (delay > 0) {
            this.action = "Delay excess requests " + delay + "ms";
        } else {
            this.action = "Reject excess requests";
        }
    }

    @Override
    public void requestComplete(Request request) {
        // nothing to do
    }

    public boolean requestIncoming(Request request, long timeout) {
        if (!matcher.apply(request)) {
            return true;
        }

        boolean retval = true;
        long now = System.currentTimeMillis();
        long currPeriodId = now / timeInterval;
        String userKey = keyGenerator.getUserKey(request);

        // grab/generate the counter
        Counter counter = counters.get(userKey);
        if (counter == null) {
            userKey = canonicalizer.unique(userKey);
            synchronized (userKey) {
                counter = counters.get(userKey);
                if (counter == null) {
                    counter = new Counter();
                    counters.put(userKey, counter);
                }
            }
        }

        // update the counters
        int requests = counter.addRequest(currPeriodId);
        int residual = maxRequests - requests;

        // set the headers
        HttpServletResponse response = request.getHttpResponse();
        response.addHeader(X_RATE_LIMIT_CONTEXT, matcher.toString());
        response.addIntHeader(X_RATE_LIMIT_LIMIT, maxRequests);
        response.addIntHeader(X_RATE_LIMIT_REMAINING, Math.max(residual, 0));
        response.addDateHeader(X_RATE_LIMIT_RESET, ((currPeriodId + 1) * timeInterval));
        response.addHeader("X-Rate-Limit-Action", action);
        // counter cleanup handling
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(this + ", residual in current time period " + residual);
        }
        if (residual < 0) {
            if (delay <= 0) {
                throw new HttpErrorCodeException(
                        429,
                        "Too many requests requests in the current time period, check X-Rate-Limit HTTP response headers");
            } else if (delay > timeout) {
                // no point in waiting
                return false;
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(this + ", delaying current request");
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, this + ", the delay was abruptly interrupted", e);
                }
            }
        }

        // cleanup stale counters if necessary
        long elapsed = now - lastCleanup;
        if (counters.size() > COUNTERS_CLEANUP_THRESHOLD
                && (elapsed > (timeInterval) || (elapsed > 10000))) {
            int cleanupCount = 0;
            synchronized (this) {
                for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                    Counter c = entry.getValue();
                    long timePeriodId = c.getTimePeriodId();
                    long age = (currPeriodId - timePeriodId) * timeInterval;
                    if (age > COUNTERS_CLEANUP_THRESHOLD) {
                        counters.remove(entry.getKey());
                    }
                }
                lastCleanup = now;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(this + ", purged " + cleanupCount + " stale counters");
                }
            }
        }

        return retval;
    }

    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    public Predicate<Request> getMatcher() {
        return matcher;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public int getPriority() {
        // higher priority, we want to go thought the rate limiters before going through
        // the concurrency ones, as the rate limiters can delay the request and are user specific
        return Integer.MIN_VALUE + maxRequests * (int) (86400 / timeInterval);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + matcher + ", action=" + action + "]";
    }
}
