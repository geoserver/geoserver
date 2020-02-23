/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.geotools.util.logging.Logging;

/**
 * Utility class for IO related utilities
 *
 * @author Andrea Aime - TOPP
 */
public class IOUtils {
    private static final Logger LOGGER = Logging.getLogger(IOUtils.class);

    protected IOUtils() {
        // singleton
    }

    /** Copies the provided input stream onto a file */
    public static void copy(InputStream from, File to) throws IOException {
        copy(from, new FileOutputStream(to));
    }

    /**
     * Copies the provided input stream onto an outputstream.
     *
     * <p>Please note that both from input stream and out output stream will be closed.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024 * 16];
            int bytes = 0;
            while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);

            out.flush();
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Copies from a file to another by performing a filtering on certain specified tokens. In
     * particular, each key in the filters map will be looked up in the reader as ${key} and
     * replaced with the associated value.
     */
    public static void filteredCopy(File from, File to, Map<String, String> filters)
            throws IOException {
        filteredCopy(new BufferedReader(new FileReader(from)), to, filters);
    }
    /**
     * Capture contents of {@link InputStream} as a String.
     *
     * @param input InputStream, closed after use.
     * @return contents of input
     */
    public static String toString(InputStream input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream output;
        try {
            output = new ByteArrayOutputStream();
            copy(input, output);
        } finally {
            input.close();
            input.close();
        }
        return output.toString();
    }

