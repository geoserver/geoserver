/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/** utility interface for thread and memory heap info */
public final class ConsoleInfoUtils {

    private ConsoleInfoUtils() {}

    /**
     * @param lockedMonitors From ThreadMXBean docs: if true, dump all locked monitors
     * @param lockedSynchronizers From ThreadMXBean docs: if true, dump all locked ownable
     *     synchronizers
     * @return large text with information about JVM threads
     */
    static String getThreadsInfo(boolean lockedMonitors, boolean lockedSynchronizers) {
        return Arrays.stream(
                        ManagementFactory.getThreadMXBean()
                                .dumpAllThreads(lockedMonitors, lockedSynchronizers))
                .map(ThreadInfo::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * To retrieve the PID of the Java process is used the method
     * ManagementFactory.getRuntimeMXBean().getName() The returned name usually is the format
     * [pid]@[machinename] but depends on the implementation of the JVM
     *
     * @return the histo memory dump in the format produced by the jmap -histo:live
     */
    static String getHistoMemoryDump() {
        try {
            int pid =
                    Optional.ofNullable(ManagementFactory.getRuntimeMXBean().getName())
                            .map(processName -> Arrays.asList(processName.split("@")))
                            .filter(elements -> !elements.isEmpty())
                            .map(maybePid -> Integer.valueOf(maybePid.get(0)))
                            .orElseThrow(
                                    () ->
                                            new IOException(
                                                    "Problem on getting the PID for the java process"));

            final Process jmapProcess =
                    Runtime.getRuntime().exec(String.format("jmap -histo:live %d", pid));
            try (final BufferedReader stdConsole =
                    new BufferedReader(new InputStreamReader(jmapProcess.getInputStream()))) {

                final StringBuilder accumulator = new StringBuilder();
                String buffer;
                while ((buffer = stdConsole.readLine()) != null) {
                    accumulator.append(buffer).append("\n");
                }
                return accumulator.toString();
            }
        } catch (IOException e) {
            return String.format(
                    "Error reading the histo memory dump with the jmap command. Exception: %s",
                    e.getMessage());
        }
    }
}
