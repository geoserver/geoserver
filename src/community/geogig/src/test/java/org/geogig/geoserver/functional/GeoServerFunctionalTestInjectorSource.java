package org.geogig.geoserver.functional;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import cucumber.api.guice.CucumberModules;
import cucumber.runtime.java.guice.InjectorSource;

/**
 * Creates the injector for cucumber functional tests.
 */
public class GeoServerFunctionalTestInjectorSource implements InjectorSource {
    @Override
    public Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, CucumberModules.SCENARIO,
                new GeoServerFunctionalTestModule());
    }
}
