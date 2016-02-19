/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;

import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;

public class Archive extends Directory {

    private static final long serialVersionUID = -6007727652626093242L;

    /**
     * 
     * Create archive from a resource.
     * 
     * @param resource the resource
     * @throws IOException
     */
    public Archive(Resource resource) throws IOException {
        super(Directory.createFromArchive(resource).getFile());
    }

    /**
     * 
     * Create archive from a file.
     * 
     * @param file the file
     * @throws IOException
     * @deprecated Use Resource instead of File
     */
    @Deprecated
    public Archive(File file) throws IOException {
        this(Files.asResource(file));
    }

}
