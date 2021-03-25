/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.QuicklookResults;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Writes out metadata in the requested format. Trusts that the steps before (kvp parsing, request
 * processing) have verified the requested mime type makes sense
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QuicklookResponse extends Response {

    public QuicklookResponse() {
        super(QuicklookResults.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return ((QuicklookResults) value).getMimeType();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        QuicklookResults results = (QuicklookResults) value;
        IOUtils.write(results.getPayload(), output);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        final QuicklookResults results = (QuicklookResults) value;
        String mime = results.getMimeType();
        String extension = ".dat";
        int idx = mime.lastIndexOf("/");
        if (mime.startsWith("image") && idx >= 0 && idx < mime.length() - 1) {
            extension = mime.substring(idx + 1);
        }
        return results.getRequest().getId().toLowerCase() + "." + extension;
    }
}
