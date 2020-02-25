/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class handles the management of a single json-ld template file, giving access to the ${@link
 * RootBuilder} produced from the template file and issuing the reloading of the file when needed
 * through ${@link JsonLdTemplateWatcher}
 */
public class JsonLdTemplate {

    private Resource templateFile;
    private JsonLdTemplateWatcher watcher;
    private RootBuilder builderTree;

    private static final Logger LOGGER = Logging.getLogger(JsonLdTemplate.class);

    public JsonLdTemplate(Resource templateFile, NamespaceSupport namespaces) {
        this.templateFile = templateFile;
        this.watcher = new JsonLdTemplateWatcher(templateFile, namespaces);
        try {
            this.builderTree = watcher.getJsonLdTemplate();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /** Check if json-ld template file has benn modified an eventually reload it. */
    public boolean checkTemplate() {
        if (watcher != null && watcher.isModified()) {
            LOGGER.log(
                    Level.INFO,
                    "Reloading json-ld template for Feature Type {0}",
                    templateFile.name());
            synchronized (this) {
                if (watcher != null && watcher.isModified()) {
                    try {
                        RootBuilder root = watcher.getJsonLdTemplate();
                        this.builderTree = root;
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            }
        }
        return false;
    }

    public RootBuilder getRootBuilder() {
        return builderTree;
    }
}
