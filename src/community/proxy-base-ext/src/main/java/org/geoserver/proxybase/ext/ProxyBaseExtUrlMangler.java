/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import static org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO.getProxyBaseExtensionRules;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.HTTPHeadersCollector;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.resource.Resource;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geotools.util.logging.Logging;

/** {@link URLMangler} implementation that allows to mangle the URLs returned by the OGC services */
public class ProxyBaseExtUrlMangler implements URLMangler {
    private static final Logger LOGGER = Logging.getLogger(ProxyBaseExtUrlMangler.class);
    private static final Pattern TEMPLATE_LITERAL_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");
    public static final String TEMPLATE_PREFIX = "${";
    public static final String TEMPLATE_POSTFIX = "}";
    public static final String PROTOCOL_SEPARATOR = "://";

    private List<ProxyBaseExtensionRule> proxyBaseExtensionRules;

    /**
     * Constructor
     *
     * @param dataDirectory the {@link GeoServerDataDirectory}
     */
    public ProxyBaseExtUrlMangler(GeoServerDataDirectory dataDirectory) {
        Resource resource = dataDirectory.get(ProxyBaseExtRuleDAO.PROXY_BASE_EXT_RULES_PATH);
        proxyBaseExtensionRules = getProxyBaseExtensionRules(resource);
        resource.addListener(
                notify -> proxyBaseExtensionRules = getProxyBaseExtensionRules(resource));
    }

    /** Constructor for testing purposes */
    public ProxyBaseExtUrlMangler(List<ProxyBaseExtensionRule> proxyBaseExtensionRules) {
        this.proxyBaseExtensionRules = proxyBaseExtensionRules;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        getURL(baseURL, path, Optional.empty(), false);
    }

    private void getURL(
            StringBuilder baseURL,
            StringBuilder path,
            Optional<String> headers,
            boolean returnLogAsOutput) {
        // does the path match any of the rules?
        Optional<String> transformer = getFirstMatchingTransformer(path.toString());
        if (transformer.isPresent()) {
            // get the template literals from the transformer
            List<String> templateLiterals =
                    TEMPLATE_LITERAL_PATTERN
                            .matcher(transformer.get())
                            .results()
                            .map(matchResult -> matchResult.group(1))
                            .collect(Collectors.toList());
            if (templateLiterals.isEmpty()) {
                // if there are no template literals, just replace the url with the transformer
                convertURL(baseURL, path, transformer.get(), false);
                return;
            }
            // if there are template literals, collect the headers
            Map<String, String> headersOut = Collections.emptyMap();
            if (headers.isPresent()) { // if headers are provided from test page, parse them
                try {
                    headersOut = collectHeaders(templateLiterals, headers.get());
                } catch (IOException e) {
                    LOGGER.info(
                            "Proxy Base Ext: Unable to parse test headers: " + transformer.get());
                }
            } else { // if headers are not provided from test page, collect them from the request
                headersOut = collectHeaders(templateLiterals);
            }
            // check if headers are missing
            if (headersOut.size() != templateLiterals.size()) {
                LOGGER.info(
                        "Proxy Base Ext: Some headers are missing, cannot transform the URL using the transformer: "
                                + transformer.get());
                return;
            }
            convertURL(baseURL, path, transformer.get(), headersOut, false);
        }
    }

