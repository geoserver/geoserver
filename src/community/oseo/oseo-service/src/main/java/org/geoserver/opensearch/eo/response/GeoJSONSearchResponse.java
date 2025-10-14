/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.OpenSearchTemplates;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.api.data.Parameter;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.function.EnvFunction;

public class GeoJSONSearchResponse extends Response {

    public static final String MIME = "application/geo+json";
    public static final String OSEO_GEOJSON_PROFILE = "http://www.opengis.net/spec/owc-geojson/1.0/req/core";
    private final OpenSearchTemplates templates;
    private GeoServer gs;

    public GeoJSONSearchResponse(GeoServer gs, OpenSearchTemplates templates) {
        super(SearchResults.class, MIME);
        this.gs = gs;
        this.templates = templates;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException, ServiceException {
        SearchResults results = (SearchResults) value;

        try (GeoJSONWriter writer = new GeoJSONWriter(
                new JsonFactory().createGenerator(output, JsonEncoding.UTF8), TemplateIdentifier.GEOJSON)) {
            writer.startTemplateOutput(null);
            try (FeatureIterator features = results.getResults().features()) {
                while (features.hasNext()) {
                    Feature f = features.next();
                    RootBuilder builder = getTemplate(results.getRequest(), f);
                    builder.evaluate(writer, new TemplateBuilderContext(f));
                }
            }
            writer.writeEndArray();
            writeAdditionalFields(writer, results);
            writer.endTemplateOutput(null);
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            EnvFunction.clearLocalValues();
        }
    }

    private RootBuilder getTemplate(SearchRequest request, Feature feature) throws IOException {
        if (request.getParentIdentifier() == null) {
            String id = (String) feature.getProperty("identifier").getValue();
            return templates.getCollectionsTemplate(id);
        }
        return templates.getProductsTemplate(request.getParentIdentifier());
    }

    private void writeAdditionalFields(GeoJSONWriter w, SearchResults results) throws IOException {
        writeSimple(w, "id", getRequestedURL());
        if (results.getTotalResults() != null) writeSimple(w, "totalResults", results.getTotalResults());
        Query query = results.getRequest().getQuery();
        writeSimple(w, "itemsPerPage", query.getMaxFeatures());
        // OpenSearch is 1-based, not zero based
        writeSimple(w, "startIndex", Optional.ofNullable(query.getStartIndex()).orElse(0) + 1);

        w.writeElementName("queries", null);
        w.writeStartObject();
        w.writeElementName("request", null);
        writeQuery(w, results.getRequest());
        w.writeEndObject();

        w.writeElementName("properties", null);
        w.writeStartObject();
        writeCollectionProperties(results, w);
        w.writeEndObject();
    }

    private void writeSimple(GeoJSONWriter w, String fieldName, Object value) throws IOException {
        if (value != null) {
            w.writeElementName(fieldName, null);
            w.writeValue(value);
        }
    }

    private void writeCollectionProperties(SearchResults results, GeoJSONWriter w) throws IOException {
        OSEOInfo oseo = gs.getService(OSEOInfo.class);

        writeSimple(w, "title", oseo.getName() + " - Search Response");
        writeSimple(w, "creator", oseo.getName());

        w.writeElementName("authors", null);
        w.writeStartArray();
        w.writeStartObject();
        writeSimple(w, "name", gs.getSettings().getContact().getContactOrganization());
        writeSimple(w, "email", gs.getSettings().getContact().getContactEmail());
        writeSimple(w, "type", "Agent");
        w.writeEndObject();
        w.writeEndArray();

        writeSimple(w, "updated", new Date());
        writeSimple(w, "lang", "en");

        PaginationLinkBuilder builder = new PaginationLinkBuilder(results, oseo, GeoJSONSearchResponse.MIME);
        w.writeElementName("links", null);
        w.writeStartObject();
        writeSingleLink(w, "profiles", new Link(OSEO_GEOJSON_PROFILE));
        writeSingleLink(w, "first", new Link(builder.getFirst(), "First results", GeoJSONSearchResponse.MIME));
        if (builder.getNext() != null)
            writeSingleLink(w, "next", new Link(builder.getNext(), "Next results", GeoJSONSearchResponse.MIME));
        if (builder.getPrevious() != null)
            writeSingleLink(
                    w, "previous", new Link(builder.getPrevious(), "Previous results", GeoJSONSearchResponse.MIME));
        if (builder.getLast() != null)
            writeSingleLink(w, "last", new Link(builder.getLast(), "Last results", GeoJSONSearchResponse.MIME));
        w.writeEndObject();
    }

    private void writeSingleLink(GeoJSONWriter w, String category, Link link) throws IOException {
        w.writeElementName(category, null);
        w.writeStartArray();
        w.writeStartObject();
        writeSimple(w, "href", link.getHref());
        if (link.getType() != null) writeSimple(w, "type", link.getType());
        if (link.getTitle() != null) writeSimple(w, "title", link.getTitle());
        w.writeEndObject();
        w.writeEndArray();
    }

    private String getRequestedURL() {
        HttpServletRequest request = Dispatcher.REQUEST.get().getHttpRequest();
        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    private void writeQuery(GeoJSONWriter w, SearchRequest request) throws IOException {
        w.writeStartObject();

        // TODO: there are likely mappings to be done here, maybe do them in the Parameter
        // declaration directly
        for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
            Parameter k = entry.getKey();
            String v = entry.getValue();
            String fieldName = k.getName();
            String prefix = (String) k.metadata.get(OpenSearchParameters.PARAM_PREFIX);
            if ("eop".equals(prefix)) prefix = "eo"; // eop is used only internally
            if (prefix != null) fieldName = prefix + ":" + fieldName;
            writeSimple(w, fieldName, v);
        }

        w.writeEndObject();
    }
}