    /**
     * Copies from a reader to a file by performing a filtering on certain specified tokens. In
     * particular, each key in the filters map will be looked up in the reader as ${key} and
     * replaced with the associated value.
     */
    public static void filteredCopy(BufferedReader from, File to, Map<String, String> filters)
            throws IOException {
        // prepare the escaped ${key} keys so that it won't be necessary to do
        // it over and over
        // while parsing the file
        Map<String, String> escapedMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            escapedMap.put("${" + entry.getKey() + "}", entry.getValue());
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(to))) {
            String line = null;
            while ((line = from.readLine()) != null) {
                for (Map.Entry<String, String> entry : escapedMap.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
                out.write(line);
                out.newLine();
            }
            out.flush();
        } finally {
            from.close();
        }
    }

    /** Copies the provided file onto the specified destination file */
    public static void copy(File from, File to) throws IOException {
        copy(new FileInputStream(from), to);
    }

    /** Copy the contents of fromDir into toDir (if the latter is missing it will be created) */
    public static void deepCopy(File fromDir, File toDir) throws IOException {
        if (!fromDir.isDirectory() || !fromDir.exists())
            throw new IllegalArgumentException(
                    "Invalid source directory "
                            + "(it's either not a directory, or does not exist");
        if (toDir.exists() && toDir.isFile())
            throw new IllegalArgumentException(
                    "Invalid destination directory, " + "it happens to be a file instead");

        // create destination if not available
        if (!toDir.exists()) if (!toDir.mkdir()) throw new IOException("Could not create " + toDir);

        File[] files = fromDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destination = new File(toDir, file.getName());
                if (file.isDirectory()) deepCopy(file, destination);
                else copy(file, destination);
            }
        }
    }

    /**
     * Creates a directory as a child of baseDir. The directory name will be preceded by prefix and
     * followed by suffix
     */
    public static File createRandomDirectory(String baseDir, String prefix, String suffix)
            throws IOException {
        File tempDir = File.createTempFile(prefix, suffix, new File(baseDir));
        tempDir.delete();
        if (!tempDir.mkdir())
            throw new IOException("Could not create the temp directory " + tempDir.getPath());
        return tempDir;
    }

    /**
     * Creates a temporary directory whose name will start by prefix
     *
     * <p>Strategy is to leverage the system temp directory, then create a sub-directory.
     */
    public static File createTempDirectory(String prefix) throws IOException {
        File dummyTemp = File.createTempFile("blah", null);
        String sysTempDir = dummyTemp.getParentFile().getAbsolutePath();
        dummyTemp.delete();

        File reqTempDir = new File(sysTempDir + File.separator + prefix + Math.random());
        reqTempDir.mkdir();

        return reqTempDir;
    }

    /**
     * Recursively deletes the contents of the specified directory, and finally wipes out the
     * directory itself. For each file that cannot be deleted a warning log will be issued.
     *
     * @param directory Directory to delete
     * @returns true if the directory could be deleted, false otherwise
     */
    public static boolean delete(File directory) throws IOException {
        return emptyDirectory(directory, false);
    }

    /**
     * Recursively deletes the contents of the specified directory, and finally wipes out the
     * directory itself. If running in quite mode no exception or log message will be issued by this
     * method. Otherwise, an exception will be throw if the directory doesn't exists and a warning
     * log will be issued for each file that cannot be deleted.
     *
     * @param directory the directory to delete recursively
     * @return TRUE if the directory and its content could be deleted, FALSE otherwise
     */
    public static boolean delete(File directory, boolean quiet) throws IOException {
        try {
            // recursively delete the directory content
            emptyDirectory(directory, quiet);
            // delete the directory
            return directory.delete();
        } catch (Exception exception) {
            if (!quiet) {
                // no quiet mode, let's rethrow the exception
                throw exception;
            }
            // quite mode, something bad happen
            return false;
        }
    }

    /**
     * Recursively deletes the contents of the specified directory (but not the directory itself).
     * For each file that cannot be deleted a warning log will be issued.
     *
     * @param directory the directory whose content will be deleted
     * @return TRUE if all the directory contents could be deleted, FALSE otherwise
     */
    public static boolean emptyDirectory(File directory) throws IOException {
        return emptyDirectory(directory, false);
    }

    /**
     * Recursively deletes the contents of the specified directory, but not the directory itself. If
     * running in quite mode no exception or log message will be issued by this method. Otherwise,
     * an exception will be throw if the directory doesn't exists and a warning log will be issued
     * for each file that cannot be deleted.
     *
     * @param directory the directory whose content will be deleted
     * @param quiet if TRUE no exception or log will be issued
     * @return TRUE if all the directory contents could be deleted, FALSE otherwise
     */
    public static boolean emptyDirectory(File directory, boolean quiet) throws IOException {
        if (!directory.isDirectory()) {
            if (!quiet) {
                throw new IllegalArgumentException(
                        String.format(
                                "The provide file '%s' doesn't appear to be a directory.",
                                directory.getAbsolutePath()));
            }
            // in quiet mode, let's just move on
            return false;
        }
        // get directory list of files
        File[] files = directory.listFiles();
        if (files == null) {
            // this should only happen if the file is not a directory or some IO exception happened
            if (!quiet) {
                // unlikely to happen
                throw new IllegalStateException(
                        String.format(
                                "Not able to list files of '%s'.", directory.getAbsolutePath()));
            }
            // in quiet mode, let's just move on
            return false;
        }
        // let's remove the directory files
        boolean allClean = true;
        for (File file : files) {
            if (file.isDirectory()) {
                // recursively delete directory content
                allClean &= delete(file);
            } else {
                if (!file.delete()) {
                    if (!quiet) {
                        LOGGER.log(Level.WARNING, "Could not delete {0}", file.getAbsolutePath());
                    }
                    allClean = false;
                }
            }
        }
        return allClean;
    }

    /**
     * Zips up the directory contents into the specified {@link ZipOutputStream}.
     *
     * <p>Note this method does not take ownership of the provided zip output stream, meaning the
     * client code is responsible for calling {@link ZipOutputStream#finish() finish()} when it's
     * done adding zip entries.
     *
     * @param directory The directory whose contents have to be zipped up
     * @param zipout The {@link ZipOutputStream} that will be populated by the files found
     * @param filter An optional filter that can be used to select only certain files. Can be null,
     *     in that case all files in the directory will be zipped
     */
    public static void zipDirectory(
            File directory, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        zipDirectory(directory, "", zipout, filter);
    }

    /**
     * See {@link #zipDirectory(File, ZipOutputStream, FilenameFilter)}, this version handles the
     * prefix needed to recursively zip data preserving the relative path of each
     */
    private static void zipDirectory(
            File directory, String prefix, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        File[] files = directory.listFiles(filter);
        // copy file by reading 4k at a time (faster than buffered reading)
        byte[] buffer = new byte[4 * 1024];
        if (files != null) {
            for (File file : files) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        // recurse and append
                        String newPrefix = prefix + file.getName() + "/";
                        zipout.putNextEntry(new ZipEntry(newPrefix));
                        zipDirectory(file, newPrefix, zipout, filter);
                    } else {
                        ZipEntry entry = new ZipEntry(prefix + file.getName());
                        zipout.putNextEntry(entry);

                        InputStream in = new FileInputStream(file);
                        int c;
                        try {
                            while (-1 != (c = in.read(buffer))) {
                                zipout.write(buffer, 0, c);
                            }
                            zipout.closeEntry();
                        } finally {
                            in.close();
                        }
                    }
                }
            }
        }
        zipout.flush();
    }

    /**
     * Gets the output file for the provided zip entry and checks that it will not be written
     * outside of the target directory.
     *
     * @param destDir the output directory
     * @param entry the zip entry
     * @return the output file
     * @throws IOException if the zip entry is outside of the target directory
     */
    public static File getZipOutputFile(File destDir, ZipEntry entry) throws IOException {
        String canonicalDirectory = destDir.getCanonicalPath();
        File file = new File(destDir, entry.getName());
        String canonicalFile = file.getCanonicalPath();
        if (canonicalFile.startsWith(canonicalDirectory + File.separator)) {
            return file;
        }
        throw new IOException("Entry is outside of the target directory: " + entry.getName());
    }

    public static void decompress(InputStream input, File destDir) throws IOException {
        ZipInputStream zin = new ZipInputStream(input);
        ZipEntry entry = null;

        byte[] buffer = new byte[1024];
        while ((entry = zin.getNextEntry()) != null) {
            File f = getZipOutputFile(destDir, entry);
            if (entry.isDirectory()) {
                f.mkdirs();
                continue;
            }

            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

            int n = -1;
            while ((n = zin.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            out.flush();
            out.close();
        }
    }

    public static void decompress(final File inputFile, final File destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(inputFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File newFile = getZipOutputFile(destDir, entry);
                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    newFile.mkdir();
                    continue;
                }

                InputStream stream = zipFile.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(newFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = stream.read(buf)) >= 0) saveCompressedStream(buf, fos, len);

                } catch (IOException e) {
                    IOException ioe = new IOException("Not valid archive file type.");
                    ioe.initCause(e);
                    throw ioe;
                } finally {
                    fos.flush();
                    fos.close();

                    stream.close();
                }
            }
        }
    }

    /** */
    public static void saveCompressedStream(
            final byte[] buffer, final OutputStream out, final int len) throws IOException {
        try {
            out.write(buffer, 0, len);

        } catch (Exception e) {
            out.flush();
            out.close();
            IOException ioe = new IOException("Not valid archive file type.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Backs up a directory <tt>dir</tt> by creating a .bak next to it.
     *
     * @param dir The directory to back up.
     */
    public static void backupDirectory(File dir) throws IOException {
        File bak = new File(dir.getCanonicalPath() + ".bak");
        if (bak.exists()) {
            FileUtils.deleteDirectory(bak);
        }
        dir.renameTo(bak);
    }

    /**
     * Renames a file.
     *
     * @param f The file to rename.
     * @param newName The new name of the file.
     */
    public static void rename(File f, String newName) throws IOException {
        rename(f, new File(f.getParentFile(), newName));
    }

    /**
     * Renames a file.
     *
     * @param source The file to rename.
     * @param dest The file to rename to.
     */
    public static void rename(File source, File dest) throws IOException {
        // same path? Do nothing
        if (source.getCanonicalPath().equalsIgnoreCase(dest.getCanonicalPath())) return;

        // windows needs special treatment, we cannot rename onto an existing file
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if (win && dest.exists()) {
            // windows does not do atomic renames, and can not rename a file if the dest file
            // exists
            if (!dest.delete()) {
                throw new IOException("Could not delete: " + dest.getCanonicalPath());
            }
        }
        // make sure the rename actually succeeds
        if (!source.renameTo(dest)) {
            FileUtils.deleteQuietly(dest);
            if (source.isDirectory()) {
                FileUtils.moveDirectory(source, dest);
            } else {
                FileUtils.moveFile(source, dest);
            }
        }
    }

    /**
     * Replacement for the now deprecated {@link
     * org.apache.commons.io.IOUtils#closeQuietly(Closeable)}, to be used only when then "quiet"
     * behavior bit is really rneeded
     */
    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }
}
