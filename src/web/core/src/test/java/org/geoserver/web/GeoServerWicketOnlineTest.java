package org.geoserver.web;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geotools.data.Base64;
import org.junit.Assert;
import org.junit.Test;

/**
 * Online integration test for the Web GUI
 *
 * <p>This test assumes a running GeoServer on port 8080 with the release data directory.
 */
public class GeoServerWicketOnlineTest extends GeoServerWicketOnlineTestSupport {

    @Test
    public void testLogin() throws IOException {
        assumeTrue(isOnline());
        String jsessionid = login("admin", "geoserver");
        get("web/", jsessionid);
        logout(jsessionid);
    }

    // Tries fetching the new service page and adding a rule until wicket increments the page id
    // sufficently
    @Test
    public void testCannotAddAccessRuleIterative() throws IOException {
        assumeTrue(isOnline());
        List<ServiceAccessRule> initialRules = getServiceAccessRules();
        HttpURLConnection connection;

        String jsessionid = login("admin", "geoserver");
        try {
            for (int j = 0; j < 20; j++) {
                // Park on an arbitrary i value larger than the starting index until wicket
                // increments to here
                // (Note: Could also do this programatically by examining responses from wicket)
                int i = 13;
                // navigate to a page
                getNewServiceAccessRulePageWicket(jsessionid);

                // Post service selection
                addServiceAccessRuleWicket(jsessionid, i, false);
                // Check if this had an effect
                assertServiceAccessRuleNotAdded(
                        initialRules, "Added access rule using only an exising session id");
            }
        } finally {
            logout(jsessionid);
        }
    }

    // Tries fetching the new service page and examining the response to determine which page id to
    // submit the form with.
    @Test
    public void testCannotAddAccessRuleProgramatic() throws IOException {
        assumeTrue(isOnline());
        List<ServiceAccessRule> initialRules = getServiceAccessRules();
        HttpURLConnection connection;

        String jsessionid = login("admin", "geoserver");
        try {
            // navigate to a page and fetch index from response URL after redirects are handled
            int i = getNewServiceAccessRulePageWicket(jsessionid);

            // Post service selection
            addServiceAccessRuleWicket(jsessionid, i, false);

            // Check if this had an effect
            assertServiceAccessRuleNotAdded(
                    initialRules,
                    "Added access rule using only an exising session id and response from page requests");
        } finally {
            logout(jsessionid);
        }
    }

    private int getNewServiceAccessRulePageWicket(String jsessionid) throws IOException {
        HttpURLConnection connection =
                get(
                        "web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage",
                        jsessionid,
                        null);
        String response = IOUtils.toString(connection.getInputStream(), "UTF-8");
        // Fetch index from response URL after redirects are handled
        int i = Integer.parseInt(connection.getURL().getQuery());
        connection.disconnect();
        return i;
    }

