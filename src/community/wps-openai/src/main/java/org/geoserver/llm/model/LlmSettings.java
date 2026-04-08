/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.model;

import java.io.Serializable;

/** LLM Settings */
public class LlmSettings implements Serializable {
    public static final String LLM_METADATA_KEY = "llmSettings";
    private static final String DEFAULT_CHAT_MODEL = "gpt-4.1";
    private String apiKey;
    public static final Long DEFAULT_EXPIRE = 20L;
    public static final Long DEFAULT_CACHE = 20L;
    public static final Integer DEFAULT_SAMPLE_SIZE = 3;
    public static final Integer DEFAULT_RETURN_SIZE = -1;
    private Long expireAfterAccessMinutes = DEFAULT_EXPIRE;
    private Long cacheSize = DEFAULT_CACHE;
    private Integer sampleData = DEFAULT_SAMPLE_SIZE;
    private Integer returnSize = DEFAULT_RETURN_SIZE;
    public static final String DEFAULT_LAYERS_PROMPT =
            "You are a helpful assistant. Use the provided GeoServer layers and return the layer that the input most closely matches: %(allLayers)";
    public static final String DEFAULT_ECQL_PROMPT =
            "You are a helpful assistant. The GeoServer layer is %(layerName) and the layer fields are: %(fields) and here is some sample data from that layer: %(sampleData). Use this information to generate GeoServer ecql from the provided question: ";
    private String layersPrompt = DEFAULT_LAYERS_PROMPT;
    private String ecqlPrompt = DEFAULT_ECQL_PROMPT;
    private String chatModel = DEFAULT_CHAT_MODEL;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getExpireAfterAccessMinutes() {
        return expireAfterAccessMinutes;
    }

    public void setExpireAfterAccessMinutes(Long expireAfterAccessMinutes) {
        this.expireAfterAccessMinutes = expireAfterAccessMinutes;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public String getLayersPrompt() {
        return layersPrompt;
    }

    public void setLayersPrompt(String layersPrompt) {
        this.layersPrompt = layersPrompt;
    }

    public String getEcqlPrompt() {
        return ecqlPrompt;
    }

    public void setEcqlPrompt(String ecqlPrompt) {
        this.ecqlPrompt = ecqlPrompt;
    }

    public Integer getSampleData() {
        return sampleData;
    }

    public void setSampleData(Integer sampleData) {
        this.sampleData = sampleData;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public Integer getReturnSize() {
        return returnSize;
    }

    public void setReturnSize(Integer returnSize) {
        this.returnSize = returnSize;
    }
}
