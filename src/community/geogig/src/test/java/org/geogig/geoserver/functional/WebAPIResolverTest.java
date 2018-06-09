/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.functional;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 *
 * <p>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    strict = true,
    features = {"classpath:features/resolvers"},
    glue = {"org.geogig.web.functional", "org.geogig.geoserver.functional"},
    plugin = {"pretty", "html:cucumber-report", "json:cucumber-report/cucumber.json"}
)
public class WebAPIResolverTest {

    public static @BeforeClass void setUpGeoServer() throws Exception {
        GeoServerTestSupport helper = new GeoServerTestSupport();
        helper.setUpGeoServer();
        GeoServerFunctionalTestContext.helper = helper;
    }

    public static @AfterClass void shutDownUpGeoServer() throws Exception {
        GeoServerFunctionalTestContext.helper.shutDownUpGeoServer();
    }
}
