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
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Base class for plugin smoke testers; subclasses override hooks to customize setup, the readiness path, and
 * post-startup verification.
 */
abstract class AbstractPluginTester {

    /** Runs the standard startup probe and then any plugin-specific verification. */
    public final void verify(TestContext context) throws Exception {
        StartupProbeResult probe = waitForStartup(context);
        verifyStarted(context, probe);
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
        HttpURLConnection connection = (HttpURLConnection) new URL(context.baseUrl() + path).openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(5000);
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
