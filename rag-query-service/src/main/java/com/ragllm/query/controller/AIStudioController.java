package com.ragllm.query.controller;

import com.ragllm.common.dto.*;
import com.ragllm.common.security.UserPrincipal;
import com.ragllm.query.service.AIStudioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/studio")
public class AIStudioController {

    private final AIStudioService aiStudioService;

    public AIStudioController(AIStudioService aiStudioService) {
        this.aiStudioService = aiStudioService;
    }

    /**
     * Execute a custom user-created prompt, optionally with document context.
     */
    @PostMapping("/prompt")
    public ResponseEntity<ApiResponse<StudioResponse>> customPrompt(
            @Valid @RequestBody CustomPromptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        StudioResponse response = aiStudioService.executeCustomPrompt(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Summarize a document (book) — brief, detailed, or bullet-points.
     */
    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<StudioResponse>> summarize(
            @Valid @RequestBody SummarizeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        StudioResponse response = aiStudioService.summarizeDocument(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Generate Q&A pairs from a document.
     */
    @PostMapping("/generate-qa")
    public ResponseEntity<ApiResponse<StudioResponse>> generateQA(
            @Valid @RequestBody GenerateQARequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        StudioResponse response = aiStudioService.generateQA(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Text-to-Speech — returns audio/mpeg binary.
     */
    @PostMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(
            @Valid @RequestBody TTSRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] audioData = aiStudioService.generateSpeech(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(audioData.length)
                .body(audioData);
    }
}
