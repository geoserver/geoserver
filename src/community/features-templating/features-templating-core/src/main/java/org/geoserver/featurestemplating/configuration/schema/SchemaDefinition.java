/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * This class handles the management of a single template file, giving access to the ${@link RootBuilder} produced from
 * it and issuing the reloading of the file when needed through ${@link TemplateWatcher}
 */
public class SchemaDefinition {

    private Resource schemaDefinitionFile;
    private FileWatcher<String> watcher;
    private String schemaContent;

    private static final Logger LOGGER = Logging.getLogger(SchemaDefinition.class);

    public SchemaDefinition(Resource schemaFile) {
        this.schemaDefinitionFile = schemaFile;
        this.watcher = new SchemaWatcher(schemaDefinitionFile);
        try {
            this.schemaContent = watcher.read();
        } catch (IOException ioe) {
            throw new RuntimeException("Failure parsing " + schemaDefinitionFile, ioe);
        }
    }

    /** Check if the template file has benn modified and eventually reload it. */
    public boolean checkSchema() {
        if (needsReload()) {
            LOGGER.log(Level.INFO, "Reloading json-ld template for Feature Type {0}", schemaDefinitionFile.name());
            synchronized (this) {
                if (needsReload()) {
                    try {
                        this.schemaContent = watcher.read();
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
        return watcher != null && (watcher.isModified() || (schemaContent != null));
    }

    public void reloadTemplate() {
        synchronized (this) {
            if (watcher != null) {
                try {
                    String root = watcher.read();
                    this.schemaContent = root;
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
    public String getSchemaContent() {
        return schemaContent;
    }
}
