/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Tracks and cleans up a set of files and directories
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class WPSFileResource implements WPSResource {
    List<File> files;

    public WPSFileResource(List<File> files) {
        this.files = files;
    }

    public WPSFileResource(File file) {
        this(Collections.singletonList(file));
    }

    public void delete() throws Exception {
        for (File file : files) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }

    public String getName() {
        if (files.size() == 1) {
            return files.get(0).getPath();
        } else {
            StringBuilder sb = new StringBuilder("Files: ");
            for (File file : files) {
                sb.append(file.getPath()).append(" ");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

}