    private void addServiceAccessRuleWicket(String jsessionid, int i, boolean setReferer)
            throws IOException {
        HttpURLConnection connection;
        String response;

        // Post service selection
        // NOTE: This is required, as wicket checks the model content against the form content
        // we post later on
        String body = "service=4";
        connection =
                preparePost(
                        "web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                                + i
                                + "-1.IBehaviorListener.0-form-service",
                        body.length(),
                        "application/x-www-form-urlencoded",
                        jsessionid);
        connection.setRequestProperty("Wicket-Ajax", "true");
        connection.setRequestProperty(
                "Wicket-Ajax-BaseURL",
                "wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                        + i);
        connection.setRequestProperty("Wicket-FocusedElementId", "service");
        if (setReferer) {
            connection.setRequestProperty(
                    "Referer",
                    GEOSERVER_BASE_URL
                            + "/web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                            + i);
        }
        connection = doPost(connection, body);
        if (connection.getResponseCode() < 400) {
            response = IOUtils.toString(connection.getInputStream(), "UTF-8");
        }
        connection.disconnect();

        // Post "anyRole"
        // NOTE: This is required, as wicket checks the model content against the form content
        // we post later on
        body = "roles:anyRole=on";
        connection =
                preparePost(
                        "web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                                + i
                                + "-1.IBehaviorListener.0-form-roles-anyRole",
                        body.length(),
                        "application/x-www-form-urlencoded",
                        jsessionid);
        connection.setRequestProperty("Wicket-Ajax", "true");
        if (setReferer) {
            connection.setRequestProperty(
                    "Referer",
                    GEOSERVER_BASE_URL
                            + "/web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                            + i);
        }
        connection.setRequestProperty(
                "Wicket-Ajax-BaseURL",
                "wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                        + i);
        connection.setRequestProperty("Wicket-FocusedElementId", "id3c");
        connection = doPost(connection, body);
        if (connection.getResponseCode() < 400) {
            response = IOUtils.toString(connection.getInputStream(), "UTF-8");
        }
        connection.disconnect();

        // Submit form post
        body = "save=x&service=4&p::method=12&roles:anyRole=on";
        connection =
                preparePost(
                        "web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                                + i
                                + "-1.IFormSubmitListener-form",
                        body.length(),
                        "application/x-www-form-urlencoded",
                        jsessionid);
        connection.setInstanceFollowRedirects(false);
        if (setReferer) {
            connection.setRequestProperty(
                    "Referer",
                    GEOSERVER_BASE_URL
                            + "/web/wicket/bookmarkable/org.geoserver.security.web.service.NewServiceAccessRulePage?"
                            + i);
        }
        connection = doPost(connection, body);
        if (connection.getResponseCode() < 400) {
            response = IOUtils.toString(connection.getInputStream(), "UTF-8");
        }
        connection.disconnect();
    }

    private void assertServiceAccessRuleNotAdded(
            List<ServiceAccessRule> initialRules, String message) throws IOException {
        List<ServiceAccessRule> updatedRules = getServiceAccessRules();
        if (updatedRules.size() > initialRules.size()) {
            // Remove the rule
            ServiceAccessRule addedRule = updatedRules.get(updatedRules.size() - 1);
            deleteServiceAccessRule(addedRule.getKey());
            // CRSF protections not strong enough, fail.
            Assert.fail(message);
        }
    }

    /*
     * Fetches the list of service access rules
     *
     * Uses the REST API and HTTP Basic authentication so as not to interfere with the web session
     */
    protected List<ServiceAccessRule> getServiceAccessRules() throws IOException {
        // Use rest api to fetch service access rules
        HttpURLConnection connection =
                prepareGet("rest/security/acl/services", null, "application/json");
        connection.setRequestProperty(
                "Authorization",
                "Basic " + new String(Base64.encodeBytes(("admin:geoserver").getBytes())));
        connection = doGet(connection);
        String response = IOUtils.toString(connection.getInputStream(), "UTF-8");
        connection.disconnect();

        JSONObject jsonObject = JSONObject.fromObject(response);

        List<ServiceAccessRule> serviceAccessRules = new ArrayList<>();
        for (Object key : jsonObject.keySet()) {
            String[] serviceMethod = ((String) key).split("\\.");
            String[] roles = ((String) jsonObject.get(key)).split(",");
            serviceAccessRules.add(
                    new ServiceAccessRule(serviceMethod[0], serviceMethod[1], roles));
        }

        return serviceAccessRules;
    }

    /*
     * Deletes the named serviceAccessRule
     *
     * Uses the REST API and HTTP Basic authentication so as not to interfere with the web session
     */
    protected boolean deleteServiceAccessRule(String ruleName) throws IOException {
        HttpURLConnection connection =
                prepareDelete("rest/security/acl/services/" + ruleName, null);
        connection.setRequestProperty(
                "Authorization",
                "Basic " + new String(Base64.encodeBytes(("admin:geoserver").getBytes())));
        connection = doGet(connection);
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        return responseCode == 200;
    }
}
