package org.geogig.geoserver.functional;

import org.geogig.web.functional.FunctionalTestContext;

import com.google.inject.AbstractModule;

/**
 * Binds the {@link GeoServerFunctionalTestContext} for use in web API functional tests.
 */
public class GeoServerFunctionalTestModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FunctionalTestContext.class).to(GeoServerFunctionalTestContext.class);
    }
}
