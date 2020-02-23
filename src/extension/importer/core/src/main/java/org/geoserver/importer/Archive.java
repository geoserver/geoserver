/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;

public class Archive extends Directory {

    private static final long serialVersionUID = -6007727652626093242L;

    /**
     * Create archive from a file.
     *
     * @param file the file
     */
    public Archive(File file) throws IOException {
        super(Directory.createFromArchive(file).getFile());
    }
}
