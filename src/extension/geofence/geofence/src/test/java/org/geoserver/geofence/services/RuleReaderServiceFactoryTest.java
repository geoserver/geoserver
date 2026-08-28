/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.services;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.geofence.core.services.RuleReaderService;
import org.geoserver.geofence.utils.RuleReaderServiceAdapter;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class RuleReaderServiceFactoryTest {

    private static final String BEAN_NAME = "testRuleReader";

    @Test
    public void testDenyUntilRecovered() {
        RuleReaderService backend = new RuleReaderServiceAdapter();
        RuleReaderServiceFactory factory = factoryFor(backend);
        assertSame(backend, factory.getService());

        AtomicBoolean healthy = new AtomicBoolean(false);
        factory.denyUntilRecovered("source", healthy::get);

        assertTrue(
                "a failed recovery attempt must keep denying",
                factory.getService() instanceof DenyAllRuleReaderService);

        healthy.set(true);
        assertSame("a successful recovery attempt must restore the backend", backend, factory.getService());
        assertSame("recovery must not be re-attempted once cleared", backend, factory.getService());
    }

    /** One source recovering must not clear another source's deny. */
    @Test
    public void testDenyUntilAllSourcesRecovered() {
        RuleReaderService backend = new RuleReaderServiceAdapter();
        RuleReaderServiceFactory factory = factoryFor(backend);

        AtomicBoolean config = new AtomicBoolean(false);
        AtomicBoolean datasource = new AtomicBoolean(false);
        factory.denyUntilRecovered("config", config::get);
        factory.denyUntilRecovered("datasource", datasource::get);

        config.set(true);
        assertTrue(
                "still denying while one source is broken", factory.getService() instanceof DenyAllRuleReaderService);

        datasource.set(true);
        assertSame(backend, factory.getService());
    }

    private RuleReaderServiceFactory factoryFor(RuleReaderService backend) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(BEAN_NAME, RuleReaderService.class, () -> backend);
        context.refresh();

        RuleReaderServiceFactory factory = new RuleReaderServiceFactory(BEAN_NAME, true);
        factory.setApplicationContext(context);
        return factory;
    }
}
