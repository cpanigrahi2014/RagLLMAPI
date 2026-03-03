package com.ragllm.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 4096, message = "Text must not exceed 4096 characters for TTS")
    private String text;

    /** Voice: alloy, echo, fable, onyx, nova, shimmer */
    @Builder.Default
    private String voice = "nova";

    /** Model: tts-1 or tts-1-hd */
    @Builder.Default
    private String model = "tts-1";

    /** Speed: 0.25 to 4.0 */
    @Builder.Default
    private Double speed = 1.0;
}
