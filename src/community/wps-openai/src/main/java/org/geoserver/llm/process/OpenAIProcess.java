/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.llm.cache.LlmCacheManager;
import org.geoserver.llm.model.LlmSettings;
import org.geoserver.llm.web.CryptUtil;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;

/** WPS Process to call OpenAI API with Catalog for Context */
@DescribeProcess(
        title = "OpenAI Process",
        description = "Asks OpenAI to generate CQL based on the query and the GeoServer Catalog.")
public class OpenAIProcess implements GeoServerProcess, DisposableBean {
    private GeoServer geoServer;
    private LlmCacheManager cacheManager;

    @DescribeResult(
            name = "result",
            description = "OpenAI query result.",
            meta = {"mimeTypes=application/json"},
            type = String.class)
    public String execute(
            @DescribeParameter(name = "question", description = "question to ask OpenAI") String question,
            @DescribeParameter(
                            name = "session_id",
                            description = "session id to support LLM context memory across multiple questions",
                            min = 0)
                    String sessionId,
            @DescribeParameter(
                            name = "return_data",
                            description = "if true, return GeoJSON otherwise just return ECQL",
                            min = 0)
                    Boolean returnData,
            ProgressListener monitor) {
        // null safety for the progress listener
        if (monitor == null) {
            monitor = new NullProgressListener();
        }
        monitor.started();
        SettingsInfo info = geoServer.getSettings();
        MetadataMap metadata = info.getMetadata();
        LlmSettings llmSettings = metadata.get(LlmSettings.LLM_METADATA_KEY, LlmSettings.class);
        String apiKey = null;
        String answer = null;
        Catalog catalog = null;
        if (llmSettings != null) {
            String rawKey = llmSettings.getApiKey();
            if (rawKey != null && !rawKey.isEmpty()) {
                try {
                    Session session = new Session();
                    apiKey = CryptUtil.decrypt(rawKey);
                    OpenAIClient client =
                            OpenAIOkHttpClient.builder().apiKey(apiKey).build();
                    ObjectMapper mapper = new ObjectMapper();
                    List<Cql> cqls = new ArrayList<>();
                    catalog = geoServer.getCatalog();
                    // Get list of all available GeoServer layers and convert to comma-separated string
                    String allLayers = catalog.getLayers().stream()
                            .map(PublishedInfo::prefixedName)
                            .collect(Collectors.joining(", "));
                    if (sessionId == null) { // new session, so start from beginning
                        sessionId = UUID.randomUUID().toString();
                        session.setSessionId(sessionId);

                        List<Layer> layers = getLayersFromUserQuestion(question, allLayers, client, null, llmSettings);
                        getCqls(question, session, client, cqls, catalog, layers, null, llmSettings, returnData);
                    } else { // existing session cache, let's pull the memory
                        List<ChatCompletionMessageParam> memory =
                                cacheManager.getCache().getIfPresent(sessionId);
                        assert memory != null;
                        session.setSessionId(sessionId);
                        ChatCompletionMessageParam userMessage =
                                ChatCompletionMessageParam.ofUser(new ChatCompletionUserMessageParam.Builder()
                                        .content(question)
                                        .build());
                        memory.add(userMessage);
                        List<Layer> layers =
                                getLayersFromUserQuestion(question, allLayers, client, memory, llmSettings);
                        getCqls(question, session, client, cqls, catalog, layers, memory, llmSettings, returnData);
                    }

                    answer = mapper.writeValueAsString(session);
                } catch (Exception e) {
                    monitor.complete();
                    throw new WPSException("Failed to decrypt OpenAI API key: " + e.getMessage(), e);
                }
            }
        }
        if (apiKey == null || apiKey.isEmpty()) {
            monitor.complete();
            throw new WPSException("OpenAI API key is not set in the settings.");
        }
        monitor.complete();
        return answer;
    }

