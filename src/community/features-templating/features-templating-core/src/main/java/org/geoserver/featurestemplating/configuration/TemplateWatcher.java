/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
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
     * Parse template file and return a builder tree as a {@link RootBuilder}
     *
     * @return builderTree as a RootBuilder
     */
    @Override
    public RootBuilder parseFileContents(InputStream in) throws IOException {
        ObjectMapper mapper =
                new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
        JSONTemplateReader templateReader =
                new JSONTemplateReader(mapper.readTree(in), configuration);
        return templateReader.getRootBuilder();
    }
}
