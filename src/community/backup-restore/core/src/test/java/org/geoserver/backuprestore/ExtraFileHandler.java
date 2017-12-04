/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.backuprestore.tasklet.GenericTaskletHandler;
import org.geoserver.backuprestore.tasklet.GenericTaskletUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.util.IOUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Test generic handler that backup and restore an extra file that is not used by GeoServer.
 */
public final class ExtraFileHandler implements GenericTaskletHandler {

    public static final String EXTRA_FILE_NAME = "extra_file.properties";

    private final GeoServerDataDirectory dataDirectory;

    public ExtraFileHandler(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void initialize(StepExecution stepExecution, BackupRestoreItem context) {
        // nothing to do here
    }

    @Override
    public RepeatStatus handle(StepContribution contribution, ChunkContext chunkContext,
                               JobExecution jobExecution, BackupRestoreItem context) {
        File inputDirectory;
        File outputDirectory;
        if (GenericTaskletUtils.isBackup(context)) {
            // we are doing a backup
            inputDirectory = dataDirectory.root();
            outputDirectory = GenericTaskletUtils.getOutputDirectory(jobExecution);
        } else {
            // we are doing a restore
            inputDirectory = GenericTaskletUtils.getInputDirectory(jobExecution);
            outputDirectory = dataDirectory.root();
        }
        copyFile(inputDirectory, EXTRA_FILE_NAME, outputDirectory, EXTRA_FILE_NAME);
        return RepeatStatus.FINISHED;
    }

    /**
     * Helper method for copying a file from a directory to another.
     */
    private void copyFile(File inputDirectory, String inputFileName, File outputDirectory, String outputFileName) {
        File inputFile = new File(inputDirectory, inputFileName);
        if (!inputFile.exists()) {
            // nothing to copy
            return;
        }
        if (outputDirectory.exists() && outputDirectory.isDirectory()) {
            File outputFile = new File(outputDirectory, outputFileName);
            try (InputStream input = new FileInputStream(inputFile);
                    // copy the file to is destination
                    OutputStream output = new FileOutputStream(outputFile)) {
                IOUtils.copy(input, output);
            } catch (Exception exception) {
                throw new RuntimeException(String.format("Error copying file '%s' to file '%s'.",
                        inputFile, outputFile.getAbsolutePath()), exception);
            }
        }
    }
}
