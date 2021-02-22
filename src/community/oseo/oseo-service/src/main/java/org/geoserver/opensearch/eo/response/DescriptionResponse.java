/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEODescription;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Writes out a OSDD with the help of OSEODescriptionTransformer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescriptionResponse extends Response {

    public static final String OS_DESCRIPTION_MIME = "application/opensearchdescription+xml";
    private final GeoServer gs;

    public DescriptionResponse(GeoServer gs) {
        super(OSEODescription.class, OS_DESCRIPTION_MIME);
        this.gs = gs;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return OS_DESCRIPTION_MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        OSEODescription description = (OSEODescription) value;

        try {
            DescriptionTransformer transformer =
                    new DescriptionTransformer(gs.getService(OSEOInfo.class));
            transformer.setIndentation(2);
            transformer.transform(description, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return "description.xml";
    }
}
