/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.rest.util.RESTFileValidatorCallback;
import org.geoserver.util.FileTypes;
import org.geotools.api.coverage.grid.Format;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * A validator that will attempt to read the file (or zip) as a coverage of the given format, and will throw an
 * exception if no coverage read is found able to handle the file.
 */
public class CoverageStoreFileValidator implements RESTFileValidatorCallback {
    static final Logger LOGGER = Logging.getLogger(CoverageStoreFileValidator.class);

    AbstractGridFormat coverageFormat;
    Hints hints;

    /**
     * Builds a validator for the given coverage format.
     *
     * @param format name of the "image" format (from the user request)
     */
    public CoverageStoreFileValidator(Format format) {
        this(format, new Hints());
    }

    /**
     * Builds a validator for the given coverage format.
     *
     * @param format name of the "image" format (from the user request)
     * @param hints hints to use when constructing the reader (from the user request)
     */
    public CoverageStoreFileValidator(Format format, Hints hints) {
        if (format == null || format instanceof UnknownFormat) {
            throw new IllegalArgumentException(
                    "CoverageStoreFileValidator: format is not recognized - must be non-null and not Unknown. format="
                            + format);
        }
        if (!(format instanceof AbstractGridFormat)) {
            throw new IllegalArgumentException(
                    "CoverageStoreFileValidator: format is not recognized - should be a AbstractGridFormat but is "
                            + format);
        }
        this.coverageFormat = (AbstractGridFormat) format;
        this.hints = hints;
    }

    /** @param inputStream input stream that is either a simple file or a zip */
    @Override
    public void accept(InputStream inputStream, String fname) {
        if (StringUtils.isBlank(fname) || fname.contains(File.separator)) {
            throw new IllegalArgumentException("CoverageStoreFileValidator: File name is invalid:" + fname);
        }

        Path tempFile = null;
        Path tempDir = null;
        try {
            tempDir = createTempDirectory();
            tempFile = writeToDir(inputStream, tempDir, fname);
            if (FileTypes.isZip(tempFile)) {
                acceptZip(tempFile);
            } else {
                acceptFile(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            // cleanup temp file
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOGGER.log(Level.INFO, "error deleting temp file: " + tempFile, e);
                }
            }
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (IOException e) {
                    LOGGER.log(Level.INFO, "error deleting temp dir: " + tempDir, e);
                }
            }
        }
    }

    public Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("CoverageStoreFileValidator");
    }

    public Path writeToDir(InputStream inputStream, Path tempdir, String fname) throws IOException {
        Path filePath = Paths.get(tempdir.toString(), fname);
        java.nio.file.Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    private void acceptFile(Path tempfile) throws IOException {

        AbstractGridCoverage2DReader reader = null;
        try {
            reader = coverageFormat.getReader(tempfile.toFile(), hints);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
        if (reader == null) {
            throw new IOException(
                    "Unsupported file format: " + tempfile.getFileName() + ", couldn't construct reader for it.");
        }
    }

    public Path findPrimaryFile(Path directory) throws IOException {
        // first check if the format accepts a whole directory
        if (coverageFormat.accepts(directory.toFile())) {
            return directory;
        }
        // iterate through all the top-level files and select the first one the coverage store can handle
        try (Stream<Path> paths = Files.list(directory)) {
            for (Iterator<Path> it = paths.iterator(); it.hasNext(); ) {
                Path insideFile = it.next();
                if (insideFile.toFile().isDirectory()) {
                    continue; // don't handle subdirectories
                }

                if (coverageFormat.accepts(insideFile.toFile())) {
                    return insideFile;
                }
            }
        }
        return null;
    }

    public static void unzip(Path zipFilePath, Path destDirectory) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                File filePath = new File(destDirectory.toFile(), entry.getName());

                // SECURITY CHECK
                String canonicalDestinationPath = destDirectory.toFile().getCanonicalPath();
                String canonicalTargetPath = filePath.getCanonicalPath();
                if (!canonicalTargetPath.startsWith(canonicalDestinationPath + File.separator)) {
                    throw new IOException("bad zip: Zip internal file is trying to escape where its being unzipped: "
                            + entry.getName());
                }

                if (!entry.isDirectory()) {
                    Files.copy(zipIn, filePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    filePath.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void acceptZip(Path tempfile) throws IOException {
        Path tempDir = null;
        try {
            tempDir = createTempDirectory();
            unzip(tempfile, tempDir);
            Path primaryFile = findPrimaryFile(tempDir); // might be dir
            if (primaryFile == null) {
                throw new IOException("couldnt find a primary converage file inside the zip");
            }
            acceptFile(primaryFile);
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (IOException e) {
                    LOGGER.log(Level.INFO, "error deleting temp dir: " + tempDir, e);
                }
            }
        }
    }
}
