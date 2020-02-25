/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;
import org.geotools.data.Parameter;
import org.opengis.feature.type.Name;

/**
 * Just a utility class to store some info associated to the new WPS Processes created dynamically
 * by the {@link RemoteProcessFactory}
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RemoteServiceDescriptor {

    private Name name;

    private String title;

    private String description;

    private Map<String, Parameter<?>> paramInfo;

    private Map<String, Parameter<?>> outputInfo;

    private Map<String, Object> metadata;

    /** */
    public RemoteServiceDescriptor(
            Name name,
            String title,
            String description,
            Map<String, Parameter<?>> paramInfo,
            Map<String, Parameter<?>> outputInfo,
            Map<String, Object> metadata) {
        super();
        this.name = name;
        this.title = title;
        this.description = description;
        this.paramInfo = paramInfo;
        this.outputInfo = outputInfo;
        this.metadata = metadata;
    }

    /** @return the name */
    public Name getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(Name name) {
        this.name = name;
    }

    /** @return the title */
    public String getTitle() {
        return title;
    }

    /** @param title the title to set */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the paramInfo */
    public Map<String, Parameter<?>> getParamInfo() {
        return paramInfo;
    }

    /** @param paramInfo the paramInfo to set */
    public void setParamInfo(Map<String, Parameter<?>> paramInfo) {
        this.paramInfo = paramInfo;
    }

    /** @return the outputInfo */
    public Map<String, Parameter<?>> getOutputInfo() {
        return outputInfo;
    }

    /** @param outputInfo the outputInfo to set */
    public void setOutputInfo(Map<String, Parameter<?>> outputInfo) {
        this.outputInfo = outputInfo;
    }

    /** @return the metadata */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /** @param metadata the metadata to set */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
