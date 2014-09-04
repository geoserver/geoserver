/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.model.IModel;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Makes sure the file path for files do start with file:// otherwise
 * stuff like /home/user/file.shp won't be recognized as valid. Also, if a 
 * path is inside the data directory it will be turned into a relative path 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class FileModel implements IModel {
    static final Logger LOGGER = Logging.getLogger(FileModel.class);
    
    IModel delegate;
    File rootDir;
    
    public FileModel(IModel delegate) {
        this(delegate, GeoserverDataDirectory.getGeoserverDataDirectory());
    }

    public FileModel(IModel delegate, File rootDir) {
        this.delegate = delegate;
        this.rootDir = rootDir;
    }
    
    
    private boolean isSubfile(File root, File selection) {
        if(selection == null || "".equals(selection.getPath()))
            return false;
        if(selection.equals(root))
            return true;
        
        return isSubfile(root, selection.getParentFile());
    }

    public Object getObject() {
        return delegate.getObject();
    }

    public void detach() {
        // TODO Auto-generated method stub
        
    }

    public void setObject(Object object) {
        String location = (String) object;
        
        if(location != null) {
            File dataDirectory = canonicalize(rootDir);
            File file = canonicalize(new File(location));
            if(isSubfile(dataDirectory, file)) {
                File curr = file;
                String path = null;
                // paranoid check to avoid infinite loops
                while(curr != null && !curr.equals(dataDirectory)){
                    if(path == null) {
                        path = curr.getName();
                    } else {
                        path = curr.getName() + "/" + path;
                    }
                    curr = curr.getParentFile();
                } 
                location = "file:" + path;
            } else if(!GeoserverDataDirectory.findDataFile(location).equals(file)) {
                // relative to the data directory, does not need fixing
            } else {
                location = "file://" + file.getAbsolutePath();
            }
        }
        
        delegate.setObject(location);
    }


    /**
     * Turns a file in canonical form if possible
     * @param file
     * @return
     */
    File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch(IOException e) {
            LOGGER.log(Level.INFO, "Could not convert " + file + " into canonical form", e);
            return file;
        }
    }
}
