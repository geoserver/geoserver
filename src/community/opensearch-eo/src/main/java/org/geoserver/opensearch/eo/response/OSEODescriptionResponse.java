/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.geoserver.opensearch.eo.OSEODescription;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Writes out a OSDD with the help of OSEODescriptionTransformer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODescriptionResponse extends Response {

    public OSEODescriptionResponse() {
        super(OSEODescription.class, "text/xml");
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        OSEODescription description = (OSEODescription) value;

        try {
            OSEODescriptionTransformer transformer = new OSEODescriptionTransformer();
            transformer.transform(description, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }

    }

}
