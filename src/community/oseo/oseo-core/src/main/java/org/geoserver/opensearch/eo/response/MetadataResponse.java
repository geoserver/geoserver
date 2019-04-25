/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.MetadataResults;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Writes out metadata in the requested format. Trusts that the steps before (kvp parsing, request
 * processing) have verified the requested mime type makes sense
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MetadataResponse extends Response {

    public MetadataResponse() {
        super(MetadataResults.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return ((MetadataResults) value).getRequest().getHttpAccept();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        MetadataResults results = (MetadataResults) value;
        IOUtils.write(results.getMetadata(), output, "UTF-8");
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return ((MetadataResults) value).getRequest().getId().toLowerCase() + "-metadata.xml";
    }
}
