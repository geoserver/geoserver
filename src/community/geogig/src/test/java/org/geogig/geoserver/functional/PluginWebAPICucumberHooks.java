/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.functional;

import com.google.inject.Inject;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.java.guice.ScenarioScoped;
import java.io.IOException;
import org.geogig.web.functional.WebAPICucumberHooks;
import org.locationtech.geogig.repository.Repository;

/**
 * Extensions to the GeoGig Web API Functional tests. These these are specific to the GeoServer
 * plugin.
 */
@ScenarioScoped
@StepDefAnnotation
public class PluginWebAPICucumberHooks {

    public GeoServerFunctionalTestContext context;

    private String repoName;

    /**
     * Create an instance of this set of Steps with the GeoGig Web API Hooks as a parent. Since you
     * cannot extend a Step Definition class, just inject the one that gets created during the test
     * run and grab the Context. It <i>should</i> be an instance of {@link
     * GeoServerFunctionalTestContext}.
     *
     */
    @Inject
    public PluginWebAPICucumberHooks(WebAPICucumberHooks parent) {
        if (GeoServerFunctionalTestContext.class.isAssignableFrom(parent.context.getClass())) {
            this.context = GeoServerFunctionalTestContext.class.cast(parent.context);
        }
    }

    String systemTempPath() throws IOException {
        return context.getTempFolder().getCanonicalPath().replace("\\", "/");
    }

    @Given("^I have \"([^\"]*)\" that is not managed by GeoServer$")
    public void setupExtraUnMangedRepo(String repoName) throws Exception {
        context.createUnManagedRepoWithAltRoot(repoName)
                .init("geogigUser", "repo1_Owner@geogig.org")
                .loadDefaultData()
                .getRepo()
                .close();
        this.repoName = repoName;
    }

    @After
    public void after() {
        if (repoName != null) {
            try {
                Repository repo = this.context.getRepo(repoName);
                if (repo != null) {
                    repo.close();
                }
            } catch (Exception ex) {
                // repo doesn't exist
            }
        }
        context.after();
    }
}
