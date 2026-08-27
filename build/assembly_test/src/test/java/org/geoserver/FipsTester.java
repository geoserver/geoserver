/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Installs the fips plugin the way the documentation says, then checks that the running GeoServer really is in FIPS
 * mode.
 *
 * <p>Unpacking the archive is not enough for this plugin: the regular BouncyCastle jars have to go, or both they and
 * the FIPS-validated ones share the {@code org.bouncycastle} package space and GeoServer refuses to start. See
 * {@code doc/en/user/community/fips/installing.md}, whose step 4 this repeats.
 */
public class FipsTester extends DefaultPluginTester {

    /** Jars the release ships and a FIPS installation replaces, as listed in the installation instructions. */
    private static final List<String> REGULAR_JARS = List.of("bcprov-lts8on-", "bcpkix-lts8on-", "bcutil-lts8on-");

    /** What the plugin archive brings in their place. */
    private static final List<String> FIPS_JARS = List.of("bc-fips-", "bcpkix-fips-", "bcutil-fips-");

    @Override
    protected void prepareTestDirectory(Path testWorkDir) throws Exception {
        Path lib = testWorkDir.resolve("webapps/geoserver/WEB-INF/lib");
        for (String prefix : REGULAR_JARS) {
            for (Path jar : jars(lib, prefix)) {
                Files.delete(jar);
            }
        }
        // a renamed jar in either list would leave the classpath mixed, which fails later and reads as a FIPS bug
        for (String prefix : REGULAR_JARS) {
            assertTrue(jars(lib, prefix).isEmpty(), "Regular BouncyCastle jar left in WEB-INF/lib: " + prefix);
        }
        for (String prefix : FIPS_JARS) {
            assertTrue(!jars(lib, prefix).isEmpty(), "The fips plugin archive brought no " + prefix + " jar");
        }
    }

    /** The admin account of a fresh data directory, needed to read the module status over REST. */
    @Override
    protected String basicAuthCredentials() {
        return "admin:geoserver";
    }

    /**
     * Beyond the default capabilities check: the FIPS module has to report itself, and report the validated crypto
     * module as ready. A GeoServer that started on the regular provider would answer the capabilities request just as
     * well.
     *
     * <p>The check reads the HTML page rather than {@code status.json}: the JSON and XML representations carry only the
     * module names and links to themselves, while the module id and the message with the facts appear in the HTML one.
     */
    @Override
    protected void verifyStarted(TestContext context, StartupProbeResult probe) throws Exception {
        super.verifyStarted(context, probe);

        String status = getAsString(context, "/geoserver/rest/about/status.html");
        if (!status.contains("gs-fips-provider")) {
            fail(context.pluginName() + " - the FIPS module is missing from /rest/about/status");
        }
        if (!status.contains("Crypto module: READY")) {
            fail(context.pluginName() + " - the validated crypto module is not ready, module reported: "
                    + fipsReport(status));
        }
        if (!status.contains("Approved-only mode: on")) {
            fail(context.pluginName() + " - approved-only mode is off, module reported: " + fipsReport(status));
        }
    }

    /** The part of the status page the FIPS module wrote, so a failure does not print the whole page. */
    private static String fipsReport(String status) {
        int start = status.indexOf("gs-fips-provider");
        return status.substring(start, Math.min(start + 1000, status.length()));
    }

    private static List<Path> jars(Path lib, String prefix) throws IOException {
        try (Stream<Path> files = Files.list(lib)) {
            return files.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .toList();
        }
    }
}
