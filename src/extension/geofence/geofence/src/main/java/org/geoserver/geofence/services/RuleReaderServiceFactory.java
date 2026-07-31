/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.List;
import java.util.stream.Collectors;
import org.geofence.core.services.RuleReaderService;
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

    /** Fails startup fast if the configured active service name doesn't match any registered RuleReaderService. */
    @Override
    public void afterSingletonsInstantiated() {
        if (fixedService == null) {
            setActiveServiceName(activeServiceName);
        }
    }

    public RuleReaderService getService() {
        return fixedService != null ? fixedService : resolve(activeServiceName);
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
        if (!context.containsBean(name) || !context.isTypeMatch(name, RuleReaderService.class) || !isEligible(name)) {
            throw new IllegalArgumentException(
                    "No such RuleReaderService bean: " + name + ". Available: " + getAvailableServiceNames());
        }
        this.activeServiceName = name;
    }

    private boolean isEligible(String name) {
        return allowDecorators || !context.isTypeMatch(name, RuleReaderDecorator.class);
    }

    private RuleReaderService resolve(String name) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext was not injected into the factory.");
        }
        if (name == null || !context.containsBean(name)) {
            throw new IllegalStateException(
                    "No active RuleReaderService selected [" + name + "]. Available: " + getAvailableServiceNames());
        }
        return context.getBean(name, RuleReaderService.class);
    }
}