    private void getCqls(
            String question,
            Session session,
            OpenAIClient client,
            List<Cql> cqls,
            Catalog catalog,
            List<Layer> layers,
            List<ChatCompletionMessageParam> memory,
            LlmSettings llmSettings,
            Boolean returnData)
            throws CQLException, IOException {
        if (!layers.isEmpty()) {
            String layerName = layers.get(0).getName();
            // get the fields of the layer
            LayerInfo layerInfo = catalog.getLayerByName(layerName);
            ResourceInfo resourceInfo = layerInfo.getResource();
            if (resourceInfo instanceof FeatureTypeInfo featureTypeInfo) {
                String fields = featureTypeInfo.getAttributes().stream()
                        .map(AttributeTypeInfo::getName)
                        .collect(Collectors.joining(", "));

                SimpleFeatureCollection sampleFeatures =
                        getSampleFeatures(featureTypeInfo, llmSettings.getSampleData());
                String sampleFeaturesJson = convertToGeoJSON(sampleFeatures);

                if (memory == null) {
                    memory = new ArrayList<>();
                }
                cqls = getCQLSFromUserQuestion(
                        question, layerName, fields, memory, client, llmSettings, sampleFeaturesJson);
                if (!cqls.isEmpty()) {
                    cqls.get(0).layerName = layerName;
                }
                session.setCqls(cqls);
                if (Boolean.TRUE.equals(returnData)) {
                    populateJSON(session, memory, featureTypeInfo, llmSettings.getReturnSize());
                }
                // add the memory to the cache
                cacheManager.getCache().put(session.getSessionId(), memory);
            }
        }
    }

    private void populateJSON(
            Session session,
            List<ChatCompletionMessageParam> messages,
            FeatureTypeInfo featureTypeInfo,
            Integer maxFeatures)
            throws CQLException, IOException {
        String firstECQL = session.getCqls().get(0).getEcql();
        // add assistant response to memory
        ChatCompletionMessageParam assistantMessage =
                ChatCompletionMessageParam.ofAssistant(new ChatCompletionAssistantMessageParam.Builder()
                        .content(firstECQL)
                        .build());
        messages.add(assistantMessage);
        session.setGeoJSON(getGeoJSONFromECQL(firstECQL, featureTypeInfo, maxFeatures));
    }

    @SuppressWarnings("unchecked")
    protected static String getGeoJSONFromECQL(String firstECQL, FeatureTypeInfo featureTypeInfo, Integer maxFeatures)
            throws CQLException, IOException {
        String geoJSON = null;
        try {
            Filter filter = ECQL.toFilter(firstECQL);
            FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                    (FeatureSource<SimpleFeatureType, SimpleFeature>) featureTypeInfo.getFeatureSource(null, null);
            // apply the filter through a query
            Query query = new Query(fs.getSchema().getTypeName(), filter);
            if (maxFeatures == null || maxFeatures < 0) {
                maxFeatures = Query.DEFAULT_MAX;
            }
            query.setMaxFeatures(maxFeatures);

            SimpleFeatureCollection filtered = (SimpleFeatureCollection) fs.getFeatures(query);
            geoJSON = convertToGeoJSON(filtered);
            if (geoJSON.contains("\"features\":[]")) {
                geoJSON = "";
            }
        } catch (Exception e) {
            geoJSON = "";
        }
        return geoJSON;
    }

    @NotNull
    protected static List<Cql> getCQLSFromUserQuestion(
            String question,
            String layerName,
            String fields,
            List<ChatCompletionMessageParam> messages,
            OpenAIClient client,
            LlmSettings llmSettings,
            String sampleData) {
        List<Cql> cqls;
        String ecqlPrompt = getCQLPrompt(llmSettings.getEcqlPrompt(), layerName, fields, sampleData);
        ChatCompletionMessageParam systemMessage =
                ChatCompletionMessageParam.ofSystem(new ChatCompletionSystemMessageParam.Builder()
                        .content(ecqlPrompt)
                        .build());
        messages.add(systemMessage);

        ChatCompletionMessageParam userMessage = ChatCompletionMessageParam.ofUser(
                new ChatCompletionUserMessageParam.Builder().content(question).build());
        messages.add(userMessage);

        StructuredChatCompletionCreateParams<Cql> params2 = ChatCompletionCreateParams.builder()
                .messages(messages)
                .model(ChatModel.of(llmSettings.getChatModel()))
                .responseFormat(Cql.class)
                .build();
        List<StructuredChatCompletion.Choice<Cql>> choices =
                client.chat().completions().create(params2).choices();
        cqls = choices.stream()
                .flatMap(choice -> choice.message().content().stream())
                .collect(Collectors.toList());
        return cqls;
    }

