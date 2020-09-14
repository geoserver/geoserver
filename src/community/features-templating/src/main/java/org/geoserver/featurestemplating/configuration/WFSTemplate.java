/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class handles the management of a single template file, giving access to the ${@link
 * RootBuilder} produced from it and issuing the reloading of the file when needed through ${@link
 * WFSTemplateWatcher}
 */
public class WFSTemplate {

    private Resource templateFile;
    private WFSTemplateWatcher watcher;
    private RootBuilder builderTree;

    private static final Logger LOGGER = Logging.getLogger(WFSTemplate.class);

    public WFSTemplate(Resource templateFile, NamespaceSupport namespaces) {
        this.templateFile = templateFile;
        this.watcher = new WFSTemplateWatcher(templateFile, namespaces);
        try {
            this.builderTree = watcher.getTemplate();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /** Check if the template file has benn modified and eventually reload it. */
    public boolean checkTemplate() {
        if (watcher != null && watcher.isModified()) {
            LOGGER.log(
                    Level.INFO,
                    "Reloading json-ld template for Feature Type {0}",
                    templateFile.name());
            synchronized (this) {
                if (watcher != null && watcher.isModified()) {
                    try {
                        RootBuilder root = watcher.getTemplate();
                        this.builderTree = root;
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Provides the template in the form of a builder tree
     *
     * @return the builder tree as a RootBuilder
     */
    public RootBuilder getRootBuilder() {
        return builderTree;
    }
}
