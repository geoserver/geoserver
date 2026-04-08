/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/** utility interface for thread and memory heap info */
public final class ConsoleInfoUtils {

    public static final int MAX_HISTO_LINES = 50;

    private ConsoleInfoUtils() {}

    /**
     * @param lockedMonitors From ThreadMXBean docs: if true, dump all locked monitors
     * @param lockedSynchronizers From ThreadMXBean docs: if true, dump all locked ownable synchronizers
     * @return large text with information about JVM threads
     */
    static String getThreadsInfo(boolean lockedMonitors, boolean lockedSynchronizers) {
        return Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(lockedMonitors, lockedSynchronizers))
                .map(t -> getThreadStackTraces(t))
                .collect(Collectors.joining("\n"));
    }

    static String getThreadStackTraces(ThreadInfo ti) {
        StringBuilder sb = new StringBuilder("\""
                + ti.getThreadName()
                + "\""
                + (ti.isDaemon() ? " daemon" : "")
                + " prio="
                + ti.getPriority()
                + " Id="
                + ti.getThreadId()
                + " "
                + ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on " + ti.getLockName());
        }
        if (ti.getLockOwnerName() != null) {
            sb.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        StackTraceElement[] stackTrace = ti.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement ste = stackTrace[i];
            sb.append("\tat " + formatStackTraceElement(ste));
            sb.append('\n');
            LockInfo lockInfo = ti.getLockInfo();
            if (i == 0 && lockInfo != null) {
                Thread.State ts = ti.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on " + lockInfo);
                        sb.append('\n');
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on " + lockInfo);
                        sb.append('\n');
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on " + lockInfo);
                        sb.append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : ti.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked " + mi);
                    sb.append('\n');
                }
            }
        }

        LockInfo[] locks = ti.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = " + locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- " + li);
                sb.append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    private static String formatStackTraceElement(StackTraceElement s) {
        return s.getClassName()
                + "."
                + s.getMethodName()
                + "("
                + (s.isNativeMethod()
                        ? "Native Method)"
                        : (s.getFileName() != null && s.getLineNumber() >= 0)
                                ? s.getFileName() + ":" + s.getLineNumber() + ")"
                                : "");
    }

    /**
     * To retrieve the PID of the Java process is used the method ManagementFactory.getRuntimeMXBean().getName() The
     * returned name usually is the format [pid]@[machinename] but depends on the implementation of the JVM
     *
     * @return the histo memory dump in the format produced by the jmap -histo:live
     */
    static String getHistoMemoryDump() {
        try {
            // 1. Get the Platform MBean Server
            MBeanServerConnection server = ManagementFactory.getPlatformMBeanServer();

            // 2. Identify the DiagnosticCommand MBean
            // The ObjectName for the DiagnosticCommand MBean is standard in HotSpot JVMs.
            ObjectName dcmdName = new ObjectName("com.sun.management:type=DiagnosticCommand");

            // 3. Define the command to execute
            String commandName = "gcClassHistogram";

            // Passing the -all=false argument to get only live objects
            Object[] params = {new String[] {"-all=false"}};
            String[] signature = {String[].class.getName()};

            // 4. Invoke the command and return
            return (String) server.invoke(dcmdName, commandName, params, signature);
        } catch (Exception e) {
            return "Error invoking DiagnosticCommand MBean: " + e.getMessage();
        }
    }
}
