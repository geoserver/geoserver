/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.geotools.util.logging.Logging;

/**
 * Resource Store based file service
 *
 * @author Niels Charlier
 */
public class ResourceFileServiceImpl extends SecuredImpl implements FileService {

    private static final long serialVersionUID = -1948411877746516243L;

    private static final Logger LOGGER = Logging.getLogger(ResourceFileServiceImpl.class);

    private ResourceStore store;

    private Resource rootFolder;

    private String description;

    public ResourceFileServiceImpl(ResourceStore resourceStore) {
        this.store = resourceStore;
        this.rootFolder = resourceStore.get("/");
    }

    @Override
    public String getDescription() {
        return "Resource Store: " + (description == null ? description : getName());
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = store.get(rootFolder);
    }

    @Override
    public String getRootFolder() {
        return rootFolder.path();
    }

    @Override
    public boolean checkFileExists(String filePath) {
        return Resources.exists(rootFolder.get(filePath));
    }

    @Override
    public void create(String path, InputStream content, boolean doPrepare) throws IOException {
        // Check parameters
        if (content == null) {
            throw new IllegalArgumentException("Content of a resource can not be null.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Name of a resource can not be null.");
        }
        if (checkFileExists(path)) {
            throw new IllegalArgumentException("The resource already exists");
        }

        try (OutputStream output = rootFolder.get(path).out()) {
            IOUtils.copy(content, output);
        }
    }

    @Override
    public boolean delete(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path of a resource can not be null.");
        }
        if (checkFileExists(path)) {
            return rootFolder.get(path).delete();
        } else {
            return false;
        }
    }

    @Override
    public InputStream read(String path) throws IOException {
        if (checkFileExists(path)) {
            return rootFolder.get(path).in();
        } else {
            throw new IOException("The resource does not exist:" + path);
        }
    }

    @Override
    public List<String> listSubfolders() {
        List<String> paths = new ArrayList<>();
        for (Resource child : rootFolder.list()) {
            if (child.getType() == Type.DIRECTORY) {
                paths.add(child.name());
            }
        }
        return paths;
    }

    @Override
    public FileReference getVersioned(String path) {
        if (path.indexOf(FileService.PLACEHOLDER_VERSION) < 0) {
            return new FileReferenceImpl(this, path, path);
        }

        Resource parent = rootFolder.get(path).parent();

        SortedSet<Integer> set = new TreeSet<Integer>();
        Pattern pattern =
                Pattern.compile(
                        Pattern.quote(path).replace(FileService.PLACEHOLDER_VERSION, "\\E(.*)\\Q"));
        for (Resource res : parent.list()) {
            Matcher matcher = pattern.matcher(res.name());
            if (matcher.matches()) {
                try {
                    set.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "could not parse version in versioned file " + res.name(),
                            e);
                }
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "this shouldn't happen: couldn't find version in versioned file "
                                + res.name());
            }
        }
        int last = set.isEmpty() ? 0 : set.last();
        return new FileReferenceImpl(
                this,
                path.replace(FileService.PLACEHOLDER_VERSION, last + ""),
                path.replace(FileService.PLACEHOLDER_VERSION, (last + 1) + ""));
    }

    @Override
    public URI getURI(String path) {
        try {
            return new URI(
                    "resource:"
                            + URLEncoder.encode(rootFolder.get(path).path(), "UTF-8")
                                    .replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
