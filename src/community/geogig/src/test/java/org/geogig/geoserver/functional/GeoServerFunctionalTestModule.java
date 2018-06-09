/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.functional;

import com.google.inject.AbstractModule;
import org.geogig.web.functional.FunctionalTestContext;

/** Binds the {@link GeoServerFunctionalTestContext} for use in web API functional tests. */
public class GeoServerFunctionalTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FunctionalTestContext.class).to(GeoServerFunctionalTestContext.class);
    }
}
