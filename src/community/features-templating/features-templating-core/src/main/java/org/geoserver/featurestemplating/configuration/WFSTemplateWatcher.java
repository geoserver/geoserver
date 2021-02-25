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
import org.geoserver.featurestemplating.readers.JsonTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReader;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class extends {@link FileWatcher} to provide functionalities to dynamically reload a
 * template
 */
public class WFSTemplateWatcher extends FileWatcher<RootBuilder> {

    private NamespaceSupport namespaces;
    private String fileName;

    public WFSTemplateWatcher(Resource resource, NamespaceSupport namespaces) {
        super(resource);
        this.fileName = resource.name();
        this.namespaces = namespaces;
    }

    /**
     * Parse template file and return a builder tree as a {@link RootBuilder}
     *
     * @return builderTree as a RootBuilder
     */
    @Override
    public RootBuilder parseFileContents(InputStream in) throws IOException {
        TemplateReader templateReader = getReader(in);
        if (templateReader != null) return templateReader.getRootBuilder();
        else return null;
    }

    public RootBuilder getTemplate() throws IOException {
        return read();
    }

    private TemplateReader getReader(InputStream in) throws IOException {
        TemplateReader templateReader = null;
        if (isJsonTemplateType()) {
            ObjectMapper mapper =
                    new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
            templateReader = new JsonTemplateReader(mapper.readTree(in), namespaces);
        }
        return templateReader;
    }

    private boolean isJsonTemplateType() {
        return fileName.equals(TemplateIdentifier.JSON.getFilename())
                || fileName.equals(TemplateIdentifier.JSONLD.getFilename())
                || fileName.equals(TemplateIdentifier.GEOJSON.getFilename());
    }
}
