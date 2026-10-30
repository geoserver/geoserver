/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Base class for plugin smoke testers; subclasses override hooks to customize setup, the readiness path, and
 * post-startup verification.
 */
abstract class AbstractPluginTester {

    private static final String ADMIN = "admin:geoserver";

    private static final Pattern GS_JAR = Pattern.compile("(gs-.+?)-\\d+\\.\\d+.*\\.jar");

    /** Runs the standard startup probe, any plugin-specific verification, and the module status check. */
    public final void verify(TestContext context) throws Exception {
        StartupProbeResult probe = waitForStartup(context);
        verifyStarted(context, probe);
        if (context.pluginZip() == null) {
            verifyCoreModuleStatus(context);
        } else if (!PluginRegistry.isModuleStatusIgnored(context.pluginName())) {
            verifyModuleStatus(context);
        }
    }

    /** Checks that every GeoServer jar of the bin package, and PostGIS, reports a module status with a version. */
    protected void verifyCoreModuleStatus(TestContext context) throws Exception {
        String html = getAsAdmin(context, "/geoserver/rest/about/status.html");
        List<String> missingVersion = Stream.of(html.split("<h3>Module name: "))
                .skip(1)
                .filter(module -> !module.contains("<b>Version</b>"))
                .map(module -> module.substring(0, module.indexOf("</h3>")))
                .toList();
        if (!missingVersion.isEmpty()) {
            fail(context.pluginName() + " - Module status without a version: " + missingVersion);
        }
        List<String> missingStatus;
        try (Stream<Path> jars = Files.list(context.workDir().resolve("webapps/geoserver/WEB-INF/lib"))) {
            missingStatus = jars.map(jar -> GS_JAR.matcher(jar.getFileName().toString()))
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .filter(module -> !html.contains("<b>Module</b> : " + module + "</li>"))
                    .sorted()
                    .toList();
        }
        if (!missingStatus.isEmpty()) {
            fail(context.pluginName() + " - Jars without a module status: " + missingStatus);
        }
        if (!html.contains("PostgreSQL JDBC driver version: ")) {
            fail(context.pluginName() + " - PostGIS module status not found");
        }
    }

    /** Checks that a GeoServer jar from the plugin zip reports a module status with a version. */
    protected void verifyModuleStatus(TestContext context) throws Exception {
        List<String> artifacts = gsArtifacts(context.pluginZip());
        for (String artifact : artifacts) {
            String path = "/geoserver/rest/about/status/" + artifact + ".html";
            HttpURLConnection connection = openConnection(context, path, ADMIN);
            try {
                if (connection.getResponseCode() == 200
                        && readProbeResult(connection, 200).bodyAsString().contains("<b>Version</b>")) {
                    return;
                }
            } finally {
                connection.disconnect();
            }
        }
        fail(context.pluginName() + " - No module status with a version found for any of " + artifacts);
    }

    private String getAsAdmin(TestContext context, String path) throws Exception {
        HttpURLConnection connection = openConnection(context, path, ADMIN);
        try {
            if (connection.getResponseCode() != 200) {
                fail(context.pluginName() + " - Request to " + path + " returned HTTP " + connection.getResponseCode());
            }
            return readProbeResult(connection, 200).bodyAsString();
        } finally {
            connection.disconnect();
        }
    }

