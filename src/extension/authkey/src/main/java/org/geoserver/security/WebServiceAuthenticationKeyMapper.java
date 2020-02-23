/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.FilterConfigException;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.SimpleHttpClient;
import org.springframework.util.StringUtils;

/**
 * AuthenticationMapper using an external REST webservice to get username for a given authkey. The
 * web service URL can be configured using a template in the form:
 *
 * <p>http://<server>:<port>/<webservice>?<key>={key}
 *
 * <p>where {key} will be replaced by the received authkey.
 *
 * <p>A regular expression can be configured to extract the username from the web service response.
 *
 * @author Mauro Bartolomeoli
 */
public class WebServiceAuthenticationKeyMapper extends AbstractAuthenticationKeyMapper {

    /** Thread local holding the current response */
    public static final ThreadLocal<String> RECORDED_RESPONSE = new ThreadLocal<String>();

    public static final String AUTH_KEY_WEBSERVICE_PLACEHOLDER_REQUIRED =
            "AUTH_KEY_WEBSERVICE_PLACEHOLDER_REQUIRED";

    public static final String AUTH_KEY_WEBSERVICE_MALFORMED_REGEX =
            "AUTH_KEY_WEBSERVICE_MALFORMED_REGEX";

    public static final String AUTH_KEY_WEBSERVICE_WRONG_TIMEOUT =
            "AUTH_KEY_WEBSERVICE_WRONG_TIMEOUT";

    // web service url (must contain the {key} placeholder for the authkey parameter)
    private String webServiceUrl;

    // regular expression, used to extract the user name from the webservice response
    private String searchUser;

    // compiled regex
    Pattern searchUserRegex = null;

    // connection timeout to the mapper web service (in seconds)
    int connectTimeout = 5;

    // read timeout to the mapper web service (in seconds)
    int readTimeout = 10;

    // optional external httpClient for web service connection (used mainly for tests)
    private HTTPClient httpClient = null;

    public WebServiceAuthenticationKeyMapper() {
        super();
    }

