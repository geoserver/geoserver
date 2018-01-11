/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.io.Serializable;
import java.util.Map;

/**
 * This class is used to store the data to serialize to recreate previous get feature request
 *
 * @author sandr
 */
class RequestData implements Serializable {

    private static final long serialVersionUID = 6687946816946977568L;

    private Map kvp;

    private Map rawKvp;

    private String postRequest;

    public Map getKvp() {
        return kvp;
    }

    public void setKvp(Map kvp) {
        this.kvp = kvp;
    }

    public Map getRawKvp() {
        return rawKvp;
    }

    public void setRawKvp(Map rawKvp) {
        this.rawKvp = rawKvp;
    }

    public String getPostRequest() {
        return postRequest;
    }

    public void setPostRequest(String postRequest) {
        this.postRequest = postRequest;
    }
}
