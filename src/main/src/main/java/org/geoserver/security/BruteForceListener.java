/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.security.config.BruteForcePreventionConfig;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureProviderNotFoundEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Brute force attack preventer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class BruteForceListener implements ApplicationListener<AbstractAuthenticationEvent>, GeoServerLifecycleHandler {

    static final Logger LOGGER = Logging.getLogger(BruteForceListener.class);

    private static final ThreadLocal<Boolean> THROTTLING_DISABLED = new ThreadLocal<>();

    /**
     * Simple single node delayed login tracker. Should be made pluggable to allow by some sort of network service for a
     * clustered installation
     */
    Map<String, AtomicInteger> delayedUsers = new ConcurrentHashMap<>();

    private GeoServerSecurityManager securityManager;

    public BruteForceListener(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    /**
     * Executes a task with authentication throttling disabled for the current thread.
     *
     * @param task the task to execute
     * @param <V> the return type of the task
     * @return the result of the task
     * @throws Exception if the task fails
     */
    public static <V> V withThrottlingDisabled(Callable<V> task) throws Exception {
        Boolean previous = THROTTLING_DISABLED.get();
        try {
            setThrottlingDisabled(true);
            return task.call();
        } finally {
            if (Boolean.TRUE.equals(previous)) {
                THROTTLING_DISABLED.set(Boolean.TRUE);
            } else {
                THROTTLING_DISABLED.remove();
            }
        }
    }

    /**
     * Disables the authentication delay for the current thread. This is useful for internal geoserver checks that don't
     * want to wait for the brute force delay
     *
     * @param disabled true to disable, false to enable
     */
    private static void setThrottlingDisabled(boolean disabled) {
        if (disabled) {
            THROTTLING_DISABLED.set(Boolean.TRUE);
        } else {
            THROTTLING_DISABLED.remove();
        }
    }

    private BruteForcePreventionConfig getConfig() {
        BruteForcePreventionConfig config = securityManager.getSecurityConfig().getBruteForcePrevention();
        if (config == null) {
            return BruteForcePreventionConfig.DEFAULT;
        } else {
            return config;
        }
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (Boolean.TRUE.equals(THROTTLING_DISABLED.get())) {
            return;
        }

        // is it enabled?
        BruteForcePreventionConfig config = getConfig();
        if (!config.isEnabled()) {
            return;
        }

        // some addresses can be whitelisted and allowed to login anyways
        HttpServletRequest request = GeoServerSecurityFilterChainProxy.REQUEST.get();
        if (requestAddressInWhiteList(request, config)) {
            return;
        }

        // Yes, enabled, check for concurrent login attempt
        Authentication authentication = event.getAuthentication();
        String name = getUserName(authentication);
        if (name == null) {
            LOGGER.warning("Brute force attack prevention enabled, but Spring Authentication "
                    + "does not provide a user name, skipping: "
                    + authentication);
            return;
        }

        // do we have a delayed login in flight already? If so, kill this login attempt
        // no matter if successful or not
        final AtomicInteger counter = delayedUsers.get(name);
        if (counter != null) {
            int count = counter.incrementAndGet();
            logFailedRequest(request, name, count);
            throw new ConcurrentAuthenticationException(name, count);
        }

        if (event instanceof AuthenticationFailureBadCredentialsEvent
                || event instanceof AuthenticationFailureProviderNotFoundEvent) {
            // are we above the max number of blocked threads already?
            int maxBlockedThreads = config.getMaxBlockedThreads();
            if (maxBlockedThreads > 0 && delayedUsers.size() > maxBlockedThreads) {
                throw new MaxBlockedThreadsException(1);
            }

            delayedUsers.put(name, new AtomicInteger(1));
            try {
                logFailedRequest(request, name, 0);
                long delay = computeDelay(config);
                if (delay > 0) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Brute force attack prevention, delaying login for " + delay + "ms");
                    }
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                // duh
            } finally {
                delayedUsers.remove(name);
            }
        }
    }

    private boolean requestAddressInWhiteList(HttpServletRequest request, BruteForcePreventionConfig config) {
        // is there a white list?
        if (config.getWhitelistAddressMatchers() == null) {
            return false;
        }

        return config.getWhitelistAddressMatchers().stream().anyMatch(matcher -> matcher.matches(request));
    }

    @com.google.common.annotations.VisibleForTesting
    long computeDelay(BruteForcePreventionConfig config) {
        long min = config.getMinDelaySeconds() * 1000;
        long max = config.getMaxDelaySeconds() * 1000;
        return min + (long) ((max - min) * ThreadLocalRandom.current().nextDouble());
    }

    /** Returns the username for this authentication, or null if missing or cannot be determined */
    private String getUserName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal != null) {
            if (principal instanceof UserDetails details) {
                return details.getUsername();
            } else if (principal instanceof String string) {
                return string;
            }
        }
        return authentication.getName();
    }

    private void logFailedRequest(HttpServletRequest request, String name, int count) {
        StringBuilder sb = new StringBuilder("Failed login, user ").append(name).append(" from ");
        sb.append(request.getRemoteAddr());
        // log x-forwarded-for too, but not exclusively as it can be spoofed
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            sb.append(", forwarded for ").append(forwardedFor);
        }
        if (count > 0) {
            sb.append(", stopped ").append(count).append(" concurrent logins during authentication delay");
        }

        LOGGER.warning(sb.toString());
    }

    @Override
    public void onReset() {
        delayedUsers.clear();
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        delayedUsers.clear();
    }
}
