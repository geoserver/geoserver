/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.Map;

/**
 * Represents the tool configuration as a whole. Only used for XStream driven de-serialization.
 *
 * @author Andrea Aime - OpenGeo
 * @author Stefano Costa - GeoSolutions
 */
public class ToolConfiguration {

    protected String executable;
    protected Map<String, String> environment;
    protected Format[] formats;

    public ToolConfiguration() {}

    public ToolConfiguration(String executable, Map<String, String> environment, Format[] formats) {
        super();
        this.executable = executable;
        this.environment = environment;
        this.formats = formats;
    }

    /** @return the executable */
    public String getExecutable() {
        return executable;
    }

    /** @param executable the executable to set */
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    /** @return the environment */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /** @param environment the environment to set */
    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    /** @return the formats */
    public Format[] getFormats() {
        return formats;
    }

    /** @param formats the formats to set */
    public void setFormats(Format[] formats) {
        this.formats = formats;
    }
}
