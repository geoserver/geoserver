/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that GeoServer assembly results (bin.zip and plugins) work correctly.
 *
 * @see #testBin()
 * @see #testExtensions(String, Path)
 * @see #testCommunityModules(String, Path)
 */
@SuppressWarnings("PMD.SystemPrintln")
// very long test , prints on the stdout to provide information about current work
public class AssemblyTest {

    private static final int STARTUP_TIMEOUT_SECONDS = getIntProp("startup.timeout", "STARTUP_TIMEOUT", 60);
    private static final int STARTUP_POLL_INTERVAL_MS = getIntProp("startup.poll", "STARTUP_POLL", 200);

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 15;
    private static final boolean DUMP_LOGS =
            Boolean.getBoolean("dump.logs") || "true".equalsIgnoreCase(System.getenv("DUMP_LOGS"));
    /** Options to speed up startup, the JVM is going to be short-lived anyways */
    private static final String TEST_JAVA_OPTS = System.getProperty(
            "test.java.opts",
            System.getenv()
                    .getOrDefault("TEST_JAVA_OPTS", "-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms128m -Xmx512m"));
    /** Use a limited SRS list to speed up */
    private static final String TEST_WMS_SRS =
            """
                      <srs>
                        <string>4326</string>
                        <string>3857</string>
                      </srs>
                    """;
    /** Computes the execution parallelism based on the JUnit configuration, or defaults to 1 test per core. */
    private static final int PARALLELISM = getParallelism();

    /**
     * Determines the parallelism level for test execution based on JUnit configuration, if not set defaults to using
     * all available cores.
     */
    private static int getParallelism() {
        String strategy = System.getProperty("junit.jupiter.execution.parallel.config.strategy");
        int cores = Runtime.getRuntime().availableProcessors();
        if ("fixed".equals(strategy)) {
            return Integer.getInteger("junit.jupiter.execution.parallel.config.fixed.parallelism", cores);
        } else if ("dynamic".equals(strategy)) {
            String factorStr = System.getProperty("junit.jupiter.execution.parallel.config.dynamic.factor");
            double factor = factorStr != null ? Double.parseDouble(factorStr) : 1.0;
            return (int) Math.max(1, Math.ceil(cores * factor));
        }
        return cores;
    }

    private static Path binZip;
    private static List<Path> extensionZips;
    private static List<Path> communityZips;
    private static final Map<String, Path> pluginNameToZip = new HashMap<>();

    private static String gsVersion;

    @TempDir
    static Path sharedTempDir;

    private static Path binTemplate;
    private static Path persistentLogsDir;
    private static Set<String> SELECTED_PLUGINS;

    /** Collection of test results, used at the end of the test to print out the startup logs */
    private static final Queue<TestResult> TEST_RESULTS = new ConcurrentLinkedQueue<>();

    /**
     * Pool of TCP ports that were determined to be free at the test startup. Contains as many ports as twice the
     * PARALLELISM, one HTTP port and one STOP per test. Ports are acquired before each test and returned back to the
     * pool at the end of the test, so they can be reused by other tests.
     */
    private static final Queue<Integer> PORT_POOL = new ConcurrentLinkedQueue<>();

