/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Objects.equal;

import java.io.File;
import java.io.Serializable;

import com.google.common.base.Objects;

public class RepositoryInfo implements Serializable {

    private static final long serialVersionUID = -5946705936987075713L;

    private String id;

    private String parentDirectory;

    private String name;

    public RepositoryInfo() {
        this(null);
    }

    RepositoryInfo(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        if (parentDirectory == null || name == null) {
            return null;
        }
        return new File(parentDirectory, name).getAbsolutePath();
    }

    public void setLocation(String location) {
        File repoDir = new File(location);
        setName(repoDir.getName());
        setParentDirectory(repoDir.getParent());
    }

    public void setParentDirectory(String parent) {
        this.parentDirectory = parent;
    }

    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepositoryInfo)) {
            return false;
        }
        RepositoryInfo r = (RepositoryInfo) o;
        return equal(getId(), r.getId()) && equal(getName(), r.getName())
                && equal(getParentDirectory(), r.getParentDirectory());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getName(), getParentDirectory());
    }

    @Override
    public String toString() {
        return new StringBuilder("[name:").append(getName()).append(", parent:")
                .append(getParentDirectory()).append("]").toString();
    }
}
