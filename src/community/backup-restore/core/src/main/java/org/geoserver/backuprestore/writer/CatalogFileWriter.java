/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.config.util.XStreamPersister;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.batch.item.util.FileUtils;
import org.springframework.batch.support.transaction.TransactionAwareBufferedWriter;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Concrete Spring Batch {@link AbstractItemStreamItemWriter}.
 *
 * <p>Streams {@link Catalog} resource items to JSON via {@link XStreamPersister} on mass storage.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogFileWriter<T> extends CatalogWriter<T> {

    private static final boolean DEFAULT_TRANSACTIONAL = false;

    protected static final Log logger = LogFactory.getLog(CatalogFileWriter.class);

    private static final String WRITTEN_STATISTICS_NAME = "written";

    private static final String RESTART_DATA_NAME = "current.count";

    private Resource resource;

    private OutputState state = null;

    private boolean saveState = true;

    private boolean shouldDeleteIfExists = true;

    private boolean forceSync = false;

    private boolean transactional = DEFAULT_TRANSACTIONAL;

    private String encoding = OutputState.DEFAULT_CHARSET;

    private boolean append = false;

    public CatalogFileWriter(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    protected String getItemName(XStreamPersister xp) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }

    @Override
    public void write(List<? extends T> items) throws Exception {

        if (!getOutputState().isInitialized()) {
            throw new WriterNotOpenException("Writer must be open before it can be written to");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Writing to flat file with " + items.size() + " items.");
        }

        OutputState state = getOutputState();

        StringBuilder lines =
                new StringBuilder(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n");
        int lineCount = 0;

        if (items.size() > 0) {
            lines.append("<items>\n");
        }

        for (T item : items) {
            lines.append(doWrite(item));
            lineCount++;

            try {
                firePostWrite(item, resource);
            } catch (IOException e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new WriteFailedException(
                                "Could not write data.  The file may be corrupt.", e));
            }
        }

        if (items.size() > 0) {
            lines.append("</items>\n");
        }

        try {
            state.write(lines.toString());
        } catch (IOException e) {
            logValidationExceptions(
                    (ValidationResult) null,
                    new WriteFailedException("Could not write data.  The file may be corrupt.", e));
        }
        state.linesWritten += lineCount;
    }

    //
    protected String doWrite(T item) {
        // unwrap dynamic proxies
        item = (T) xstream.unwrapProxies(item);
        return getXp().toXML(item) + "\n";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (append) {
            shouldDeleteIfExists = false;
        }
    }

    /** Setter for resource. Represents a file that can be written. */
    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Set the flag indicating whether or not state should be saved in the provided {@link
     * ExecutionContext} during the {@link ItemStream} call to update. Setting this to false means
     * that it will always start at the beginning on a restart.
     */
    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    /**
     * Flag to indicate that the target file should be deleted if it already exists, otherwise it
     * will be created. Defaults to true, so no appending except on restart. If set to false and
     * {@link #setAppendAllowed(boolean) appendAllowed} is also false then there will be an
     * exception when the stream is opened to prevent existing data being potentially corrupted.
     *
     * @param shouldDeleteIfExists the flag value to set
     */
    public void setShouldDeleteIfExists(boolean shouldDeleteIfExists) {
        this.shouldDeleteIfExists = shouldDeleteIfExists;
    }

    /**
     * Flag to indicate that the target file should be appended if it already exists. If this flag
     * is set then the flag {@link #setShouldDeleteIfExists(boolean) shouldDeleteIfExists} is
     * automatically set to false, so that flag should not be set explicitly. Defaults value is
     * false.
     *
     * @param append the flag value to set
     */
    public void setAppendAllowed(boolean append) {
        this.append = append;
        // this.shouldDeleteIfExists = false;
    }

    /**
     * Flag to indicate that writing to the buffer should be delayed if a transaction is active.
     * Defaults to true.
     */
    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    /**
     * Initialize the reader. This method may be called multiple times before close is called.
     *
     * @see ItemStream#open(ExecutionContext)
     */
    @Override
    public void open(ExecutionContext executionContext) {
        super.open(executionContext);

        Assert.notNull(resource, "The resource must be set");

        if (!getOutputState().isInitialized()) {
            try {
                doOpen(executionContext);
            } catch (ItemStreamException e) {
                logValidationExceptions(
                        (T) null,
                        new WriteFailedException(
                                "Could not write data.  The file may be corrupt.", e));
            }
        }
    }

    private void doOpen(ExecutionContext executionContext) throws ItemStreamException {
        OutputState outputState = getOutputState();

        if (executionContext.containsKey(getExecutionContextKey(RESTART_DATA_NAME))) {
            outputState.restoreFrom(executionContext);
        }

        try {
            outputState.initializeBufferedWriter();
        } catch (IOException ioe) {
            throw new ItemStreamException("Failed to initialize writer", ioe);
        }
    }

    /** @see ItemStream#update(ExecutionContext) */
    @Override
    public void update(ExecutionContext executionContext) {
        super.update(executionContext);
        if (state == null) {
            throw new ItemStreamException("ItemStream not open or already closed.");
        }

        Assert.notNull(executionContext, "ExecutionContext must not be null");

        if (saveState) {
            try {
                executionContext.putLong(
                        getExecutionContextKey(RESTART_DATA_NAME), state.position());
            } catch (IOException e) {
                logValidationExceptions(
                        (T) null,
                        new ItemStreamException(
                                "ItemStream does not return current position properly", e));
            }

            executionContext.putLong(
                    getExecutionContextKey(WRITTEN_STATISTICS_NAME), state.linesWritten);
        }
    }

    /** @see ItemStream#close() */
    @Override
    public void close() {
        super.close();
        if (state != null) {
            state.close();
            state = null;
        }
    }

    // Returns object representing state.
    private OutputState getOutputState() {
        if (state == null) {
            File file;
            try {
                file = resource.getFile();
            } catch (IOException e) {
                throw new ItemStreamException(
                        "Could not convert resource to file: [" + resource + "]", e);
            }
            Assert.state(
                    !file.exists() || file.canWrite(),
                    "Resource is not writable: [" + resource + "]");
            state = new OutputState();
            state.setDeleteIfExists(shouldDeleteIfExists);
            state.setAppendAllowed(append);
            state.setEncoding(encoding);
        }
        return state;
    }

    /**
     * Encapsulates the runtime state of the writer. All state changing operations on the writer go
     * through this class.
     */
    private class OutputState {
        // default encoding for writing to output files - set to UTF-8.
        private static final String DEFAULT_CHARSET = "UTF-8";

        private FileOutputStream os;

        // The bufferedWriter over the file channel that is actually written
        Writer outputBufferedWriter;

        FileChannel fileChannel;

        // this represents the charset encoding (if any is needed) for the
        // output file
        String encoding = DEFAULT_CHARSET;

        boolean restarted = false;

        long lastMarkedByteOffsetPosition = 0;

        long linesWritten = 0;

        boolean shouldDeleteIfExists = true;

        boolean initialized = false;

        private boolean append = false;

        private boolean appending = false;

        /** Return the byte offset position of the cursor in the output file as a long integer. */
        public long position() throws IOException {
            long pos = 0;

            if (fileChannel == null) {
                return 0;
            }

            outputBufferedWriter.flush();
            pos = fileChannel.position();
            if (transactional) {
                pos += ((TransactionAwareBufferedWriter) outputBufferedWriter).getBufferSize();
            }

            return pos;
        }

        /** @param append */
        public void setAppendAllowed(boolean append) {
            this.append = append;
        }

        /** @param executionContext */
        public void restoreFrom(ExecutionContext executionContext) {
            lastMarkedByteOffsetPosition =
                    executionContext.getLong(getExecutionContextKey(RESTART_DATA_NAME));
            linesWritten =
                    executionContext.getLong(getExecutionContextKey(WRITTEN_STATISTICS_NAME));
            /*
             * if (shouldDeleteIfEmpty && linesWritten == 0) { // previous execution deleted the output file because no items were written restarted =
             * false; lastMarkedByteOffsetPosition = 0; } else { restarted = true; }
             */
            restarted = true;
        }

        /** @param shouldDeleteIfExists */
        public void setDeleteIfExists(boolean shouldDeleteIfExists) {
            this.shouldDeleteIfExists = shouldDeleteIfExists;
        }

        /** @param encoding */
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        /** Close the open resource and reset counters. */
        public void close() {
            initialized = false;
            restarted = false;
            try {
                if (outputBufferedWriter != null) {
                    outputBufferedWriter.close();
                }
            } catch (IOException ioe) {
                throw new ItemStreamException("Unable to close the the ItemWriter", ioe);
            } finally {
                if (!transactional) {
                    closeStream();
                }
            }
        }

        private void closeStream() {
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (IOException ioe) {
                throw new ItemStreamException("Unable to close the the ItemWriter", ioe);
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException ioe) {
                    throw new ItemStreamException("Unable to close the the ItemWriter", ioe);
                }
            }
        }

        /** */
        public void write(String line) throws IOException {
            if (!initialized) {
                initializeBufferedWriter();
            }

            outputBufferedWriter.write(line);
            outputBufferedWriter.flush();
        }

        /** Truncate the output at the last known good point. */
        public void truncate() throws IOException {
            fileChannel.truncate(lastMarkedByteOffsetPosition);
            fileChannel.position(lastMarkedByteOffsetPosition);
        }

        /**
         * Creates the buffered writer for the output file channel based on configuration
         * information.
         */
        private void initializeBufferedWriter() throws IOException {
            File file = resource.getFile();
            FileUtils.setUpOutputFile(file, restarted, append, shouldDeleteIfExists);

            os = new FileOutputStream(file.getAbsolutePath(), true);
            fileChannel = os.getChannel();

            outputBufferedWriter = getBufferedWriter(fileChannel, encoding);
            outputBufferedWriter.flush();

            if (append) {
                // Bug in IO library? This doesn't work...
                // lastMarkedByteOffsetPosition = fileChannel.position();
                if (file.length() > 0) {
                    appending = true;
                    // Don't write the headers again
                }
            }

            Assert.state(outputBufferedWriter != null);
            // in case of restarting reset position to last committed point
            if (restarted) {
                checkFileSize();
                truncate();
            }

            initialized = true;
        }

        public boolean isInitialized() {
            return initialized;
        }

        /**
         * Returns the buffered writer opened to the beginning of the file specified by the absolute
         * path name contained in absoluteFileName.
         */
        private Writer getBufferedWriter(FileChannel fileChannel, String encoding) {
            try {
                final FileChannel channel = fileChannel;
                if (transactional) {
                    TransactionAwareBufferedWriter writer =
                            new TransactionAwareBufferedWriter(
                                    channel,
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            closeStream();
                                        }
                                    });

                    writer.setEncoding(encoding);
                    writer.setForceSync(forceSync);
                    return writer;
                } else {
                    Writer writer =
                            new BufferedWriter(Channels.newWriter(fileChannel, encoding)) {

                                @Override
                                public void flush() throws IOException {
                                    super.flush();
                                    if (forceSync) {
                                        channel.force(false);
                                    }
                                }
                            };

                    return writer;
                }
            } catch (UnsupportedCharsetException ucse) {
                throw new ItemStreamException(
                        "Bad encoding configuration for output file " + fileChannel, ucse);
            }
        }

        /**
         * Checks (on setState) to make sure that the current output file's size is not smaller than
         * the last saved commit point. If it is, then the file has been damaged in some way and
         * whole task must be started over again from the beginning.
         *
         * @throws IOException if there is an IO problem
         */
        private void checkFileSize() throws IOException {
            long size = -1;

            outputBufferedWriter.flush();
            size = fileChannel.size();

            if (size < lastMarkedByteOffsetPosition) {
                throw new ItemStreamException(
                        "Current file size is smaller than size at last commit");
            }
        }
    }
}
