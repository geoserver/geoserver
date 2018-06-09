/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Objects.equal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import org.locationtech.geogig.repository.RepositoryResolver;

public class RepositoryInfo implements Serializable, Cloneable {

    private static final long serialVersionUID = -5946705936987075713L;

    private String id;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *     repositories
     */
    private String parentDirectory;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *     repositories
     */
    private String name;

    private java.net.URI location;

    private String maskedLocation;

    /**
     * Stores the "nice" name for a repo. This is the name that is shown in the Repository list, as
     * well as what is stored in the GeoGIG repository config. It is transient, as we don't want to
     * serialize this. It's just a place holder for the name until it can be persisted into the repo
     * config.
     */
    private transient String repoName;

    private transient long lastModified;

    public RepositoryInfo() {
        this(null);
    }

    RepositoryInfo(String id) {
        this.id = id;
    }

    public @Override RepositoryInfo clone() {
        try {
            return (RepositoryInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }
    }

    private Object readResolve() {
        if (parentDirectory != null && name != null) {
            File file = new File(new File(parentDirectory), name).getAbsoluteFile();
            this.location = file.toURI();
            this.parentDirectory = null;
            this.name = null;
        }
        return this;
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public URI getLocation() {
        readResolve();
        return this.location;
    }

    public String getMaskedLocation() {
        if (maskedLocation == null) {
            // ensure the location is set
            getLocation();
            // get the masked version of the URI
            this.maskedLocation = GeoServerGeoGigRepositoryResolver.getURI(getRepoName());
        }
        return this.maskedLocation;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    @VisibleForTesting
    void setParentDirectory(String parent) {
        this.parentDirectory = parent;
    }

    @VisibleForTesting
    void setName(String name) {
        this.name = name;
    }

    public void setRepoName(String name) {
        this.repoName = name;
    }

    public String getRepoName() {
        if (this.location != null) {
            if (this.repoName == null) {
                // lookup the resolver
                if (RepositoryResolver.resolverAvailableForURIScheme(this.location.getScheme())) {
                    RepositoryResolver resolver = RepositoryResolver.lookup(this.location);
                    this.repoName = resolver.getName(this.location);
                }
            }
        }
        return this.repoName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepositoryInfo)) {
            return false;
        }
        RepositoryInfo r = (RepositoryInfo) o;
        return equal(getId(), r.getId()) && equal(getLocation(), r.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getLocation());
    }

    @Override
    public String toString() {
        return new StringBuilder("[id:")
                .append(getId())
                .append(", URI:")
                .append(getLocation())
                .append("]")
                .toString();
    }

    long getLastModified() {
        return lastModified;
    }

    void setLastModified(long timestamp) {
        this.lastModified = timestamp;
    }
}
