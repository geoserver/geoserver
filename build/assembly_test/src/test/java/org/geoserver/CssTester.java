/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.fail;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

/**
 * Smoke tester for the CSS plugin: creates a CSS style over REST and reads it back as SLD, so the CSS parser and its
 * runtime dependencies (parboiled, asm) have to load.
 */
public class CssTester extends DefaultPluginTester {

    private static final String STYLE = "css_smoke";

    @Override
    protected void verifyStarted(TestContext context, StartupProbeResult probe) throws Exception {
        super.verifyStarted(context, probe);
        postStyle(context, "* { fill: #FF0000; }");

        Document sld = getAsDom(context, "/geoserver/rest/styles/" + STYLE + ".sld");
        XPath xpath = XPathFactory.newInstance().newXPath();
        String symbolizers =
                xpath.evaluate("count(//*[local-name()='Rule']/*[contains(local-name(), 'Symbolizer')])", sld);
        String fill =
                xpath.evaluate("//*[local-name()='PolygonSymbolizer']/*[local-name()='Fill']/*[@name='fill']", sld);
        if (!"1".equals(symbolizers) || !"#FF0000".equals(fill)) {
            fail(context.pluginName() + " - Style " + STYLE + " translated to " + symbolizers
                    + " symbolizers with fill '" + fill + "', expected one polygon symbolizer with fill #FF0000");
        }
    }

    private void postStyle(TestContext context, String css) throws Exception {
        HttpURLConnection connection = openConnection(context, "/geoserver/rest/styles?name=" + STYLE);
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/vnd.geoserver.geocss+css");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(css.getBytes(UTF_8));
            }
            int status = connection.getResponseCode();
            if (status != 201) {
                fail(context.pluginName() + " - Creating style " + STYLE + " returned HTTP " + status + ": "
                        + readProbeResult(connection, status).bodyAsString());
            }
        } finally {
            connection.disconnect();
        }
    }

    /** The REST style calls need the admin. */
    @Override
    protected String basicAuthCredentials() {
        return "admin:geoserver";
    }
}
