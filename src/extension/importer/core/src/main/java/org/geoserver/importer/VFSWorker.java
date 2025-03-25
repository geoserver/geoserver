/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.geotools.util.logging.Logging;

/**
 * Utility to work with compressed files
 *
 * @author groldan
 */
public class VFSWorker {

    private static final Logger LOGGER = Logging.getLogger(VFSWorker.class);

    private static final List<String> extensions =
            Arrays.asList(".zip", ".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".gz", ".bz2", ".jar", ".kmz");

    public VFSWorker() {}

    public boolean canHandle(final File file) {
        final String name = file.getName().toLowerCase();
        for (String supportedExtension : extensions) {
            if (name.endsWith(supportedExtension)) {
                return true;
            }
        }
        return false;
    }

    public static String getExtension(String name) {
        for (String supportedExtension : extensions) {
            if (name.endsWith(supportedExtension)) {
                return supportedExtension;
            }
        }
        String extension = FilenameUtils.getExtension(name);
        if (extension.length() > 0) {
            extension = "." + extension;
        }
        return extension;
    }

    /**
     * List contents of archiveFile according to provided filter.
     *
     * @param archiveFile
     * @param filter
     */
    @SuppressWarnings({"PMD.UseTryWithResources", "PMD.ForLoopCanBeForeach"})
    public List<String> listFiles(final File archiveFile, final FilenameFilter filter) {
        FileSystemManager fsManager;
        try {
            fsManager = VFS.getManager();
            String absolutePath = resolveArchiveURI(archiveFile);
            try (FileObject resolvedFile = fsManager.resolveFile(absolutePath)) {

                FileSelector fileSelector = new FileSelector() {
                    /**
                     * @see
                     *     org.apache.commons.vfs2.FileSelector#traverseDescendents(org.apache.commons.vfs2.FileSelectInfo)
                     */
                    @Override
                    @Deprecated
                    public boolean traverseDescendents(FileSelectInfo folderInfo) throws Exception {
                        return true;
                    }

                    /** @see org.apache.commons.vfs2.FileSelector#includeFile(org.apache.commons.vfs2.FileSelectInfo) */
                    @Override
                    public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
                        File folder = archiveFile.getParentFile();
                        String name = fileInfo.getFile().getName().getFriendlyURI();
                        return filter.accept(folder, name);
                    }
                };

                FileObject fileSystem = null;
                try {
                    if (fsManager.canCreateFileSystem(resolvedFile)) {
                        fileSystem = fsManager.createFileSystem(resolvedFile);
                    } else {
                        fileSystem = resolvedFile;
                    }
                    LOGGER.fine("Listing spatial data files archived in " + archiveFile.getName());
                    FileObject[] containedFiles = fileSystem.findFiles(fileSelector);
                    List<String> names = new ArrayList<>(containedFiles.length);
                    for (int i = 0; i < containedFiles.length; i++) {
                        try (FileObject childFileObject = containedFiles[i]) {
                            // path relative to its filesystem (ie, to the archive file)
                            String pathDecoded = childFileObject.getName().getPathDecoded();
                            names.add(pathDecoded);
                        }
                    }
                    LOGGER.fine(
                            "Found " + names.size() + " spatial data files in " + archiveFile.getName() + ": " + names);
                    return names;
                } finally {
                    if (fileSystem != null) {
                        fileSystem.close();
                    }
                }
            }
        } catch (FileSystemException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return Collections.emptyList();
    }

    private String resolveArchiveURI(final File archiveFile) {
        String archivePrefix = getaArchiveURLProtocol(archiveFile);
        String absolutePath = archivePrefix + archiveFile.getAbsolutePath();
        return absolutePath;
    }

    private String getaArchiveURLProtocol(final File file) {
        if (file.exists() && file.isDirectory()) {
            return "file://";
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".kmz")) {
            return "zip://";
        }
        if (name.endsWith(".tar")) {
            return "tar://";
        }
        if (name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
            return "tgz://";
        }
        if (name.endsWith(".tbz2") || name.endsWith(".tar.bzip2") || name.endsWith(".tar.bz2")) {
            return "tbz2://";
        }
        if (name.endsWith(".gz")) {
            return "gz://";
        }
        if (name.endsWith(".bz2")) {
            return "bz2://";
        }
        if (name.endsWith(".jar")) {
            return "jar://";
        }
        return null;
    }

    /** Extracts the archive file {@code archiveFile} to {@code targetFolder}; both shall previously exist. */
    @SuppressWarnings("PMD.CloseResource")
    public void extractTo(File archiveFile, File targetFolder) throws IOException {

        FileSystemManager manager = VFS.getManager();
        String sourceURI = resolveArchiveURI(archiveFile);
        FileSelector allFiles = new AllFileSelector() {
            @Override
            public boolean includeFile(FileSelectInfo fileInfo) {
                LOGGER.fine("Uncompressing " + fileInfo.getFile().getName().getFriendlyURI());
                return true;
            }
        };

        try (FileObject source = manager.resolveFile(sourceURI);
                FileObject target = manager.resolveFile(targetFolder.getAbsolutePath());
                FileObject targetFileSystem = manager.createVirtualFileSystem(target)) {
            if (manager.canCreateFileSystem(source)) {
                try (FileObject sourceFileSystem = manager.createFileSystem(source)) {
                    targetFileSystem.copyFrom(sourceFileSystem, allFiles);
                    manager.closeFileSystem(sourceFileSystem.getFileSystem());
                }
            } else {
                target.copyFrom(source, allFiles);
            }
        }
    }

    public Collection<File> listFilesInFolder(final File targetFolder, final FilenameFilter fileNameFilter) {
        IOFileFilter fileFilter = new IOFileFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return fileNameFilter.accept(dir, name);
            }

            @Override
            public boolean accept(File file) {
                return fileNameFilter.accept(file.getParentFile(), file.getName());
            }
        };
        IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        Collection<File> listFiles = FileUtils.listFiles(targetFolder, fileFilter, dirFilter);
        return listFiles;
    }
}
