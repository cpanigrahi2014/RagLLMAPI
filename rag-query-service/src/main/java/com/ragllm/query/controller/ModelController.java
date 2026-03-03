package com.ragllm.query.controller;

import com.ragllm.common.dto.ApiResponse;
import com.ragllm.query.service.ClaudeClient;
import com.ragllm.query.service.GeminiClient;
import com.ragllm.query.service.OpenAIClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes available AI models so the frontend can offer selection.
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final GeminiClient geminiClient;
    private final ClaudeClient claudeClient;

    public ModelController(GeminiClient geminiClient, ClaudeClient claudeClient) {
        this.geminiClient = geminiClient;
        this.claudeClient = claudeClient;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableModels() {

        // --- OpenAI Chat Models ---
        List<Map<String, Object>> chatModels = new ArrayList<>();
        chatModels.add(modelEntry("gpt-4.1-mini",  "GPT-4.1 Mini",  "Fast & affordable, great for most queries",                "standard",  "openai"));
        chatModels.add(modelEntry("gpt-4.1-nano",  "GPT-4.1 Nano",  "Fastest & cheapest, good for simple questions",            "economy",   "openai"));
        chatModels.add(modelEntry("gpt-4o-mini",   "GPT-4o Mini",   "Multimodal mini model, good balance of speed & quality",   "standard",  "openai"));
        chatModels.add(modelEntry("gpt-4o",        "GPT-4o",        "Multimodal flagship, best for complex reasoning & tables", "premium",   "openai"));
        chatModels.add(modelEntry("gpt-4.1",       "GPT-4.1",       "Latest flagship, best for coding & long-context tasks",    "premium",   "openai"));
        chatModels.add(modelEntry("gpt-4-turbo",   "GPT-4 Turbo",   "High intelligence with vision support",                    "premium",   "openai"));
        chatModels.add(modelEntry("o4-mini",        "o4-mini",        "Reasoning model, best for math & logic problems",          "reasoning", "openai"));
        chatModels.add(modelEntry("o3-mini",        "o3-mini",        "Efficient reasoning model for STEM tasks",                 "reasoning", "openai"));

        // --- Gemini Chat Models (always shown) ---
        chatModels.add(modelEntry("gemini-2.5-flash",     "Gemini 2.5 Flash",     "Thinking model, great for complex tasks at high speed",      "standard", "gemini"));
        chatModels.add(modelEntry("gemini-2.5-pro",       "Gemini 2.5 Pro",       "Best Gemini model for complex reasoning & coding",           "premium",  "gemini"));
        chatModels.add(modelEntry("gemini-2.0-flash",     "Gemini 2.0 Flash",     "Fast multimodal model with next-gen features",              "standard", "gemini"));
        chatModels.add(modelEntry("gemini-2.0-flash-lite","Gemini 2.0 Flash Lite","Fastest & cheapest Gemini model",                            "economy",  "gemini"));
        chatModels.add(modelEntry("gemini-1.5-pro",       "Gemini 1.5 Pro",       "2M token context, strong long-document analysis",           "premium",  "gemini"));
        chatModels.add(modelEntry("gemini-1.5-flash",     "Gemini 1.5 Flash",     "Fast & versatile Gemini model",                             "standard", "gemini"));

        // --- Claude (Anthropic) Chat Models ---
        chatModels.add(modelEntry("claude-sonnet-4-20250514",    "Claude Sonnet 4",      "Best balance of speed & intelligence, great for coding",     "standard", "anthropic"));
        chatModels.add(modelEntry("claude-opus-4-20250514",      "Claude Opus 4",        "Most powerful Claude model, best for complex analysis",       "premium",  "anthropic"));
        chatModels.add(modelEntry("claude-3-5-sonnet-20241022",  "Claude 3.5 Sonnet",    "High performance, great for detailed Q&A & summarization",   "standard", "anthropic"));
        chatModels.add(modelEntry("claude-3-5-haiku-20241022",   "Claude 3.5 Haiku",     "Fastest Claude model, ideal for quick tasks",                "economy",  "anthropic"));
        chatModels.add(modelEntry("claude-3-opus-20240229",      "Claude 3 Opus",        "Previous flagship, excellent reasoning & instruction-following", "premium",  "anthropic"));
        chatModels.add(modelEntry("claude-3-haiku-20240307",     "Claude 3 Haiku",       "Fast & compact, good for simple queries",                    "economy",  "anthropic"));

        // --- OpenAI Embedding Models ---
        List<Map<String, Object>> embeddingModels = new ArrayList<>();
        embeddingModels.add(embeddingEntry("text-embedding-3-small", "Embedding 3 Small", "1536 dimensions, fast & cost-effective", 1536, "openai"));
        embeddingModels.add(embeddingEntry("text-embedding-3-large", "Embedding 3 Large", "3072 dimensions, highest accuracy",      3072, "openai"));
        embeddingModels.add(embeddingEntry("text-embedding-ada-002",  "Ada 002 (Legacy)",  "1536 dimensions, legacy model",          1536, "openai"));

        // --- Gemini Embedding Models (always shown) ---
        embeddingModels.add(embeddingEntry("gemini-embedding-001", "Gemini Embedding 001", "3072 dimensions, Google's latest embedding model", 3072, "gemini"));

        Map<String, Object> result = Map.of(
                "chatModels", chatModels,
                "embeddingModels", embeddingModels,
                "defaults", Map.of(
                        "chatModel", OpenAIClient.DEFAULT_CHAT_MODEL,
                        "embeddingModel", OpenAIClient.DEFAULT_EMBEDDING_MODEL
                )
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private static Map<String, Object> modelEntry(String id, String name, String description, String tier, String provider) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", description);
        m.put("tier", tier);
        m.put("provider", provider);
        return m;
    }

    private static Map<String, Object> embeddingEntry(String id, String name, String description, int dimensions, String provider) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", description);
        m.put("dimensions", dimensions);
        m.put("provider", provider);
        return m;
    }
}
