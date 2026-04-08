/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.List;

/** A simple implementation */
public class OperationMetadata {
    String name;
    List<DomainType> parameters = new ArrayList<>();
    List<DomainType> constraints = new ArrayList<>();
    boolean get = true;
    boolean post = true;

    public OperationMetadata(String name, boolean get, boolean post) {
        this.name = name;
        this.get = get;
        this.post = post;
    }

    public String getName() {
        return name;
    }

    public List<DomainType> getParameters() {
        return parameters;
    }

    public List<DomainType> getConstraints() {
        return constraints;
    }

    public boolean isGet() {
        return get;
    }

    public void setGet(boolean get) {
        this.get = get;
    }

    public boolean isPost() {
        return post;
    }

    public void setPost(boolean post) {
        this.post = post;
    }
}
