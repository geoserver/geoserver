/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptType;
import org.geotools.util.logging.Logging;

public class Script implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logging.getLogger(Script.class);

    private String name;

    private String type;

    private String extension;

    private Resource file;

    private String contents;

    public Script() {}

    public Script(Resource file) {
        this.file = file;
        this.name = getNameFromFile(file);
        this.type = findType(file);
        this.extension = FilenameUtils.getExtension(file.name());
        this.contents = readFile(file);
    }

    @Deprecated
    public Script(File file) {
        this(Files.asResource(file));
    }

    public Script(String name, String type, String extension, String contents) {
        this.file = findFile(name, type, extension);
        this.name = name;
        this.type = type;
        this.extension = extension;
        this.contents = contents;
    }

    private String getNameFromFile(Resource file) {
        String baseName = FilenameUtils.getBaseName(file.name());
        if (file.parent().parent().name().equals("wps")) {
            return file.parent().name() + ":" + baseName;
        } else {
            return FilenameUtils.getBaseName(file.name());
        }
    }

    private Resource findFile(String name, String type, String extension) {
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptManager");
        try {
            if (name.contains(":")) {
                name = name.replace(":", File.separator);
            }
            Resource f = scriptManager.scriptFile(name, ScriptType.getByLabel(type), extension);
            return f;
        } catch (IOException ex) {
            LOGGER.warning(
                    String.format(
                            "Error finding file for name = %s, type = %s extension = %s because ",
                            name, type, extension, ex.getMessage()));
        }
        return null;
    }

    private String readFile(Resource file) {

        try (InputStream in = file.in()) {
            String s = IOUtils.toString(in);
            return s;
        } catch (IOException ex) {
            LOGGER.warning(
                    String.format(
                            "Error reading file '%s' because ", file.path(), ex.getMessage()));
        }
        return "";
    }

    private String findType(Resource file) {
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptManager");
        return scriptManager.getScriptType(file).getLabel();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public Resource getResource() {
        if (file == null) {
            this.file = findFile(name, type, extension);
        }
        return this.file;
    }

    @Deprecated
    public File getFile() {
        return getResource().file();
    }

    public String getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return "Script [extension="
                + extension
                + ", file="
                + file
                + ", name="
                + name
                + ", type="
                + type
                + "]";
    }
}
