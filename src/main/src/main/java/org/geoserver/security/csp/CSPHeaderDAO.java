/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import com.thoughtworks.xstream.XStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.util.Requests;

/** This class manages persistence of the {@link CSPConfiguration} in the data directory. */
public class CSPHeaderDAO extends AbstractDispatcherCallback {

    /** The CSPHeaderDAO class logger. */
    private static final Logger LOGGER = Logging.getLogger(CSPHeaderDAO.class);

    /** The file containing the current CSP configuration */
    public static final String CONFIG_FILE_NAME = "csp.xml";

    /**
     * The file containing the default CSP configuration that is used to check for changes to the default configuration
     * with new GeoServer updates
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "csp_default.xml";

    /**
     * The thread local to store Content-Security-Policy values that contain variables for the proxy base URL in case it
     * needs to be updated based on local workspace settings
     */
    private static final ThreadLocal<String> PROXY_POLICY = new ThreadLocal<>();

    /** The GeoServer configuration */
    private final GeoServer geoServer;

    /** Watches the CSP configuration file for changes */
    private final FileWatcher<CSPConfiguration> configurationWatcher;

    /** The resource for the CSP configuration file */
    private final Resource resource;

    /** The XStream persister configuration */
    private final XStreamPersister xp;

    /** The Content Security Policy configuration */
    private CSPConfiguration configuration = null;

    /**
     * @param geoServer the GeoServer configuration
     * @param dd the GeoServer data directory
     * @param xpf the {@link XStreamPersister} factory
     * @throws IOException if the was an error reading from or writing to configuration files
     */
    public CSPHeaderDAO(GeoServer geoServer, GeoServerDataDirectory dd, XStreamPersisterFactory xpf)
            throws IOException {
        this.geoServer = geoServer;
        this.configurationWatcher = new CSPConfigurationWatcher(dd);
        this.resource = this.configurationWatcher.getResource();
        this.xp = createXMLPersister(xpf);
        initializeConfigurationFiles();
    }

