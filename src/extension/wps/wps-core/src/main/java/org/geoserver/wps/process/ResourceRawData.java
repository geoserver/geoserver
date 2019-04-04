/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.io.FileNotFoundException;
import java.io.InputStream;
import org.geoserver.platform.resource.Resource;

/**
 * An implementation of RawData backed by a Resource
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ResourceRawData extends AbstractRawData {

    Resource file;

    public ResourceRawData(Resource file, String mimeType) {
        super(mimeType);
        this.file = file;
    }

    public ResourceRawData(Resource file, String mimeType, String extension) {
        super(mimeType, extension);
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return file.in();
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "FileRawData [file="
                + file
                + ", mimeType="
                + mimeType
                + ", extension="
                + extension
                + "]";
    }

    public Resource getResource() {
        return file;
    }
}
