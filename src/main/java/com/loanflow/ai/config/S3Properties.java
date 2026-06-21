package com.loanflow.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loanflow.s3")
public record S3Properties(
        String bucketName
) {
}
