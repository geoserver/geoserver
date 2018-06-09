/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Import data that will be fetched from a remote location during the init phase, similar to an
 * upload, but operated by GeoServer, and turned into another type of import data
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RemoteData extends ImportData {

    private static final long serialVersionUID = -1748855285827081507L;

    String location;

    String domain;

    String username;

    String password;

    public RemoteData(String location) {
        this.location = location;
    }

    public RemoteData(RemoteData other) {
        super(other);
        this.location = other.location;
        this.domain = other.domain;
        this.username = other.username;
        this.password = other.password;
    }

    @Override
    public String getName() {

        return location;
    }

    /**
     * The location from which to fetch the data
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /** @return the username */
    public String getUsername() {
        return username;
    }

    /** @param username the username to set */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return the password */
    public String getPassword() {
        return password;
    }

    /** @param password the password to set */
    public void setPassword(String password) {
        this.password = password;
    }

    public ImportData resolve(Importer importer) throws IOException {
        // prepare the target
        Directory target = Directory.createNew(importer.getUploadRoot());

        FileSystemManager manager = null;
        FileObject fo = null;
        try {
            manager = VFS.getManager();

            if (username != null) {
                StaticUserAuthenticator auth =
                        new StaticUserAuthenticator(domain, username, password);
                FileSystemOptions opts = new FileSystemOptions();
                DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
                fo = manager.resolveFile(location, opts);
            } else {
                fo = manager.resolveFile(location);
            }

            target.accept(fo);

        } finally {
            if (fo != null) {
                FileSystem fs = fo.getFileSystem();
                fo.close();
                manager.closeFileSystem(fs);
            }
        }

        return target;
    }

    /** @return the domain */
    public String getDomain() {
        return domain;
    }

    /** @param domain the domain to set */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RemoteData other = (RemoteData) obj;
        if (domain == null) {
            if (other.domain != null) return false;
        } else if (!domain.equals(other.domain)) return false;
        if (location == null) {
            if (other.location != null) return false;
        } else if (!location.equals(other.location)) return false;
        if (password == null) {
            if (other.password != null) return false;
        } else if (!password.equals(other.password)) return false;
        if (username == null) {
            if (other.username != null) return false;
        } else if (!username.equals(other.username)) return false;
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RemoteData [location="
                + location
                + ", domain="
                + domain
                + ", username="
                + username
                + ", password="
                + password
                + "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        RemoteData encrypted = new RemoteData(this);
        encrypted.setPassword(manager.getConfigPasswordEncryptionHelper().encode(password));
        encrypted.defaultWriteObject(out);
    }

    private void defaultWriteObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        this.password = manager.getConfigPasswordEncryptionHelper().decode(this.password);
    }
}
