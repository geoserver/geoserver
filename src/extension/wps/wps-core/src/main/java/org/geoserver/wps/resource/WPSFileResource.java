/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;

/**
 * Tracks and cleans up a set of files and directories
 *
 * @author Andrea Aime - OpenGeo
 */
public class WPSFileResource implements WPSResource {
    List<Resource> files;

    public WPSFileResource(List<Resource> files) {
        this.files = files;
    }

    public WPSFileResource(Resource file) {
        this(Collections.singletonList(file));
    }

    public WPSFileResource(File file) {
        this(Files.asResource(file));
    }

    public void delete() throws Exception {
        for (Resource file : files) {
            file.delete();
        }
    }

    public String getName() {
        if (files.size() == 1) {
            return files.get(0).path();
        } else {
            StringBuilder sb = new StringBuilder("Files: ");
            for (Resource file : files) {
                sb.append(file.path()).append(" ");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }
}
