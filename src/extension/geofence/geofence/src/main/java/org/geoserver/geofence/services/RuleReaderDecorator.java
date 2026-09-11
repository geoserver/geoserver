/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.services;

import org.geofence.core.services.RuleReaderService;

/**
 * Marker for {@link RuleReaderService} implementations that wrap/decorate another {@code RuleReaderService} (e.g.
 * {@code CachedRuleReader}, which adds caching) rather than being a terminal engine.
 *
 * <p>{@link RuleReaderServiceFactory} excludes decorators from its backend candidate list by default: selecting a
 * decorator as the backend would create a call loop, since the decorator's own implementation calls back through the
 * backend factory to satisfy cache misses.
 */
public interface RuleReaderDecorator {}
