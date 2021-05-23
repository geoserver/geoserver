/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import static org.geoserver.platform.resource.Resource.Type.RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.readers.TemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.readers.TemplateReaderProvider;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;

/**
 * This class extends {@link FileWatcher} to provide functionalities to dynamically reload a
 * template
 */
public class TemplateWatcher extends FileWatcher<RootBuilder> {

    private TemplateReaderConfiguration configuration;

    public TemplateWatcher(Resource resource, TemplateReaderConfiguration configuration) {
        super(resource);
        this.configuration = configuration;
    }

    /**
     * Reads the file updating the last check timestamp.
     *
     * <p>Subclasses can override {@link #parseFileContents(InputStream)} to do something when the
     * file is read.
     *
     * @return parsed file contents
     */
    public RootBuilder read() throws IOException {
        RootBuilder result = null;

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
    public RootBuilder parseResource(Resource resource) throws IOException {
        String extension = FilenameUtils.getExtension(resource.name());
        TemplateReader reader =
                TemplateReaderProvider.findReader(extension, resource, configuration);
        return reader.getRootBuilder();
    }
}
