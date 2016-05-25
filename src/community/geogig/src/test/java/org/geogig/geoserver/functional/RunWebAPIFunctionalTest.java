package org.geogig.geoserver.functional;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 * 
 */
@RunWith(Cucumber.class)
@CucumberOptions(strict = true, features = {"classpath:org/geogig/web/functional/"}, glue = { "org.geogig.web.functional" }, plugin = { "pretty", "html:cucumber-report",
        "json:cucumber-report/cucumber.json" })
public class RunWebAPIFunctionalTest {
}