    private static List<String> gsArtifacts(Path pluginZip) throws IOException {
        try (ZipFile zip = new ZipFile(pluginZip.toFile())) {
            return zip.stream()
                    .map(entry -> Path.of(entry.getName()).getFileName().toString())
                    .map(GS_JAR::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .toList();
        }
    }

    /**
     * Allows a subclass to manipulate the test working directory before the plugin is launched (e.g. to add config
     * files). By default does nothing.
     *
     * @param testWorkDir The directory containing the whole GeoServer bin structure. If looking for the data directory,
     *     it's under data_dir.
     * @throws Exception if any error occurs during preparation
     */
    protected void prepareTestDirectory(Path testWorkDir) throws Exception {
        // Default no-op.
    }

    /**
     * Called after the spawned GeoServer process has been shut down, symmetric with {@link #prepareTestDirectory}.
     * Subclasses override to release resources acquired there (e.g. stop a database container). By default does
     * nothing.
     *
     * @param testWorkDir The same directory passed to {@link #prepareTestDirectory}, still present at this point.
     */
    protected void cleanupTestDirectory(Path testWorkDir) {
        // Default no-op.
    }

    /**
     * Extra {@code -D} system properties to pass to the spawned GeoServer JVM via {@code JAVA_OPTS}. Subclasses
     * override to configure plugins that require system properties to reach a healthy startup state (e.g. remote
     * service URLs, credentials, feature toggles). Returned values are not shell-quoted — keep them simple.
     */
    protected Map<String, String> systemProperties() {
        return Map.of();
    }

    /**
     * Basic-auth credentials ({@code "user:password"}) for all HTTP requests the tester makes — the readiness probe and
     * any helper {@code GET} calls. Subclasses override when the plugin requires an authenticated caller (e.g. the ACL
     * plugin, which invokes its access manager even for GetCapabilities and refuses anonymous callers when its remote
     * API is unreachable). Returning {@code null} (the default) sends no {@code Authorization} header.
     */
    protected String basicAuthCredentials() {
        return null;
    }

    /** Polls the startup check path until it returns HTTP 200 or the process exits/times out. */
    protected StartupProbeResult waitForStartup(TestContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeoutMs = context.startupTimeoutSeconds() * 1000L;
        String readinessPath = getStartupCheckPath(context);

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            Process process = context.process();
            if (!process.isAlive()) {
                fail(context.pluginName() + " - Process died prematurely with exit code " + process.exitValue());
            }

            HttpURLConnection connection = null;
            try {
                connection = openConnection(context, readinessPath);
                int responseCode = connection.getResponseCode();
                StartupProbeResult probe = readProbeResult(connection, responseCode);
                if (responseCode == 200) {
                    return verifyReadiness(context, probe);
                }
            } catch (IOException e) {
                // Not ready yet
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Thread.sleep(context.startupPollIntervalMs());
        }

        fail(context.pluginName() + " - GeoServer failed to start within " + context.startupTimeoutSeconds()
                + " seconds.");
        return null;
    }

    protected String getStartupCheckPath(TestContext context) {
        return "/geoserver/wms?request=GetCapabilities&version=1.3.0";
    }

    /** Verifies the successful startup probe and returns it for later reuse. */
    protected StartupProbeResult verifyReadiness(TestContext context, StartupProbeResult probe) throws Exception {
        verifyStartupCheck(context, probe);
        return probe;
    }

    /** Validates the startup probe response. Defaults to a WMS GetCapabilities check. */
    protected void verifyStartupCheck(TestContext context, StartupProbeResult probe) throws Exception {
        verifyCapabilitiesResponse(context, probe.bodyAsString(), parseXml(probe.body()), "WMS_Capabilities");
    }

    /** Runs plugin-specific checks using the cached startup probe. */
    protected abstract void verifyStarted(TestContext context, StartupProbeResult probe) throws Exception;

    protected HttpURLConnection openConnection(TestContext context, String path) throws IOException {
        return openConnection(context, path, basicAuthCredentials());
    }

    protected HttpURLConnection openConnection(TestContext context, String path, String credentials)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(context.baseUrl() + path).openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(5000);
        if (credentials != null) {
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encoded);
        }
        return connection;
    }

    protected Document getAsDom(TestContext context, String path) throws Exception {
        HttpURLConnection connection = openConnection(context, path);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                fail(context.pluginName() + " - Request to " + path + " returned HTTP " + responseCode);
            }
            return parseXml(connection);
        } finally {
            connection.disconnect();
        }
    }

    protected String getAsString(TestContext context, String path) throws Exception {
        HttpURLConnection connection = openConnection(context, path);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                fail(context.pluginName() + " - Request to " + path + " returned HTTP " + responseCode);
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    protected StartupProbeResult get(TestContext context, String path) throws Exception {
        HttpURLConnection connection = openConnection(context, path);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                fail(context.pluginName() + " - Request to " + path + " returned HTTP " + responseCode);
            }
            return readProbeResult(connection, responseCode);
        } finally {
            connection.disconnect();
        }
    }

    protected void verifyCapabilitiesDocument(TestContext context, Document document, String expectedRootElement) {
        String rootName = document.getDocumentElement() != null
                ? document.getDocumentElement().getLocalName()
                : null;
        if (!expectedRootElement.equals(rootName)) {
            fail(context.pluginName() + " - Verification FAILED: expected root element " + expectedRootElement
                    + " but got " + rootName + ".");
        }
    }

    protected void verifyCapabilitiesResponse(
            TestContext context, String responseBody, Document document, String expectedRootElement) {
        verifyCapabilitiesDocument(context, document, expectedRootElement);
        if (responseBody.contains("<ServiceException")) {
            fail(context.pluginName() + " - Verification FAILED: ServiceException found in response.");
        }
        if (!responseBody.contains(":" + context.httpPort() + "/")) {
            fail(context.pluginName() + " - Verification FAILED: Response does not contain the expected port "
                    + context.httpPort() + ".");
        }
    }

    protected Document parseXml(HttpURLConnection connection) throws Exception {
        try (InputStream is = connection.getInputStream()) {
            return parseXML(is);
        }
    }

    protected Document parseXml(byte[] responseBytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(responseBytes)) {
            return parseXML(is);
        }
    }

    private static Document parseXML(InputStream is) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(is);
    }

    protected StartupProbeResult readProbeResult(HttpURLConnection connection) throws IOException {
        return readProbeResult(connection, connection.getResponseCode());
    }

    protected StartupProbeResult readProbeResult(HttpURLConnection connection, int statusCode) throws IOException {
        byte[] body;
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            body = new byte[0];
        } else {
            try (InputStream in = stream) {
                body = in.readAllBytes();
            }
        }
        return new StartupProbeResult(statusCode, connection.getContentType(), body);
    }

    /** Minimal HTTP response data captured from the startup probe or helper GET requests. */
    protected record StartupProbeResult(int statusCode, String contentType, byte[] body) {
        String bodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
