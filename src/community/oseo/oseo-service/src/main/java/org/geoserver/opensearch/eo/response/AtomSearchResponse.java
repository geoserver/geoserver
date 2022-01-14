/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.FreemarkerTemplateSupport;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

public class AtomSearchResponse extends Response {

    private static final Logger LOGGER = Logging.getLogger(AtomSearchResponse.class);
    public static final String MIME = "application/atom+xml";
    private final FreemarkerTemplateSupport freemarkerTemplates;
    private GeoServer gs;

    public AtomSearchResponse(GeoServer gs, FreemarkerTemplateSupport freemarkerTemplates) {
        super(SearchResults.class, MIME);
        this.gs = gs;
        this.freemarkerTemplates = freemarkerTemplates;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        SearchResults results = (SearchResults) value;

        TemplatesProcessor processor =
                new TemplatesProcessor(
                        freemarkerTemplates, gs.getGlobal(), gs.getService(OSEOInfo.class));
        String result = null;
        try {
            result = processor.processTemplate(results);
        } catch (TemplateException e) {
            LOGGER.warning("Error processing template: " + e.getMessage());
        }
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output);
        outputStreamWriter.write(result);
        outputStreamWriter.flush();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return "search.xml";
    }
}
