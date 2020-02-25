/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.File;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class DirectoryResourceLoader extends DefaultResourceLoader {

    File root;

    public DirectoryResourceLoader(File root) {
        super();
        this.root = root;
    }

    @Override
    protected Resource getResourceByPath(String path) {
        return new FileSystemContextResource(root, path);
    }

    private class FileSystemContextResource extends FileSystemResource implements ContextResource {

        public FileSystemContextResource(File root, String path) {
            super(new File(root, path).getAbsolutePath());
        }

        public String getPathWithinContext() {
            return getPath();
        }
    }
}
