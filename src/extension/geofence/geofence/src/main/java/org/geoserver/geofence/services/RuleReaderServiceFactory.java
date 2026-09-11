/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geofence.core.services.RuleReaderService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Selects, among all {@link RuleReaderService} beans available in the Spring context (e.g. the embedded engine provided
 * by geofence-server, and/or the REST client to a standalone GeoFence), which one is currently active.
 *
 * <p>The active bean can be changed at runtime via {@link #setActiveServiceName(String)}, so a GeoServer instance
 * running with multiple candidate backends on the classpath can switch between them without a restart.
 *
 * <p>The initial name is validated once all singletons have been instantiated ({@link #afterSingletonsInstantiated()}),
 * so a stale or misspelled {@code ruleReaderBackend} fails GeoServer startup with a clear message instead of failing on
 * the first request.
 *
 * <p>Instances used as a <em>backend</em> selector must exclude {@link RuleReaderDecorator}s (e.g. the cache wrapper):
 * selecting a decorator as the backend would create a call loop, since the decorator itself calls back through the
 * backend factory to satisfy cache misses. Instances used as a <em>frontend</em> selector should allow them, since the
 * cache wrapper is the normal, expected frontend choice.
 *
 * @author etj
 */
public class RuleReaderServiceFactory implements ApplicationContextAware, SmartInitializingSingleton {

    /** Bean name of the embedded engine, provided by geofence-server when it's on the classpath. */
    public static final String INTERNAL_RULE_READER_NAME = "ruleReaderServiceImpl";

    /** Bean name of the REST client to a standalone GeoFence instance. */
    public static final String REMOTE_RULE_READER_NAME = "restRuleReaderService";

    private ApplicationContext context;
    private volatile String activeServiceName;
    private final String defaultServiceName;
    private final boolean allowDecorators;

    /** Non-null only when built via {@link #of(RuleReaderService)}; bypasses context lookup entirely. */
    private final RuleReaderService fixedService;

    private static final Logger LOGGER = Logging.getLogger(RuleReaderServiceFactory.class);

    /** Fail-safe fallback served when the active backend can't be resolved (e.g. embedded engine failed to boot). */
    private final RuleReaderService denyAll = new DenyAllRuleReaderService();

    /** Throttles the "backend unavailable" warning to once per unavailable stretch. */
    private volatile boolean backendUnavailableWarned = false;

    /** While non-empty, deny-all is served; each entry reports whether its source has become usable again. */
    private final Map<String, BooleanSupplier> pendingRecoveries = new ConcurrentHashMap<>();

    /**
     * @param defaultServiceName the initial bean name, resolved from a ruleReaderBackend/Frontend property
     * @param allowDecorators whether {@link RuleReaderDecorator} beans (e.g. the cache wrapper) are valid candidates;
     *     {@code false} for a backend factory, {@code true} for a frontend factory
     */
    public RuleReaderServiceFactory(String defaultServiceName, boolean allowDecorators) {
        this.activeServiceName = defaultServiceName;
        this.defaultServiceName = defaultServiceName;
        this.allowDecorators = allowDecorators;
        this.fixedService = null;
    }

    private RuleReaderServiceFactory(RuleReaderService fixedService) {
        this.defaultServiceName = null;
        this.allowDecorators = true;
        this.fixedService = fixedService;
    }

    /**
     * Wraps an already-resolved {@link RuleReaderService}, bypassing Spring context lookup entirely. Useful for tests
     * that build the cache object graph by hand, outside of Spring.
     */
    public static RuleReaderServiceFactory of(RuleReaderService fixedService) {
        return new RuleReaderServiceFactory(fixedService);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Fails startup fast if the configured active service name doesn't match any registered bean definition.
     * Deliberately checks only bean-definition existence: the fuller type/eligibility check can force Spring to
     * construct the bean to determine its type, defeating this factory's point of keeping backends lazy. That check
     * runs in {@link #resolve}, on first actual use.
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (fixedService != null) {
            return;
        }
        requireContext();
        if (activeServiceName == null || !context.containsBean(activeServiceName)) {
            throw new IllegalArgumentException("No such RuleReaderService bean: " + activeServiceName);
        }
    }

    /**
     * Serves the deny-all fallback until {@code recovery} reports {@code source} usable again. Retried on every
     * {@link #getService()} call, so fixing the configuration takes effect without another GeoServer reload. Access
     * resumes only once every registered source has recovered.
     */
    public void denyUntilRecovered(String source, BooleanSupplier recovery) {
        pendingRecoveries.put(source, recovery);
    }

    public RuleReaderService getService() {
        if (fixedService != null) {
            return fixedService;
        }
        if (!pendingRecoveries.isEmpty()) {
            pendingRecoveries.entrySet().removeIf(entry -> entry.getValue().getAsBoolean());
            if (!pendingRecoveries.isEmpty()) {
                return denyAll;
            }
            LOGGER.log(Level.INFO, "GeoFence rule reader backend recovered; resuming normal access");
        }
        try {
            RuleReaderService service = resolve(activeServiceName);
            if (backendUnavailableWarned) {
                LOGGER.log(Level.INFO, "GeoFence rule reader backend ''{0}'' is available again", activeServiceName);
                backendUnavailableWarned = false;
            }
            return service;
        } catch (RuntimeException e) {
            // backend unavailable (e.g. no datasource configured) - deny rather than propagate; recovers automatically.
            if (!backendUnavailableWarned) {
                LOGGER.log(
                        Level.SEVERE,
                        "GeoFence rule reader backend ''{0}'' is unavailable; denying all access until it recovers. "
                                + "Cause: {1}",
                        new Object[] {activeServiceName, rootCauseMessage(e)});
                // the full trace is mostly Spring bean-creation frames; keep it for whoever needs to dig
                LOGGER.log(Level.FINE, "GeoFence rule reader backend resolution failed", e);
                backendUnavailableWarned = true;
            }
            return denyAll;
        }
    }

    /**
     * The deepest cause that carries a message. The layers wrapping the real problem (Spring bean creation, Hikari pool
     * init) repeat each other, and the innermost one is sometimes message-less, so neither end of the chain reliably
     * says what went wrong.
     */
    private static String rootCauseMessage(Throwable thrown) {
        String message = thrown.toString();
        Throwable cause = thrown;
        for (int depth = 0; cause != null && depth < 20; cause = cause.getCause(), depth++) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            }
        }
        return message;
    }

    public String getActiveServiceName() {
        return activeServiceName;
    }

    /** The bean name this factory was originally configured with, ignoring any later runtime switch. */
    public String getDefaultServiceName() {
        return defaultServiceName;
    }

    /** Names of the {@link RuleReaderService} beans eligible as a candidate (see {@link #allowDecorators}). */
    public List<String> getAvailableServiceNames() {
        return List.of(context.getBeanNamesForType(RuleReaderService.class)).stream()
                .filter(this::isEligible)
                .collect(Collectors.toList());
    }

    public void setActiveServiceName(String name) {
        validateServiceName(name);
        this.activeServiceName = name;
    }

    /**
     * Checks that {@code name} can be served by this factory, without switching to it - so a caller applying several
     * settings at once can reject an invalid one before any of them takes effect.
     *
     * @throws IllegalArgumentException if the bean is missing, of the wrong type, or ineligible here
     */
    public void validateServiceName(String name) {
        requireContext();
        String reason = rejectionReason(name);
        if (reason != null) {
            throw new IllegalArgumentException(
                    "Cannot select GeoFence rule reader: " + reason + ". Available: " + getAvailableServiceNames());
        }
    }

    private boolean isEligible(String name) {
        return allowDecorators || !context.isTypeMatch(name, RuleReaderDecorator.class);
    }

    /** Why {@code name} can't be served by this factory, or {@code null} if it can. */
    private String rejectionReason(String name) {
        if (name == null || !context.containsBean(name)) {
            return "no such bean: " + name;
        }
        if (!context.isTypeMatch(name, RuleReaderService.class)) {
            return "bean '" + name + "' is not a RuleReaderService";
        }
        if (!isEligible(name)) {
            // a decorator selected as backend would recurse into this factory on every cache miss
            return "bean '" + name + "' is a decorator, usable only as a frontend";
        }
        return null;
    }

    private void requireContext() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext was not injected into the factory.");
        }
    }

    private RuleReaderService resolve(String name) {
        requireContext();
        String reason = rejectionReason(name);
        if (reason != null) {
            throw new IllegalStateException(
                    "Cannot resolve GeoFence rule reader: " + reason + ". Available: " + getAvailableServiceNames());
        }
        return context.getBean(name, RuleReaderService.class);
    }
}
