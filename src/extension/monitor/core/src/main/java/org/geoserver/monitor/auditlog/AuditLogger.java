/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.auditlog;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.makeFileOnly;
import static org.apache.commons.io.filefilter.FileFilterUtils.prefixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.template.TemplateUtils;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Writes all requests to a log file. The log file can be configured in the MonitorConfig, as well
 * as a Freemarker template to drive its contents
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AuditLogger implements RequestDataListener, ApplicationListener<ApplicationEvent> {

    static final String AUDIT = "audit";

    private static final Logger LOGGER = Logging.getLogger(MemoryMonitorDAO.class);

    private static final RequestData END_MARKER = new RequestData();

    public static final int DEFAULT_ROLLING_LIMIT = 10000;

    Configuration templateConfig;

    MonitorConfig config;

    volatile RequestDumper dumper;

    int rollLimit;

    String path;

    String headerTemplate;

    String contentTemplate;

    String footerTemplate;

    public AuditLogger(MonitorConfig config, GeoServerResourceLoader loader) throws IOException {
        this.config = config;
        templateConfig = TemplateUtils.getSafeConfiguration();
        templateConfig.setTemplateLoader(new AuditTemplateLoader(loader));
    }

    synchronized void initDumper() throws IOException {
        if (this.dumper == null && getProperty("enabled", Boolean.class, false)) {
            // prepare the config
            rollLimit = getProperty("roll_limit", Integer.class, DEFAULT_ROLLING_LIMIT);
            path = System.getProperty("GEOSERVER_AUDIT_PATH");
            if (path == null || "".equals(path.trim())) {
                path = config.getProperty(AUDIT, "path", String.class);
            }
            headerTemplate = getProperty("ftl.header", String.class, null);
            contentTemplate = getProperty("ftl.content", String.class, null);
            footerTemplate = getProperty("ftl.footer", String.class, null);

            // check the path
            Resource loggingDir = Resources.fromPath(path);

            path = config.getProperty(AUDIT, "path", String.class);

            // setup the dumper
            this.dumper =
                    new RequestDumper(
                            loggingDir.dir(),
                            rollLimit,
                            headerTemplate,
                            contentTemplate,
                            footerTemplate);
        }
    }

    <T> T getProperty(String name, Class<T> target, T defaultValue) {
        T value = config.getProperty(AUDIT, name, target);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    @Override
    public void requestStarted(RequestData rd) {
        // nothing to do
    }

    @Override
    public void requestUpdated(RequestData rd) {
        // nothing to do
    }

    @Override
    public void requestCompleted(RequestData rd) {
        // nothing to do
    }

    @Override
    public void requestPostProcessed(RequestData rd) {
        if (rd == null) {
            return;
        }

        try {
            if (dumper == null) {
                // first request eh?
                initDumper();
            } else {
                // not first, check the config did not change, if so, reinstantiate the dumper
                boolean enabled = getProperty("enabled", Boolean.class, false);
                if (!enabled) {
                    closeDumper(dumper);
                    dumper = null;
                } else {
                    int newLimit = getProperty("roll_limit", Integer.class, DEFAULT_ROLLING_LIMIT);
                    String newPath = getProperty("path", String.class, null);
                    String newHeaderTemplate = getProperty("ftl.header", String.class, null);
                    String newContentTemplate = getProperty("ftl.content", String.class, null);
                    String newFooterTemplate = getProperty("ftl.footer", String.class, null);
                    // the comparison of newTemplateName using != is intended, works fine with nulls
                    // and the strings we get do not change unless the property file has been
                    // reloaded. We also rework if the dumper died for some reason (e.g., improper
                    // config, invalid templates)
                    if (newLimit != rollLimit
                            || !Objects.equals(newPath, path)
                            || !Objects.equals(newHeaderTemplate, headerTemplate)
                            || !Objects.equals(newContentTemplate, contentTemplate)
                            || !Objects.equals(newFooterTemplate, footerTemplate)
                            || !dumper.isAlive()) {
                        // config changed, close the current dumper and create a new one
                        closeDumper(dumper);
                        dumper = null;
                        initDumper();
                    }
                }
            }

            // if we have a dumper, add in the logging queue
            if (dumper != null) {
                if (!dumper.queue.offer(rd)) {
                    LOGGER.log(
                            Level.WARNING,
                            "Auditing subsystem overload, the logging queue is full, stopping the world on it");
                    dumper.queue.put(rd);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected error occurred while trying to "
                            + "store the request data in the logger queue",
                    e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            closeDumper(dumper);
        }
    }

    private void closeDumper(RequestDumper dumper) {
        if (dumper != null) {
            dumper.exit();
            try {
                dumper.join(5000);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Failed to properly close the event dumper", e);
            }
        }
    }

    private final class RequestDumper extends Thread {

        private long lineCounter = 0;

        private long fileRollCounter = 0;

        /**
         * We use a {@link BlockingQueue} to decouple to incoming flux of {@link RequestData} to
         * audit with the thread that writes to disk.
         */
        BlockingQueue<RequestData> queue = new ArrayBlockingQueue<RequestData>(10000);

        /** The {@link File} where we audit to. */
        private File logFile;

        private File path;

        private int day = -1;

        private int lineRollingLimit;

        private String headerTemplate;

        private String contentTemplate;

        private String footerTemplate;

        /**
         * Constructs and starts a new thread as a daemon. This thread will be sleeping most of the
         * time. It will run only some few nanoseconds each time a new {@link RequestData} is
         * enqueded.
         */
        private RequestDumper(
                final File path,
                final int lineRollingLimit,
                String headerTemplate,
                String contentTemplate,
                String footerTemplate) {
            super("RequestDumper");

            // save path to use
            this.path = path;
            this.lineRollingLimit = lineRollingLimit;
            this.headerTemplate = headerTemplate == null ? "header.ftl" : headerTemplate;
            this.contentTemplate = contentTemplate == null ? "content.ftl" : contentTemplate;
            this.footerTemplate = contentTemplate == null ? "footer.ftl" : footerTemplate;
            setPriority(NORM_PRIORITY - 1);
            setDaemon(true);
            start();
        }

        /** Loop to be run during the virtual machine lifetime. */
        @Override
        public void run() {

            BufferedWriter writer = null;
            try {
                while (true) {
                    // grab as many items from the queue as possible
                    List<RequestData> rds = new ArrayList<RequestData>();
                    if (queue.size() > 0) {
                        queue.drainTo(rds);
                    } else {
                        rds.add(queue.take());
                    }

                    // roll the writer if necessary
                    writer = rollWriter(writer);

                    // get the template
                    Template template = templateConfig.getTemplate(contentTemplate);

                    // write out each of the request data
                    for (RequestData rd : rds) {
                        if (rd == END_MARKER) {
                            return;
                        }

                        template.process(rd, writer);
                        this.lineCounter++;
                    }

                    // flush the writer so that the file is up to date, otherwise a request
                    // might keep in the buffer for hours under low traffic situations
                    try {
                        if (writer != null) {
                            writer.flush();
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                    }
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(
                            Level.WARNING,
                            "Request Dumper exiting due to :" + e.getLocalizedMessage(),
                            e);
            } finally {
                closeWriter(writer);
            }
            LOGGER.info("Request Dumper stopped");
        }

        /** Performs log-rolling if necessary */
        BufferedWriter rollWriter(BufferedWriter writer) throws Exception {
            // get date
            final GregorianCalendar current = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

            // check if we have to close the file and reopen it for rolling
            if (this.lineCounter >= lineRollingLimit
                    || (day > 0 && day != current.get(GregorianCalendar.DAY_OF_YEAR))
                    || (logFile != null && !logFile.exists())) {
                closeWriter(writer);

                // play with counters
                this.fileRollCounter++;
                this.lineCounter = 0;

                // clean
                writer = null;
            }

            // new start or rolling just happened?
            if (writer == null) {
                // create proper file to write to
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                final String auditFileName =
                        "geoserver_audit_" + dateFormat.format(current.getTime()) + "_";

                // look for similar files to pick up numbering
                if (fileRollCounter == 0) {
                    final String[] files =
                            path.list(
                                    makeFileOnly(
                                            and(
                                                    prefixFileFilter("geoserver_audit_"),
                                                    suffixFileFilter(".log"))));
                    if (files != null && files.length > 0) {
                        Arrays.sort(
                                files,
                                new Comparator<String>() {

                                    @Override
                                    public int compare(String o1, String o2) {
                                        // extract dates and compare
                                        final String[] o1s =
                                                o1.substring(0, o1.length() - 4).split("_");
                                        final String[] o2s =
                                                o2.substring(0, o2.length() - 4).split("_");
                                        int dateCompare;
                                        try {
                                            dateCompare =
                                                    dateFormat
                                                            .parse(o1s[2])
                                                            .compareTo(dateFormat.parse(o2s[2]));
                                        } catch (ParseException e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (dateCompare == 0) {
                                            // compare counter
                                            return Integer.valueOf(o1s[3])
                                                    .compareTo(Integer.valueOf(o2s[3]));

                                        } else return dateCompare;
                                    }
                                });
                        // get the max counter
                        final String target = files[files.length - 1];
                        int start = target.lastIndexOf("_") + 1;
                        int end = target.lastIndexOf(".");
                        fileRollCounter = Integer.parseInt(target.substring(start, end));
                        // move to the next one
                        fileRollCounter++;
                    }
                }

                // create file
                this.logFile = new File(path, auditFileName + fileRollCounter + ".log");
                if (!logFile.exists() && !this.logFile.createNewFile()) {
                    throw new IllegalStateException(
                            "Unable to create monitoring file:" + logFile.getCanonicalPath());
                }
                // save day
                day =
                        new GregorianCalendar(TimeZone.getTimeZone("GMT"))
                                .get(GregorianCalendar.DAY_OF_YEAR);

                // now the writer
                writer = new BufferedWriter(new FileWriter(logFile, true));
                Template template = templateConfig.getTemplate(headerTemplate);
                template.process(null, writer);
            }

            return writer;
        }

        private void closeWriter(BufferedWriter writer) {
            try {
                if (writer != null) {
                    Template template = templateConfig.getTemplate(footerTemplate);
                    template.process(null, writer);
                    writer.flush();
                }
            } catch (Exception e) {
                // eat me
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                // eat me
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
        }

        /**
         * Stops the cleaner thread. Calling this method is recommended in all long running
         * applications with custom class loaders (e.g., web applications).
         */
        @SuppressWarnings("deprecation")
        public void exit() {
            if (queue != null && isAlive()) {
                // try to stop it gracefully
                try {
                    queue.put(END_MARKER);
                    this.join(1000);
                } catch (InterruptedException e) {
                    // eat me
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }

                this.interrupt();
                try {
                    this.join(1000);
                } catch (InterruptedException e) {
                    // eat me
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }
                // last resort tentative to kill the cleaner thread
                if (this.isAlive()) this.stop();
            }
        }
    }
}
