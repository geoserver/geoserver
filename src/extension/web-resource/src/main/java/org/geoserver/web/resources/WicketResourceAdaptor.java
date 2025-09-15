/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URLConnection;
import java.time.Instant;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.geoserver.platform.resource.Resource;

/**
 * Adaptor for Resource -> wicket ResourceStream
 *
 * @author Niels Charlier
 */
public class WicketResourceAdaptor extends AbstractResourceStream {

    @Serial
    private static final long serialVersionUID = -1009868612769713937L;

    protected Resource resource;

    public WicketResourceAdaptor(Resource resource) {
        if (!resource.isInternal()) {
            // double check resource browser cannot be used to edit
            // files outside of resource store
            throw new IllegalStateException("Path location not supported by Resource Browser");
        }
        this.resource = resource;
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException {
        return resource.in();
    }

    @Override
    public String getContentType() {
        String mimeType = URLConnection.guessContentTypeFromName(resource.name());
        if (mimeType == null) {
            // try guessing from data
            try (InputStream is = new BufferedInputStream(resource.in())) {
                mimeType = URLConnection.guessContentTypeFromStream(is);
            } catch (IOException e) {
                // do nothing, we'll just use application/octet-stream
            }
        }
        return mimeType == null ? "application/octet-stream" : mimeType;
    }

    @Override
    public Instant lastModifiedTime() {
        return Instant.ofEpochMilli(resource.lastmodified());
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
