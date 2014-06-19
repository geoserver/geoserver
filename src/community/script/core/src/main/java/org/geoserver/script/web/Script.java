/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptType;
import org.geotools.util.logging.Logging;

public class Script implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logging.getLogger(Script.class);

    private String name;

    private String type;

    private String extension;

    private File file;

    private String contents;

    public Script() {
    }

    public Script(File file) {
        this.file = file;
        this.name = getNameFromFile(file);
        this.type = findType(file);
        this.extension = FilenameUtils.getExtension(file.getName());
        this.contents = readFile(file);
    }

    public Script(String name, String type, String extension, String contents) {
        this.file = findFile(name, type, extension);
        this.name = name;
        this.type = type;
        this.extension = extension;
        this.contents = contents;
    }

    private String getNameFromFile(File file) {
        String baseName = FilenameUtils.getBaseName(file.getName());
        if (file.getParentFile().getParentFile().getName().equals("wps")) {
            return file.getParentFile().getName() + ":" + baseName;
        } else {
            return FilenameUtils.getBaseName(file.getName());
        }
    }

    private File findFile(String name, String type, String extension) {
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptMgr");
        try {
            if (name.contains(":")) {
                name = name.replace(":",File.separator);
            }
            File f = scriptManager.getFile(name, ScriptType.getByLabel(type), extension);
            return f;
        } catch (IOException ex) {
            LOGGER.warning(String.format(
                    "Error finding file for name = %s, type = %s extension = %s because ", name,
                    type, extension, ex.getMessage()));
        }
        return null;
    }

    private String readFile(File file) {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException ex) {
            LOGGER.warning(String.format("Error reading file '%s' because ",
                    file.getAbsolutePath(), ex.getMessage()));
        }
        return "";
    }

    private String findType(File file) {
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptMgr");
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

    public File getFile() {
        if (file == null) {
            this.file = findFile(name, type, extension);
        }
        return this.file;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return "Script [extension=" + extension + ", file=" + file + ", name=" + name + ", type="
                + type + "]";
    }

}
