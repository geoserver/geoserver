/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.web;

import com.openai.models.ChatModel;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.llm.model.LlmSettings;
import org.geoserver.web.util.MetadataMapModel;

/** WPS-OpenAI Settings Panel */
public class LlmSettingsPanel extends Panel {
    public LlmSettingsPanel(String id, IModel<SettingsInfo> settingsInfoIModel) {
        super(id, settingsInfoIModel);
        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(settingsInfoIModel, "metadata");

        MetadataMap metadataMap = metadata.getObject();

        // Initialize LLM settings if missing
        if (metadataMap != null && !metadataMap.containsKey(LlmSettings.LLM_METADATA_KEY)) {
            LlmSettings llmSettings = new LlmSettings();
            metadataMap.put(LlmSettings.LLM_METADATA_KEY, llmSettings);
        }
        MetadataMapModel<Object> metadataModel =
                new MetadataMapModel<>(metadata, LlmSettings.LLM_METADATA_KEY, LlmSettings.class);

        IModel<String> apiKeyModel = new PropertyModel<>(metadataModel, "apiKey");

        IModel<String> encryptedApiKeyModel = new IModel<>() {
            @Override
            public String getObject() {
                String encrypted = apiKeyModel.getObject();
                try {
                    return CryptUtil.decrypt(encrypted);
                } catch (Exception e) {
                    return "[DECRYPTION FAILED]";
                }
            }

            @Override
            public void setObject(String plainTextKey) {
                try {
                    String encrypted = CryptUtil.encrypt(plainTextKey);
                    apiKeyModel.setObject(encrypted);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encrypt API key", e);
                }
            }

            @Override
            public void detach() {
                apiKeyModel.detach();
            }
        };

        TextField<String> apiKey = new TextField<>("apiKey", encryptedApiKeyModel);
        add(apiKey);
        IModel<Long> expireModel = new PropertyModel<>(metadataModel, "expireAfterAccessMinutes");
        TextField<Long> expire = new TextField<>("expireAfterAccessMinutes", expireModel);
        add(expire);
        IModel<Long> returnSizeModel = new PropertyModel<>(metadataModel, "returnSize");
        TextField<Long> returnSize = new TextField<>("returnSize", returnSizeModel);
        add(returnSize);
        IModel<Long> sizeModel = new PropertyModel<>(metadataModel, "cacheSize");
        TextField<Long> cacheSize = new TextField<>("cacheSize", sizeModel);
        add(cacheSize);
        IModel<Long> sampleModel = new PropertyModel<>(metadataModel, "sampleData");
        TextField<Long> sampleData = new TextField<>("sampleData", sampleModel);
        add(sampleData);
        IModel<String> chatModelModel = new PropertyModel<>(metadataModel, "chatModel");
        List<String> modelStrings = new ArrayList<>();
        for (Field f : ChatModel.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == ChatModel.class) {
                try {
                    ChatModel m = (ChatModel) f.get(null);
                    modelStrings.add(m.asString());
                } catch (Exception ignored) {
                }
            }
        }
        Collections.sort(modelStrings);
        final DropDownChoice<String> chatModel = new DropDownChoice<>("chatModel", chatModelModel, modelStrings);
        add(chatModel);
        IModel<String> ecqlModel = new PropertyModel<>(metadataModel, "ecqlPrompt");
        TextField<String> ecqlPrompt = new TextField<>("ecqlPrompt", ecqlModel);
        add(ecqlPrompt);
        IModel<String> layersModel = new PropertyModel<>(metadataModel, "layersPrompt");
        TextField<String> layersPrompt = new TextField<>("layersPrompt", layersModel);
        add(layersPrompt);
    }
}
