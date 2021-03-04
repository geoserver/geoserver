/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import org.geoserver.featurestemplating.builders.BuilderFactory;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides a configuration to setup a {@link JSONTemplateReader} */
public class TemplateReaderConfiguration {

    private NamespaceSupport namespaces;

    public TemplateReaderConfiguration(NamespaceSupport namespaces) {
        this.namespaces = namespaces;
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public BuilderFactory getBuilderFactory(boolean isJSONLD) {
        return new BuilderFactory(isJSONLD);
    }
}
