/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import java.io.IOException;
import org.geoserver.platform.resource.Resource;

/**
 * This class provides the necessary logic to retrieve a reader based on the extension value passed.
 */
public class TemplateReaderProvider {
    enum SupportedExtension {
        JSON,
        XML,
        XHTML
    }

    /**
     * Find the proper TemplateReader.
     *
     * @param resourceExtension the resource file extension can be xml, xhtml, json.
     * @param is the input stream of the resource to be parsed.
     * @param configuration the TemplateReaderConfiguration.
     * @return
     * @throws IOException
     */
    public static TemplateReader findReader(
            String resourceExtension, Resource resource, TemplateReaderConfiguration configuration)
            throws IOException {
        TemplateReader reader;
        if (resourceExtension.equalsIgnoreCase(SupportedExtension.JSON.name())) {
            RecursiveJSONParser parser = new RecursiveJSONParser(resource);
            reader = new JSONTemplateReader(parser.parse(), configuration, parser.getWatchers());
        } else if (resourceExtension.equalsIgnoreCase(SupportedExtension.XHTML.name())
                || resourceExtension.equalsIgnoreCase(SupportedExtension.XML.name())) {
            reader = new XMLTemplateReader(resource, configuration.getNamespaces());
        } else {
            throw new UnsupportedOperationException(
                    "Not a supported extension " + resourceExtension);
        }
        return reader;
    }
}
