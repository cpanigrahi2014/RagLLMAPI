package com.ragllm.embedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ragllm.embedding", "com.ragllm.common"})
@EntityScan(basePackages = {"com.ragllm.common.entity"})
@EnableAsync
public class EmbeddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddingServiceApplication.class, args);
    }
}
