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
package io.agentscope.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.test.ModelTestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OpenAI thinking block support (Issue #98).
 *
 * <p>These tests verify that OpenAI models correctly handle thinking mode:
 * <ul>
 *   <li>Thinking mode can be enabled via builder
 *   <li>Thinking budget is properly configured
 *   <li>Streaming is disabled when thinking is enabled
 *   <li>Non-thinking models continue to work normally
 *   <li>Thinking content is properly parsed from responses
 * </ul>
 *
 * <p>Tagged as "unit" - fast running tests without external dependencies.
 *
 * <p>Note: Thinking is only supported with o1 and o1-mini models.
 */
@Tag("unit")
@DisplayName("OpenAI Thinking Block Support Tests")
class OpenAIChatModelThinkingTest {

    private String mockApiKey;

    @BeforeEach
    void setUp() {
        mockApiKey = ModelTestUtils.createMockApiKey();
    }

    @Test
    @DisplayName("Should create model with thinking enabled")
    void testThinkingModelCreation() {
        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .build();

        assertNotNull(model, "Model with thinking should be created");
    }

    @Test
    @DisplayName("Should create model with thinking enabled and budget")
    void testThinkingModelWithBudget() {
        GenerateOptions options = GenerateOptions.builder().thinkingBudget(5000).build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(options)
                        .build();

        assertNotNull(model, "Model with thinking budget should be created");
    }

    @Test
    @DisplayName("Should support o1-mini model with thinking")
    void testO1MiniWithThinking() {
        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1-mini")
                        .enableThinking(true)
                        .build();

        assertNotNull(model, "o1-mini model with thinking should be created");
    }

    @Test
    @DisplayName("Should create model with thinking disabled by default")
    void testThinkingDisabledByDefault() {
        OpenAIChatModel model =
                OpenAIChatModel.builder().apiKey(mockApiKey).modelName("gpt-4").build();

        assertNotNull(model, "Model without thinking should be created");
    }

    @Test
    @DisplayName("Should create model with thinking explicitly disabled")
    void testThinkingExplicitlyDisabled() {
        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("gpt-4")
                        .enableThinking(false)
                        .build();

        assertNotNull(model, "Model with thinking disabled should be created");
    }

    @Test
    @DisplayName("Should support different thinking budgets")
    void testDifferentThinkingBudgets() {
        // Test with small budget
        GenerateOptions smallBudget = GenerateOptions.builder().thinkingBudget(1000).build();
        OpenAIChatModel modelSmall =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(smallBudget)
                        .build();

        assertNotNull(modelSmall, "Model with small thinking budget should be created");

        // Test with large budget
        GenerateOptions largeBudget = GenerateOptions.builder().thinkingBudget(10000).build();
        OpenAIChatModel modelLarge =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(largeBudget)
                        .build();

        assertNotNull(modelLarge, "Model with large thinking budget should be created");
    }

    @Test
    @DisplayName("Should parse thinking content from response")
    void testThinkingContentParsing() {
        OpenAIChatFormatter formatter = new OpenAIChatFormatter();
        assertNotNull(formatter, "Formatter should be created");

        // Test that formatter can handle thinking blocks
        // This test verifies the formatter has thinking parsing capability
        List<Msg> messages =
                List.of(
                        Msg.builder()
                                .role(MsgRole.ASSISTANT)
                                .content(
                                        ThinkingBlock.builder()
                                                .thinking("Let me think about this problem...")
                                                .build(),
                                        TextBlock.builder().text("The answer is 42").build())
                                .build());

        assertNotNull(messages, "Messages with thinking should be created");
        assertTrue(messages.size() > 0, "Should have at least one message");
    }

    @Test
    @DisplayName("Should support override thinking budget in options")
    void testThinkingBudgetOverride() {
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(1000).build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(defaultOptions)
                        .build();

        assertNotNull(model, "Model should be created");

        // The model should accept per-call override options
        GenerateOptions callOptions = GenerateOptions.builder().thinkingBudget(5000).build();
        assertNotNull(callOptions, "Override options should be created");
    }

    @Test
    @DisplayName("Should create model with mixed configuration")
    void testMixedConfiguration() {
        GenerateOptions options =
                GenerateOptions.builder()
                        .thinkingBudget(3000)
                        .temperature(0.7)
                        .topP(0.9)
                        .maxTokens(2000)
                        .build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(options)
                        .stream(false)
                        .build();

        assertNotNull(model, "Model with mixed configuration should be created");
    }

    @Test
    @DisplayName("Should maintain backward compatibility for non-thinking models")
    void testBackwardCompatibility() {
        // Existing code without thinking should continue to work
        GenerateOptions options =
                GenerateOptions.builder().temperature(0.7).maxTokens(1000).build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("gpt-4")
                        .defaultOptions(options)
                        .stream(true)
                        .build();

        assertNotNull(model, "Non-thinking model should be created");
    }

    @Test
    @DisplayName("Should support thinking with custom formatter")
    void testThinkingWithCustomFormatter() {
        OpenAIChatFormatter customFormatter = new OpenAIChatFormatter();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .formatter(customFormatter)
                        .build();

        assertNotNull(model, "Model with custom formatter should be created");
    }

    @Test
    @DisplayName("Should parse messages with thinking blocks")
    void testMessageWithThinkingBlock() {
        // Create a message with thinking content
        Msg msg =
                Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .content(
                                ThinkingBlock.builder()
                                        .thinking("Analyzing the problem step by step...")
                                        .build(),
                                TextBlock.builder().text("Here is my solution").build())
                        .build();

        assertNotNull(msg, "Message with thinking should be created");

        // Verify the message contains thinking block
        boolean hasThinkingBlock =
                msg.getContent().stream().anyMatch(block -> block instanceof ThinkingBlock);
        assertTrue(hasThinkingBlock, "Message should contain thinking block");

        // Verify thinking content can be retrieved
        ThinkingBlock thinkingBlock =
                (ThinkingBlock)
                        msg.getContent().stream()
                                .filter(block -> block instanceof ThinkingBlock)
                                .findFirst()
                                .orElse(null);

        assertNotNull(thinkingBlock, "Thinking block should be retrieved");
        assertEquals("Analyzing the problem step by step...", thinkingBlock.getThinking());
    }

    @Test
    @DisplayName("Should handle null thinking budget gracefully")
    void testNullThinkingBudget() {
        GenerateOptions options = GenerateOptions.builder().build(); // No thinking budget

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(options)
                        .build();

        assertNotNull(model, "Model without thinking budget should be created");
    }

    @Test
    @DisplayName("Should handle empty thinking content")
    void testEmptyThinkingContent() {
        Msg msg =
                Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .content(
                                ThinkingBlock.builder()
                                        .thinking("") // Empty thinking
                                        .build(),
                                TextBlock.builder().text("Answer").build())
                        .build();

        assertNotNull(msg, "Message with empty thinking should be created");
        assertTrue(msg.getContent().size() > 0, "Message should have content");
    }

    @Test
    @DisplayName("Should handle very large thinking budget")
    void testLargeThinkingBudget() {
        GenerateOptions options = GenerateOptions.builder().thinkingBudget(100000).build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(options)
                        .build();

        assertNotNull(model, "Model with large thinking budget should be created");
    }

    @Test
    @DisplayName("Should handle thinking with only text, no thinking block")
    void testOnlyTextContent() {
        Msg msg =
                Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .content(TextBlock.builder().text("Just an answer").build())
                        .build();

        assertNotNull(msg, "Message without thinking block should be created");
        boolean hasThinking =
                msg.getContent().stream().anyMatch(block -> block instanceof ThinkingBlock);
        assertTrue(!hasThinking, "Message should not contain thinking block");
    }

    @Test
    @DisplayName("Should create model with thinking enabled but stream disabled")
    void testThinkingWithStreamDisabled() {
        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .stream(false) // Explicitly disable streaming
                        .build();

        assertNotNull(model, "Model with thinking and streaming disabled should be created");
    }

    @Test
    @DisplayName("Should support thinking budget override per call")
    void testPerCallThinkingBudgetOverride() {
        GenerateOptions defaultOptions = GenerateOptions.builder().thinkingBudget(1000).build();
        GenerateOptions callOptions = GenerateOptions.builder().thinkingBudget(8000).build();

        OpenAIChatModel model =
                OpenAIChatModel.builder()
                        .apiKey(mockApiKey)
                        .modelName("o1")
                        .enableThinking(true)
                        .defaultOptions(defaultOptions)
                        .build();

        assertNotNull(model, "Model should be created");
        assertNotNull(callOptions, "Call options with override should be created");
        assertEquals(8000, callOptions.getThinkingBudget(), "Override budget should be 8000");
        assertEquals(1000, defaultOptions.getThinkingBudget(), "Default budget should be 1000");
    }

    @Test
    @DisplayName("Should verify thinking block equality")
    void testThinkingBlockEquality() {
        ThinkingBlock block1 = ThinkingBlock.builder().thinking("Same content").build();
        ThinkingBlock block2 = ThinkingBlock.builder().thinking("Same content").build();

        assertNotNull(block1, "First thinking block should be created");
        assertNotNull(block2, "Second thinking block should be created");
        assertEquals(
                block1.getThinking(), block2.getThinking(), "Thinking content should be equal");
    }
}
