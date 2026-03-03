package com.ragllm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUploadResponse {
    private String bookId;
    private String name;
    private String status;
    private String message;
}
