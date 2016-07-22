/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Objects.equal;

import java.io.File;
import java.io.Serializable;
import java.net.URI;

import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

public class RepositoryInfo implements Serializable {

    private static final long serialVersionUID = -5946705936987075713L;

    private String id;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *             repositories
     */
    private String parentDirectory;

    /**
     * @deprecated field to support deserialization of old format when it only allowed file:
     *             repositories
     */
    private String name;

    private java.net.URI location;

    /**
     * Stores the "nice" name for a repo. This is the name that is shown in the Repository list, as
     * well as what is stored in the GeoGIG repository config. It is transient, as we don't want to
     * serialize this. It's just a place holder for the name until it can be persisted into the repo
     * config.
     */
    private transient String repoName;

    public RepositoryInfo() {
        this(null);
    }

    RepositoryInfo(String id) {
        this.id = id;
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
        if (this.repoName != null) {
            return repoName;
        }
        if (this.location != null) {
            Repository repo = null;
            try {
                // lookup the resolver
                RepositoryResolver resolver = RepositoryResolver.lookup(this.location);
                // if the repo exists, get the name from it
                if (resolver.repoExists(this.location)) {
                    // it exists, load it and fetch the name
                    repo = RepositoryResolver.load(this.location);
                    return repo.command(ResolveRepositoryName.class).call();
                }
                // the repo doesn't exist, derive the name from the location
                return resolver.getName(this.location);
            } catch (RepositoryConnectionException e) {
                Throwables.propagate(e);
            } finally {
                if (repo != null) {
                    repo.close();
                }
            }
        }
        return null;
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
        return new StringBuilder("[id:").append(getId()).append(", URI:").append(getLocation())
                .append("]").toString();
    }
}
