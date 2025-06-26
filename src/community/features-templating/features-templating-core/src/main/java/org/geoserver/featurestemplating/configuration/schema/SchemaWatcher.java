/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import static org.geoserver.platform.resource.Resource.Type.RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;

/** This class extends {@link FileWatcher} to provide functionalities to dynamically reload a template */
public class SchemaWatcher extends FileWatcher<String> {

    public SchemaWatcher(Resource resource) {
        super(resource);
    }

    /**
     * Reads the file updating the last check timestamp.
     *
     * <p>Subclasses can override {@link #parseFileContents(InputStream)} to do something when the file is read.
     *
     * @return parsed file contents
     */
    public String read() throws IOException {
        String result = null;

        if (resource.getType() == RESOURCE) {

            try (InputStream is = resource.in()) {
                result = parseResource(resource);

                lastModified = resource.lastmodified();
                lastCheck = System.currentTimeMillis();
                stale = false;
            }
        }

        return result;
    }

    /**
     * Parse template file and return a builder tree as a {@link RootBuilder}
     *
     * @return builderTree as a RootBuilder
     */
    public String parseResource(Resource resource) throws IOException {
        String content = new String(resource.getContents(), StandardCharsets.UTF_8);
        return content;
    }
}
