/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import org.geofence.core.services.RuleReaderService;
import org.mockito.Mockito;

/**
 * Spring factory-bean helper with an explicit {@link RuleReaderService} return type.
 *
 * <p>{@code Mockito.mock(Class)} is a generic method that erases to {@code Object}, so a bean defined directly with
 * {@code factory-method="mock"} on {@code org.mockito.Mockito} isn't recognized as a {@code RuleReaderService} by
 * Spring's type-based bean lookups (used e.g. by {@code RuleReaderServiceFactory}). This wrapper's declared return type
 * fixes that.
 */
public final class MockRuleReaderServiceFactory {

    private MockRuleReaderServiceFactory() {}

    public static RuleReaderService mock() {
        return Mockito.mock(RuleReaderService.class);
    }
}
