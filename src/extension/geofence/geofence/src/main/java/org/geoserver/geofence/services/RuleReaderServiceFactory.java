/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.geofence.core.services.RuleReaderService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Returns the selected service (local or remote)
 *
 * @author etj
 */
public class RuleReaderServiceFactory implements ApplicationContextAware {

    private ApplicationContext context;
    private final List<String> preferredServiceNames;

    // The list of preferred names is injected via XML constructor argument
    public RuleReaderServiceFactory(List<String> preferredServiceNames) {
        this.preferredServiceNames = preferredServiceNames;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public RuleReaderService getService() {
        return selectPreferredService(getAllServices());
    }

    private Collection<RuleReaderService> getAllServices() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext was not injected into the factory.");
        }
        return context.getBeansOfType(RuleReaderService.class).values();
    }

    private RuleReaderService selectPreferredService(Collection<RuleReaderService> services) {
        if (services == null || services.isEmpty()) {
            throw new IllegalStateException("No RuleReaderService beans found in the Spring context.");
        }

        // 1. Loop through preferred names configured in applicationContext.xml
        if (preferredServiceNames != null) {
            for (String beanName : preferredServiceNames) {
                if (context.containsBean(beanName) && context.isTypeMatch(beanName, RuleReaderService.class)) {
                    return context.getBean(beanName, RuleReaderService.class);
                }
            }
        }

        throw new IllegalStateException("No RuleReaderService available. Preferred " + preferredServiceNames + "."
                + " Found " + Arrays.toString(context.getBeanNamesForType(RuleReaderService.class)));
    }
}
