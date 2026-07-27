# Assembly Test

This module smoke-tests the GeoServer distribution artifacts after they have been assembled.

It verifies:

- The main `bin.zip`.
- Each extension plugin ZIP found in the main release directory.
- Each community plugin ZIP found in the community release directory.

For each case, the test unpacks a fresh copy of the binary distribution, installs the plugin under test together with any manually declared plugin dependencies, starts GeoServer on dedicated preallocated ports, and checks that WMS `GetCapabilities` responds successfully.

Before startup, the copied test data directory is adjusted to keep the WMS service SRS list small so capabilities documents stay compact during smoke testing.

These tests are meant to catch packaging and startup regressions that unit and module tests do not see:

- missing jars in plugin ZIPs
- broken distribution layout
- startup regressions caused by packaging changes
- missing plugin dependencies in assembled artifacts

This is a smoke test for assembled deliverables, not a full functional test suite for each module.

## Prerequisites

The main GeoServer distribution must be built before running this module.

The expected locations are:

- main release artifacts: `../../src/target/release`
- community release artifacts: `../../src/community/target/release`

The artifacts can be built with:

```bash
cd src
# compile and install every module to the local Maven repository
mvn -nsu -fae clean install -DskipTests -Prelease -T1C
# package the main deliverables: bin, data, war, javadoc
mvn -nsu -fae assembly:single -N
# package each extension through its per-module assembly descriptor
mvn -nsu -fae install -DskipTests -Prelease,assembly -f extension/pom.xml -T1
cd community
mvn -nsu -fae clean install -DskipTests -PcommunityRelease,assembly -T1
```

All main and extension ZIPs land in `src/target/release/`; community ZIPs land in `src/community/target/release/`.

Behavior when artifacts are missing:

- If the main release directory is missing, the test fails immediately with a message explaining that the distribution must be built first
- If no extension plugin ZIPs are present in the main release directory, extension tests are skipped
- If the community release directory is missing, community module tests are skipped

## Running It

Run from this module:

```bash
mvn -nsu -fae test
```

System properties and environment variables affecting the build:

| Purpose | System property | Environment variable | Default |
| --- | --- | --- | --- |
| Restrict tested plugins | `test.plugins` | `TEST_PLUGINS` | all discovered plugins |
| Startup timeout in seconds | `startup.timeout` | `STARTUP_TIMEOUT` | `60` |
| Poll interval in milliseconds | `startup.poll` | `STARTUP_POLL` | `200` |
| JVM options for spawned GeoServer processes | `test.java.opts` | `TEST_JAVA_OPTS` | `-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms128m -Xmx512m` |
| Dump successful logs too | `dump.logs` | `DUMP_LOGS` | `false` |

Notes:

- `test.plugins` and `TEST_PLUGINS` accept a comma-separated list of plugin names, for example `wps,importer`
- the plugin filter applies to both extension and community plugin ZIPs when present
- plugin names are derived from ZIP names such as `geoserver-<version>-wps-plugin.zip -> wps`
- successful logs are only printed in the final summary when `dump.logs` or `DUMP_LOGS` is enabled
- spawned GeoServer JVMs default to low-startup-overhead flags and can be retuned with `test.java.opts` or `TEST_JAVA_OPTS`

## Parallel Execution

The suite runs plugin tests concurrently through JUnit 5 parallel execution.

By default, the module configures parallel execution with a parallelism factor of `1`, that is,
one concurrent test per core, it can be tweaked by setting ``junit.jupiter.execution.parallel.config.dynamic.factor``.
to a different value, for example `0.5` to use half of the available cores, or `2` to use twice the available cores.

## Plugin test customization

Some plugin ZIPs do not start successfully unless another plugin ZIP is unpacked first. Those relationships, together with custom plugin testers, are maintained in `PluginRegistry.java`.

When a plugin fails because another plugin ZIP also needs to be installed:

1. Reproduce the failure with `-Dtest.plugins=<plugin>`.
2. Identify the missing plugin ZIP.
3. Add the dependency or custom tester registration to `PluginRegistry.java`.
4. Rerun the test.

In the same class it is also possible to customize the test behavior for specific plugins, 
for example to customize the data directory contents, or to use different requests 
to check if the plugin is working, instead of the default WMS `GetCapabilities` request.

## Logs And Failure Analysis

Each test stores its startup log in a persistent temporary location during the run.

At the end of the suite:

- failed test logs are always printed
- successful test logs are printed only when `dump.logs` is enabled

Failure messages include the plugin name and the saved log path to make local debugging and CI diagnosis easier.
