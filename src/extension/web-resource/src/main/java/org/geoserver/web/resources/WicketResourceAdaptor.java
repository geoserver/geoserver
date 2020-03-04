/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.geoserver.platform.resource.Resource;

/**
 * Adaptor for Resource -> wicket ResourceStream
 *
 * @author Niels Charlier
 */
public class WicketResourceAdaptor extends AbstractResourceStream {

    private static final long serialVersionUID = -1009868612769713937L;

    protected Resource resource;

    public WicketResourceAdaptor(Resource resource) {
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
    public Time lastModifiedTime() {
        return Time.millis(resource.lastmodified());
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
