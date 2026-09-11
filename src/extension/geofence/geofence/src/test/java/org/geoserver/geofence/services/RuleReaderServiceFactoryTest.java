/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.services;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.geofence.core.services.RuleReaderService;
import org.geoserver.geofence.utils.RuleReaderServiceAdapter;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class RuleReaderServiceFactoryTest {

    private static final String BEAN_NAME = "testRuleReader";

    /** Kept as a field so it outlives factoryFor(), which hands it to the factory. */
    private GenericApplicationContext context;

    @After
    public void closeContext() {
        if (context != null) {
            context.close();
        }
    }

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

    /**
     * A decorator calls back into the backend factory on cache misses, so selecting one as the backend would recurse
     * endlessly: it must be rejected (denied) rather than served.
     */
    @Test
    public void testDecoratorRejectedAsBackend() {
        context = new GenericApplicationContext();
        context.registerBean(BEAN_NAME, DecoratingRuleReader.class, DecoratingRuleReader::new);
        context.refresh();

        RuleReaderServiceFactory backendFactory = new RuleReaderServiceFactory(BEAN_NAME, false);
        backendFactory.setApplicationContext(context);
        assertTrue(
                "a decorator must not be usable as a backend",
                backendFactory.getService() instanceof DenyAllRuleReaderService);

        // the same bean is a legitimate choice for a frontend factory
        RuleReaderServiceFactory frontendFactory = new RuleReaderServiceFactory(BEAN_NAME, true);
        frontendFactory.setApplicationContext(context);
        assertTrue(
                "a decorator must still be usable as a frontend",
                frontendFactory.getService() instanceof DecoratingRuleReader);

        // the runtime-switch entry point rejects it up front, rather than switching to deny-all
        assertThrows(IllegalArgumentException.class, () -> backendFactory.setActiveServiceName(BEAN_NAME));
        frontendFactory.setActiveServiceName(BEAN_NAME);
    }

    /** A rejection must say why, not claim the bean doesn't exist when it does. */
    @Test
    public void testRejectionReasonIsAccurate() {
        context = new GenericApplicationContext();
        context.registerBean(BEAN_NAME, DecoratingRuleReader.class, DecoratingRuleReader::new);
        context.registerBean("notAReader", String.class, () -> "");
        context.refresh();

        RuleReaderServiceFactory factory = new RuleReaderServiceFactory(BEAN_NAME, false);
        factory.setApplicationContext(context);

        assertTrue(rejectionMessage(factory, "missingBean").contains("no such bean: missingBean"));
        assertTrue(rejectionMessage(factory, "notAReader").contains("is not a RuleReaderService"));
        assertTrue(rejectionMessage(factory, BEAN_NAME).contains("is a decorator"));
    }

    /** Without a context, callers get a clear failure rather than a NullPointerException. */
    @Test
    public void testMissingContextFailsClearly() {
        RuleReaderServiceFactory factory = new RuleReaderServiceFactory(BEAN_NAME, false);

        assertThrows(IllegalStateException.class, () -> factory.setActiveServiceName(BEAN_NAME));
        assertThrows(IllegalStateException.class, factory::afterSingletonsInstantiated);
        assertTrue("resolution failures still fail closed", factory.getService() instanceof DenyAllRuleReaderService);
    }

    private String rejectionMessage(RuleReaderServiceFactory factory, String name) {
        return assertThrows(IllegalArgumentException.class, () -> factory.validateServiceName(name))
                .getMessage();
    }

    private RuleReaderServiceFactory factoryFor(RuleReaderService backend) {
        context = new GenericApplicationContext();
        context.registerBean(BEAN_NAME, RuleReaderService.class, () -> backend);
        context.refresh();

        RuleReaderServiceFactory factory = new RuleReaderServiceFactory(BEAN_NAME, true);
        factory.setApplicationContext(context);
        return factory;
    }

    private static class DecoratingRuleReader extends RuleReaderServiceAdapter implements RuleReaderDecorator {}
}
