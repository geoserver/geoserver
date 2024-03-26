/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FilePaths;
import org.geoserver.platform.resource.Paths;
import org.geotools.util.logging.Logging;

/**
 * Makes sure the file path for files do start with file:// otherwise stuff like /home/user/file.shp
 * won't be recognized as valid. Also, if a path is inside the data directory it will be turned into
 * a relative path
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileModel implements IModel<String> {
    private static final long serialVersionUID = 3911203737278340528L;

    static final Logger LOGGER = Logging.getLogger(FileModel.class);

    IModel<String> delegate;
    File rootDir;

    public FileModel(IModel<String> delegate) {
        this(delegate, GeoServerExtensions.bean(GeoServerResourceLoader.class).getBaseDirectory());
    }

    public FileModel(IModel<String> delegate, File rootDir) {
        this.delegate = delegate;
        this.rootDir = rootDir;
    }

    @Override
    public String getObject() {
        Object obj = delegate.getObject();
        if (obj instanceof URL) {
            URL url = (URL) obj;
            return url.toExternalForm();
        }
        return (String) obj;
    }

    @Override
    public void detach() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setObject(String location) {

        if (location != null) {
            location = Paths.convert(location);
            String locationPath;
            try {
                locationPath = new URL(location).getFile();
            } catch (MalformedURLException e) {
                locationPath = location;
            }
            locationPath = Paths.convert(rootDir, new File(locationPath));
            if (FilePaths.isAbsolute(locationPath)) {
                location = "file://" + locationPath;
            } else if (!locationPath.equals(location)) {
                location = "file:" + locationPath;
            }
        }
        delegate.setObject(location);
    }

    /** Turns a file in canonical form if possible */
    File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not convert " + file + " into canonical form", e);
            return file;
        }
    }
}
