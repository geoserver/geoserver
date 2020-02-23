/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;
import org.opengis.feature.type.Name;

/**
 * Base class describing the remote requests.
 *
 * <p>If a load-balancing strategy is implemented by the concrete RemoteProcessClient, there is the
 * possibility to use a queue of pending requests to be executed as soon as a new remote processing
 * node becomes available.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RemoteRequestDescriptor {

    private Name servicename;

    private Map<String, Object> input;

    private Map<String, Object> metadata;

    private String pid;

    private String baseURL;

    /** */
    public RemoteRequestDescriptor(
            Name servicename,
            Map<String, Object> input,
            Map<String, Object> metadata,
            String pid,
            String baseURL) {
        super();
        this.servicename = servicename;
        this.input = input;
        this.metadata = metadata;
        this.pid = pid;
        this.baseURL = baseURL;
    }

    /** @return the servicename */
    public Name getServicename() {
        return servicename;
    }

    /** @param servicename the servicename to set */
    public void setServicename(Name servicename) {
        this.servicename = servicename;
    }

    /** @return the input */
    public Map<String, Object> getInput() {
        return input;
    }

    /** @param input the input to set */
    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    /** @return the metadata */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /** @param metadata the metadata to set */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /** @return the pid */
    public String getPid() {
        return pid;
    }

    /** @param pid the pid to set */
    public void setPid(String pid) {
        this.pid = pid;
    }

    /** @return the baseURL */
    public String getBaseURL() {
        return baseURL;
    }

    /** @param baseURL the baseURL to set */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }
}
