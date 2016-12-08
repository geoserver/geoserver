/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.functional;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 * <p>
 */
@RunWith(Cucumber.class)
@CucumberOptions(strict = true,
        features = {"classpath:features/commands/", "classpath:features/repo/"},
        tags = {"~@HttpTest"},
        glue = {"org.geogig.web.functional"},
        plugin = {"pretty", "html:cucumber-report","json:cucumber-report/cucumber.json"})
public class RunWebAPIFunctionalTest {

    // keep track of temp root so we can clean everything up at the end
    // This is explicitly not a Rule as we want 1 temp directory to hold all the temp resources (so we don't have to keep
    // track of all of them during the tests). We'll use @BeforeClass to create it and @AfterClass to delete it.
    public static final TemporaryFolder TEMP_ROOT = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException {
        TEMP_ROOT.create();
    }

    @AfterClass
    public static void tearDown() {
        TEMP_ROOT.delete();
    }
}
