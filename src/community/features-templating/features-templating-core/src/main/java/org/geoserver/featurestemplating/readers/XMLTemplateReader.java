/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class return a RootBuilder from an xml Resource. The xml parsing work is totally delegated
 * to the internal XMLRecursiveReader
 */
public class XMLTemplateReader implements TemplateReader {

    private RootBuilder rootBuilder;

    public XMLTemplateReader(Resource resource, NamespaceSupport namespaceSupport)
            throws IOException {
        this.rootBuilder = new RootBuilder();
        try (XMLRecursiveReader recursiveParser =
                new XMLRecursiveReader(resource, namespaceSupport)) {
            recursiveParser.iterateReader(rootBuilder);
            rootBuilder.setWatchers(recursiveParser.getWatchers());
        }
    }

    @Override
    public RootBuilder getRootBuilder() {
        return rootBuilder;
    }
}
