/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletionMessage;
import com.openai.services.blocking.ChatService;
import com.openai.services.blocking.chat.ChatCompletionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.llm.model.LlmSettings;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wps.WPSException;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;

public class OpenAIProcessTest extends GeoServerSystemTestSupport {

    @Test
    public void testExecuteThrowsWhenApiKeyNotSet() {
        OpenAIProcess process = new OpenAIProcess();
        process.setGeoServer(getGeoServer());

        Exception ex =
                assertThrows(WPSException.class, () -> process.execute("What is the population?", null, null, null));

        assertTrue(ex.getMessage().contains("OpenAI API key is not set"));
    }

    @Test
    public void testConvertFeatureCollection() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        FeatureCollection<? extends FeatureType, ? extends Feature> fc =
                fti.getFeatureSource(null, null).getFeatures();
        String geoJSON = OpenAIProcess.convertToGeoJSON((SimpleFeatureCollection) fc);
        assertTrue(geoJSON.contains("\"type\":\"FeatureCollection\""));
    }

    @Test
    public void testGetGeoJSONFromECQL() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        String cql = "ADDRESS = '123 Main Street'";
        String geoJSON = OpenAIProcess.getGeoJSONFromECQL(cql, fti, -1);
        assertTrue(geoJSON.contains("\"type\":\"Feature\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetLayersFromUserQuestionReturnsExpectedLayers() {
        // Arrange
        String question = "Show me the roads layer";
        String allLayers = "roads, buildings, rivers";
        OpenAIProcess.Layer expectedLayer = new OpenAIProcess.Layer();
        expectedLayer.setName("roads");
        StructuredChatCompletionMessage<OpenAIProcess.Layer> mockMessage = mock(StructuredChatCompletionMessage.class);
        when(mockMessage.content()).thenReturn(Optional.of(expectedLayer));

        StructuredChatCompletion.Choice<OpenAIProcess.Layer> mockChoice = mock(StructuredChatCompletion.Choice.class);
        when(mockChoice.message()).thenReturn(mockMessage);

        StructuredChatCompletion<OpenAIProcess.Layer> mockCompletion = mock(StructuredChatCompletion.class);
        when(mockCompletion.choices()).thenReturn(Collections.singletonList(mockChoice));

        OpenAIClient mockClient = mock(OpenAIClient.class);
        ChatCompletionService mockChatCompletionService;
        ChatService mockChatService = mock(ChatService.class);
        mockChatCompletionService = mock(ChatCompletionService.class);

        when(mockClient.chat()).thenReturn(mockChatService);
        when(mockChatService.completions()).thenReturn(mockChatCompletionService);

        when(mockChatCompletionService.create((StructuredChatCompletionCreateParams<OpenAIProcess.Layer>) any()))
                .thenReturn(mockCompletion);
        LlmSettings llmSettings = new LlmSettings();

        // Act
        List<OpenAIProcess.Layer> result =
                OpenAIProcess.getLayersFromUserQuestion(question, allLayers, mockClient, null, llmSettings);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("roads", result.get(0).getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCQLSFromUserQuestionReturnsExpectedCql() {
        String question = "Find roads where type = highway";
        String layerName = "roads";
        String fields = "id, name, type";
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        OpenAIProcess.Cql expectedCql = new OpenAIProcess.Cql();
        expectedCql.setEcql("type = 'highway'");
        StructuredChatCompletionMessage<OpenAIProcess.Cql> mockMessage = mock(StructuredChatCompletionMessage.class);
        when(mockMessage.content()).thenReturn(Optional.of(expectedCql));

        StructuredChatCompletion.Choice<OpenAIProcess.Cql> mockChoice = mock(StructuredChatCompletion.Choice.class);
        when(mockChoice.message()).thenReturn(mockMessage);

        StructuredChatCompletion<OpenAIProcess.Cql> mockCompletion = mock(StructuredChatCompletion.class);
        when(mockCompletion.choices()).thenReturn(Collections.singletonList(mockChoice));

        OpenAIClient mockClient = mock(OpenAIClient.class);
        ChatCompletionService mockChatCompletionService;
        ChatService mockChatService = mock(ChatService.class);
        mockChatCompletionService = mock(ChatCompletionService.class);

        when(mockClient.chat()).thenReturn(mockChatService);
        when(mockChatService.completions()).thenReturn(mockChatCompletionService);

        when(mockChatCompletionService.create((StructuredChatCompletionCreateParams<OpenAIProcess.Cql>) any()))
                .thenReturn(mockCompletion);
        LlmSettings llmSettings = new LlmSettings();

        // Act
        List<OpenAIProcess.Cql> result = OpenAIProcess.getCQLSFromUserQuestion(
                question, layerName, fields, messages, mockClient, llmSettings, "");

        // Assert result
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("type = 'highway'", result.get(0).getEcql());
    }
}
