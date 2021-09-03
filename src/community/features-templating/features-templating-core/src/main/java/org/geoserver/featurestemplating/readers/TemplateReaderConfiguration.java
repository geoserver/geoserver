/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides a configuration to setup a {@link TemplateReader} */
public class TemplateReaderConfiguration {

    private NamespaceSupport namespaces;
    private String rootCollectionName = "features";

    public TemplateReaderConfiguration(NamespaceSupport namespaces) {
        this.namespaces = namespaces;
    }

    public TemplateReaderConfiguration(NamespaceSupport namespaces, String rootCollectionName) {
        this.namespaces = namespaces;
        this.rootCollectionName = rootCollectionName;
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public TemplateBuilderMaker getBuilderMaker() {
        return new TemplateBuilderMaker(rootCollectionName);
    }

    public String getRootCollectionName() {
        return rootCollectionName;
    }
}
