package org.geoserver.security.jdbc;

import org.junit.rules.ExternalResource;

/**
 * A JUnit4 {@link org.junit.ClassRule} to configure JNDI properties for H2 database testing.
 *
 * <p>This rule sets up the required JNDI system properties before test execution and clears them afterward, ensuring a
 * clean test environment.
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;ClassRule
 * public static final H2JNDITestConfig jndiConfig = new H2JNDITestConfig();
 * </pre>
 */
class H2JNDITestConfig extends ExternalResource {

    /** Configures the system properties required for JNDI setup before test execution. */
    @Override
    protected void before() {
        System.setProperty("java.naming.factory.initial", "org.osjava.sj.SimpleContextFactory");
        System.setProperty("org.osjava.sj.root", "target/test-classes/config");
        System.setProperty("org.osjava.jndi.delimiter", "/");
        System.setProperty("org.osjava.sj.jndi.shared", "true");
    }

    /** Clears the JNDI-related system properties after test execution to ensure a clean state for subsequent tests. */
    @Override
    protected void after() {
        System.clearProperty("java.naming.factory.initial");
        System.clearProperty("org.osjava.sj.root");
        System.clearProperty("org.osjava.jndi.delimiter");
        System.clearProperty("org.osjava.sj.jndi.shared");
    }
}
