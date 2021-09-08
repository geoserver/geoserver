/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * This class handles the management of a single template file, giving access to the ${@link
 * RootBuilder} produced from it and issuing the reloading of the file when needed through ${@link
 * TemplateWatcher}
 */
public class Template {

    private Resource templateFile;
    private FileWatcher<RootBuilder> watcher;
    private RootBuilder builderTree;

    private static final Logger LOGGER = Logging.getLogger(Template.class);

    public Template(Resource templateFile, TemplateReaderConfiguration configuration) {
        this.templateFile = templateFile;
        this.watcher = new TemplateWatcher(templateFile, configuration);
        try {
            this.builderTree = watcher.read();
        } catch (IOException ioe) {
            throw new RuntimeException("Failure parsing " + templateFile, ioe);
        }
    }

    /** Check if the template file has benn modified and eventually reload it. */
    public boolean checkTemplate() {
        if (needsReload()) {
            LOGGER.log(
                    Level.INFO,
                    "Reloading json-ld template for Feature Type {0}",
                    templateFile.name());
            synchronized (this) {
                if (needsReload()) {
                    try {
                        RootBuilder root = watcher.read();
                        this.builderTree = root;
                        return true;
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            }
        }
        return false;
    }

    private boolean needsReload() {
        return watcher != null
                && (watcher.isModified() || (builderTree != null && builderTree.needsReload()));
    }

    public void reloadTemplate() {
        synchronized (this) {
            if (watcher != null) {
                try {
                    RootBuilder root = watcher.read();
                    this.builderTree = root;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
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
