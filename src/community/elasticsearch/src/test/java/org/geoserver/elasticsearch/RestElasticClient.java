/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.geotools.data.elasticsearch.ElasticDataStoreFactory;
import org.geotools.data.elasticsearch.ElasticResponse;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** ElasticSearch client implementation using the REST API */
public class RestElasticClient implements ElasticClient {

    static final double DEFAULT_VERSION = 7.0;

    private static final Logger LOGGER = Logging.getLogger(RestElasticClient.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final RestClient client;

    private final RestClient proxyClient;

    private final boolean enableRunAs;

    private final int responseBufferLimit;

    private final ObjectMapper mapper;

    private Double version;

    public RestElasticClient(RestClient client) {
        this(client, null, false, (Integer) ElasticDataStoreFactory.RESPONSE_BUFFER_LIMIT.sample);
    }

    public RestElasticClient(
            RestClient client,
            RestClient proxyClient,
            boolean enableRunAs,
            int responseBufferLimit) {
        this.client = client;
        this.proxyClient = proxyClient;
        this.responseBufferLimit = responseBufferLimit;
        this.mapper = new ObjectMapper();
        this.mapper.setDateFormat(DATE_FORMAT);
        this.enableRunAs = enableRunAs;
    }

    @Override
    public double getVersion() {
        if (version != null) {
            return version;
        }

        final Pattern pattern = Pattern.compile("(\\d+\\.\\d+)\\.\\d+");
        try {
            final Response response = performRequest("GET", "/", null, true);
            try (final InputStream inputStream = response.getEntity().getContent()) {
                Map<String, Object> info =
                        mapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> ver =
                        (Map<String, Object>) info.getOrDefault("version", Collections.emptyMap());
                final Matcher m = pattern.matcher((String) ver.get("number"));
                if (!m.find()) {
                    version = DEFAULT_VERSION;
                } else {
                    version = Double.valueOf(m.group(1));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error getting server version: " + e);
            version = DEFAULT_VERSION;
        }

        return version;
    }

    @Override
    public List<String> getTypes(String indexName) throws IOException {
        return new ArrayList<>(getMappings(indexName, null).keySet());
    }

    @Override
    public Map<String, Object> getMapping(String indexName, String type) throws IOException {
        final Map<String, ElasticMappings.Mapping> mappings = getMappings(indexName, type);
        final Map<String, Object> properties;
        if (getVersion() < 7 && mappings.containsKey(type)) {
            properties = mappings.get(type).getProperties();
        } else if (getVersion() >= 7) {
            final ElasticMappings.Mapping mapping =
                    mappings.values().stream().findFirst().orElse(null);
            properties = mapping != null ? mapping.getProperties() : null;
        } else {
            properties = null;
        }
        return properties;
    }

    private Map<String, ElasticMappings.Mapping> getMappings(String indexName, String type)
            throws IOException {
        final Response response;
        try {
            final StringBuilder path = new StringBuilder("/").append(indexName).append("/_mapping");
            if (type != null && getVersion() < 7) {
                path.append("/").append(type);
            }
            response = performRequest("GET", path.toString(), null, true);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return Collections.emptyMap();
            }
            throw e;
        }

        final String aliasedIndex = getIndices(indexName).stream().findFirst().orElse(null);

        try (final InputStream inputStream = response.getEntity().getContent()) {
            final Map<String, ElasticMappings> values;
            if (getVersion() < 7) {
                values =
                        this.mapper.readValue(
                                inputStream, new TypeReference<Map<String, ElasticMappings>>() {});
            } else {
                final Map<String, ElasticMappings.Untyped> res =
                        this.mapper.readValue(
                                inputStream,
                                new TypeReference<Map<String, ElasticMappings.Untyped>>() {});
                values = new HashMap<>();
                for (final Entry<String, ElasticMappings.Untyped> entry : res.entrySet()) {
                    final ElasticMappings mappings = new ElasticMappings();
                    mappings.setMappings(new HashMap<>());
                    if (aliasedIndex != null && aliasedIndex.equals(entry.getKey())) {
                        mappings.getMappings().put(aliasedIndex, entry.getValue().getMappings());
                        values.put(aliasedIndex, mappings);
                    } else {
                        mappings.getMappings().put(indexName, entry.getValue().getMappings());
                        values.put(entry.getKey(), mappings);
                    }
                }
            }
            final Map<String, ElasticMappings.Mapping> mappings;
            if (values.containsKey(indexName)) {
                mappings = values.get(indexName).getMappings();
            } else {
                if (values.containsKey(aliasedIndex)) {
                    mappings = values.get(aliasedIndex).getMappings();
                } else if (!values.isEmpty()) {
                    mappings = values.values().iterator().next().getMappings();
                } else {
                    LOGGER.severe("No types found for index/alias " + indexName);
                    mappings = Collections.emptyMap();
                }
            }
            return mappings;
        }
    }

    @Override
    public ElasticResponse search(String searchIndices, String type, ElasticRequest request)
            throws IOException {
        final StringBuilder pathBuilder = new StringBuilder("/" + searchIndices);
        if (getVersion() < 7) {
            pathBuilder.append("/" + type);
        }
        pathBuilder.append("/_search");

        final Map<String, Object> requestBody = new HashMap<>();

        if (request.getSize() != null) {
            requestBody.put("size", request.getSize());
        }

        if (request.getFrom() != null) {
            requestBody.put("from", request.getFrom());
        }

        if (request.getScroll() != null) {
            pathBuilder.append("?scroll=").append(request.getScroll()).append("s");
        }

        final List<String> sourceIncludes = request.getSourceIncludes();
        if (sourceIncludes.size() == 1) {
            requestBody.put("_source", sourceIncludes.get(0));
        } else if (!sourceIncludes.isEmpty()) {
            requestBody.put("_source", sourceIncludes);
        }

        if (!request.getFields().isEmpty()) {
            final String key = getVersion() >= 5 ? "stored_fields" : "fields";
            requestBody.put(key, request.getFields());
        }

        if (!request.getSorts().isEmpty()) {
            requestBody.put("sort", request.getSorts());
        }

        if (request.getQuery() != null) {
            requestBody.put("query", request.getQuery());
        }

        if (request.getAggregations() != null) {
            requestBody.put("aggregations", request.getAggregations());
        }

        return parseResponse(performRequest("POST", pathBuilder.toString(), requestBody));
    }

    @Override
    public void addTextAttribute(String index, String attributeName) throws IOException {
        Map<String, Object> requestBodyConfig = new HashMap<>();
        // this avoids issues with disk space thresholds
        requestBodyConfig.put(
                "transient",
                Collections.singletonMap(
                        "cluster.routing.allocation.disk.threshold_enabled", false));
        performRequest("PUT", "_cluster/settings", requestBodyConfig, true);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(
                "properties",
                Collections.singletonMap(attributeName, Collections.singletonMap("type", "text")));
        performRequest("PUT", "/" + index + "/_mapping", requestBody);
    }

    private Response performRequest(
            String method, String path, Map<String, Object> requestBody, boolean isAdmin)
            throws IOException {
        final HttpEntity entity;
        if (requestBody != null) {
            final byte[] data = this.mapper.writeValueAsBytes(requestBody);
            entity = new ByteArrayEntity(data, ContentType.APPLICATION_JSON);
        } else {
            entity = null;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Method: " + method);
            LOGGER.fine("Path: " + path);
            final String requestString =
                    this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            LOGGER.fine("RequestBody: " + requestString);
        }

        @SuppressWarnings("PMD.CloseResource") // not managed here
        final RestClient client =
                isAdmin || this.proxyClient == null ? this.client : this.proxyClient;

        final Request request = new Request(method, path);
        request.setEntity(entity);
        final RequestOptions.Builder optionsBuilder = RequestOptions.DEFAULT.toBuilder();
        // Set the response buffer limit, default is 100MB
        optionsBuilder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(
                        responseBufferLimit));
        if (!isAdmin && enableRunAs) {
            final SecurityContext ctx = SecurityContextHolder.getContext();
            final Authentication auth = ctx.getAuthentication();
            if (auth == null) {
                throw new IllegalStateException("Authentication could not be determined!");
            }
            if (!auth.isAuthenticated()) {
                throw new IllegalStateException(
                        String.format("User is not authenticated: %s", auth.getName()));
            }
            optionsBuilder.addHeader(RUN_AS, auth.getName());
            LOGGER.fine(String.format("Performing request on behalf of user %s", auth.getName()));
        } else {
            LOGGER.fine(
                    String.format(
                            "Performing request with %s credentials", isAdmin ? "user" : "proxy"));
        }
        request.setOptions(optionsBuilder);
        final Response response = client.performRequest(request);
        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new IOException(
                    "Error executing request: " + response.getStatusLine().getReasonPhrase());
        }
        return response;
    }

