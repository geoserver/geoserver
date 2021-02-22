/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

public class AtomSearchResponse extends Response {

    public static final String MIME = "application/atom+xml";
    private GeoServer gs;

    public AtomSearchResponse(GeoServer gs) {
        super(SearchResults.class, MIME);
        this.gs = gs;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        SearchResults results = (SearchResults) value;

        try {
            AtomResultsTransformer transformer =
                    new AtomResultsTransformer(gs.getGlobal(), gs.getService(OSEOInfo.class));
            transformer.setIndentation(2);
            transformer.transform(results, output);
        } catch (TransformerException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return "search.xml";
    }
}
