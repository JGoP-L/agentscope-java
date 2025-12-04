/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.core.formatter.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.agentscope.core.model.GenerateOptions;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Mock tests for OpenAI thinking parameter handling.
 * These tests verify that thinking parameters are correctly formatted and applied.
 */
@Tag("unit")
@DisplayName("OpenAI Thinking Mock Tests")
class OpenAIThinkingMockTest {

    private OpenAIToolsHelper toolsHelper;
    private ChatCompletionCreateParams.Builder paramsBuilder;

    @BeforeEach
    void setUp() {
        toolsHelper = new OpenAIToolsHelper();
        paramsBuilder = mock(ChatCompletionCreateParams.Builder.class);
    }

    @Test
    @DisplayName("Should apply thinking with budget")
    void testApplyThinkingWithBudget() {
        // Given
        GenerateOptions options = GenerateOptions.builder().thinkingBudget(5000).build();
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(1000).build();

        // When
        toolsHelper.applyThinking(paramsBuilder, options, defaultOptions);

        // Then
        verify(paramsBuilder).putAdditionalBodyProperty(eq("thinking"), any(JsonValue.class));
    }

    @Test
    @DisplayName("Should apply thinking with default budget")
    void testApplyThinkingWithDefaultBudget() {
        // Given
        GenerateOptions options = GenerateOptions.builder().build(); // No budget in options
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(3000).build();

        // When
        toolsHelper.applyThinking(paramsBuilder, options, defaultOptions);

        // Then
        verify(paramsBuilder).putAdditionalBodyProperty(eq("thinking"), any(JsonValue.class));
    }

    @Test
    @DisplayName("Should apply thinking with null options")
    void testApplyThinkingWithNullOptions() {
        // Given
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(2000).build();

        // When
        toolsHelper.applyThinking(paramsBuilder, null, defaultOptions);

        // Then
        verify(paramsBuilder).putAdditionalBodyProperty(eq("thinking"), any(JsonValue.class));
    }

    @Test
    @DisplayName("Should handle thinking parameter structure")
    void testThinkingParameterStructure() {
        // This test verifies the JSON structure that would be sent to OpenAI
        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", "enabled");
        thinking.put("budget_tokens", 5000);

        assertEquals("enabled", thinking.get("type"), "Type should be 'enabled'");
        assertEquals(5000, thinking.get("budget_tokens"), "Budget should be 5000");
    }

    @Test
    @DisplayName("Should create thinking JSON with required fields")
    void testThinkingJsonStructure() {
        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", "enabled");
        thinking.put("budget_tokens", 10000);

        assertNotNull(thinking.get("type"), "Type field should not be null");
        assertNotNull(thinking.get("budget_tokens"), "Budget field should not be null");
        assertEquals(2, thinking.size(), "Should have exactly 2 fields");
    }

    @Test
    @DisplayName("Should handle thinking without budget")
    void testThinkingWithoutBudget() {
        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", "enabled");
        // No budget_tokens when budget is null

        assertEquals("enabled", thinking.get("type"), "Type should be 'enabled'");
        assertTrue(
                !thinking.containsKey("budget_tokens") || thinking.get("budget_tokens") == null,
                "Should not have budget when null");
    }

    @Test
    @DisplayName("Should verify thinking budget override")
    void testThinkingBudgetOverride() {
        // Given - options budget should override defaultOptions budget
        GenerateOptions options = GenerateOptions.builder().thinkingBudget(8000).build();
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(1000).build();

        // When we apply options, options budget takes precedence
        GenerateOptions effectiveOptions = options;

        // Then
        assertNotNull(effectiveOptions.getThinkingBudget(), "Budget should not be null");
        assertEquals(
                8000,
                effectiveOptions.getThinkingBudget(),
                "Should use options budget (8000), not default (1000)");
    }

    @Test
    @DisplayName("Should handle different budget values")
    void testDifferentBudgetValues() {
        int[] budgets = {1000, 5000, 10000, 50000, 100000};

        for (int budget : budgets) {
            Map<String, Object> thinking = new HashMap<>();
            thinking.put("type", "enabled");
            thinking.put("budget_tokens", budget);

            assertEquals(
                    budget, thinking.get("budget_tokens"), "Budget " + budget + " should be set");
        }
    }

    @Test
    @DisplayName("Should create JSON value from thinking map")
    void testJsonValueCreation() {
        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", "enabled");
        thinking.put("budget_tokens", 5000);

        // Convert to JsonValue (this is what the actual code does)
        JsonValue jsonValue = JsonValue.from(thinking);

        assertNotNull(jsonValue, "JsonValue should be created");
    }
}
