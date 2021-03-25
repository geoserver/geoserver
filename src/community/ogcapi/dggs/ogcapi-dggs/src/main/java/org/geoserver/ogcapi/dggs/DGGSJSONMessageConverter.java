package org.geoserver.ogcapi.dggs;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import net.sf.json.util.JSONBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.features.FeaturesResponse;
import org.geoserver.ogcapi.features.RFCGeoJSONFeaturesResponse;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Fork of {@link RFCGeoJSONFeaturesResponse} dealing with the unique elements of a DGGS */
@Component
public class DGGSJSONMessageConverter implements HttpMessageConverter<FeaturesResponse> {

    public static final String DGGS_JSON_MIME = "application/dggs+json";
    public static MediaType DGGS_JSON_TYPE = MediaType.parseMediaType(DGGS_JSON_MIME);
    private final GeoServer gs;

    public DGGSJSONMessageConverter(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public boolean canRead(Class<?> aClass, MediaType mediaType) {
        // write only
        return false;
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        // first match class and media type, bail out if not a match
        if (!FeaturesResponse.class.isAssignableFrom(aClass)) return false;

        if (mediaType != null && !DGGS_JSON_TYPE.isCompatibleWith(mediaType)) return false;

        // then make this one specific to the DGGS service
        Request request = Dispatcher.REQUEST.get();
        return request != null && "DGGS".equals(request.getService());
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(DGGS_JSON_TYPE);
    }

    @Override
    public FeaturesResponse read(
            Class<? extends FeaturesResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public void write(
            FeaturesResponse featureResponse,
            MediaType mediaType,
            HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        Writer writer = getWriter(httpOutputMessage);
        JSONBuilder jsonWriter = new JSONBuilder(writer);
        jsonWriter.object().key("type").value("FeatureCollection");
        jsonWriter.key("features");
        jsonWriter.array();

        // DGGS always generates a single collection in output
        FeatureCollectionResponse response = featureResponse.getResponse();
        GetFeatureRequest request = GetFeatureRequest.adapt(featureResponse.getRequest());
        SimpleFeatureCollection fc = (SimpleFeatureCollection) response.getFeature().get(0);
        writeCollection(fc, jsonWriter);

        jsonWriter.endArray(); // end features

        // write the set of collection wide informations
        // get feature count for request
        BigInteger totalNumberOfFeatures =
                Optional.ofNullable(response.getTotalNumberOfFeatures())
                        .filter(n -> n.signum() >= 0)
                        .orElse(null);
        BigInteger featureCount =
                Optional.ofNullable(response.getNumberOfFeatures())
                        .filter(n -> n.signum() >= 0)
                        .orElse(null);
        writeCollectionCounts(totalNumberOfFeatures, featureCount, jsonWriter);
        writeCollectionTimeStamp(jsonWriter);
        writeLinks(response, request, jsonWriter, null);

        jsonWriter.endObject(); // end featurecollection
        writer.flush();
    }

    private void writeCollection(SimpleFeatureCollection fc, JSONBuilder jsonWriter) {
        SimpleFeatureType fType = fc.getSchema();
        List<AttributeDescriptor> types = fType.getAttributeDescriptors();
        long featureCount = 0;
        try (FeatureIterator iterator = fc.features()) {
            // encode each simple feature
            while (iterator.hasNext()) {
                // get next simple feature
                SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                featureCount++;
                // start writing the JSON feature object
                jsonWriter.object();
                jsonWriter.key("type").value("Feature");

                // write out the geometry
                jsonWriter.key("geometry").object();
                jsonWriter.key("type").value("polygon");
                jsonWriter.key("identifiers");
                jsonWriter.array();
                jsonWriter.value(simpleFeature.getAttribute("zoneId"));
                jsonWriter.endArray();
                jsonWriter.endObject();

                // start writing feature properties JSON object
                jsonWriter.key("properties");
                jsonWriter.object();
                for (int j = 0; j < types.size(); j++) {
                    Object value = simpleFeature.getAttribute(j);
                    AttributeDescriptor ad = types.get(j);
                    String attributeName = ad.getLocalName();
                    if ("zoneId".equals(attributeName) || ad instanceof GeometryDescriptor) {
                        continue; // skip the geometry and the identifier
                    }
                    if (Date.class.isAssignableFrom(ad.getType().getBinding())
                            && TemporalUtils.isDateTimeFormatEnabled()) {
                        // Temporal types print handling
                        jsonWriter.key(attributeName);
                        jsonWriter.value(TemporalUtils.printDate((Date) value));
                    } else {
                        jsonWriter.key(attributeName);
                        if ((value instanceof Double && Double.isNaN((Double) value))
                                || value instanceof Float && Float.isNaN((Float) value)) {
                            jsonWriter.value(null);
                        } else if ((value instanceof Double
                                        && ((Double) value) == Double.POSITIVE_INFINITY)
                                || value instanceof Float
                                        && ((Float) value) == Float.POSITIVE_INFINITY) {
                            jsonWriter.value("Infinity");
                        } else if ((value instanceof Double
                                        && ((Double) value) == Double.NEGATIVE_INFINITY)
                                || value instanceof Float
                                        && ((Float) value) == Float.NEGATIVE_INFINITY) {
                            jsonWriter.value("-Infinity");
                        } else {
                            jsonWriter.value(value);
                        }
                    }
                }
                jsonWriter.endObject(); // end the properties

                // writeExtraFeatureProperties(simpleFeature, operation, jsonWriter);

                jsonWriter.endObject(); // end the feature
            }
        }
    }

    protected void writeCollectionCounts(
            BigInteger featureCount, BigInteger numberReturned, JSONBuilder jsonWriter) {
        // counts
        if (featureCount != null) {
            jsonWriter.key("numberMatched").value(featureCount);
        }
        jsonWriter.key("numberReturned").value(numberReturned);
    }

    private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
        return new OutputStreamWriter(
                outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
    }

    private static Charset getCharset(HttpHeaders headers) {
        Charset charset =
                (headers.getContentType() != null ? headers.getContentType().getCharset() : null);
        return (charset != null ? charset : StandardCharsets.UTF_8);
    }

    /** Writes a WFS3 compliant timeStamp collection attribute */
    protected void writeCollectionTimeStamp(JSONBuilder jw) {
        jw.key("timeStamp").value(new ISO8601Formatter().format(new Date()));
    }

    private void writeLinks(
            FeatureCollectionResponse response,
            GetFeatureRequest request,
            JSONBuilder jw,
            String featureId) {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        FeatureTypeInfo featureType = getFeatureType(request);
        String baseUrl = request.getBaseUrl();
        jw.key("links");
        jw.array();
        // paging links
        if (response != null) {
            if (response.getPrevious() != null) {
                writeLink(jw, "Previous page", DGGS_JSON_MIME, "prev", response.getPrevious());
            }
            if (response.getNext() != null) {
                writeLink(jw, "Next page", DGGS_JSON_MIME, "next", response.getNext());
            }
        }
        // alternate/self links
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            Map<String, String> kvp = APIRequestInfo.get().getSimpleQueryMap();
            kvp.put("f", format.toString());
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            APIRequestInfo.get().getRequestPath(),
                            kvp,
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(DGGS_JSON_MIME)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        // backpointer to the collection
        String basePath =
                "ogc/dggs/collections/" + ResponseUtils.urlEncode(featureType.prefixedName());
        for (MediaType format :
                requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href =
                    ResponseUtils.buildURL(
                            baseUrl,
                            basePath,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(jw, linkTitle, format.toString(), linkType, href);
        }
        jw.endArray();
    }

    protected void writeLink(
            JSONBuilder jw, String title, String mimeType, String rel, String href) {
        if (href != null) {
            jw.object();
            if (title != null) {
                jw.key("title").value(title);
            }
            if (mimeType != null) {
                jw.key("type").value(mimeType);
            }
            if (rel != null) {
                jw.key("rel").value(rel);
            }
            jw.key("href").value(href);
            jw.endObject();
        }
    }

    private FeatureTypeInfo getFeatureType(GetFeatureRequest request) {
        Query query = request.getQueries().get(0);
        QName typeName = query.getTypeNames().get(0);
        return gs.getCatalog()
                .getFeatureTypeByName(
                        new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }
}
