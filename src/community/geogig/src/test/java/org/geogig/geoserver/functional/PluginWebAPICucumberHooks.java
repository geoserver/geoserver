/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.functional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigAction.CONFIG_LIST;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigScope.LOCAL;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.geogig.web.functional.WebAPICucumberHooks;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.web.api.TestData;
import org.restlet.data.Form;
import org.restlet.data.Method;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.java.guice.ScenarioScoped;

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

    private JsonArray getArrayFromJSONResponse(String jsonPath) {
        String response = context.getLastResponseText();
        JsonObject jsonResponse = TestData.toJSON(response);
        // find the JSON object
        String[] paths = jsonPath.split("\\.");
        JsonObject path = jsonResponse;
        for (int i = 0; i < paths.length - 1; ++i) {
            // drill down
            path = path.getJsonObject(paths[i]);
        }
        return path.getJsonArray(paths[paths.length - 1]);
    }

    @Then("^the json response \"([^\"]*)\" attribute \"([^\"]*)\" should each contain \"([^\"]*)\"$")
    public void checkJsonArrayContains(final String jsonArray, final String attribute,
            final String expected) {
        JsonArray array = getArrayFromJSONResponse(jsonArray);
        for (JsonObject obj : array.getValuesAs(JsonObject.class)) {
            String actual = obj.getString(attribute);
            assertTrue("JSON response doesn't contain expected value, has: " + actual,
                    actual.contains(expected));
        }
    }

    @Then("^I save the first href link from \"([^\"]*)\" as \"([^\"]*)\"$")
    public void saveHrefLinkFromJSONResponse(final String jsonArray, final String href)
            throws JsonException {
        // get the first href link from the response
        JsonArray array = getArrayFromJSONResponse(jsonArray);
        JsonObject obj = array.getJsonObject(0);
        String link = obj.getString("href");
        // strip everything up to "repos" off the front of the href link
        String linkEnd = link.substring(link.indexOf("/repos"));
        // store the linkEnd
        context.setVariable(href, linkEnd);
    }

    @When("^I call \"([^\"]*)\" with the System Temp Directory as the parentDirectory$")
    public void callURLWithJSONPaylod(final String methodAndURL) throws JsonException, IOException {
        // build JSON payload
        JsonObject payload = TestData.toJSON("{\"parentDirectory\":\"" + systemTempPath() + "\"}");
        callURLWithJSONPayload(methodAndURL, payload);
    }

    @When("^I call \"([^\"]*)\" with a URL encoded Form containing a parentDirectory parameter$")
    public void callURLWithFormPaylod(final String methodAndURL) throws JsonException, IOException {
        final int idx = methodAndURL.indexOf(' ');
        checkArgument(idx > 0, "No METHOD given in URL definition: '%s'", methodAndURL);
        final String httpMethod = methodAndURL.substring(0, idx);
        String resourceUri = methodAndURL.substring(idx + 1).trim();
        Method method = Method.valueOf(httpMethod);
        // build URL encoded Form
        Form form = new Form();
        form.add("parentDirectory", systemTempPath());
        context.call(method, resourceUri, form);
    }

    @Then("^the parent directory of repository \"([^\"]*)\" equals System Temp directory$")
    public void checkRepositoryParent(final String repo) throws Exception {
        Repository geogig = context.getRepo(repo);
        final Optional<URI> repoLocation = geogig.command(ResolveGeogigURI.class).call();
        assertTrue("Expected Repository location to be present", repoLocation.isPresent());
        URI repoURI = repoLocation.get();
        assertEquals("Unexpected URI scheme", "file", repoURI.getScheme());
        // parent of the repo is the directory that contains the ".geogig" directory.
        // the parent of the parent of the repo is the directory that the user specifies in the Init
        // request.
        String parentDir = new File(repoURI).getParentFile().getParentFile().getAbsolutePath();
        assertEquals("Unexpected parent directory", systemTempPath(), parentDir);
    }

    @Then("^the Author config of repository \"([^\"]*)\" is set$")
    public void checkAuthorConfig(final String repo) throws Exception {
        Repository geogig = context.getRepo(repo);
        final Optional<URI> repoLocation = geogig.command(ResolveGeogigURI.class).call();
        assertTrue("Expected Repository location to be present", repoLocation.isPresent());
        // get the config
        Optional<Map<String, String>> optConfig = geogig.command(ConfigOp.class)
                .setAction(CONFIG_LIST).setScope(LOCAL).call();
        // asseert the user.name and user.email config
        assertTrue("GeoGig repo config missing", optConfig.isPresent());
        Map<String, String> config = optConfig.get();
        assertTrue("\"user.name\" missing in repository config", config.containsKey("user.name"));
        assertEquals("GeoGig User", config.get("user.name"));

        assertTrue("\"user.email\" missing in repository config", config.containsKey("user.email"));
        assertEquals("geogig@geogig.org", config.get("user.email"));
    }

    @When("^I call \"([^\"]*)\" with Author and the System Temp Directory as the parentDirectory$")
    public void callURLWithJSONPayloadAndAuthor(final String methodAndURL) throws JsonException, IOException {
        // build the JSON payload
        JsonObject payload = Json.createObjectBuilder().add("parentDirectory", systemTempPath())
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
        context.call(method, resourceUri, payload);
    }

    @When("^I call \"([^\"]*)\" with a URL encoded Form containing a parentDirectory parameter and Author$")
    public void callURLWithFormPaylodWithAuthor(final String methodAndURL) throws JsonException, IOException {
        final int idx = methodAndURL.indexOf(' ');
        checkArgument(idx > 0, "No METHOD given in URL definition: '%s'", methodAndURL);
        final String httpMethod = methodAndURL.substring(0, idx);
        String resourceUri = methodAndURL.substring(idx + 1).trim();
        Method method = Method.valueOf(httpMethod);
        // build URL encoded Form
        Form form = new Form();
        form.add("parentDirectory", systemTempPath());
        form.add("authorName", "GeoGig User");
        form.add("authorEmail", "geogig@geogig.org");
        context.call(method, resourceUri, form);
    }

    @Then("^the parent directory of repository \"([^\"]*)\" is NOT the System Temp directory$")
    public void checkRepositoryParent2(final String repo) throws Exception {
        Repository geogig = context.getRepo(repo);
        final Optional<URI> repoLocation = geogig.command(ResolveGeogigURI.class).call();
        assertTrue("Expected Repository location to be present", repoLocation.isPresent());
        URI repoURI = repoLocation.get();
        assertEquals("Unexpected URI scheme", "file", repoURI.getScheme());
        // parent of the repo is the directory that contains the ".geogig" directory.
        // the parent of the parent of the repo is the directory that the user specifies in the Init
        // request.
        String parentDir = new File(repoURI).getParentFile().getParentFile().getCanonicalPath();
        assertNotEquals("Unexpected parent directory", systemTempPath(), parentDir);
    }

    @When("^I call \"([^\"]*)\" with an unsupported media type$")
    public void callURLWithUnsupportedMediaType(final String methodAndURL) throws JsonException, IOException {
        final int idx = methodAndURL.indexOf(' ');
        checkArgument(idx > 0, "No METHOD given in URL definition: '%s'", methodAndURL);
        final String httpMethod = methodAndURL.substring(0, idx);
        String resourceUri = methodAndURL.substring(idx + 1).trim();
        Method method = Method.valueOf(httpMethod);
        // build the JSON payload
        JsonObject payload = Json.createObjectBuilder().add("parentDirectory", systemTempPath())
                .add("authorName", "GeoGig User")
                .add("authorEmail", "geogig@geogig.org")
                .build();
        context.callWithContentType(method, resourceUri, payload, "application/xml");
    }

    @Then("^there should be no \"([^\"]*)\" created$")
    public void checkRepoNotInitialized(final String repo) throws Exception {
        Repository geogig = context.getRepo(repo);
        assertTrue("Expected repository to NOT EXIST", null == geogig);
    }

    String systemTempPath() throws IOException {
        File tempFolder = context.getTempFolder();
        File tmpDir = new File(tempFolder, "tmp");
        tmpDir.mkdir();
        return tmpDir.getCanonicalPath();
    }
}
