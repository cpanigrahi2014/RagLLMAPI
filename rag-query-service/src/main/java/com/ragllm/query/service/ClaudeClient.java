package com.ragllm.query.service;

import com.ragllm.common.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for Anthropic Claude Messages API.
 *
 * Endpoint: POST https://api.anthropic.com/v1/messages
 * Auth header: x-api-key
 * API version header: anthropic-version: 2023-06-01
 */
@Service
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    public static final String DEFAULT_CLAUDE_CHAT_MODEL = "claude-sonnet-4-20250514";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public static final Set<String> ALLOWED_CHAT_MODELS = Set.of(
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-haiku-20240307"
    );

    private final WebClient webClient;
    private final boolean available;

    public ClaudeClient(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.available = apiKey != null && !apiKey.isBlank();

        if (!available) {
            log.warn("No Anthropic API key configured — Claude models will not be available");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey != null ? apiKey : "")
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Call Claude Messages API with system prompt and user message.
     * Returns an OpenAIClient.ChatCompletionResult for compatibility with existing routing.
     */
    @SuppressWarnings("unchecked")
    public OpenAIClient.ChatCompletionResult chatCompletion(String systemPrompt, String userMessage, String model) {
        if (!available) {
            throw new DocumentProcessingException("Anthropic API key not configured");
        }

        String chatModel = resolveChatModel(model);
        log.info("Using Claude chat model: {}", chatModel);

        // Build Claude Messages API request
        Map<String, Object> request = Map.of(
                "model", chatModel,
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.3
        );

        long start = System.currentTimeMillis();

        Map<String, Object> response = webClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5)))
                .block(Duration.ofSeconds(90));

        long elapsed = System.currentTimeMillis() - start;

        if (response == null) {
            throw new DocumentProcessingException("Empty response from Claude API");
        }

        // Extract content — Claude returns: { content: [ { type: "text", text: "..." } ] }
        List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            throw new DocumentProcessingException("No content in Claude response");
        }

        StringBuilder textContent = new StringBuilder();
        for (Map<String, Object> block : contentBlocks) {
            if ("text".equals(block.get("type"))) {
                textContent.append(block.get("text"));
            }
        }
        String content = textContent.toString();

        // Extract token usage — Claude returns: { usage: { input_tokens, output_tokens } }
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        int inputTokens = usage != null && usage.containsKey("input_tokens")
                ? ((Number) usage.get("input_tokens")).intValue() : 0;
        int outputTokens = usage != null && usage.containsKey("output_tokens")
                ? ((Number) usage.get("output_tokens")).intValue() : 0;
        int totalTokens = inputTokens + outputTokens;

        log.debug("Claude chat completion: {} tokens in {}ms (model={})", totalTokens, elapsed, chatModel);

        return new OpenAIClient.ChatCompletionResult(content, inputTokens, outputTokens, totalTokens, elapsed, chatModel);
    }

    public String resolveChatModel(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_CLAUDE_CHAT_MODEL;
        if (ALLOWED_CHAT_MODELS.contains(requested)) return requested;
        log.warn("Requested Claude model '{}' is not allowed, falling back to '{}'", requested, DEFAULT_CLAUDE_CHAT_MODEL);
        return DEFAULT_CLAUDE_CHAT_MODEL;
    }

    /**
     * Check if a model name is a Claude model.
     */
    public static boolean isClaudeModel(String model) {
        if (model == null) return false;
        return model.startsWith("claude-");
    }
}
