/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.llm.model.LlmSettings;

/** Wrapper around Caffeine Cache to store Chat context */
public class LlmCacheManager {
    private final Cache<String, List<ChatCompletionMessageParam>> cache;

    /**
     * Cache constructor
     *
     * @param geoServer source of configuration from the ui
     */
    public LlmCacheManager(GeoServer geoServer) {
        Long expireAfterAccessMinutes = LlmSettings.DEFAULT_EXPIRE;
        Long maximumCacheSize = LlmSettings.DEFAULT_CACHE;
        SettingsInfo info = geoServer.getSettings();
        MetadataMap metadata = info.getMetadata();
        LlmSettings llmSettings = metadata.get(LlmSettings.LLM_METADATA_KEY, LlmSettings.class);
        if (llmSettings != null) {
            if (llmSettings.getExpireAfterAccessMinutes() != null) {
                expireAfterAccessMinutes = llmSettings.getExpireAfterAccessMinutes();
            }
            if (llmSettings.getCacheSize() != null) {
                maximumCacheSize = llmSettings.getCacheSize();
            }
        }
        cache = Caffeine.newBuilder()
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .maximumSize(maximumCacheSize) // Also set a maximum size for effective eviction
                .build();
    }

    public Cache<String, List<ChatCompletionMessageParam>> getCache() {
        return cache;
    }
}
