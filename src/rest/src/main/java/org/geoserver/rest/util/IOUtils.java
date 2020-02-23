/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2015 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.URLs;

/**
 * Assorted IO related utilities
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class IOUtils extends org.apache.commons.io.IOUtils {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(FileCleaner.class);

    /** Default size of element for {@link FileChannel} based copy method. */
    private static final int DEFAULT_SIZE = 10 * 1024 * 1024;

    /** Background to perform file deletions. */
    private static final FileCleaner FILE_CLEANER = new FileCleaner();

    private static final Set<String> FILES_PATH =
            Collections.synchronizedSet(new HashSet<String>());
    private static final Map<String, Integer> FILE_ATTEMPTS_COUNTS =
            Collections.synchronizedMap(new HashMap<String, Integer>());

    /** 30 seconds is the default period beteen two checks. */
    private static long DEFAULT_PERIOD = 5L;

    /** The default number of attempts is 50 */
    private static final int DEF_MAX_ATTEMPTS = 50;

    static {
        FILE_CLEANER.setMaxAttempts(100);
        FILE_CLEANER.setPeriod(30);
        FILE_CLEANER.setPriority(1);
        FILE_CLEANER.start();
    }

    /**
     * Simple class implementing a periodic Thread that periodically tries to delete the files that
     * were provided to him.
     *
     * <p>It tries to delete each file at most {@link FileCleaner#maxAttempts} number of times. If
     * this number is exceeded it simply throws the file away notifying the users with a warning
     * message.
     *
     * @author Simone Giannecchini, GeoSolutions.
     */
    public static final class FileCleaner extends Thread {

        /**
         * Maximum number of attempts to delete a given {@link File}.
         *
         * <p>If the provided number of attempts is exceeded we simply drop warn the user and we
         * remove the {@link File} from our list.
         */
        private int maxAttempts = DEF_MAX_ATTEMPTS;

        /** Period in seconds between two checks. */
        private volatile long period = DEFAULT_PERIOD;

        /**
         * Asks this {@link FileCleaner} to clean up this file.
         *
         * @param fileToDelete {@link File} that we want to permanently delete.
         */
        public void addFile(final File fileToDelete) {
            // does it exists
            if (!fileToDelete.exists()) return;
            synchronized (FILES_PATH) {
                synchronized (FILE_ATTEMPTS_COUNTS) {
                    // /////////////////////////////////////////////////////////////////
                    //
                    // We add the file to our lists for later check.
                    //
                    // /////////////////////////////////////////////////////////////////
                    if (!FILES_PATH.contains(fileToDelete.getAbsolutePath())) {
                        FILES_PATH.add(fileToDelete.getAbsolutePath());
                        FILE_ATTEMPTS_COUNTS.put(fileToDelete.getAbsolutePath(), 0);
                    }
                }
            }
        }

        /** Default constructor for a {@link FileCleaner}. */
        public FileCleaner() {
            this(DEFAULT_PERIOD, Thread.NORM_PRIORITY - 3, DEF_MAX_ATTEMPTS);
        }

        /**
         * Constructor for a {@link FileCleaner}.
         *
         * @param period default time period between two cycles.
         * @param priority is the priority for the cleaner thread.
         * @param maxattempts maximum number of time the cleaner thread tries to delete a file.
         */
        public FileCleaner(long period, int priority, int maxattempts) {
            this.period = period;
            this.setName("FileCleaner");
            this.setPriority(priority);
            this.setDaemon(true);
            this.maxAttempts = maxattempts;
        }

        /**
         * This method does the magic:
         *
         * <ol>
         *   <li>iterate over all the files
         *   <li>try to delete it
         *   <li>if successful drop the file references
         *   <li>if not successful increase the attempts count for the file and call the gc. If the
         *       maximum number was exceeded drop the file and warn the user
         */
        public void run() {
            while (true) {
                try {
                    synchronized (FILES_PATH) {
                        synchronized (FILE_ATTEMPTS_COUNTS) {
                            final Iterator<String> it = FILES_PATH.iterator();
                            while (it.hasNext()) {

                                // get next file path and its count
                                final String sFile = it.next();
                                if (LOGGER.isLoggable(Level.INFO))
                                    LOGGER.info("Trying to remove file " + sFile);
                                int attempts = FILE_ATTEMPTS_COUNTS.get(sFile);
                                if (!new File(sFile).exists()) {
                                    it.remove();
                                    FILE_ATTEMPTS_COUNTS.remove(sFile);
                                } else {
                                    // try to delete it
                                    if (new File(sFile).delete()) {
                                        if (LOGGER.isLoggable(Level.INFO))
                                            LOGGER.info("Successfully removed file " + sFile);
                                        it.remove();
                                        FILE_ATTEMPTS_COUNTS.remove(sFile);
                                    } else {
                                        if (LOGGER.isLoggable(Level.INFO))
                                            LOGGER.info("Unable to  remove file " + sFile);
                                        attempts++;
                                        if (maxAttempts < attempts) {
                                            if (LOGGER.isLoggable(Level.INFO))
                                                LOGGER.info("Dropping file " + sFile);
                                            it.remove();
                                            FILE_ATTEMPTS_COUNTS.remove(sFile);
                                            if (LOGGER.isLoggable(Level.WARNING))
                                                LOGGER.warning("Unable to delete file " + sFile);
                                        } else {
                                            FILE_ATTEMPTS_COUNTS.remove(sFile);
                                            FILE_ATTEMPTS_COUNTS.put(sFile, attempts);
                                            // might help, see
                                            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
                                            Runtime.getRuntime().gc();
                                            Runtime.getRuntime().gc();
                                            Runtime.getRuntime().gc();
                                            Runtime.getRuntime().gc();
                                            Runtime.getRuntime().gc();
                                            Runtime.getRuntime().gc();
                                            System.runFinalization();
                                            System.runFinalization();
                                            System.runFinalization();
                                            System.runFinalization();
                                            System.runFinalization();
                                            System.runFinalization();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Thread.sleep(period * 1000);

                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
                }
            }
        }

        /**
         * Retrieves the maximum number of times we try to delete a file before giving up.
         *
         * @return the maximum number of times we try to delete a file before giving up.
         */
        public int getMaxAttempts() {
            synchronized (FILES_PATH) {
                synchronized (FILE_ATTEMPTS_COUNTS) {
                    return maxAttempts;
                }
            }
        }

        /**
         * Sets the maximum number of times we try to delete a file before giving up.
         *
         * @param maxAttempts the maximum number of times we try to delete a file before giving up.
         */
        public void setMaxAttempts(int maxAttempts) {
            synchronized (FILES_PATH) {
                synchronized (FILE_ATTEMPTS_COUNTS) {
                    this.maxAttempts = maxAttempts;
                }
            }
        }

        /**
         * Retrieves the period in seconds for this {@link FileCleaner} .
         *
         * @return the period in seconds for this {@link FileCleaner} .
         */
        public long getPeriod() {
            return period;
        }

        /**
         * Sets the period in seconds for this {@link FileCleaner} .
         *
         * @param period the new period for this {@link FileCleaner} .
         */
        public void setPeriod(long period) {
            this.period = period;
        }
    }

    /**
     * Copies the content of the source channel onto the destination channel.
     *
     * @param bufferSize size of the temp buffer to use for this copy.
     * @param source the source {@link ReadableByteChannel}.
     * @param destination the destination {@link WritableByteChannel};.
     * @throws IOException in case something bad happens.
     */
    public static void copyChannel(
            int bufferSize, ReadableByteChannel source, WritableByteChannel destination)
            throws IOException {

        inputNotNull(source, destination);
        if (!source.isOpen() || !destination.isOpen())
            throw new IllegalStateException("Source and destination channels must be open.");

        final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(bufferSize);
        while (source.read(buffer) != -1) {
            // prepare the buffer for draining
            buffer.flip();

            // write to destination
            while (buffer.hasRemaining()) destination.write(buffer);

            // clear
            buffer.clear();
        }
    }

    /**
     * Optimize version of copy method for file channels.
     *
     * @param bufferSize size of the temp buffer to use for this copy.
     * @param source the source {@link ReadableByteChannel}.
     * @param destination the destination {@link WritableByteChannel};.
     * @throws IOException in case something bad happens.
     */
    public static void copyFileChannel(int bufferSize, FileChannel source, FileChannel destination)
            throws IOException {

        inputNotNull(source, destination);
        if (!source.isOpen() || !destination.isOpen())
            throw new IllegalStateException("Source and destination channels must be open.");
        FileLock lock = null;
        try {

            lock = destination.lock();
            final long sourceSize = source.size();
            long pos = 0;
            while (pos < sourceSize) {
                // read and flip
                final long remaining = (sourceSize - pos);
                final int mappedZoneSize = remaining >= bufferSize ? bufferSize : (int) remaining;
                destination.transferFrom(source, pos, mappedZoneSize);
                // update zone
                pos += mappedZoneSize;
            }
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
                }
            }
        }
    }

    /**
     * Close the specified input <code>FileChannel</code>
     *
     * @throws IOException in case something bad happens.
     */
    public static void closeQuietly(Channel channel) throws IOException {
        inputNotNull(channel);
        if (channel.isOpen()) channel.close();
    }

    /**
     * Checks if the input is not null.
     *
     * @param oList list of elements to check for null.
     */
    private static void inputNotNull(Object... oList) {
        for (Object o : oList)
            if (o == null) throw new NullPointerException("Input objects cannot be null");
    }

    /**
     * Copy the input file onto the output file using a default buffer size.
     *
     * @param sourceFile the {@link File} to copy from.
     * @param destinationFile the {@link File} to copy to.
     * @throws IOException in case something bad happens.
     */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        copyFile(sourceFile, destinationFile, DEFAULT_SIZE);
    }

    /**
     * Copies the content of the source channel onto the destination file.
     *
     * @param bufferSize size of the temp buffer to use for this copy.
     * @param source the source {@link ReadableByteChannel}.
     * @param destination the {@link FileChannel} to copy to.
     * @param initialWritePosition position of destination file to start appends source bytes.
     * @return total bytes written
     * @throws IOException in case something bad happens.
     */
    public static Long copyToFileChannel(
            int bufferSize,
            ReadableByteChannel source,
            FileChannel destination,
            Long initialWritePosition)
            throws IOException {
        Long writedByte = 0L;
        inputNotNull(source, destination);
        if (!source.isOpen() || !destination.isOpen())
            throw new IllegalStateException("Source and destination channels must be open.");

        final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(bufferSize);
        FileLock lock = null;
        try {
            lock = destination.lock();

            // Move destination to position
            destination.position(initialWritePosition);

            while (source.read(buffer) != -1) {
                // prepare the buffer for draining
                buffer.flip();
                // write to destination
                while (buffer.hasRemaining()) writedByte = writedByte + destination.write(buffer);
                // clear
                buffer.clear();
            }
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
                }
            }
        }
        return writedByte;
    }

    /**
     * Copy the input file onto the output file using the specified buffer size.
     *
     * @param sourceFile the {@link File} to copy from.
     * @param destinationFile the {@link File} to copy to.
     * @param size buffer size.
     * @throws IOException in case something bad happens.
     */
    public static void copyFile(File sourceFile, File destinationFile, int size)
            throws IOException {
        inputNotNull(sourceFile, destinationFile);
        if (!sourceFile.exists() || !sourceFile.canRead() || !sourceFile.isFile())
            throw new IllegalStateException("Source is not in a legal state.");
        if (!destinationFile.exists()) {
            destinationFile.createNewFile();
        }
        if (destinationFile.getAbsolutePath().equalsIgnoreCase(sourceFile.getAbsolutePath()))
            throw new IllegalArgumentException("Cannot copy a file on itself");

        FileChannel source;
        FileChannel destination;
        source = new RandomAccessFile(sourceFile, "r").getChannel();
        destination = new RandomAccessFile(destinationFile, "rw").getChannel();
        try {
            copyFileChannel(size, source, destination);
        } finally {
            try {
                if (source != null) {
                    try {
                        source.close();
                    } catch (Throwable t) {
                        if (LOGGER.isLoggable(Level.INFO))
                            LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
                    }
                }
            } finally {
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (Throwable t) {
                        if (LOGGER.isLoggable(Level.INFO))
                            LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * Delete all the files with matching the specified {@link FilenameFilter} in the specified
     * directory. The method can work recursively.
     *
     * @param sourceDirectory the directory to delete files from.
     * @param filter the {@link FilenameFilter} to use for selecting files to delete.
     * @param recursive boolean that specifies if we want to delete files recursively or not.
     */
    public static boolean deleteDirectory(
            File sourceDirectory, FilenameFilter filter, boolean recursive, boolean deleteItself) {
        inputNotNull(sourceDirectory, filter);
        if (!sourceDirectory.exists()
                || !sourceDirectory.canRead()
                || !sourceDirectory.isDirectory())
            throw new IllegalStateException("Source is not in a legal state.");

        final File[] files =
                (filter != null ? sourceDirectory.listFiles(filter) : sourceDirectory.listFiles());
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive) {
                        deleteDirectory(file, filter, recursive, deleteItself);
                    }
                } else {
                    if (!file.delete()) return false;
                }
            }
        }
        return !deleteItself || sourceDirectory.delete();
    }

    /**
     * Delete the specified File.
     *
     * @param file the file to delete
     */
    public static void deleteFile(File file) {
        inputNotNull(file);
        if (!file.exists() || !file.canRead() || !file.isFile())
            throw new IllegalStateException("Source is not in a legal state.");

        if (file.delete()) return;

        IOUtils.FILE_CLEANER.addFile(file);
    }

    /**
     * Get an input <code>FileChannel</code> for the provided <code>File</code>
     *
     * @param source <code>File</code> for which we need to get an input <code>FileChannel</code>
     * @return a <code>FileChannel</code>
     * @throws IOException in case something bad happens.
     */
    public static FileChannel getInputChannel(File source) {
        inputNotNull(source);
        if (!source.exists() || !source.canRead() || !source.isDirectory())
            throw new IllegalStateException("Source is not in a legal state.");
        FileChannel channel = null;
        while (channel == null) {
            try {
                channel = new FileInputStream(source).getChannel();
            } catch (Exception e) {
                channel = null;
            }
        }
        return channel;
    }

    /**
     * Move the specified input file to the specified destination directory.
     *
     * @param source the input <code>File</code> which need to be moved.
     * @param destDir the destination directory where to move the file.
     */
    public static void moveFileTo(File source, File destDir, boolean removeInputFile)
            throws IOException {
        inputNotNull(source, destDir);
        if (!source.exists() || !source.canRead() || source.isDirectory())
            throw new IllegalStateException("Source is not in a legal state.");
        if (!destDir.exists() || !destDir.canWrite() || !destDir.isDirectory())
            throw new IllegalStateException("Source is not in a legal state.");
        if (destDir.getAbsolutePath().equalsIgnoreCase(source.getParentFile().getAbsolutePath()))
            return;
        // ///////////////////////////////////////////////////////////////
        //
        // Copy the inputFile in the specified destination directory
        //
        // ///////////////////////////////////////////////////////////////
        copyFile(source, new File(destDir, source.getName()));

        // ///////////////////////////////////////////////////////////////
        //
        // Delete the source file.
        //
        // ///////////////////////////////////////////////////////////////
        // we need to call the gc, see
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
        if (removeInputFile) FILE_CLEANER.addFile(source);
    }

    /**
     * Tries to convert a {@link URL} into a {@link File}. Return null if something bad happens
     *
     * @param fileURL {@link URL} to be converted into a {@link File}.
     * @return {@link File} for this {@link URL} or null.
     */
    public static File URLToFile(URL fileURL) {
        inputNotNull(fileURL);
        try {

            return URLs.urlToFile(fileURL);

        } catch (Throwable t) {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, t.getLocalizedMessage(), t);
        }
        return null;
    }

    /**
     * Copy {@link InputStream} to {@link OutputStream}.
     *
     * @param sourceStream {@link InputStream} to copy from.
     * @param destinationStream {@link OutputStream} to copy to.
     * @param closeInput quietly close {@link InputStream}.
     * @param closeOutput quietly close {@link OutputStream}
     * @throws IOException in case something bad happens.
     */
    public static void copyStream(
            InputStream sourceStream,
            OutputStream destinationStream,
            boolean closeInput,
            boolean closeOutput)
            throws IOException {
        copyStream(sourceStream, destinationStream, DEFAULT_SIZE, closeInput, closeOutput);
    }

    /**
     * Copy {@link InputStream} to {@link OutputStream}.
     *
     * @param sourceStream {@link InputStream} to copy from.
     * @param destinationStream {@link OutputStream} to copy to.
     * @param size size of the buffer to use internally.
     * @param closeInput quietly close {@link InputStream}.
     * @param closeOutput quietly close {@link OutputStream}
     * @throws IOException in case something bad happens.
     */
    public static void copyStream(
            InputStream sourceStream,
            OutputStream destinationStream,
            int size,
            boolean closeInput,
            boolean closeOutput)
            throws IOException {

        inputNotNull(sourceStream, destinationStream);
        byte[] buf = new byte[size];
        int n;
        try {
            while (-1 != (n = sourceStream.read(buf))) {
                destinationStream.write(buf, 0, n);
                destinationStream.flush();
            }
        } finally {
            // closing streams and connections
            try {
                destinationStream.flush();
            } finally {
                try {
                    if (closeOutput) destinationStream.close();
                } finally {
                    if (closeInput) sourceStream.close();
                }
            }
        }
    }

    /**
     * Convert the input from the provided {@link InputStream} into a {@link String}.
     *
     * @param inputStream the {@link InputStream} to copy from.
     * @return a {@link String} that contains the content of the provided {@link InputStream}.
     * @throws IOException in case something bad happens.
     */
    public static String getStringFromStream(InputStream inputStream) throws IOException {
        inputNotNull(inputStream);
        final Reader inReq = new InputStreamReader(inputStream);
        return getStringFromReader(inReq);
    }

    /**
     * Convert the input from the provided {@link Reader} into a {@link String}.
     *
     * @param inputReader the {@link Reader} to copy from.
     * @return a {@link String} that contains the content of the provided {@link Reader}.
     * @throws IOException in case something bad happens.
     */
    public static String getStringFromReader(final Reader inputReader) throws IOException {
        inputNotNull(inputReader);
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        int len;
        while ((len = inputReader.read(buffer)) >= 0) {
            char[] read = new char[len];
            System.arraycopy(buffer, 0, read, 0, len);
            sb.append(read);
        }
        return sb.toString();
    }

    /**
     * Convert the input from the provided {@link Reader} into a {@link String}.
     *
     * @param src the {@link StreamSource} to copy from.
     * @return a {@link String} that contains the content of the provided {@link Reader}.
     * @throws IOException in case something bad happens.
     */
    public static String getStringFromStreamSource(StreamSource src) throws IOException {

        inputNotNull(src);
        InputStream inputStream = src.getInputStream();
        if (inputStream != null) {
            return getStringFromStream(inputStream);
        } else {

            final Reader r = src.getReader();
            return getStringFromReader(r);
        }
    }

    /**
     * Inflate the provided {@link ZipFile} in the provided output directory.
     *
     * @param archive the {@link ZipFile} to inflate.
     * @param outputDirectory the directory where to inflate the archive.
     * @param fileName name of the file if present.
     * @throws IOException in case something bad happens.
     */
    public static void inflate(ZipFile archive, Resource outputDirectory, String fileName)
            throws IOException {
        inflate(archive, outputDirectory, fileName, null, null, null, false, false);
    }

    /**
     * Inflate the provided {@link ZipFile} in the provided output directory.
     *
     * @param archive the {@link ZipFile} to inflate.
     * @param outputDirectory the directory where to inflate the archive.
     * @param fileName name of the file if present.
     * @throws IOException in case something bad happens.
     */
    public static void inflate(
            ZipFile archive, Resource outputDirectory, String fileName, boolean external)
            throws IOException {
        inflate(archive, outputDirectory, fileName, null, null, null, external, false);
    }

    /**
     * Inflate the provided {@link ZipFile} in the provided output directory.
     *
     * @param archive the {@link ZipFile} to inflate.
     * @param outputDirectory the directory where to inflate the archive.
     * @param fileName name of the file if present.
     * @param files empty list of the extracted files (or null if there is no desire to collect the
     *     list)
     * @throws IOException in case something bad happens.
     */
    public static void inflate(
            ZipFile archive,
            Resource outputDirectory,
            String fileName,
            String workspace,
            String store,
            List<Resource> files,
            boolean external)
            throws IOException {
        inflate(archive, outputDirectory, fileName, null, null, files, external, true);
    }

    /**
     * Inflate the provided {@link ZipFile} in the provided output directory.
     *
     * @param archive the {@link ZipFile} to inflate.
     * @param outputDirectory the directory where to inflate the archive.
     * @param fileName name of the file if present.
     * @param saveFile boolean to specify to save or not the list of the extracted files
     * @param files empty list of the extracted files (or null if there is no desire to collect the
     *     list)
     * @throws IOException in case something bad happens.
     */
    public static void inflate(
            ZipFile archive,
            Resource outputDirectory,
            String fileName,
            String workspace,
            String store,
            List<Resource> files,
            boolean external,
            boolean saveFile)
            throws IOException {

        final Enumeration<? extends ZipEntry> entries = archive.entries();
        try {
            File destDir = outputDirectory.dir();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Verify that the file will not be written outside of the target directory
                org.geoserver.util.IOUtils.getZipOutputFile(destDir, entry);
                if (!entry.isDirectory()) {
                    final String name = entry.getName();
                    final String ext = FilenameUtils.getExtension(name);
                    final InputStream in = new BufferedInputStream(archive.getInputStream(entry));
                    // Builder associated to the path for the item
                    StringBuilder itemPath =
                            fileName != null
                                    ? new StringBuilder(fileName).append(".").append(ext)
                                    : new StringBuilder(name);
                    // String associated to the filename
                    String initialFileName =
                            fileName != null ? fileName + "." + ext : FilenameUtils.getName(name);
                    // If the RESTUploadPathMapper are present then the output file position is
                    // changed
                    if (!external) {
                        Map<String, String> storeParams = new HashMap<>();
                        RESTUtils.remapping(
                                workspace, store, itemPath, initialFileName, storeParams);
                    }

                    final Resource outFile = outputDirectory.get(itemPath.toString());
                    final OutputStream out = new BufferedOutputStream(outFile.out());

                    IOUtils.copyStream(in, out, true, true);
                    // If the file must be listed, then the file is added to the list
                    if (saveFile && files != null) {
                        files.add(outFile);
                    }
                }
            }
        } finally {
            try {
                archive.close();
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.isLoggable(Level.FINE);
            }
        }
    }

    /** Singleton */
    private IOUtils() {}
}
