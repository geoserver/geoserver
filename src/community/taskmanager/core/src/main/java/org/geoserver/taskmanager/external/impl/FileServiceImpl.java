/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.geotools.util.logging.Logging;
import org.springframework.web.context.ServletContextAware;

/**
 * Local file storage. All actions are relative to the rootFolder. If the data directory is
 * configured the root folder is placed in the data directory.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class FileServiceImpl extends SecuredImpl implements FileService, ServletContextAware {

    private static final long serialVersionUID = -1948411877746516243L;

    private static final Logger LOGGER = Logging.getLogger(FileServiceImpl.class);

    private Path dataDirectory;

    private Path rootFolder;

    private String description;

    private String prepareScript;

    @Override
    public String getDescription() {
        return "Local File System: " + (description == null ? description : getName());
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrepareScript() {
        return prepareScript;
    }

    public void setPrepareScript(String prepareScript) {
        this.prepareScript = prepareScript;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = Paths.get(rootFolder);
    }

    @Override
    public String getRootFolder() {
        return rootFolder.toString();
    }

    @Override
    public boolean checkFileExists(String filePath) {
        return Files.exists(getAbsolutePath(filePath));
    }

    @Override
    public void create(String filePath, InputStream content, boolean doPrepare) throws IOException {
        // Check parameters
        if (content == null) {
            throw new IllegalArgumentException("Content of a file can not be null.");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("Name of a file can not be null.");
        }
        if (checkFileExists(filePath)) {
            throw new IllegalArgumentException("The file already exists");
        }

        File targetFile = new File(getAbsolutePath(filePath).toUri());
        FileUtils.copyInputStreamToFile(content, targetFile);

        if (doPrepare && prepareScript != null) {
            Process p =
                    Runtime.getRuntime().exec(prepareScript + " " + targetFile.getAbsolutePath());
            LOGGER.info(new String(IOUtils.toByteArray(p.getInputStream())));
            LOGGER.warning(new String(IOUtils.toByteArray(p.getErrorStream())));
            try {
                int e = p.waitFor();
                if (e != 0) {
                    throw new IOException("Preparation script ended with exit code " + e);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean delete(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("Name of a filePath can not be null.");
        }
        if (checkFileExists(filePath)) {
            File file = new File(getAbsolutePath(filePath).toUri());
            return file.delete();
        } else {
            return false;
        }
    }

    @Override
    public InputStream read(String filePath) throws IOException {
        if (checkFileExists(filePath)) {
            File file = new File(getAbsolutePath(filePath).toUri());
            return FileUtils.openInputStream(file);
        } else {
            throw new IOException("The file does not exit:" + filePath.toString());
        }
    }

    @Override
    public List<String> listSubfolders() {
        File file = new File(rootFolder.toUri());
        file.mkdirs();
        ArrayList<String> paths = listFolders(file.toURI(), file);
        return paths;
    }

    private ArrayList<String> listFolders(URI rootfolder, File file) {
        String[] folders = file.list(FileFilterUtils.directoryFileFilter());
        ArrayList<String> paths = new ArrayList<>();
        if (folders != null) {
            for (String folder : folders) {
                paths.add(
                        Paths.get(rootfolder)
                                .relativize(Paths.get(file.toString(), folder))
                                .toString());
                paths.addAll(
                        listFolders(
                                rootfolder, new File(Paths.get(file.toString(), folder).toUri())));
            }
        }
        return paths;
    }

    @Override
    public FileReference getVersioned(String filePath) {
        if (filePath.indexOf(FileService.PLACEHOLDER_VERSION) < 0) {
            return new FileReferenceImpl(this, filePath, filePath);
        }

        Path parent = getAbsolutePath(filePath).getParent();
        String[] fileNames =
                parent.toFile()
                        .list(
                                new WildcardFileFilter(
                                        filePath.replace(FileService.PLACEHOLDER_VERSION, "*")));

        SortedSet<Integer> set = new TreeSet<Integer>();
        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(filePath)
                                .replace(FileService.PLACEHOLDER_VERSION, "\\E(.*)\\Q"));
        for (String fileName : fileNames) {
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                try {
                    set.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "could not parse version in versioned file " + fileName,
                            e);
                }
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "this shouldn't happen: couldn't find version in versioned file "
                                + fileName);
            }
        }
        int last = set.isEmpty() ? 0 : set.last();
        return new FileReferenceImpl(
                this,
                filePath.replace(FileService.PLACEHOLDER_VERSION, last + ""),
                filePath.replace(FileService.PLACEHOLDER_VERSION, (last + 1) + ""));
    }

    @Override
    public URI getURI(String filePath) {
        if (dataDirectory == null) {
            return getAbsolutePath(filePath).toUri();
        } else {
            try {
                return new URI("file:" + dataDirectory.relativize(getAbsolutePath(filePath)));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private Path getAbsolutePath(String file) {
        if (rootFolder == null) {
            throw new IllegalStateException(
                    "No rootFolder is not configured in this file service.");
        }
        return rootFolder.resolve(Paths.get(file));
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        String dataDirectory = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
        if (dataDirectory != null) {
            this.dataDirectory = Paths.get(dataDirectory);
        } else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }
}