    private HTTPClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new SimpleHttpClient();
        }
        return httpClient;
    }

    /** Returns the connection timeout to the mapper web service (in seconds). */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout to the mapper web service (in seconds).
     *
     * @param connectTimeout timeout in seconds
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /** Returns the read timeout to the mapper web service (in seconds). */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout to the mapper web service (in seconds).
     *
     * @param readTimeout read timeout in seconds
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /** Returns the web service url */
    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    /**
     * Sets the web service url (must contain the {key} placeholder for the authkey parameter).
     *
     * @param webServiceUrl service url (must contain {key} placeholder for authkey)
     */
    public void setWebServiceUrl(String webServiceUrl) {
        this.webServiceUrl = webServiceUrl;
    }

    /**
     * Returns the regular expression used to extract the user name from the webservice response.
     */
    public String getSearchUser() {
        return searchUser;
    }

    /**
     * Sets the regular expression used to extract the user name from the webservice response.
     *
     * @param searchUser search user
     */
    public void setSearchUser(String searchUser) {
        this.searchUser = searchUser;
        searchUserRegex = Pattern.compile(searchUser);
    }

    /** Configures the HTTPClient implementation to be used to connect to the web service. */
    public void setHttpClient(HTTPClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected void checkProperties() throws IOException {
        super.checkProperties();
        if (StringUtils.hasLength(webServiceUrl) == false) {
            throw new IOException("Web service url is unset");
        }
        if (StringUtils.hasLength(searchUser)) {
            try {
                Pattern.compile(searchUser);
            } catch (PatternSyntaxException e) {
                throw new IOException("Search User regex is malformed");
            }
        }
    }

    public boolean supportsReadOnlyUserGroupService() {
        return true;
    }

    @Override
    public GeoServerUser getUser(String key) throws IOException {
        checkProperties();
        final String responseBody = callWebService(key);
        if (responseBody == null) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not find any user associated to webservice url ["
                            + webServiceUrl
                            + "] with authkey: "
                            + key);
            RECORDED_RESPONSE.remove();
            return null;
        } else {
            RECORDED_RESPONSE.set(responseBody);
        }

        if (getUserGroupService() != null) {
            String username = null;
            if (searchUserRegex == null) {
                username = responseBody;
            } else {
                Matcher matcher = searchUserRegex.matcher(responseBody);
                if (matcher.find()) {
                    username = matcher.group(1);
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            "Error in WebServiceAuthenticationKeyMapper, cannot find userName in response");
                }
            }

            if (username != null) {
                return (GeoServerUser) getUserGroupService().loadUserByUsername(username);
            }
        }

        LOGGER.log(
                Level.WARNING,
                "No User Group Service configured for webservice url ["
                        + webServiceUrl
                        + "] with authkey: "
                        + key);
        return null;
    }

    /**
     * Calls the external web service with the given key and parses the result to extract the
     * userName.
     */
    private String callWebService(String key) {
        String url = webServiceUrl.replace("{key}", key);
        HTTPClient client = getHttpClient();

        client.setConnectTimeout(connectTimeout);
        client.setReadTimeout(readTimeout);
        try {
            LOGGER.log(Level.FINE, "Issuing request to authkey webservice: " + url);
            HTTPResponse response = client.get(new URL(url));
            BufferedReader reader = null;
            InputStream responseStream = response.getResponseStream();
            StringBuilder result = new StringBuilder();
            try {
                reader = new BufferedReader(new InputStreamReader(responseStream));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                LOGGER.log(
                        Level.FINE,
                        "Response received from authkey webservice: " + result.toString());
                return result.toString();
            } finally {
                reader.close();
            }
        } catch (MalformedURLException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error in WebServiceAuthenticationKeyMapper, web service url is invalid: "
                            + url,
                    e);
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error in WebServiceAuthenticationKeyMapper, error in web service communication",
                    e);
        }
        return null;
    }

    @Override
    public void configureMapper(Map<String, String> mapperParams) {
        super.configureMapper(mapperParams);

        if (mapperParams != null) {
            if (mapperParams.containsKey("webServiceUrl")) {
                setWebServiceUrl((String) mapperParams.get("webServiceUrl"));
            }
            if (mapperParams.containsKey("searchUser")) {
                setSearchUser((String) mapperParams.get("searchUser"));
            }
            if (mapperParams.containsKey("connectTimeout")) {
                try {
                    connectTimeout = Integer.parseInt((String) mapperParams.get("connectTimeout"));
                } catch (NumberFormatException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "WebServiceAuthenticationKeyMapper connectTimeout wrong format",
                            e);
                }
            }
            if (mapperParams.containsKey("readTimeout")) {
                try {
                    readTimeout = Integer.parseInt((String) mapperParams.get("readTimeout"));
                } catch (NumberFormatException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "WebServiceAuthenticationKeyMapper readTimeout wrong format",
                            e);
                }
            }
        }
    }

    @Override
    public Set<String> getAvailableParameters() {
        return new HashSet(
                Arrays.asList("webServiceUrl", "searchUser", "connectTimeout", "readTimeout"));
    }

    @Override
    protected String getDefaultParamValue(String paramName) {
        if (paramName.equalsIgnoreCase("searchUser")) {
            return "^\\s*(.*)\\s*$";
        }
        if (paramName.equalsIgnoreCase("connectTimeout")) {
            return "5";
        }
        if (paramName.equalsIgnoreCase("readTimeout")) {
            return "10";
        }
        if (paramName.equalsIgnoreCase("webServiceUrl")) {
            return "http://host:port/service?authkey={key}";
        }
        return super.getDefaultParamValue(paramName);
    }

    public void validateParameter(String paramName, String value) throws FilterConfigException {
        if (value == null) {
            value = "";
        }
        if (paramName.equalsIgnoreCase("searchUser") && !value.isEmpty()) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                throw createFilterException(AUTH_KEY_WEBSERVICE_MALFORMED_REGEX, value);
            }
        }
        if (paramName.equalsIgnoreCase("connectTimeout")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw createFilterException(AUTH_KEY_WEBSERVICE_WRONG_TIMEOUT, value);
            }
        }
        if (paramName.equalsIgnoreCase("readTimeout")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw createFilterException(AUTH_KEY_WEBSERVICE_WRONG_TIMEOUT, value);
            }
        }
        if (paramName.equalsIgnoreCase("webServiceUrl")) {
            if (!value.contains("{key}")) {
                throw createFilterException(AUTH_KEY_WEBSERVICE_PLACEHOLDER_REQUIRED, value);
            }
        }
    }

    @Override
    public synchronized int synchronize() throws IOException {
        // synchronization functionality is not supported for web services
        return 0;
    }
}
