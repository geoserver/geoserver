/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;

/**
 * Adaptor from Geoserver resource to Spring Resource
 *
 * @author Niels Charlier
 */
public class SpringResourceAdaptor implements org.springframework.core.io.Resource {

    /**
     * Spring Resource is made relative to Data Directory if path is relative.
     *
     * @param resource Spring resource
     * @param store the Resource Store
     * @return Spring resource relative to Data Directory
     */
    public static org.springframework.core.io.Resource relative(
            org.springframework.core.io.Resource resource, ResourceStore store) throws IOException {
        File f = resource.getFile();
        if (f != null) {
            if (!f.isAbsolute()) {
                // make relative to data directory -- or create file from resource store
                Resource res = store.get(Paths.convert(f.getPath()));
                return new SpringResourceAdaptor(res);
            } else {
                return new SpringResourceAdaptor(Files.asResource(f));
            }

        } else {
            return resource;
        }
    }

    private Resource resource;

    public SpringResourceAdaptor(Resource resource) {
        this.resource = resource;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return resource.in();
    }

    @Override
    public boolean exists() {
        return Resources.exists(resource);
    }

    @Override
    public boolean isReadable() {
        return Resources.canRead(resource);
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        return Resources.find(resource).toURI().toURL();
    }

    @Override
    public URI getURI() throws IOException {
        return Resources.find(resource).toURI();
    }

    @Override
    public File getFile() throws IOException {
        return Resources.find(resource);
    }

    @Override
    public long contentLength() throws IOException {
        return Resources.find(resource).length();
    }

    @Override
    public long lastModified() throws IOException {
        return resource.lastmodified();
    }

    @Override
    public org.springframework.core.io.Resource createRelative(String relativePath)
            throws IOException {
        return new SpringResourceAdaptor(resource.get(relativePath));
    }

    @Override
    public String getFilename() {
        return resource.name();
    }

    @Override
    public String getDescription() {
        return resource.path();
    }

    public Resource getResource() {
        return resource;
    }
}
