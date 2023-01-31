/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

import java.util.List;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Retrieve real system information metrics defined in {@link MetricInfo} from a collector thread.
 * The collector thread is started and stopped according to the statistics status.
 *
 * <p>This class is located as a singleton bean by a &lt;context:component-scan
 * base-package="org.geoserver.system.status"/&lgt;
 *
 * @author d.stueken
 * @author sandr
 */
@Component
public class OSHISystemInfoMonitor extends BaseSystemInfoCollector implements DisposableBean {

    private static final long serialVersionUID = 502867203324474735L;

    public static String NAME = OSHISystemInfoCollector.class.getSimpleName();

    // collector thread is started on demand.
    private transient OSHISystemInfoCollector collector;

    public OSHISystemInfoMonitor() {
        if (getStatisticsStatus() == ENABLED) {
            // initial startup (currently false by default)
            collector = start();
        }
    }

    /**
     * Delegate to the collector thread.
     *
     * @param info the element to retrieve
     * @return the values of the MetricInfo element.
     */
    @Override
    List<MetricValue> retrieveSystemInfo(MetricInfo info) {
        if (getStatisticsStatus() == ENABLED) {
            return getCollector().retrieveSystemInfo(info);
        } else {
            // this returns an empty list of values.
            return super.retrieveSystemInfo(info);
        }
    }

    /**
     * Get the current collector thread or start a new one.
     *
     * @return the started collector thread.
     */
    private OSHISystemInfoCollector getCollector() {
        try {
            // defensive copy
            OSHISystemInfoCollector thread = collector;
            if (thread == null || !thread.isAlive()) {
                thread = start();
                collector = thread;
            }
            return thread;
        } catch (Throwable e) {
            // in case of an error, collection is turned off.
            collector = null;
            super.setStatisticsStatus(DISABLED);
            throw e;
        }
    }

    /**
     * Create and start a collector thread.
     *
     * @return a running OSHISystemInfoCollector.
     */
    private static OSHISystemInfoCollector start() {
        OSHISystemInfoCollector collector = new OSHISystemInfoCollector();
        // easier to locate on debugger.
        collector.setName(NAME);
        // system shutdown is not blocked by this thread.
        collector.setDaemon(true);
        collector.start();
        return collector;
    }

    /**
     * This is merely used by unit test only.
     *
     * @return if the thread is running.
     */
    public boolean isRunning() {
        return collector != null;
    }

    /** Stop any running collector. */
    void stop() {
        // defensive copy
        final Thread thread = collector;
        if (thread != null) {
            collector = null;
            // Interrupt the thread, no need to wait for termination.
            thread.interrupt();
        }
    }

    /**
     * Enable or disable the collector. This also starts or stops the collector thread.
     *
     * @param status if statistics shall be collected.
     */
    @Override
    public void setStatisticsStatus(Boolean status) {
        if (status == ENABLED) {
            getCollector();
        } else {
            stop();
        }
        // in case of an error the state is left unchanged
        super.setStatisticsStatus(status);
    }

    /** Shutdown the collector thread if the bean gets disposed. */
    @Override
    public void destroy() {
        stop();
    }
}