    /**
     * If the Content-Security-Policy header for an OGC request contains proxy base URL properties, set the header a
     * second time using the URL from the local workspace settings if it results in a different header value than with
     * the global settings.
     */
    @Override
    public Request init(Request request) {
        try {
            // Update the CSP if the proxy base URL is set in the local workspace settings
            String policy = PROXY_POLICY.get();
            if (policy != null && hasLocalProxyBase()) {
                policy = replaceVariables(request.getHttpRequest(), getConfig(), policy);
                HttpServletResponse response = request.getHttpResponse();
                String name = getConfig().isReportOnly()
                        ? HttpHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY
                        : HttpHeaders.CONTENT_SECURITY_POLICY;
                if (!policy.equals(response.getHeader(name))) {
                    logPolicy(request.getHttpRequest(), policy);
                    response.setHeader(name, policy);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to update CSP with local proxy base URL", e);
        }
        return request;
    }

    /**
     * Returns the configuration read from the file, or a default one if the file does not exist.
     *
     * @return the CSP configuration
     * @throws IOException if an error occurs while reading the configuration
     */
    public CSPConfiguration getConfig() throws IOException {
        if (this.configurationWatcher.isModified() || this.configuration == null) {
            configurationAction(() -> this.configuration = doGetConfig());
        }
        return this.configuration;
    }

    /**
     * Writes the configuration object to the file.
     *
     * @param config the CSP configuration
     * @throws IOException if an error occurs while writing the configuration
     */
    public void setConfig(CSPConfiguration config) throws IOException {
        configurationAction(() -> this.configuration = doSetConfig(config.parseFilters(), this.resource));
    }

    /**
     * Sets the Content-Security-Policy header on the HTTP response. The request will be checked against the currently
     * configured rules and settings to determine the appropriate header value. A secure fallback policy is used if
     * there is a misconfiguration or a bug that prevents determining the header value.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return an HTTP response wrapper
     */
    public HttpServletResponse setContentSecurityPolicy(HttpServletRequest request, HttpServletResponse response) {
        CSPConfiguration config = new CSPConfiguration();
        config.setReportOnly(false);
        String policy;
        try {
            config = getConfig();
            policy = getContentSecurityPolicy(config, request, false);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error setting Content-Security-Policy header", t);
            policy = CSPUtils.getStringProperty(CSPUtils.GEOSERVER_CSP_FALLBACK, CSPUtils.DEFAULT_FALLBACK);
        }
        if (!policy.equals("NONE")) {
            String name = config.isReportOnly()
                    ? HttpHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY
                    : HttpHeaders.CONTENT_SECURITY_POLICY;
            response.setHeader(name, policy);
        }
        // The wrapper will merge parts of GeoServer's Content-Security-Policy header value with
        // the value set by other libraries such as Wicket.
        return new CSPHttpResponseWrapper(response, config);
    }

    /**
     * Executes a {@link ThrowingRunnable} on the configuration file, locking it to avoid concurrent access
     *
     * @param action the action to execute
     * @throws IOException if there was an error with the configuration file
     */
    private void configurationAction(ThrowingRunnable action) throws IOException {
        Resource.Lock lock = this.resource.lock();
        try {
            action.run();
        } finally {
            lock.release();
        }
    }

    /**
     * Reads the CSP configuration from the file if it exists. Otherwise, save the default configuration to the file and
     * return it.
     *
     * @return the CSP configuration
     * @throws IOException if the was an error reading from the configuration file
     */
    private CSPConfiguration doGetConfig() throws IOException {
        CSPConfiguration config = this.configurationWatcher.read();
        if (this.resource.getType() == Resource.Type.RESOURCE) {
            return config;
        }
        LOGGER.warning("Re-creating missing csp.xml with the default configuration");
        return doSetConfig(config, this.resource);
    }

    /**
     * Saves the provided CSP configuration to the specified configuration file.
     *
     * @param config the CSP configuration to write to the configuration file
     * @param resource the GeoServer resource to write the configuration to
     * @return the CSP configuration that was written to the configuration
     * @throws IOException if the was an error writing to configuration file
     */
    private CSPConfiguration doSetConfig(CSPConfiguration config, Resource resource) throws IOException {
        try (OutputStream fos = resource.out()) {
            this.xp.save(config, fos);
        }
        return doGetConfig();
    }

    /**
     * Determines if the the proxy base URL being used for the current request comes from the local workspaces settings.
     * Will be false if the PROXY_BASE_URL system property is set or of the proxy base URL comes from the global
     * settings.
     *
     * @return whether the request uses a local workspace proxy base URL
     */
    private boolean hasLocalProxyBase() {
        if (GeoServerExtensions.getProperty(Requests.PROXY_PARAM) != null) {
            return false;
        }
        SettingsInfo settings = this.geoServer.getSettings();
        return settings.getWorkspace() != null && settings.getProxyBaseUrl() != null;
    }

    /**
     * Creates the configuration files csp.xml and csp_default.xml from the default configuration if either file does
     * not exist. If both configuration files exist and have the exact same policies but this does not match the default
     * configuration's policies, then update csp.xml with the policies from the current default configuration. If
     * csp_default.xml exists, update it with the current default configuration if there were any changes to the default
     * configuration.
     *
     * @throws IOException if the was an error reading from or writing to configuration files
     */
    private void initializeConfigurationFiles() throws IOException {
        CSPConfiguration newDefaultConfig = CSPDefaultConfiguration.newInstance();
        Resource defaultResource = this.resource.parent().get(DEFAULT_CONFIG_FILE_NAME);
        CSPConfiguration oldDefaultConfig = null;
        if (defaultResource.getType() != Resource.Type.UNDEFINED) {
            try (InputStream in = defaultResource.in()) {
                oldDefaultConfig = this.xp.load(in, CSPConfiguration.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading csp_default.xml, re-creating file", e);
            }
        }
        if (this.resource.getType() == Resource.Type.UNDEFINED) {
            // create csp.xml with the default config if it doesn't exist
            LOGGER.info("Creating csp.xml with the default configuration");
            setConfig(newDefaultConfig);
        } else if (oldDefaultConfig != null) {
            // update csp.xml with the new default config policies if the current policies match the
            // old default policies in csp_default.xml and the default config was updated
            CSPConfiguration oldMainConfig = getConfig();
            if (oldMainConfig.equals(oldDefaultConfig) && !oldMainConfig.equals(newDefaultConfig)) {
                LOGGER.info("Updating csp.xml with the new default configuration");
                setConfig(newDefaultConfig);
            } else if (oldMainConfig.getPolicies().equals(oldDefaultConfig.getPolicies())
                    && !oldMainConfig.getPolicies().equals(newDefaultConfig.getPolicies())) {
                LOGGER.info("Updating csp.xml with the new default configuration");
                oldMainConfig.setPolicies(newDefaultConfig.getPolicies());
                setConfig(oldMainConfig);
            } else {
                LOGGER.fine("Leaving the csp.xml file alone");
            }
        } else {
            LOGGER.warning("Unable to check for default configuration changes. "
                    + "csp.xml exists but csp_default.xml is missing");
        }
        if (defaultResource.getType() == Resource.Type.UNDEFINED) {
            // create csp_default.xml with the default config if it doesn't exist
            LOGGER.info("Creating csp_default.xml with the default configuration");
            doSetConfig(newDefaultConfig, defaultResource);
        } else if (!newDefaultConfig.equals(oldDefaultConfig)) {
            // update csp_default.xml with the new default config if the default config was updated
            LOGGER.info("Updating csp_default.xml with the new default configuration");
            doSetConfig(newDefaultConfig, defaultResource);
        } else {
            LOGGER.fine("Leaving the csp_default.xml file alone");
        }
    }

    /** Resets the DAO, forcing it to reload the configuration from disk the next time it is accessed. */
    @VisibleForTesting
    public void reset() {
        this.configuration = null;
        this.configurationWatcher.setKnownLastModified(Long.MIN_VALUE);
    }

    /**
     * Gets the value for the Content-Security-Policy header. The keyword NONE will be returned if no CSP directives
     * were found from the policies and rules that matched the provided request. If multiple sets of directives were
     * found, they will be joined together into a comma-separate string (which is equivalent to setting multiple
     * Content-Security-Policy headers). The proxy base URL properties will be injected if necessary and all variables
     * in the policy will be lookup up and replaced to generate the final value for the header.
     *
     * @param config the CSP configuration
     * @param request the HTTP request
     * @param test whether this is for the test form
     * @return the value for the Content-Security-Policy header or NONE
     */
    public static String getContentSecurityPolicy(CSPConfiguration config, HttpServletRequest request, boolean test) {
        if (!config.isEnabled()) {
            return "NONE";
        }
        CSPHttpRequestWrapper wrapper = new CSPHttpRequestWrapper(request, config);
        List<String> directives = config.getPolicies().stream()
                .map(p -> p.getDirectives(wrapper))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (directives.isEmpty()) {
            return "NONE";
        }
        String policy = directives.stream().collect(Collectors.joining(", "));
        policy = injectProxyBase(config, policy, test);
        policy = replaceVariables(wrapper, config, policy);
        logPolicy(request, policy);
        return policy;
    }

    /** Removes the Content-Security-Policy value from the thread local. */
    public static void removeProxyPolicy() {
        PROXY_POLICY.remove();
    }

    /**
     * Initializes the configuration of the XStream persister.
     *
     * @param xpf the {@link XStreamPersister} factory
     * @return the XStream persister configuration
     */
    private static XStreamPersister createXMLPersister(XStreamPersisterFactory xpf) {
        XStreamPersister xp = xpf.createXMLPersister();
        XStream xs = xp.getXStream();
        xs.alias("config", CSPConfiguration.class);
        xs.alias("policy", CSPPolicy.class);
        xs.alias("rule", CSPRule.class);
        xs.addImplicitCollection(CSPConfiguration.class, "policies", CSPPolicy.class);
        xs.addImplicitCollection(CSPPolicy.class, "rules", CSPRule.class);
        return xp;
    }

    /**
     * Extracts the value of a specific part of the Forwarded request header.
     *
     * @param forwarded the value of the Forwarded request header
     * @param part the name of the part to extract from the header
     * @return the value of the extracted part
     */
    private static String getForwardedPart(String forwarded, String part) {
        if (forwarded == null) {
            return null;
        }
        Matcher matcher = ProxifyingURLMangler.FORWARDED_PATTERNS.get(part).matcher(forwarded);
        return matcher.matches() ? matcher.group(2) : null;
    }

    /**
     * Looks up the value of the provided property key. If the key does not match the regular expression for allowed
     * property keys, an empty string is returned. For valid keys, the system property will be looked up and if it is
     * not defined, it will be looked up from the CSP configuration. An empty string will be returned for keys without a
     * value.
     *
     * @param request the HTTP request
     * @param config the CSP configuration
     * @param key the property key
     * @return the property value or an empty string
     */
    @VisibleForTesting
    protected static String getPropertyValue(HttpServletRequest request, CSPConfiguration config, String key) {
        if (key.equals("proxy.base.url")) {
            return getProxyBase(request, config);
        } else if (CSPUtils.PROPERTY_KEY_REGEX.matcher(key).matches()) {
            String value = CSPUtils.getStringProperty(key, config.getField(key));
            // HIDE is a keyword to allow hiding a directive
            if (!value.isEmpty() && !"HIDE".equals(value)) {
                if (!CSPUtils.PROPERTY_VALUE_REGEX.matcher(value).matches()) {
                    LOGGER.fine(() -> "Ignoring invalid property value: " + value);
                } else if (value.contains("'self'")
                        || value.contains("'none'")
                        || !(key.equals(CSPUtils.GEOSERVER_CSP_FORM_ACTION)
                                || key.equals(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS))) {
                    return value;
                } else {
                    // automatically add 'self' to form-action and frame-ancestors sources that don't contain
                    // 'self' or 'none'
                    return "'self' " + value;
                }
            }
        } else {
            LOGGER.fine(() -> "Ignoring invalid property key: " + key);
        }
        return "";
    }

    /**
     * This method will look up the proxy base URL. If no proxy base URL is set or the request was sent to the proxy
     * base URL then an empty string will be returned. Otherwise, returns a truncated proxy base URL that only contains
     * the protocol, host name and port number (the port is only included if it is not the protocol's default port).
     *
     * @param request the HTTP request
     * @param config the CSP configuration
     * @return the proxy base URL or an empty string
     */
    private static String getProxyBase(HttpServletRequest request, CSPConfiguration config) {
        String proxyBase = ResponseUtils.buildURL("/", null, null, URLMangler.URLType.RESOURCE);
        if (proxyBase.equals("/")) {
            // proxy base URL is not set
            return "";
        }
        URL url;
        try {
            url = new URL(proxyBase);
        } catch (Exception e) {
            // proxy base URL is malformed or requires request headers
            return "";
        }
        if (matchesProxyBase(request, url)) {
            // use empty string if request was sent through the proxy
            return "";
        }
        // remove the path from the proxy base URL
        proxyBase = url.getProtocol() + "://" + url.getHost();
        return proxyBase + (url.getPort() == -1 ? "" : ":" + url.getPort());
    }

    /**
     * If proxy base URL injection is enabled in the CSP configuration, injects the variable for the proxy base URL into
     * the form-action directive and all fetch directives in the policy with the 'self' source. This implementation is
     * very strict and requires 'self' to be the first source in each directive. If the final policy contains a proxy
     * base URL variable, sets the thread local in case the header needs to be updated using the local workspace
     * settings.
     *
     * @param config the CSP configuration
     * @param policy the original CSP policy
     * @param test whether this is for the test form
     * @return the CSP policy with the inject proxy base URL variables
     */
    private static String injectProxyBase(CSPConfiguration config, String policy, boolean test) {
        if (config.isInjectProxyBase()) {
            // inject the proxy base url into the form-action directive and all fetch directives
            // with a 'self' source
            policy = policy.replace("form-action 'self'", "form-action 'self' ${proxy.base.url}")
                    .replace("-src 'self'", "-src 'self' ${proxy.base.url}")
                    .replace("-src-attr 'self'", "-src-attr 'self' ${proxy.base.url}")
                    .replace("-src-elem 'self'", "-src-elem 'self' ${proxy.base.url}");
            String formAction = "form-action ${" + CSPUtils.GEOSERVER_CSP_FORM_ACTION + "}";
            int index = policy.indexOf(formAction);
            if (index >= 0) {
                String value = getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_FORM_ACTION);
                if (value.contains("'self'")) {
                    policy = policy.replace(formAction, formAction + " ${proxy.base.url}");
                }
            }
        }
        if (!test && policy.contains("${proxy.base.url}")) {
            setProxyPolicy(policy);
        }
        return policy;
    }

    /**
     * Logs the value of the Content-Security-Policy header that was calculated for the provided HTTP request.
     *
     * @param request the HTTP request
     * @param policy the Content-Security-Policy value
     */
    private static void logPolicy(HttpServletRequest request, String policy) {
        LOGGER.fine(() -> {
            String query = request.getQueryString();
            return "Content-Security-Policy for request:\n"
                    + request.getMethod()
                    + ' '
                    + request.getRequestURI()
                    + (query = query != null ? '?' + query : "")
                    + '\n'
                    + policy;
        });
    }

    /**
     * Attempts to determine whether the HTTP request was originally sent to the proxy base URL by checking the
     * protocol, host name and port number. The request protocol will be determined from the X-Forwarded-Proto header,
     * the Forwarded header or the HTTP request. The request host name will be determined from the X-Forwarded-Host
     * header, the Forwarded header or the Host header. The port will be determined from the X-Forwarded-Port header,
     * the previously determined host value or the default port of the protocol.
     *
     * @param request the HTTP request
     * @param proxyBaseURL the proxy base URL
     * @return whether the request was sent to the proxy
     */
    @VisibleForTesting
    protected static boolean matchesProxyBase(HttpServletRequest request, URL proxyBaseURL) {
        String forwarded = request.getHeader(HttpHeaders.FORWARDED);
        String proto = request.getHeader(HttpHeaders.X_FORWARDED_PROTO);
        proto = proto != null ? proto : getForwardedPart(forwarded, "proto");
        proto = proto != null ? proto : request.getScheme();
        if (!proxyBaseURL.getProtocol().equals(proto)) {
            return false;
        }
        String host = request.getHeader(HttpHeaders.X_FORWARDED_HOST);
        host = host != null ? host : getForwardedPart(forwarded, "host");
        host = host != null ? host : request.getHeader(HttpHeaders.HOST);
        if (host == null) {
            return false;
        }
        String port = request.getHeader(HttpHeaders.X_FORWARDED_PORT);
        int index = host.indexOf(':');
        if (index >= 0) {
            port = port != null ? port : host.substring(index + 1);
            host = host.substring(0, index);
        }
        int reqPort = port != null ? Integer.parseInt(port) : proxyBaseURL.getDefaultPort();
        int proxyPort = proxyBaseURL.getPort();
        proxyPort = proxyPort != -1 ? proxyPort : proxyBaseURL.getDefaultPort();
        return proxyBaseURL.getHost().equals(host) && proxyPort == reqPort;
    }

    /**
     * Looks up the value for each variable in the CSP policy and replaces that variables in the policy with that value
     * if it is value. If the variable is not defined or contains an invalid value, the variable is replace with an
     * empty string.
     *
     * @param request the HTTP request
     * @param config the CSP configuration
     * @param policy the CSP value with variable names
     * @return the CSP value with all variables replaced with their values
     */
    private static String replaceVariables(HttpServletRequest request, CSPConfiguration config, String policy) {
        for (int start = policy.indexOf("${"); start >= 0; start = policy.indexOf("${")) {
            int end = policy.indexOf('}', start + 2);
            String key = policy.substring(start + 2, end);
            String value = getPropertyValue(request, config, key);
            policy = policy.replace(policy.substring(start, end + 1), value);
        }
        // remove empty form-action and frame-ancestors directives
        policy = CSPUtils.cleanDirectives(policy)
                .replace("form-action;", "")
                .replace("frame-ancestors;", "")
                .trim();
        return CSPUtils.cleanDirectives(policy);
    }

    /**
     * Set the Content-Security-Policy value from the thread local.
     *
     * @param policy the header value
     */
    @VisibleForTesting
    protected static void setProxyPolicy(String policy) {
        PROXY_POLICY.set(policy);
    }

    /** File watcher for the CSP configuration file */
    private class CSPConfigurationWatcher extends FileWatcher<CSPConfiguration> {

        /** @param dd the GeoServer data directory */
        public CSPConfigurationWatcher(GeoServerDataDirectory dd) {
            super(dd.getSecurity(CSPHeaderDAO.CONFIG_FILE_NAME));
        }

        /** @return the configuration read from the file, or a default one if the file does not exist */
        @Override
        public CSPConfiguration read() throws IOException {
            return Optional.ofNullable(super.read()).orElseGet(CSPDefaultConfiguration::newInstance);
        }

        /** @return the CSPConfiguration object parsed from the configuration file. */
        @Override
        protected CSPConfiguration parseFileContents(InputStream in) throws IOException {
            return xp.load(in, CSPConfiguration.class);
        }
    }

    /** A runnable that can throw an exception */
    private interface ThrowingRunnable {

        void run() throws IOException;
    }
}
