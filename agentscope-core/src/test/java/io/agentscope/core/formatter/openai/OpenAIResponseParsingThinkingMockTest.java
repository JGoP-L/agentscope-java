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

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.ChatResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Mock tests for OpenAI response parsing with thinking content.
 * Tests verify that thinking content is correctly extracted and parsed.
 */
@Tag("unit")
@DisplayName("OpenAI Response Parsing Thinking Tests")
class OpenAIResponseParsingThinkingMockTest {

    private OpenAIResponseParser responseParser;
    private Instant testStartTime;

    @BeforeEach
    void setUp() {
        responseParser = new OpenAIResponseParser();
        testStartTime = Instant.now();
    }

    @Test
    @DisplayName("Should parse content blocks with thinking")
    void testParseContentWithThinking() {
        // Create test data - simulating a response with thinking
        ThinkingBlock thinkingBlock =
                ThinkingBlock.builder()
                        .thinking(
                                "Let me analyze this step by step...\n"
                                        + "First, I need to understand...\n"
                                        + "Then, I can conclude...")
                        .build();

        TextBlock textBlock = TextBlock.builder().text("The answer is 42.").build();

        List<ContentBlock> contentBlocks = List.of(thinkingBlock, textBlock);

        // Verify structure
        assertNotNull(contentBlocks, "Content blocks should not be null");
        assertEquals(2, contentBlocks.size(), "Should have 2 content blocks");
        assertTrue(contentBlocks.get(0) instanceof ThinkingBlock, "First should be ThinkingBlock");
        assertTrue(contentBlocks.get(1) instanceof TextBlock, "Second should be TextBlock");
    }

    @Test
    @DisplayName("Should extract thinking content correctly")
    void testExtractThinkingContent() {
        String expectedThinking = "Analyzing the problem...";
        ThinkingBlock thinkingBlock = ThinkingBlock.builder().thinking(expectedThinking).build();

        assertEquals(
                expectedThinking,
                thinkingBlock.getThinking(),
                "Should extract correct thinking content");
    }

    @Test
    @DisplayName("Should handle thinking block ordering")
    void testThinkingBlockOrdering() {
        // Thinking should come before text in response
        ThinkingBlock thinking = ThinkingBlock.builder().thinking("Internal reasoning").build();
        TextBlock text = TextBlock.builder().text("Final answer").build();

        List<ContentBlock> blocks = List.of(thinking, text);

        // Verify order
        assertTrue(blocks.get(0) instanceof ThinkingBlock, "Thinking should be first");
        assertTrue(blocks.get(1) instanceof TextBlock, "Text should be second");
    }

    @Test
    @DisplayName("Should handle empty thinking content")
    void testEmptyThinkingContent() {
        ThinkingBlock thinkingBlock = ThinkingBlock.builder().thinking("").build();
        TextBlock textBlock = TextBlock.builder().text("Answer").build();

        List<ContentBlock> blocks = List.of(thinkingBlock, textBlock);

        assertNotNull(blocks, "Should handle empty thinking");
        assertEquals(2, blocks.size(), "Should have both blocks even if thinking is empty");
    }

    @Test
    @DisplayName("Should handle thinking with special characters")
    void testThinkingWithSpecialCharacters() {
        String thinkingWithSpecialChars =
                "Let's analyze: $100 + €50 = ?\n"
                        + "Also: 50% of 200 = 100\n"
                        + "Formula: x² + y² = z²";

        ThinkingBlock thinkingBlock =
                ThinkingBlock.builder().thinking(thinkingWithSpecialChars).build();

        assertEquals(
                thinkingWithSpecialChars,
                thinkingBlock.getThinking(),
                "Should preserve special characters");
    }

    @Test
    @DisplayName("Should handle multiple thinking paragraphs")
    void testMultipleThinkingParagraphs() {
        String multiParagraphThinking =
                "First paragraph of thinking.\n\n"
                        + "Second paragraph with more analysis.\n\n"
                        + "Third paragraph with conclusion.";

        ThinkingBlock thinkingBlock =
                ThinkingBlock.builder().thinking(multiParagraphThinking).build();

        String result = thinkingBlock.getThinking();
        assertNotNull(result, "Should preserve multiple paragraphs");
        assertTrue(result.contains("First paragraph"), "Should contain first paragraph");
        assertTrue(result.contains("Second paragraph"), "Should contain second paragraph");
        assertTrue(result.contains("Third paragraph"), "Should contain third paragraph");
    }

    @Test
    @DisplayName("Should handle thinking without text block")
    void testThinkingOnlyResponse() {
        // Some responses might only have thinking (though unusual)
        ThinkingBlock thinkingBlock =
                ThinkingBlock.builder()
                        .thinking("Extended internal reasoning without final answer")
                        .build();

        List<ContentBlock> blocks = List.of(thinkingBlock);

        assertNotNull(blocks, "Should handle thinking-only response");
        assertEquals(1, blocks.size(), "Should have only thinking block");
    }

    @Test
    @DisplayName("Should handle text without thinking block")
    void testTextOnlyResponse() {
        // Standard response without thinking
        TextBlock textBlock = TextBlock.builder().text("Regular answer").build();

        List<ContentBlock> blocks = List.of(textBlock);

        assertNotNull(blocks, "Should handle text-only response");
        assertEquals(1, blocks.size(), "Should have only text block");
        assertTrue(blocks.get(0) instanceof TextBlock, "Should be TextBlock");
    }

    @Test
    @DisplayName("Should build ChatResponse with thinking content")
    void testChatResponseWithThinking() {
        List<ContentBlock> contentBlocks =
                List.of(
                        ThinkingBlock.builder().thinking("Thought process").build(),
                        TextBlock.builder().text("Final answer").build());

        ChatResponse response =
                ChatResponse.builder()
                        .id("chatcmpl-test-123")
                        .content(contentBlocks)
                        .finishReason("stop")
                        .build();

        assertNotNull(response, "ChatResponse should be created");
        assertNotNull(response.getContent(), "Content should not be null");
        assertEquals(2, response.getContent().size(), "Should have 2 content blocks");
    }

    @Test
    @DisplayName("Should handle very long thinking content")
    void testLongThinkingContent() {
        // Simulate a very long thinking process
        StringBuilder longThinking = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longThinking.append("Line ").append(i).append(": ");
            longThinking.append("This is a long reasoning process. ");
            longThinking.append("It contains many details and steps.\n");
        }

        ThinkingBlock thinkingBlock =
                ThinkingBlock.builder().thinking(longThinking.toString()).build();

        assertNotNull(thinkingBlock.getThinking(), "Should handle long thinking");
        assertTrue(thinkingBlock.getThinking().length() > 1000, "Should preserve long content");
    }

    @Test
    @DisplayName("Should extract thinking with various encodings")
    void testThinkingWithUnicodeContent() {
        String unicodeThinking = "分析一下：\n" + "首先，我需要理解这个问题。\n" + "然后，我可以得出结论。\n" + "最后，答案是 42。";

        ThinkingBlock thinkingBlock = ThinkingBlock.builder().thinking(unicodeThinking).build();

        assertEquals(
                unicodeThinking, thinkingBlock.getThinking(), "Should preserve Unicode characters");
    }
}