    /**
     * Transforms the URL using provided headers in properties file form
     *
     * @param urlString the URL to transform
     * @param headersAsProperties the headers as a properties file style string
     * @return the transformed URL
     * @throws IOException if the URL is malformed
     */
    public String transformURL(String urlString, String headersAsProperties) throws IOException {
        URL url = new URL(urlString);
        Path p = Path.of(url.getPath());
        // separate in baseURL (which contains the context path) and path inside the application
        // also handling odd situations where path or contex path are missing, just in case
        StringBuilder baseURL, path;
        if (p.getNameCount() == 0) {
            baseURL = new StringBuilder(url.getProtocol() + PROTOCOL_SEPARATOR + url.getHost());
            path = new StringBuilder(p.subpath(1, p.getNameCount()).toString());
        } else if (p.getNameCount() == 1) {
            baseURL =
                    new StringBuilder(
                            url.getProtocol()
                                    + PROTOCOL_SEPARATOR
                                    + url.getHost()
                                    + PROTOCOL_SEPARATOR
                                    + p.getName(0));
            path = new StringBuilder("");
        } else {
            baseURL =
                    new StringBuilder(
                            url.getProtocol()
                                    + PROTOCOL_SEPARATOR
                                    + url.getHost()
                                    + PROTOCOL_SEPARATOR
                                    + p.getName(0));
            path = new StringBuilder(p.subpath(1, p.getNameCount()).toString());
        }
        getURL(baseURL, path, Optional.ofNullable(headersAsProperties), true);
        return baseURL.toString() + path.toString();
    }

    private void convertURL(
            StringBuilder baseURL,
            StringBuilder path,
            String transformer,
            Map<String, String> headers,
            boolean returnLogAsOutput) {
        String transformerReplacedLiterals = transformer;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                transformerReplacedLiterals =
                        transformerReplacedLiterals.replace(
                                TEMPLATE_PREFIX + entry.getKey() + TEMPLATE_POSTFIX,
                                entry.getValue());
            }
        }
        try {
            URL url = new URL(transformerReplacedLiterals);
            baseURL.setLength(0);
            baseURL.append(url.getProtocol() + PROTOCOL_SEPARATOR + url.getHost());
            // add port if necessary
            if (url.getPort() != -1
                    && (("http".equals(url.getProtocol()) && url.getPort() != 80)
                            || ("https".equals(url.getProtocol()) && url.getPort() != 443))) {
                baseURL.append(":" + url.getPort());
            }
            path.setLength(0);
            path.append(url.getPath());
        } catch (MalformedURLException e) {
            String message =
                    "The transformer, after header template replacement (if headers were provided): "
                            + transformerReplacedLiterals
                            + " is not a valid URL: "
                            + e.getMessage();
            LOGGER.log(Level.ALL, message, e);
            if (returnLogAsOutput) {
                baseURL.setLength(0);
                path.setLength(0);
                path.append(message);
            }
            return;
        }
    }

    private void convertURL(
            StringBuilder baseURL,
            StringBuilder path,
            String transformer,
            boolean returnLogAsOutput) {
        convertURL(baseURL, path, transformer, null, returnLogAsOutput);
    }

    private Optional<String> getFirstMatchingTransformer(String path) {
        proxyBaseExtensionRules.sort(ProxyBaseExtensionRule::compareTo);
        return proxyBaseExtensionRules.stream()
                .filter(rule -> ruleMatches(path, rule))
                .map(ProxyBaseExtensionRule::getTransformer)
                .findFirst();
    }

    private boolean ruleMatches(String path, ProxyBaseExtensionRule rule) {
        return rule.isActivated() && path.matches(rule.getMatcher());
    }

    private Map<String, String> collectHeaders(List<String> requiredHeaders) {
        return requiredHeaders.stream()
                .filter(headerName -> HTTPHeadersCollector.getHeader(headerName) != null)
                .collect(Collectors.toMap(item -> item, HTTPHeadersCollector::getHeader));
    }

    private Map<String, String> collectHeaders(
            List<String> requiredHeaders, String headersAsProperties) throws IOException {
        Properties properties = parsePropertiesString(headersAsProperties);
        return requiredHeaders.stream()
                .filter(headerName -> properties.getProperty(headerName) != null)
                .collect(Collectors.toMap(item -> item, properties::getProperty));
    }

    private Properties parsePropertiesString(String s) throws IOException {
        final Properties p = new Properties();
        p.load(new StringReader(s));
        return p;
    }
}