    Response performRequest(String method, String path, Map<String, Object> requestBody)
            throws IOException {
        return performRequest(method, path, requestBody, false);
    }

    private ElasticResponse parseResponse(final Response response) throws IOException {
        try (final InputStream inputStream = response.getEntity().getContent()) {
            return this.mapper.readValue(inputStream, ElasticResponse.class);
        }
    }

    @Override
    public ElasticResponse scroll(String scrollId, Integer scrollTime) throws IOException {
        final String path = "/_search/scroll";

        final Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("scroll_id", scrollId);
        requestBody.put("scroll", scrollTime + "s");
        return parseResponse(performRequest("POST", path, requestBody));
    }

    @Override
    public void clearScroll(Set<String> scrollIds) throws IOException {
        final String path = "/_search/scroll";
        if (!scrollIds.isEmpty()) {
            final Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("scroll_id", scrollIds);
            performRequest("DELETE", path, requestBody);
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.fine("Closing proxyClient: " + this.client);
        try {
            this.client.close();
        } finally {
            if (this.proxyClient != null) {
                LOGGER.fine("Closing proxyClient: " + this.proxyClient);
                this.proxyClient.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeMapping(
            String parent, String key, Map<String, Object> data, String currentParent) {
        Iterator<Entry<String, Object>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();
            if (Objects.equals(currentParent, parent) && entry.getKey().equals(key)) {
                it.remove();
            } else if (entry.getValue() instanceof Map) {
                removeMapping(parent, key, (Map<String, Object>) entry.getValue(), entry.getKey());
            } else if (entry.getValue() instanceof List) {
                ((List<Object>) entry.getValue())
                        .stream()
                                .filter(item -> item instanceof Map)
                                .forEach(
                                        item ->
                                                removeMapping(
                                                        parent,
                                                        key,
                                                        (Map<String, Object>) item,
                                                        currentParent));
            }
        }
    }

    private Set<String> getIndices(String alias) {
        Set<String> indices;
        try {
            final Response response = performRequest("GET", "/_alias/" + alias, null, true);
            try (final InputStream inputStream = response.getEntity().getContent()) {
                final Map<String, Object> result =
                        this.mapper.readValue(
                                inputStream, new TypeReference<Map<String, Object>>() {});
                indices = result.keySet();
            }
        } catch (IOException e) {
            indices = new HashSet<>();
        }
        return indices;
    }
}