    private static int getIntProp(String sysProp, String envVar, int defaultValue) {
        String val = System.getProperty(sysProp, System.getenv(envVar));
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                System.err.println("Invalid value for " + sysProp + "/" + envVar + ": " + val + ". Using default: "
                        + defaultValue);
            }
        }
        return defaultValue;
    }

    /** Basic test result information (plugin, success, log file location) */
    private static class TestResult {
        String pluginName;
        boolean success;
        Path logFile;

        TestResult(String pluginName, boolean success, Path logFile) {
            this.pluginName = pluginName;
            this.success = success;
            this.logFile = logFile;
        }
    }

    @BeforeAll
    static void setup() throws IOException {
        discoverArtifacts();

        persistentLogsDir = sharedTempDir.resolve("persistent-logs");
        Files.createDirectories(persistentLogsDir);

        binTemplate = sharedTempDir.resolve("bin-template");
        Files.createDirectories(binTemplate);
        unpackZip(binZip, binTemplate);

        initializePortPool();
    }

    /**
     * Figure out the location of all zip files to be tested:
     *
     * <ul>
     *   <li>bin package
     *   <li>extension zips (optional)
     *   <li>community modules zips (optional)
     * </ul>
     *
     * @throws IOException
     */
    private static synchronized void discoverArtifacts() throws IOException {
        if (extensionZips != null && communityZips != null && gsVersion != null) {
            return;
        }

        String releaseDirPath = "../../src/target/release";
        Path releaseDir = Paths.get(releaseDirPath).toAbsolutePath();
        if (!Files.isDirectory(releaseDir)) {
            throw new IllegalStateException("GeoServer release artifacts not found at " + releaseDir
                    + ". Build the distribution first, then rerun this test.");
        }

        List<Path> releaseZipFiles = getZipFiles(releaseDir);

        // main bin file
        binZip = getBinZip(releaseZipFiles);
        String binName = binZip.getFileName().toString();
        gsVersion = binName.substring("geoserver-".length(), binName.length() - "-bin.zip".length());

        // extensions
        extensionZips = getPluginZips(releaseZipFiles);
        pluginNameToZip.clear();
        extensionZips.forEach(
                p -> pluginNameToZip.put(extractPluginName(p.getFileName().toString()), p));

        // community modules
        Path communityReleaseDir =
                Paths.get("../../src/community/target/release").toAbsolutePath();
        communityZips = List.of();
        if (Files.isDirectory(communityReleaseDir)) {
            List<Path> communityZipFiles = getZipFiles(communityReleaseDir);
            communityZips = getPluginZips(communityZipFiles);
            communityZips.forEach(
                    p -> pluginNameToZip.put(extractPluginName(p.getFileName().toString()), p));
        } else {
            System.out.println(
                    "Community release directory not found, skipping community module tests: " + communityReleaseDir);
        }
        loadSelectedPlugins();
    }

    /** Initialize port pool with 2*PARALLELISM ports (HTTP + STOP) */
    private static void initializePortPool() throws IOException {
        PORT_POOL.clear();
        List<ServerSocket> sockets = new ArrayList<>();
        try {
            for (int i = 0; i < PARALLELISM * 2; i++) {
                ServerSocket socket = new ServerSocket(0);
                socket.setReuseAddress(false);
                sockets.add(socket);
            }
            for (ServerSocket s : sockets) {
                PORT_POOL.add(s.getLocalPort());
            }
        } finally {
            for (ServerSocket s : sockets) {
                s.close();
            }
        }
    }

    /** Checks if there is a list of test plugins to be verified (otherwise all zips will be tested) */
    private static void loadSelectedPlugins() {
        String selected = System.getProperty("test.plugins", System.getenv("TEST_PLUGINS"));
        if (selected != null && !selected.isBlank()) {
            SELECTED_PLUGINS = Stream.of(selected.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Find the bin zip amongst the provided list of zip files, or throws an exception if not found.
     *
     * @param zipFiles The list of zip files in the src/target/release directory
     * @return The bin-zip file Path
     */
    private static Path getBinZip(List<Path> zipFiles) {
        return zipFiles.stream()
                .filter(p -> p.getFileName().toString().contains("-bin.zip"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find a GeoServer bin.zip amongst the provided zip list " + zipFiles));
    }

    /**
     * Returns the list of plugin zips found in the zip file list, based on naming convention.
     *
     * @param zipFiles All the zip files found in target/release (either top level, or community)
     * @return The zip files that match the plugin naming convention
     */
    private static List<Path> getPluginZips(List<Path> zipFiles) {
        return zipFiles.stream()
                .filter(p -> p.getFileName().toString().contains("-plugin.zip"))
                .collect(Collectors.toList());
    }

    /**
     * Returns all the zip files contained in the target directory
     *
     * @param dir The directory to be explored
     * @return The zip files in the target directory
     * @throws IOException
     */
    private static List<Path> getZipFiles(Path dir) throws IOException {
        try (Stream<Path> zips = Files.list(dir)) {
            return zips.filter(p -> p.toString().endsWith(".zip")).collect(Collectors.toList());
        }
    }

    /**
     * Tests the bin file on its own, ensuring that a vanilla GeoServer can start
     *
     * @throws Exception
     */
    @Test
    void testBin() throws Exception {
        runStartupTest(null, "bin");
    }

    /** Tests each extension plugin zip package with the bin file, ensuring that the combo works */
    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("extensionPlugins")
    @Execution(ExecutionMode.CONCURRENT)
    @SuppressWarnings("PMD.UnusedFormalParameter")
    void testExtensions(String displayName, Path pluginZip) throws Exception {
        runStartupTest(pluginZip, extractPluginName(pluginZip.getFileName().toString()));
    }

    private static Stream<Arguments> extensionPlugins() {
        return extensionZips.stream().filter(AssemblyTest::isPluginSelected).map(pluginZip -> {
            String pluginName = extractPluginName(pluginZip.getFileName().toString());
            return Arguments.of("Test extension: " + pluginName, pluginZip);
        });
    }

    /** Tests each community module zip package with the bin file, ensuring that the combo works */
    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("communityPlugins")
    @Execution(ExecutionMode.CONCURRENT)
    @SuppressWarnings("PMD.UnusedFormalParameter")
    void testCommunityModules(String displayName, Path pluginZip) throws Exception {
        runStartupTest(pluginZip, extractPluginName(pluginZip.getFileName().toString()));
    }

    private static Stream<Arguments> communityPlugins() {
        return communityZips.stream().filter(AssemblyTest::isPluginSelected).map(pluginZip -> {
            String pluginName = extractPluginName(pluginZip.getFileName().toString());
            return Arguments.of("Test community module: " + pluginName, pluginZip);
        });
    }

    private static boolean isPluginSelected(Path pluginZip) {
        if (SELECTED_PLUGINS == null || SELECTED_PLUGINS.isEmpty()) {
            return true;
        }
        String pluginName = extractPluginName(pluginZip.getFileName().toString());
        return SELECTED_PLUGINS.contains(pluginName);
    }

    /**
     * Extracts the plugin name from the file name based on the <code>geoserver-<version>-name-<plugin></code> naming
     * convention
     *
     * @param fileName The full zip file name
     * @return
     */
    private static String extractPluginName(String fileName) {
        String prefix = "geoserver-" + gsVersion + "-";
        String suffix = "-plugin.zip";
        if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
            return fileName.substring(prefix.length(), fileName.length() - suffix.length());
        }
        return fileName;
    }

    /**
     * Unpacks the bin template, installs the plugin and its dependencies, starts GeoServer, verifies it, then shuts it
     * down. Saves the log regardless of outcome.
     *
     * @param pluginZip The plugin zip
     * @param name The plugin name
     */
    private void runStartupTest(Path pluginZip, String name) throws Exception {
        long startNanos = System.nanoTime();
        Integer httpPort = null;
        Integer stopPort = null;

        boolean succeeded = false;
        Path persistentLogFile = null;

        try {
            logTestProgress("START", name, null);
            httpPort = acquirePort(name, "HTTP");
            stopPort = acquirePort(name, "STOP");
            Path testWorkDir = Files.createTempDirectory(sharedTempDir, "test-" + name);
            try {
                AbstractPluginTester tester = getPluginTester(name);
                FileUtils.copyDirectory(binTemplate.toFile(), testWorkDir.toFile());
                Path libDir = testWorkDir.resolve("webapps/geoserver/WEB-INF/lib");

                if (pluginZip != null) {
                    // Resolve and unpack all dependencies
                    Set<String> allDeps = new LinkedHashSet<>();
                    resolveDependencies(name, allDeps, new LinkedHashSet<>());
                    for (String depName : allDeps) {
                        if (depName.equals(name)) continue;
                        Path depZip = pluginNameToZip.get(depName);
                        if (depZip != null) {
                            unpackZip(depZip, libDir);
                        } else {
                            System.err.println("Warning: dependency " + depName + " for " + name + " not found.");
                        }
                    }
                    // Unpack the plugin itself
                    unpackZip(pluginZip, libDir);
                }

                Path logDir = testWorkDir.resolve("data_dir/logs");
                if (Files.exists(logDir)) {
                    FileUtils.cleanDirectory(logDir.toFile());
                }
                configureDataDir(testWorkDir);

                Path startupScript = testWorkDir.resolve("bin/startup.sh");
                patchStartupScript(startupScript);
                startupScript.toFile().setExecutable(true);
                tester.prepareTestDirectory(testWorkDir);

                ProcessBuilder pb = new ProcessBuilder("./bin/startup.sh");
                pb.directory(testWorkDir.toFile());
                pb.environment().put("JETTY_OPTS", "-Djetty.http.port=" + httpPort);
                StringBuilder javaOpts = new StringBuilder(TEST_JAVA_OPTS)
                        .append(" -DSTOP.PORT=")
                        .append(stopPort)
                        .append(" -DSTOP.KEY=stopkey-")
                        .append(httpPort);
                tester.systemProperties()
                        .forEach((k, v) ->
                                javaOpts.append(" -D").append(k).append('=').append(v));
                pb.environment().put("JAVA_OPTS", javaOpts.toString());

                pb.redirectErrorStream(true);
                File stdoutLogFile =
                        testWorkDir.resolve("geoserver-startup.log").toFile();
                pb.redirectOutput(stdoutLogFile);

                Process process = pb.start();
                try {
                    TestContext context = new TestContext(
                            name,
                            testWorkDir,
                            process,
                            httpPort,
                            stopPort,
                            STARTUP_TIMEOUT_SECONDS,
                            STARTUP_POLL_INTERVAL_MS);
                    tester.verify(context);
                    succeeded = true;
                } finally {
                    // Copy log to persistent storage
                    String resultSuffix = succeeded ? "-success.log" : "-failure.log";
                    persistentLogFile = persistentLogsDir.resolve(name + resultSuffix);
                    Files.copy(stdoutLogFile.toPath(), persistentLogFile);

                    process.destroy();
                    if (!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } finally {
                FileUtils.deleteDirectory(testWorkDir.toFile());
            }
        } finally {
            // return ports to pool
            if (httpPort != null) {
                PORT_POOL.offer(httpPort);
            }
            if (stopPort != null) {
                PORT_POOL.offer(stopPort);
            }

            // save test results for logging later
            TEST_RESULTS.add(new TestResult(name, succeeded, persistentLogFile));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String status = succeeded ? "SUCCESS" : "FAIL";
            String detail =
                    "after " + elapsedMs + " ms" + (persistentLogFile != null ? ", log: " + persistentLogFile : "");
            logTestProgress(status, name, detail);
            // finally verify if the plugin broke the startup, or not
            if (!succeeded)
                fail("GeoServer failed to start or verify with plugin " + name + ". Check startup logs for details.");
        }
    }

    private static void logTestProgress(String phase, String pluginName, String detail) {
        String message = "[" + phase + "] " + pluginName;
        if (detail != null && !detail.isBlank()) {
            message += " - " + detail;
        }
        System.out.println(message);
    }

    private AbstractPluginTester getPluginTester(String pluginName) {
        return PluginRegistry.testerFor(pluginName);
    }

    /** Grabs a port from the pool, throws an exception if there was none free */
    private int acquirePort(String pluginName, String portKind) {
        Integer port = PORT_POOL.poll();
        if (port == null) {
            throw new IllegalStateException("No preallocated " + portKind + " port available for " + pluginName
                    + ". The test parallelism exceeded the preallocated port pool size (" + (PARALLELISM * 2)
                    + " ports for " + PARALLELISM + " concurrent tests).");
        }
        return port;
    }

    /**
     * Resolves cross-plugin dependencies, so that we can unpack all plugins needed for the current test
     *
     * @param pluginName The plugin to be tested
     * @param resolved The Set of resolved plugins
     * @param visiting The set of plugins already visited (to avoid infinite loops with potential circular loops)
     */
    private void resolveDependencies(String pluginName, Set<String> resolved, Set<String> visiting) {
        if (visiting.contains(pluginName)) {
            return; // Circularity detected or already visiting
        }
        visiting.add(pluginName);
        List<String> deps = PluginRegistry.dependenciesFor(pluginName);
        if (deps != null) {
            for (String dep : deps) {
                if (!resolved.contains(dep)) {
                    resolveDependencies(dep, resolved, visiting);
                    resolved.add(dep);
                }
            }
        }
        visiting.remove(pluginName);
    }

    /** Patches the startup script so that we can have a dynamic stop port */
    private void patchStartupScript(Path scriptPath) throws IOException {
        String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
        content = content.replace("-DSTOP.PORT=8079", "");
        content = content.replace("-DSTOP.KEY=geoserver", "");
        Files.writeString(scriptPath, content, StandardCharsets.UTF_8);
    }

    private void configureDataDir(Path testWorkDir) throws IOException {
        Files.createDirectories(testWorkDir.resolve("data_dir/legendsamples"));
        Path wms = testWorkDir.resolve("data_dir/wms.xml");
        String xml = Files.readString(wms, StandardCharsets.UTF_8);
        if (xml.contains("<srs>")) {
            xml = xml.replaceAll("(?s)\\s*<srs>.*?</srs>", "\n" + TEST_WMS_SRS);
        } else {
            xml = xml.replace("</wms>", TEST_WMS_SRS + "</wms>");
        }
        Files.writeString(wms, xml, StandardCharsets.UTF_8);
    }

    /**
     * Dumps the startup logs of all failing tests (and of the ones succeeding too, if so configured). Run after all the
     * tests to avoid issues during concurrent test execution, and to have all logs in one place at the end of the test
     * run, with a clear indication of which log belongs to which plugin and whether the test succeeded or failed.
     */
    @AfterAll
    static void dumpFinalLogs() {
        System.out.println("\n=== FINAL TEST LOGS SUMMARY ===\n");

        List<TestResult> results = new ArrayList<>(TEST_RESULTS);

        // 1. Dump failures first
        List<TestResult> failures = results.stream().filter(r -> !r.success).toList();
        if (!failures.isEmpty()) {
            System.out.println("--- FAILED TESTS LOGS ---");
            for (TestResult r : failures) {
                dumpResultLog(r);
            }
        }

        // 2. Dump successes if DUMP_LOGS is true
        if (DUMP_LOGS) {
            List<TestResult> successes = results.stream().filter(r -> r.success).toList();
            if (!successes.isEmpty()) {
                System.out.println("--- SUCCESSFUL TESTS LOGS ---");
                for (TestResult r : successes) {
                    dumpResultLog(r);
                }
            }
        }
        System.out.println("=== END OF LOGS SUMMARY ===\n");
    }

    /** Dumps the logs for a specific test result */
    private static void dumpResultLog(TestResult r) {
        System.out.println("LOG FOR [" + r.pluginName + "] (" + (r.success ? "SUCCESS" : "FAILURE") + ")");
        if (r.logFile != null && Files.exists(r.logFile)) {
            try (Stream<String> lines = Files.lines(r.logFile)) {
                lines.forEach(System.out::println);
            } catch (IOException e) {
                System.err.println("Could not read log file: " + r.logFile);
            }
        } else {
            System.err.println("Log file not found for " + r.pluginName);
        }
        System.out.println("--------------------------------------------------\n\n");
    }

    /** Unpacks a zip file */
    private static void unpackZip(Path zipFilePath, Path destDir) throws IOException {
        try (ZipFile zipFile = ZipFile.builder().setFile(zipFilePath.toFile()).get()) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                File entryDestination = destDir.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zipFile.getInputStream(entry);
                            FileOutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }
}
