/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * An implementation of RawData backed by a File
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FileRawData extends AbstractRawData {

    File file;

    public FileRawData(File file, String mimeType) {
        super(mimeType);
        this.file = file;
    }

    public FileRawData(File file, String mimeType, String extension) {
        super(mimeType, extension);
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "FileRawData [file=" + file + ", mimeType=" + mimeType + ", extension=" + extension
                + "]";
    }

}
