package org.geogig.geoserver.functional;

import com.google.inject.Inject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.geogig.web.functional.WebAPICucumberHooks;
import org.locationtech.geogig.web.api.TestData;
import org.restlet.data.MediaType;
import org.restlet.data.Method;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Extensions to the GeoGig Web API Functional tests. These these are specific to the GeoServer
 * plugin.
 */
@ScenarioScoped
@StepDefAnnotation
public class PluginWebAPICucumberHooks {

    public GeoServerFunctionalTestContext context;

    /**
     * Create an instance of this set of Steps with the GeoGig Web API Hooks as a parent. Since you
     * cannot extend a Step Definition class, just inject the one that gets created during the test
     * run and grab the Context. It <i>should</i> be an instance of
     * {@link GeoServerFunctionalTestContext}.
     *
     * @param parent
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

    @Given("^A repository named \"([^\"]*)\" is initialized$")
    public void initEmptyRepo(String repoName) throws Exception {
        context.createRepo(repoName);
    }

    @When("^A JSON POST request is made to \"([^\"]*)\"$")
    public void callURLWithJSONPayload(final String methodAndURL) throws JsonException, IOException {
        // build JSON payload
        JsonObject payload = Json.createObjectBuilder().add("parentDirectory", systemTempPath())
                .add("leafDirectory", "geogigRepo")
                .add("authorName", "GeoGig User")
                .add("authorEmail", "geogig@geogig.org")
                .build();
        callURLWithJSONPayload(methodAndURL, payload);
    }

    private void callURLWithJSONPayload(final String methodAndURL, JsonObject payload)
            throws JsonException {
        final int idx = methodAndURL.indexOf(' ');
        checkArgument(idx > 0, "No METHOD given in URL definition: '%s'", methodAndURL);
        final String httpMethod = methodAndURL.substring(0, idx);
        String resourceUri = methodAndURL.substring(idx + 1).trim();
        Method method = Method.valueOf(httpMethod);
        context.call(method, resourceUri, payload.toString(), MediaType.APPLICATION_JSON.getName());
    }
}