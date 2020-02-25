/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Handles input and output of feature collections as zipped files.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ZipArchivePPIO extends BinaryPPIO {

    public static final String ZIP = "zip";

    private static final Logger LOGGER = Logging.getLogger(ZipArchivePPIO.class);

    /** Parameter indicating the compression level to use */
    private int compressionLevel;

    /**
     * Instantiates a new zip archive ppio.
     *
     * @param resources the resources
     */
    public ZipArchivePPIO(int compressionLevel) {
        super(File.class, File.class, "application/zip");
        if (compressionLevel < ZipOutputStream.STORED
                || compressionLevel > ZipOutputStream.DEFLATED) {
            throw new IllegalArgumentException("Invalid Compression Level: " + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Using compression level " + compressionLevel);
        }
    }

    /** Default constructor using ZipOutputStream.STORED compression level. */
    public ZipArchivePPIO() {
        this(ZipOutputStream.STORED);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Using compression level " + ZipOutputStream.STORED);
        }
    }

    /**
     * Encodes the output file.
     *
     * @param output the output
     * @param os the os
     * @throws Exception the exception
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void encode(final Object output, OutputStream os) throws Exception {
        // avoid double zipping
        if (output instanceof File && isZpFile((File) output)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "File is already a zip, we have only to copy it");
            }
            FileUtils.copyFile((File) output, os);
            return;
        }

        ZipOutputStream zipout = new ZipOutputStream(os);
        zipout.setLevel(compressionLevel);

        // directory
        if (output instanceof File) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Zipping the file");
            }
            final File file = ((File) output);
            if (file.isDirectory()) {
                IOUtils.zipDirectory(file, zipout, FileFilterUtils.trueFileFilter());
            } else {
                // check if is a zip file already
                zipFile(file, zipout);
            }
        } else {
            // list of files
            if (output instanceof Collection) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Zipping the collection");
                }
                // create temp dir
                final Collection collection = (Collection) output;
                for (Object obj : collection) {
                    if (obj instanceof File) {
                        // convert to file and add to zip
                        final File file = ((File) obj);
                        if (file.isDirectory()) {
                            IOUtils.zipDirectory(file, zipout, FileFilterUtils.trueFileFilter());
                        } else {
                            // check if is a zip file already
                            zipFile(file, zipout);
                        }
                    } else {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info("Skipping object -->" + obj.toString());
                        }
                    }
                }
            } else {
                // error
                throw new IllegalArgumentException(
                        "Unable to zip provided output. Output-->" + output != null
                                ? output.getClass().getCanonicalName()
                                : "null");
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Finished to zip");
        }
        zipout.finish();
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return ZIP;
    }

    /**
     * This method zip the provided file to the provided {@link ZipOutputStream}.
     *
     * <p>It throws {@link IllegalArgumentException} in case the provided file does not exists or is
     * not a readable file.
     *
     * @param file the {@link File} to zip
     * @param zipout the {@link ZipOutputStream} to write to
     * @throws IOException in case something bad happen
     */
    public static void zipFile(File file, ZipOutputStream zipout) throws IOException {
        // copy file by reading 4k at a time (faster than buffered reading)
        byte[] buffer = new byte[4096];
        zipFileInternal(file, zipout, buffer);
    }

    /**
     * This method tells us if the provided {@link File} is a Zip File.
     *
     * <p>It throws {@link IllegalArgumentException} in case the provided file does not exists or is
     * not a readable file.
     *
     * @param file the {@link File} to check for zip
     * @throws IOException in case something bad happen
     */
    public static boolean isZpFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                    "Provided File is not valid and/or reqadable! --> File:" + file != null
                            ? file.getAbsolutePath()
                            : "null");
        }
        // Check if the file is a directory
        if (file.isDirectory()) {
            return false;
        }
        // Check on the path length
        if (file.length() < 4) {
            return false;
        }
        // Check on the first Integer

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int test = in.readInt();
            return test == 0x504b0304;
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
            return false;
        }
    }

    /**
     * This method zip the provided file to the provided {@link ZipOutputStream}.
     *
     * <p>It throws {@link IllegalArgumentException} in case the provided file does not exists or is
     * not a readable file.
     *
     * @param file the {@link File} to zip
     * @param zipout the {@link ZipOutputStream} to write to
     * @param buffer the buffer to use for reading/writing
     * @throws IOException in case something bad happen
     */
    private static void zipFileInternal(File file, ZipOutputStream zipout, byte[] buffer)
            throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                    "Provided File is not valid and/or reqadable! --> File:" + file != null
                            ? file.getAbsolutePath()
                            : "null");
        }

        final ZipEntry entry = new ZipEntry(FilenameUtils.getName(file.getAbsolutePath()));
        zipout.putNextEntry(entry);

        // copy over the file
        try (InputStream in = new FileInputStream(file)) {
            int c;
            while (-1 != (c = in.read(buffer))) {
                zipout.write(buffer, 0, c);
            }
            zipout.closeEntry();
        }
        zipout.flush();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("Decode unsupported");
    }
}
