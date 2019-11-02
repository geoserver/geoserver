/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.CountingOutputStream;

/**
 * Runs a generic executable with the given options
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractCommandLineTransform extends AbstractTransform {

    private static final long serialVersionUID = 5998049960852782644L;

    static final long DEFAULT_TIMEOUT = 60 * 60 * 1000; // one hour

    List<String> options;

    public AbstractCommandLineTransform(List<String> options) {
        this.options = Optional.ofNullable(options).orElseGet(() -> new ArrayList<>());
    }

    /** @return the options */
    public List<String> getOptions() {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options;
    }

    /** @param options the options to set */
    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Override
    public boolean stopOnError(Exception e) {
        return true;
    }

    protected void execute(CommandLine cmd, File workingDiretory) throws IOException {
        // prepare to run
        DefaultExecutor executor = new DefaultExecutor();
        // make sure we don't try to execute for too much time
        executor.setWatchdog(new ExecuteWatchdog(DEFAULT_TIMEOUT));
        if (workingDiretory != null) {
            executor.setWorkingDirectory(workingDiretory);
        }

        // grab at least some part of the outputs
        int limit = 16 * 1024;
        try (OutputStream os = new BoundedOutputStream(new ByteArrayOutputStream(), limit);
                OutputStream es = new BoundedOutputStream(new ByteArrayOutputStream(), limit)) {
            PumpStreamHandler streamHandler = new PumpStreamHandler(os, es);
            executor.setStreamHandler(streamHandler);
            try {
                int result = executor.execute(cmd);

                if (executor.isFailure(result)) {
                    // toString call is routed to ByteArrayOutputStream, which does the right string
                    // conversion
                    throw new IOException(
                            "Failed to execute command "
                                    + cmd.toString()
                                    + "\nStandard output is:\n"
                                    + os.toString()
                                    + "\nStandard error is:\n"
                                    + es.toString());
                }
            } catch (Exception e) {
                throw new IOException(
                        "Failure to execute command "
                                + cmd.toString()
                                + "\nStandard output is:\n"
                                + os.toString()
                                + "\nStandard error is:\n"
                                + es.toString(),
                        e);
            }
        }
    }

    /**
     * Output stream wrapper with a soft limit
     *
     * @author Andrea Aime - GeoSolutions
     */
    static final class BoundedOutputStream extends CountingOutputStream {

        private long maxSize;

        private OutputStream delegate;

        public BoundedOutputStream(OutputStream delegate, long maxSize) {
            super(delegate);
            this.delegate = delegate;
            this.maxSize = maxSize;
        }

        @Override
        public void write(byte[] bts) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(bts);
        }

        @Override
        public void write(byte[] bts, int st, int end) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(bts, st, end);
        }

        @Override
        public void write(int idx) throws IOException {
            if (getByteCount() > maxSize) {
                return;
            }
            super.write(idx);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