    private static String getCQLPrompt(String ecqlPrompt, String layerName, String fields, String sampleData) {
        Map<String, String> values = new HashMap<>();
        values.put("layerName", layerName);
        values.put("fields", fields);
        values.put("sampleData", sampleData);

        StringSubstitutor sub = new StringSubstitutor(values, "%(", ")");
        return sub.replace(ecqlPrompt);
    }

    private static String getAllLayersPrompt(String layersPrompt, String allLayers) {
        Map<String, String> values = new HashMap<>();
        values.put("allLayers", allLayers);

        StringSubstitutor sub = new StringSubstitutor(values, "%(", ")");
        return sub.replace(layersPrompt);
    }

    @NotNull
    protected static List<Layer> getLayersFromUserQuestion(
            String question,
            String allLayers,
            OpenAIClient client,
            List<ChatCompletionMessageParam> memory,
            LlmSettings llmSettings) {
        if (memory == null) {
            memory = new ArrayList<>();
        }
        String layersPrompt = getAllLayersPrompt(llmSettings.getLayersPrompt(), allLayers);
        ChatCompletionMessageParam layerSystemMessage =
                ChatCompletionMessageParam.ofSystem(new ChatCompletionSystemMessageParam.Builder()
                        .content(layersPrompt)
                        .build());
        memory.add(layerSystemMessage);

        ChatCompletionMessageParam layerUserMessage = ChatCompletionMessageParam.ofUser(
                new ChatCompletionUserMessageParam.Builder().content(question).build());
        memory.add(layerUserMessage);

        StructuredChatCompletionCreateParams<Layer> params = ChatCompletionCreateParams.builder()
                .messages(memory)
                .model(ChatModel.of(llmSettings.getChatModel()))
                .responseFormat(Layer.class)
                .build();
        return client.chat().completions().create(params).choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .collect(Collectors.toList());
    }

    /**
     * Convert FeatureCollection into GeoJSON
     *
     * @param featureCollection featureCollection to convert
     * @return GeoJSON representation
     * @throws IOException Exception When Converting FeatureCollection
     */
    public static String convertToGeoJSON(SimpleFeatureCollection featureCollection) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        featureJSON.writeFeatureCollection(featureCollection, outputStream);
        return outputStream.toString();
    }

    String buildWfsLink(String typeName) {
        HttpServletRequest req = GeoServerApplication.get().servletRequest();
        String baseUrl = ResponseUtils.baseURL(req);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "WFS");
        params.put("version", "1.0.0");
        params.put("request", "GetFeature");
        params.put("typeName", typeName);
        params.put("outputFormat", "application/json");

        return ResponseUtils.buildURL(baseUrl, "ows", params, URLMangler.URLType.SERVICE);
    }

    @SuppressWarnings("unchecked")
    private SimpleFeatureCollection getSampleFeatures(FeatureTypeInfo layerInfo, int count) throws IOException {
        if (layerInfo.getFeatureType() instanceof SimpleFeatureType) {
            FeatureSource<?, ?> fs = layerInfo.getFeatureSource(null, null);
            Query q = new Query();
            q.setMaxFeatures(count);
            FeatureCollection<?, ?> features = fs.getFeatures(q);
            DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, null);
            try (FeatureIterator<?> fi = features.features()) {
                featureCollection.add((SimpleFeature) fi.next());
            }
            return featureCollection;
        } else {
            return null;
        }
    }

    protected static class Layer {
        public String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class Cql {
        public String ecql;
        public String layerName;

        public String getEcql() {
            return ecql;
        }

        public String getLayerName() {
            return layerName;
        }

        public void setEcql(String ecql) {
            this.ecql = ecql;
        }

        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }
    }

    static class Session {
        private String sessionId;
        private List<Cql> cqls;
        private String geoJSON;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public List<Cql> getCqls() {
            return cqls;
        }

        public void setCqls(List<Cql> cqls) {
            this.cqls = cqls;
        }

        public String getGeoJSON() {
            return geoJSON;
        }

        public void setGeoJSON(String geoJSON) {
            this.geoJSON = geoJSON;
        }
    }

    @Override
    public void destroy() throws Exception {}

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void setCacheManager(LlmCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
