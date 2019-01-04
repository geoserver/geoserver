/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Basic implementation for Resource Cache.
 *
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class SimpleResourceCache implements ResourceCache {
    File base;
    boolean cacheChildren = true;

    public SimpleResourceCache() {}

    public SimpleResourceCache(File base) {
        this.base = base;
    }

    public File getBase() {
        return base;
    }

    public void setBase(File base) {
        this.base = base;
    }

    public boolean isCacheChildren() {
        return cacheChildren;
    }

    public void setCacheChildren(boolean cacheChildren) {
        this.cacheChildren = cacheChildren;
    }

    void cacheData(Resource res, File file) throws IOException {
        assert res.getType() == Type.RESOURCE;
        try (OutputStream out = new FileOutputStream(file)) {
            try (InputStream in = res.in()) {
                IOUtils.copy(in, out);
            }
        }
    }

    void cacheChildren(Resource res, File file) throws IOException {
        assert res.getType() == Type.DIRECTORY;

        for (Resource child : res.list()) {
            cache(child, false);
        }
        ;
    }

    @Override
    public File cache(Resource res, boolean createDirectory) throws IOException {
        String path = res.path();
        long mtime = res.lastmodified();
        File cached = new File(base, path);
        if (!cached.exists() || cached.lastModified() < mtime) {
            Resource.Type type = res.getType();
            switch (type) {
                case RESOURCE:
                    cached.getParentFile().mkdirs();
                    cacheData(res, cached);
                    break;
                case DIRECTORY:
                    cached.mkdirs();
                    if (cacheChildren) {
                        cacheChildren(res, cached);
                    }
                    break;
                case UNDEFINED:
                    if (createDirectory) {
                        cached.mkdirs();
                    } else {
                        cached.getParentFile().mkdirs();
                    }
                    break;
            }
        }
        return cached;
    }
}
