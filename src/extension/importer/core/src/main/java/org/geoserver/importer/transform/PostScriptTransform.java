/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.File;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ValidationException;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;

/**
 * Runs a post-script with the provided options.
 *
 * <p>Eventually it would be useful to pass some information about the data just imported, but will
 * require more configuration (like, for direct imports it may be the data location, for indirect
 * ones the layer just created, along with some configuration about if and how to pass said into the
 * script). If you're reading this and are interested, hop on gs-devel and discuss
 */
public class PostScriptTransform extends AbstractCommandLineTransform implements PostTransform {

    private final String name;

    public PostScriptTransform(String name, List<String> options) {
        super(options);
        this.name = name;

        // force validation since there is no setter for name
        getExecutable();
    }

    /** Name of the script to be run */
    public String getName() {
        return name;
    }

    @Override
    public void apply(ImportTask task, ImportData data) throws Exception {
        File executable = getExecutable();

        CommandLine cmd = new CommandLine(executable);
        for (String option : getOptions()) {
            cmd.addArgument(option, false);
        }

        execute(cmd, getScriptsFolder().dir());
    }

    private File getExecutable() {
        Resource scripts = getScriptsFolder();

        Resource resource = scripts.get(name);
        if (resource.getType() != Resource.Type.RESOURCE) {
            throw new ValidationException(
                    "Script named '"
                            + name
                            + "' was not found in "
                            + "$GEOSERVER_DATA_DIR/importer/scripts");
        }
        File executable = resource.file();
        if (!executable.canExecute()) {
            throw new ValidationException(
                    "Found file named '"
                            + name
                            + "' in "
                            + "$GEOSERVER_DATA_DIR/importer/scripts, but it's not executable");
        }
        return executable;
    }

    private Resource getScriptsFolder() {
        GeoServerDataDirectory dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        Resource scripts = dd.getRoot("importer", "scripts");
        if (scripts.getType() == Resource.Type.UNDEFINED) {
            throw new ValidationException("Could not find importer/scripts in data directory");
        } else if (scripts.getType() == Resource.Type.RESOURCE) {
            throw new ValidationException(
                    "Found importer/scripts in data directory, but it's a"
                            + " file and was expecting a directory");
        } else {
            return scripts;
        }
    }
}
