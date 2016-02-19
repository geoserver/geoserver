/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

public class FileData extends ImportData {

    static Logger LOGGER = Logging.getLogger(FileData.class);

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the file handle*/
    protected Resource file;

    public FileData(Resource file) {
        this.file = file;
    }

    public FileData(FileData file) {
        super(file);
        this.file = file.getFile();
    }

    public static FileData createFromFile(Resource file) throws IOException {
        if (file.getType() == Type.DIRECTORY) {
            return new Directory(file);
        }

        if (new VFSWorker().canHandle(file)) {
            return new Archive(file);
        }

        return new SpatialFile(file);
    }
    public Resource getFile() {
        return file;
    }

    @Override
    public String getName() {
        return FilenameUtils.getBaseName(file.name());
    }

    @Override
    public void cleanup() throws IOException {
        if (Resources.exists(file)) {
            if (LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("Deleting file "  + file.path());
            }

            if (!file.delete()) {
                throw new IOException("Unable to delete " + file.path());
            }
        }
    }

    public String relativePath(Directory dir) throws IOException {
        String dp = dir.getFile().path();
        String fp = getFile().path();

        if (fp.startsWith(dp)) {
            String left = fp.substring(dp.length());
            return new File(dir.getFile().name(), left).toString();
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!getClass().isInstance(obj) && !obj.getClass().isInstance(this)) {
            return false;
        }
        FileData other = (FileData) obj;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return file.path();
    }
}
