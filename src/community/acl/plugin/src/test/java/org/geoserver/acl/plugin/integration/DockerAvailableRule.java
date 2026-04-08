/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.integration;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;

/**
 * A JUnit 4 {@link TestRule} that checks for Docker availability before test execution. If Docker is not available, the
 * tests within the class will be skipped using {@link org.junit.Assume#assumeTrue(String, boolean)}.
 *
 * <p>This rule prevents {@code TestCouldNotBeSkippedException} errors when checking for Docker availability in a
 * {@literal @BeforeClass} method in JUnit 4.
 */
public class DockerAvailableRule implements TestRule {
    /**
     * Intercepts the test execution and performs the Docker availability check.
     *
     * @param base The original test statement to be evaluated.
     * @param description The description of the test being run.
     * @return The modified statement which includes the Docker check.
     */
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Check if Docker is available before running the tests
                Assume.assumeTrue(
                        "Docker is not available, skipping tests",
                        DockerClientFactory.instance().isDockerAvailable());
                base.evaluate();
            }
        };
    }
}
